package com.gitls.uploader

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Satu file yang akan dikirim ke repo. [path] relatif terhadap root repo (sudah termasuk folder tujuan). */
class LocalFile(val path: String, val mode: String, val data: ByteArray) {
    /** SHA-1 blob Git: sha1("blob <ukuran>\0" + isi). Dipakai untuk melewati file yang tidak berubah. */
    val gitSha: String by lazy {
        val md = MessageDigest.getInstance("SHA-1")
        md.update("blob ${data.size}\u0000".toByteArray(Charsets.UTF_8))
        md.update(data)
        md.digest().joinToString("") { "%02x".format(it) }
    }
}

data class SourceInfo(val name: String, val isZip: Boolean)

object UploadSource {
    const val MAX_FILE_BYTES = 50L * 1024 * 1024

    private val SKIP_DIRS = listOf("__MACOSX/", ".git/", ".gradle/", ".idea/", ".kotlin/")

    fun describe(context: Context, uris: List<Uri>): List<SourceInfo> = uris.map { uri ->
        val name = displayName(context, uri)
        SourceInfo(name, name.endsWith(".zip", ignoreCase = true))
    }

    fun displayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank()) return n
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }

    /**
     * Membaca semua sumber menjadi daftar [LocalFile].
     * - Satu ZIP: isinya diekstrak dengan struktur folder dipertahankan (folder pembungkus tunggal
     *   yang berisi proyek Gradle dibuang otomatis).
     * - File biasa: diletakkan langsung di folder tujuan.
     */
    fun read(context: Context, uris: List<Uri>, targetFolder: String, log: (String) -> Unit, cancel: CancelToken): List<LocalFile> {
        val prefix = normalizeFolder(targetFolder)
        val out = LinkedHashMap<String, LocalFile>()
        for (uri in uris) {
            cancel.check()
            val name = displayName(context, uri)
            if (name.endsWith(".zip", ignoreCase = true)) {
                readZip(context, uri, prefix, out, log, cancel)
            } else {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Tidak bisa membuka $name")
                if (bytes.size > MAX_FILE_BYTES) throw IOException("$name lebih dari 50 MB (batas aplikasi).")
                val path = prefix + name
                out[path] = LocalFile(path, modeFor(path), bytes)
            }
        }
        if (out.isEmpty()) throw IOException("Tidak ada file yang bisa diupload.")
        return out.values.toList()
    }

    private fun readZip(
        context: Context, uri: Uri, prefix: String,
        out: MutableMap<String, LocalFile>, log: (String) -> Unit, cancel: CancelToken,
    ) {
        // Lintasan 1: kumpulkan semua entri (isi dibaca ke memori satu per satu).
        val raw = LinkedHashMap<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            ZipInputStream(input).use { zin ->
                while (true) {
                    cancel.check()
                    val e = zin.nextEntry ?: break
                    if (e.isDirectory) continue
                    val name = e.name.replace('\\', '/').trimStart('/')
                    if (name.isBlank() || name.split('/').any { it == ".." }) continue // cegah path traversal
                    if (SKIP_DIRS.any { name.startsWith(it) || name.contains("/$it") }) continue
                    if (name.endsWith(".DS_Store") || name.endsWith("Thumbs.db")) continue
                    val bos = ByteArrayOutputStream()
                    val buf = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        val n = zin.read(buf)
                        if (n < 0) break
                        size += n
                        if (size > MAX_FILE_BYTES) throw IOException("$name lebih dari 50 MB (batas aplikasi).")
                        bos.write(buf, 0, n)
                    }
                    raw[name] = bos.toByteArray()
                }
            }
        } ?: throw IOException("Tidak bisa membuka ZIP")

        if (raw.isEmpty()) throw IOException("ZIP kosong.")

        // Lintasan 2: buang folder pembungkus tunggal bila proyek Gradle ada di dalamnya.
        val strip = detectWrapper(raw.keys)
        if (strip != null) log("Folder pembungkus \"$strip\" dibuang dari path.")
        for ((name, bytes) in raw) {
            val rel = if (strip != null) name.removePrefix("$strip/") else name
            val path = prefix + rel
            out[path] = LocalFile(path, modeFor(path), bytes)
        }
    }

    /** Bila semua entri berada di bawah satu folder yang sama DAN folder itu berisi proyek Gradle, kembalikan nama folder. */
    private fun detectWrapper(names: Set<String>): String? {
        val tops = names.map { it.substringBefore('/', "") }.toSet()
        if (tops.size != 1) return null
        val top = tops.first()
        if (top.isBlank()) return null
        if (names.any { !it.contains('/') }) return null
        val markers = listOf("settings.gradle", "settings.gradle.kts", "gradlew", "build.gradle", "build.gradle.kts")
        return if (markers.any { "$top/$it" in names }) top else null
    }

    fun normalizeFolder(folder: String): String {
        val f = folder.trim().replace('\\', '/').trim('/')
        return if (f.isEmpty()) "" else "$f/"
    }

    private fun modeFor(path: String): String {
        val n = path.substringAfterLast('/')
        return if (n == "gradlew" || n.endsWith(".sh")) "100755" else "100644"
    }
}
