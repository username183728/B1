package com.gitls.uploader

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

interface PipelineUi {
    fun log(msg: String)
    fun stage(text: String)
    /** total = 0 berarti progres tidak tentu. */
    fun progress(done: Long, total: Long)
}

class Outcome(
    val apk: File?,
    val buildOk: Boolean?,
    val runUrl: String?,
    val note: String,
)

class Pipeline(
    private val ctx: Context,
    private val s: UploaderSettings,
    private val ui: PipelineUi,
    private val cancel: CancelToken,
) {
    private val gh = GitHubClient(s.token, s.owner, s.repoName, cancel) { ui.log(it) }
    private val apkDir = File(ctx.cacheDir, "updates")

    private inline fun <T> soft(default: T, block: () -> T): T =
        try { block() } catch (e: CancelledException) { throw e } catch (e: IOException) { default }

    // ------------------------------------------------------------------ upload + build

    fun uploadAndBuild(uris: List<Uri>, commitMessage: String): Outcome {
        ui.stage("Memeriksa repo…"); ui.progress(0, 0)
        val (defaultBranch, canPush) = gh.repoInfo()
        if (!canPush) throw IOException("Token tidak punya izin tulis ke ${s.repo}.")
        val branch = s.branch.ifBlank { defaultBranch }
        ui.log("Repo ${s.repo} • branch $branch")

        ui.stage("Membaca file…")
        val local = UploadSource.read(ctx, uris, s.folder, { ui.log(it) }, cancel)
        ui.log("${local.size} file dibaca dari sumber.")

        ui.stage("Membandingkan dengan repo…")
        val head = try {
            gh.branchHead(branch)
        } catch (e: GitHubException) {
            if (e.code == 404) throw IOException("Branch \"$branch\" tidak ada di ${s.repo}.") else throw e
        }
        val baseTree = gh.commitTree(head)
        var (remote, truncated) = gh.remoteFiles(baseTree)
        if (truncated) {
            ui.log("Daftar file repo terlalu besar; semua file dikirim ulang.")
            remote = emptyMap()
        }

        val changed = ArrayList<LocalFile>()
        val modeOnly = ArrayList<Pair<LocalFile, String>>() // file, sha yang sudah ada
        var same = 0
        for (f in local) {
            cancel.check()
            val r = remote[f.path]
            when {
                r == null || r.sha != f.gitSha -> changed += f
                r.mode != f.mode -> modeOnly += f to r.sha
                else -> same++
            }
        }

        val deletes = ArrayList<String>()
        if (s.mirrorDelete) {
            val prefix = UploadSource.normalizeFolder(s.folder)
            val localPaths = local.map { it.path }.toHashSet()
            val keepWorkflows = local.none { it.path.startsWith(".github/") }
            for (p in remote.keys) {
                if (p in localPaths) continue
                if (prefix.isNotEmpty() && !p.startsWith(prefix)) continue
                if (keepWorkflows && p.startsWith(".github/")) continue // jangan sengaja menghapus CI
                deletes += p
            }
        }

        ui.log("Berubah/baru: ${changed.size} • sama: $same • mode: ${modeOnly.size} • dihapus: ${deletes.size}")

        val commitSha: String
        val findTimeout: Int
        if (changed.isEmpty() && modeOnly.isEmpty() && deletes.isEmpty()) {
            ui.log("Tidak ada perubahan. Memakai build dari commit terakhir (${head.take(7)}).")
            commitSha = head
            findTimeout = 12
        } else {
            commitSha = push(branch, head, baseTree, changed, modeOnly, deletes, commitMessage)
            findTimeout = 120
        }

        if (!s.waitAndInstall) {
            ui.progress(1, 1)
            return Outcome(null, null, null, "Commit ${commitSha.take(7)} terkirim. Build berjalan di GitHub.")
        }

        val run = awaitRun(commitSha, findTimeout)
            ?: return Outcome(null, null, null,
                "Tidak ada workflow yang terpicu untuk commit ${commitSha.take(7)}. " +
                    "Pastikan repo punya .github/workflows dengan trigger push.")
        val done = watch(run)
        return fetchApk(done)
    }

    private fun push(
        branch: String, head: String, baseTree: String,
        changed: List<LocalFile>, modeOnly: List<Pair<LocalFile, String>>, deletes: List<String>,
        message: String,
    ): String {
        ui.stage("Mengirim file…")
        val total = changed.size.toLong()
        var done = 0L
        ui.progress(0, total.coerceAtLeast(1))

        val entries = ArrayList<JSONObject>()
        val textEntries = ArrayList<JSONObject>()

        for (f in changed) {
            cancel.check()
            val text = asText(f.data)
            if (text != null) {
                textEntries += JSONObject().put("path", f.path).put("mode", f.mode).put("type", "blob").put("content", text)
            } else {
                val sha = gh.createBlob(f.data)
                entries += JSONObject().put("path", f.path).put("mode", f.mode).put("type", "blob").put("sha", sha)
                done++
                ui.progress(done, total)
            }
        }
        for ((f, sha) in modeOnly) {
            entries += JSONObject().put("path", f.path).put("mode", f.mode).put("type", "blob").put("sha", sha)
        }
        for (p in deletes) {
            entries += JSONObject().put("path", p).put("mode", "100644").put("type", "blob").put("sha", JSONObject.NULL)
        }

        // File teks dikirim inline di tree (tanpa 1 request per file), dipecah per ±2 MB.
        val chunks = ArrayList<List<JSONObject>>()
        var cur = ArrayList<JSONObject>(entries)
        var curBytes = 0
        for (t in textEntries) {
            val sz = t.getString("content").length
            if (cur.isNotEmpty() && (curBytes + sz > 2_000_000 || cur.size >= 150)) {
                chunks.add(cur); cur = ArrayList(); curBytes = 0
            }
            cur.add(t); curBytes += sz
        }
        if (cur.isNotEmpty()) chunks.add(cur)

        var tree = baseTree
        for (chunk in chunks) {
            cancel.check()
            val textCount = chunk.count { it.has("content") }
            tree = gh.createTree(tree, JSONArray(chunk))
            done += textCount
            ui.progress(done, total.coerceAtLeast(1))
        }

        ui.stage("Membuat commit…")
        val sha = gh.createCommit(message, tree, head)
        try {
            gh.updateRef(branch, sha)
        } catch (e: GitHubException) {
            if (e.code == 422) throw IOException("Branch $branch berubah saat upload. Coba lagi.") else throw e
        }
        ui.log("Commit ${sha.take(7)} terkirim ke $branch.")
        return sha
    }

    private fun asText(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        for (b in bytes) if (b.toInt() == 0) return null
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            null
        }
    }

    // ------------------------------------------------------------------ ambil build terakhir

    fun fetchLatest(): Outcome {
        ui.stage("Mencari build terakhir…"); ui.progress(0, 0)
        val (defaultBranch, _) = gh.repoInfo()
        val branch = s.branch.ifBlank { defaultBranch }
        val run = gh.latestSuccessRun(branch)
            ?: return Outcome(null, null, null, "Belum ada build sukses di branch $branch.")
        ui.log("Build sukses terakhir di $branch: ${run.url}")
        return fetchApk(run)
    }

    // ------------------------------------------------------------------ pantau build

    private fun awaitRun(sha: String, findTimeoutSec: Int): WorkflowRun? {
        ui.stage("Menunggu GitHub memulai build…"); ui.progress(0, 0)
        val deadline = System.currentTimeMillis() + findTimeoutSec * 1000L
        while (true) {
            val runs = gh.runsForSha(sha)
            if (runs.isNotEmpty()) {
                if (runs.size > 1) ui.log("${runs.size} workflow terpicu; memantau \"${runs[0].name}\".")
                return runs.firstOrNull { it.status != "completed" } ?: runs[0]
            }
            if (System.currentTimeMillis() > deadline) return null
            gh.sleep(4)
        }
    }

    private fun watch(first: WorkflowRun): WorkflowRun {
        ui.log("Build: ${first.url}")
        var run = first
        var lastStep = ""
        val deadline = System.currentTimeMillis() + 45 * 60_000L
        while (true) {
            run = gh.getRun(run.id)
            if (run.status == "completed") return run

            val steps = soft(emptyList<RunStep>()) { gh.steps(run.id) }
            val finished = steps.count { it.status == "completed" }
            val current = steps.firstOrNull { it.status == "in_progress" }?.name
                ?: if (steps.isEmpty()) "Menunggu runner" else lastStep
            if (current.isNotBlank() && current != lastStep) {
                ui.log("▸ $current")
                lastStep = current
            }
            ui.stage("Build: $current")
            ui.progress(finished.toLong(), steps.size.toLong())

            if (System.currentTimeMillis() > deadline) throw IOException("Build lebih dari 45 menit; pemantauan dihentikan.")
            gh.sleep(6)
        }
    }

    // ------------------------------------------------------------------ unduh APK

    private fun fetchApk(run: WorkflowRun): Outcome {
        if (run.conclusion == "cancelled") {
            return Outcome(null, false, run.url, "Build dibatalkan (kemungkinan ada push yang lebih baru).")
        }
        val ok = run.conclusion == "success"
        var failNote = ""
        if (!ok) {
            val failed = soft(emptyList<RunStep>()) { gh.steps(run.id) }
                .filter { it.conclusion == "failure" }.joinToString { it.name }
            failNote = if (failed.isNotBlank()) "Build gagal di langkah: $failed. " else "Build gagal (${run.conclusion}). "
        }

        ui.stage("Mencari APK…"); ui.progress(0, 0)
        val kw = s.artifactKeyword
        val list = gh.artifacts(run.id)
        val pick = list.firstOrNull { !it.expired && it.name.contains(kw, ignoreCase = true) }
            ?: return Outcome(null, ok, run.url,
                failNote + "Artifact \"$kw\" tidak ditemukan (" +
                    (if (list.isEmpty()) "run ini tidak punya artifact" else list.joinToString { it.name }) + ").")

        ui.stage("Mengunduh APK…")
        ui.log("Artifact: ${pick.name} (${pick.size / 1024} KB)")
        val apk = gh.downloadApk(pick, apkDir) { d, t -> ui.progress(d, t) }
        val note = if (ok) "APK siap dipasang." else failNote + "APK debug tetap tersedia; periksa sebelum memasang."
        return Outcome(apk, ok, run.url, note)
    }
}
