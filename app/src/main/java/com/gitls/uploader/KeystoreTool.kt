package com.gitls.uploader

import android.content.Context
import android.util.Base64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Date

class KeystoreMaterial(val bytes: ByteArray, val password: String, val alias: String) {
    val base64: String get() = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

class SecretsExistException : IOException("Secret keystore sudah ada di repo ini.")

/** Membuat keystore PKCS12 tetap di HP dan menyimpannya (kata sandi dienkripsi dengan Android Keystore). */
object KeystoreTool {
    const val ALIAS = "gitls"
    private const val PASS_KEY = "ks_pass_enc"
    private const val FILE_NAME = "gitls-release.p12"

    private fun file(ctx: Context) = File(ctx.filesDir, FILE_NAME)
    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("uploader_prefs", Context.MODE_PRIVATE)

    fun load(ctx: Context): KeystoreMaterial? {
        val f = file(ctx)
        if (!f.exists()) return null
        val enc = prefs(ctx).getString(PASS_KEY, null) ?: return null
        val pw = TokenVault.decrypt(enc) ?: return null
        return KeystoreMaterial(f.readBytes(), pw, ALIAS)
    }

    fun loadOrCreate(ctx: Context, log: (String) -> Unit): KeystoreMaterial {
        load(ctx)?.let { log("Memakai keystore yang sudah ada di HP ini."); return it }
        log("Membuat keystore baru (RSA 2048, berlaku ±100 tahun)…")
        return create(ctx)
    }

    private fun randomPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val r = SecureRandom()
        return String(CharArray(24) { chars[r.nextInt(chars.length)] })
    }

    private fun create(ctx: Context): KeystoreMaterial {
        val password = randomPassword()
        val enc = TokenVault.encrypt(password)
            ?: throw IOException("Android Keystore di perangkat ini tidak bisa dipakai untuk menyimpan kata sandi.")

        val bc = BouncyCastleProvider()
        val kpg = KeyPairGenerator.getInstance("RSA", bc)
        kpg.initialize(2048, SecureRandom())
        val kp = kpg.generateKeyPair()

        val name = X500Name("CN=GITLS, O=GITLS")
        val now = System.currentTimeMillis()
        val holder = JcaX509v3CertificateBuilder(
            name,
            BigInteger(63, SecureRandom()).add(BigInteger.ONE),
            Date(now - 24L * 3600 * 1000),
            Date(now + 36500L * 24 * 3600 * 1000),
            name,
            kp.public,
        ).build(JcaContentSignerBuilder("SHA256withRSA").setProvider(bc).build(kp.private))
        val cert = JcaX509CertificateConverter().setProvider(bc).getCertificate(holder)

        val ks = KeyStore.getInstance("PKCS12", bc)
        ks.load(null, null)
        ks.setKeyEntry(ALIAS, kp.private, password.toCharArray(), arrayOf(cert))
        val out = ByteArrayOutputStream()
        ks.store(out, password.toCharArray())
        val bytes = out.toByteArray()

        // Verifikasi: keystore harus bisa dibaca ulang dengan kata sandi yang sama.
        val check = KeyStore.getInstance("PKCS12", bc)
        check.load(ByteArrayInputStream(bytes), password.toCharArray())
        if (!check.containsAlias(ALIAS)) throw IOException("Keystore gagal diverifikasi.")

        val f = file(ctx)
        val tmp = File(f.parentFile, "$FILE_NAME.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(f)) throw IOException("Tidak bisa menyimpan keystore.")
        prefs(ctx).edit().putString(PASS_KEY, enc).commit()
        return KeystoreMaterial(bytes, password, ALIAS)
    }

    /** libsodium crypto_box_seal: format yang diminta GitHub untuk secret. */
    fun seal(plain: ByteArray, publicKeyBase64: String): String {
        val pk = Base64.decode(publicKeyBase64, Base64.DEFAULT)
        val cipher = ByteArray(plain.size + 48) // crypto_box_SEALBYTES = 48
        val ls = LazySodiumAndroid(SodiumAndroid())
        if (!ls.cryptoBoxSeal(cipher, plain, plain.size.toLong(), pk)) throw IOException("Enkripsi secret gagal.")
        return Base64.encodeToString(cipher, Base64.NO_WRAP)
    }

    fun backupText(m: KeystoreMaterial): String = buildString {
        append("GITLS keystore backup\n")
        append("RAHASIA: file ini berisi kata sandi. Simpan di tempat pribadi, jangan upload ke repo.\n\n")
        append("ANDROID_KEY_ALIAS=").append(m.alias).append('\n')
        append("ANDROID_KEYSTORE_PASSWORD=").append(m.password).append('\n')
        append("ANDROID_KEY_PASSWORD=").append(m.password).append("\n\n")
        append("ANDROID_KEYSTORE_BASE64=\n").append(m.base64).append('\n')
    }
}

/** Membuat/memakai keystore lalu mengirim 4 secret yang dibutuhkan workflow ke repo tujuan. */
class KeystoreSetup(
    private val ctx: Context,
    private val s: UploaderSettings,
    private val ui: PipelineUi,
    private val cancel: CancelToken,
) {
    private val names = listOf(
        "ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD",
    )

    fun run(overwrite: Boolean) {
        val gh = GitHubClient(s.token, s.owner, s.repoName, cancel) { ui.log(it) }
        ui.stage("Memeriksa repo…"); ui.progress(0, 0)
        val (_, canPush) = gh.repoInfo()
        if (!canPush) throw IOException("Token tidak punya izin tulis ke ${s.repo}.")

        try {
            if (!overwrite && names.any { gh.hasSecret(it) }) throw SecretsExistException()

            ui.stage("Menyiapkan keystore… (beberapa detik)")
            val m = KeystoreTool.loadOrCreate(ctx) { ui.log(it) }
            cancel.check()

            ui.stage("Mengirim secret ke GitHub…")
            val (keyId, key) = gh.secretsPublicKey()
            val values = mapOf(
                names[0] to m.base64, names[1] to m.password, names[2] to m.alias, names[3] to m.password,
            )
            var done = 0L
            for ((n, v) in values) {
                cancel.check()
                gh.putSecret(n, KeystoreTool.seal(v.toByteArray(Charsets.UTF_8), key), keyId)
                ui.log("✔ $n")
                ui.progress(++done, values.size.toLong())
            }
        } catch (e: GitHubException) {
            if (e.code == 403 || e.code == 404) {
                throw IOException(
                    "Token belum punya izin Secrets (Read & write) untuk ${s.repo}. " +
                        "Tambahkan izin itu di pengaturan token GitHub, lalu coba lagi."
                )
            }
            throw e
        }
    }
}
