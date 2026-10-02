package com.example.aidetest

/**
 * Registry aman untuk Bit: hanya ID/nama/deskripsi tool. Tidak menyimpan token, password,
 * atau credential. Bit memakai registry ini untuk memahami kemampuan APK.
 */
object BitToolRegistry {
    data class Match(val id: String, val name: String, val confidence: Int)

    private val aliases = mapOf(
        "githubzip" to listOf("github", "upload github", "upload ke github", "kirim ke github", "push github", "github publisher", "repo github"),
        "zip" to listOf("zip", "unzip", "extract", "ekstrak", "kompres", "compress", "arsip"),
        "filemanager" to listOf("file manager", "kelola file", "folder", "file"),
        "qr" to listOf("qr", "barcode", "scan qr", "scanner qr"),
        "dns" to listOf("dns", "domain dns", "lookup dns"),
        "hash" to listOf("hash", "sha", "sha256", "sha-256", "checksum hash"),
        "apk" to listOf("apk inspector", "inspect apk", "lihat apk"),
        "apkanalyzer" to listOf("apk analyzer", "analisis apk", "analyze apk"),
        "fileconvert" to listOf("convert file", "konversi file", "ubah format file"),
        "editor" to listOf("editor", "edit file", "teks")
    )

    fun find(question: String, tools: List<Pair<String,String>>): Match? {
        val q = question.trim().lowercase()
        if (q.isBlank()) return null

        // Intent khusus GitHub: kata upload/kirim + github + zip/file.
        if (q.contains("github") && (q.contains("upload") || q.contains("kirim") || q.contains("unggah") || q.contains("push"))) {
            val t = tools.firstOrNull { it.first == "githubzip" }
            if (t != null) return Match(t.first, t.second, 100)
        }

        var best: Match? = null
        for ((id,name) in tools) {
            var score = 0
            val n = name.lowercase()
            if (q.contains(id.lowercase())) score += 55
            if (n.split(Regex("[^a-z0-9]+" )).filter { it.length >= 3 }.any { q.contains(it) }) score += 35
            aliases[id]?.forEach { alias -> if (q.contains(alias)) score += if (alias.length >= 6) 50 else 25 }
            if (score > 0 && (best == null || score > best!!.confidence)) best = Match(id,name,score.coerceAtMost(99))
        }
        return best?.takeIf { it.confidence >= 35 }
    }
}
