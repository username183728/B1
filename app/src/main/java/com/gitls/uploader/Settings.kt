package com.gitls.uploader

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Token GitHub dienkripsi AES-256-GCM dengan kunci di Android Keystore (kunci tidak keluar dari perangkat). */
object TokenVault {
    private const val ALIAS = "gitls_uploader_token_key"
    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }.getOrNull()

    fun decrypt(stored: String): String? = runCatching {
        val raw = Base64.decode(stored, Base64.NO_WRAP)
        require(raw.size > 12)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        String(cipher.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8)
    }.getOrNull()
}

data class UploaderSettings(
    val token: String = "",
    val repo: String = "",            // "owner/repo"
    val branch: String = "",          // kosong = default branch repo
    val folder: String = "",          // folder tujuan di repo, kosong = root
    val artifactKeyword: String = "installable-debug",
    val mirrorDelete: Boolean = false,
    val waitAndInstall: Boolean = true,
) {
    val owner: String get() = repo.substringBefore('/').trim()
    val repoName: String get() = repo.substringAfter('/', "").trim()
    val isComplete: Boolean get() = token.isNotBlank() && owner.isNotBlank() && repoName.isNotBlank()
}

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("uploader_prefs", Context.MODE_PRIVATE)

    fun load(): UploaderSettings {
        val token = prefs.getString("token_enc", null)?.let { TokenVault.decrypt(it) }.orEmpty()
        return UploaderSettings(
            token = token,
            repo = prefs.getString("repo", "").orEmpty(),
            branch = prefs.getString("branch", "").orEmpty(),
            folder = prefs.getString("folder", "").orEmpty(),
            artifactKeyword = prefs.getString("artifact_kw", "installable-debug").orEmpty().ifBlank { "installable-debug" },
            mirrorDelete = prefs.getBoolean("mirror", false),
            waitAndInstall = prefs.getBoolean("wait_install", true),
        )
    }

    fun save(s: UploaderSettings) {
        val e = prefs.edit()
            .putString("repo", s.repo.trim())
            .putString("branch", s.branch.trim())
            .putString("folder", s.folder.trim())
            .putString("artifact_kw", s.artifactKeyword.trim())
            .putBoolean("mirror", s.mirrorDelete)
            .putBoolean("wait_install", s.waitAndInstall)
        if (s.token.isNotBlank()) {
            // Bila Keystore gagal, token tidak disimpan (lebih aman daripada teks polos).
            TokenVault.encrypt(s.token.trim())?.let { e.putString("token_enc", it) }
        } else {
            e.remove("token_enc")
        }
        e.apply()
    }
}
