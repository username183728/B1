package com.gitls.uploader

import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class GitHubException(val code: Int, message: String) : IOException(message)
class CancelledException : IOException("Dibatalkan")

class CancelToken {
    @Volatile var cancelled = false
    fun check() { if (cancelled) throw CancelledException() }
}

data class RemoteEntry(val mode: String, val sha: String)
data class WorkflowRun(val id: Long, val name: String, val status: String, val conclusion: String?, val url: String)
data class RunStep(val name: String, val status: String, val conclusion: String?)
data class RunArtifact(val id: Long, val name: String, val size: Long, val expired: Boolean, val downloadUrl: String)

/**
 * Klien GitHub minimal berbasis HttpURLConnection (tanpa dependensi tambahan).
 * Semua panggilan blocking: jalankan dari thread latar belakang.
 */
class GitHubClient(
    private val token: String,
    owner: String,
    repo: String,
    private val cancel: CancelToken,
    private val status: (String) -> Unit = {},
) {
    private val api = "https://api.github.com/repos/$owner/$repo"

    private class Resp(val code: Int, val text: String, val retryAfter: Int?, val resetEpoch: Long?)

    // ---------------------------------------------------------------- HTTP dasar

    private fun conn(url: String, method: String, withAuth: Boolean = true): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "GITLS-Uploader")
            if (withAuth) {
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

    private fun exec(method: String, path: String, parts: List<ByteArray>?): Resp {
        val url = if (path.startsWith("http")) path else api + path
        val c = conn(url, method)
        try {
            if (parts != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setFixedLengthStreamingMode(parts.sumOf { it.size })
                c.outputStream.use { o -> parts.forEach { o.write(it) } }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            return Resp(
                code, text,
                c.getHeaderField("Retry-After")?.trim()?.toIntOrNull(),
                c.getHeaderField("x-ratelimit-reset")?.trim()?.toLongOrNull(),
            )
        } finally {
            c.disconnect()
        }
    }

    private fun isRateLimited(r: Resp): Boolean =
        r.code == 429 || (r.code == 403 && (r.retryAfter != null ||
            r.text.contains("rate limit", true) || r.text.contains("abuse", true)))

    private fun rateDelay(r: Resp): Int {
        val fromReset = r.resetEpoch?.let { (it - System.currentTimeMillis() / 1000).toInt() + 1 }
        return (r.retryAfter ?: fromReset ?: 60).coerceIn(5, 180)
    }

    /** Tidur per 100 ms agar pembatalan langsung terasa. */
    fun sleep(seconds: Int) {
        var left = seconds * 10
        while (left-- > 0) { cancel.check(); Thread.sleep(100) }
    }

    private fun call(method: String, path: String, parts: List<ByteArray>? = null): JSONObject {
        var attempt = 0
        while (true) {
            cancel.check()
            attempt++
            val r = try {
                exec(method, path, parts)
            } catch (e: CancelledException) {
                throw e
            } catch (e: IOException) {
                if (attempt >= 4) throw IOException("Koneksi gagal: ${e.message ?: e.javaClass.simpleName}")
                status("Jaringan bermasalah, mengulang (${attempt}/3)…")
                sleep(2 * attempt)
                continue
            }
            when {
                r.code in 200..299 -> return if (r.text.isBlank()) JSONObject() else JSONObject(r.text)
                isRateLimited(r) && attempt < 6 -> {
                    val d = rateDelay(r)
                    status("Batas laju GitHub, menunggu ${d} dtk…")
                    sleep(d)
                }
                r.code in 500..599 && attempt < 4 -> {
                    status("GitHub error ${r.code}, mengulang…")
                    sleep(2 * attempt)
                }
                else -> throw GitHubException(r.code, explain(r.code, r.text))
            }
        }
    }

    private fun explain(code: Int, text: String): String {
        val msg = runCatching { JSONObject(text).optString("message") }.getOrDefault("").ifBlank { text.take(160) }
        return when (code) {
            401 -> "Token ditolak (401). Buat token baru lalu simpan di Pengaturan."
            403 -> "Akses ditolak (403): $msg. Token butuh izin Contents (tulis), Actions (baca), dan Workflows (tulis)."
            404 -> "Tidak ditemukan (404). Cek nama repo/branch, atau token belum diberi akses ke repo ini."
            409 -> if (msg.contains("empty", true))
                "Repo masih kosong. Buat 1 commit dulu (mis. tambah README di GitHub), lalu coba lagi."
            else "Konflik (409): $msg"
            410 -> "Artifact sudah kedaluwarsa (410). Jalankan build lagi."
            422 -> "Ditolak GitHub (422): $msg"
            else -> "HTTP $code: $msg"
        }
    }

    private fun encPath(p: String) = p.split('/').joinToString("/") { Uri.encode(it) }

    // ---------------------------------------------------------------- Git Data API

    /** Mengembalikan (default_branch, bisaPush). */
    fun repoInfo(): Pair<String, Boolean> {
        val j = call("GET", "")
        val push = j.optJSONObject("permissions")?.optBoolean("push", true) ?: true
        return j.optString("default_branch", "main") to push
    }

    fun branchHead(branch: String): String =
        call("GET", "/git/ref/heads/${encPath(branch)}").getJSONObject("object").getString("sha")

    fun commitTree(commitSha: String): String =
        call("GET", "/git/commits/$commitSha").getJSONObject("tree").getString("sha")

    /** Peta path -> (mode, sha) semua blob di tree; Boolean = daftar terpotong oleh GitHub. */
    fun remoteFiles(treeSha: String): Pair<Map<String, RemoteEntry>, Boolean> {
        val j = call("GET", "/git/trees/$treeSha?recursive=1")
        val arr = j.optJSONArray("tree") ?: JSONArray()
        val map = HashMap<String, RemoteEntry>(arr.length() * 2)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("type") == "blob") map[o.getString("path")] = RemoteEntry(o.getString("mode"), o.getString("sha"))
        }
        return map to j.optBoolean("truncated", false)
    }

    fun createBlob(data: ByteArray): String {
        val b64 = Base64.encode(data, Base64.NO_WRAP)
        val parts = listOf(
            "{\"encoding\":\"base64\",\"content\":\"".toByteArray(Charsets.UTF_8),
            b64,
            "\"}".toByteArray(Charsets.UTF_8),
        )
        return call("POST", "/git/blobs", parts).getString("sha")
    }

    fun createTree(baseTree: String, entries: JSONArray): String {
        val body = JSONObject().put("base_tree", baseTree).put("tree", entries).toString().toByteArray(Charsets.UTF_8)
        return call("POST", "/git/trees", listOf(body)).getString("sha")
    }

    fun createCommit(message: String, tree: String, parent: String): String {
        val body = JSONObject().put("message", message).put("tree", tree)
            .put("parents", JSONArray().put(parent)).toString().toByteArray(Charsets.UTF_8)
        return call("POST", "/git/commits", listOf(body)).getString("sha")
    }

    fun updateRef(branch: String, sha: String) {
        val body = JSONObject().put("sha", sha).put("force", false).toString().toByteArray(Charsets.UTF_8)
        call("PATCH", "/git/refs/heads/${encPath(branch)}", listOf(body))
    }

    // ---------------------------------------------------------------- Secrets

    fun hasSecret(name: String): Boolean = try {
        call("GET", "/actions/secrets/$name"); true
    } catch (e: GitHubException) {
        if (e.code == 404) false else throw e
    }

    /** Mengembalikan (key_id, kunci publik base64) untuk mengenkripsi secret. */
    fun secretsPublicKey(): Pair<String, String> {
        val j = call("GET", "/actions/secrets/public-key")
        return j.getString("key_id") to j.getString("key")
    }

    fun putSecret(name: String, encryptedBase64: String, keyId: String) {
        val body = JSONObject().put("encrypted_value", encryptedBase64).put("key_id", keyId)
            .toString().toByteArray(Charsets.UTF_8)
        call("PUT", "/actions/secrets/$name", listOf(body))
    }

    // ---------------------------------------------------------------- Actions

    private fun parseRun(o: JSONObject) = WorkflowRun(
        id = o.getLong("id"),
        name = o.optString("name"),
        status = o.optString("status"),
        conclusion = if (o.isNull("conclusion")) null else o.optString("conclusion"),
        url = o.optString("html_url"),
    )

    fun runsForSha(sha: String): List<WorkflowRun> {
        val arr = call("GET", "/actions/runs?head_sha=$sha&per_page=10").optJSONArray("workflow_runs") ?: return emptyList()
        return (0 until arr.length()).map { parseRun(arr.getJSONObject(it)) }
    }

    fun latestSuccessRun(branch: String): WorkflowRun? {
        val arr = call("GET", "/actions/runs?branch=${Uri.encode(branch)}&status=success&per_page=1")
            .optJSONArray("workflow_runs") ?: return null
        return if (arr.length() == 0) null else parseRun(arr.getJSONObject(0))
    }

    fun getRun(id: Long): WorkflowRun = parseRun(call("GET", "/actions/runs/$id"))

    fun steps(runId: Long): List<RunStep> {
        val jobs = call("GET", "/actions/runs/$runId/jobs?per_page=10").optJSONArray("jobs") ?: return emptyList()
        val out = ArrayList<RunStep>()
        for (i in 0 until jobs.length()) {
            val steps = jobs.getJSONObject(i).optJSONArray("steps") ?: continue
            for (k in 0 until steps.length()) {
                val s = steps.getJSONObject(k)
                out += RunStep(
                    s.optString("name"), s.optString("status"),
                    if (s.isNull("conclusion")) null else s.optString("conclusion"),
                )
            }
        }
        return out
    }

    fun artifacts(runId: Long): List<RunArtifact> {
        val arr = call("GET", "/actions/runs/$runId/artifacts?per_page=30").optJSONArray("artifacts") ?: return emptyList()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            RunArtifact(o.getLong("id"), o.optString("name"), o.optLong("size_in_bytes"), o.optBoolean("expired"), o.optString("archive_download_url"))
        }
    }

    /**
     * Mengunduh artifact (ZIP) dan langsung mengekstrak file .apk di dalamnya ke [outDir].
     * Redirect ke penyimpanan blob diikuti manual TANPA header Authorization.
     */
    fun downloadApk(a: RunArtifact, outDir: File, progress: (Long, Long) -> Unit): File {
        var url = a.downloadUrl
        var auth = true
        var hops = 0
        while (true) {
            cancel.check()
            val c = conn(url, "GET", auth)
            try {
                val code = c.responseCode
                if (code in 300..399) {
                    val loc = c.getHeaderField("Location") ?: throw GitHubException(code, "Redirect tanpa Location")
                    if (++hops > 5) throw GitHubException(code, "Terlalu banyak redirect")
                    url = URL(URL(url), loc).toString()
                    auth = false
                    continue
                }
                if (code !in 200..299) {
                    val t = c.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                    throw GitHubException(code, explain(code, t))
                }
                return extractApk(c.inputStream, a.size, outDir, progress)
            } finally {
                c.disconnect()
            }
        }
    }

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var count = 0L
        override fun read(): Int { val b = super.read(); if (b >= 0) count++; return b }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len); if (n > 0) count += n; return n
        }
    }

    private fun extractApk(input: InputStream, total: Long, outDir: File, progress: (Long, Long) -> Unit): File {
        outDir.mkdirs()
        outDir.listFiles()?.forEach { it.delete() }
        val counting = CountingInputStream(input)
        ZipInputStream(counting.buffered()).use { zin ->
            while (true) {
                cancel.check()
                val e = zin.nextEntry ?: break
                if (e.isDirectory || !e.name.endsWith(".apk", ignoreCase = true)) continue
                val out = File(outDir, File(e.name).name)
                FileOutputStream(out).use { fos ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = zin.read(buf)
                        if (n < 0) break
                        fos.write(buf, 0, n)
                        progress(counting.count, total)
                        cancel.check()
                    }
                }
                return out
            }
        }
        throw IOException("Artifact tidak berisi file .apk")
    }
}
