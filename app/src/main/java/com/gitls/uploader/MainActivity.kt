package com.gitls.uploader

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private companion object {
        const val REQ_PICK = 1001
        const val REQ_BACKUP = 1002
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private val main = Handler(Looper.getMainLooper())
    private lateinit var store: SettingsStore

    private var pickedUris: List<Uri> = emptyList()
    private var worker: Thread? = null
    private var cancelToken = CancelToken()
    private var readyApk: File? = null
    private var inspection: ApkInstaller.Inspection? = null
    private var pendingAutoInstall = false
    private var resumed = false
    @Volatile private var lastProgressAt = 0L
    private val logLines = ArrayDeque<String>()

    // views
    private lateinit var tokenField: EditText
    private lateinit var repoField: EditText
    private lateinit var branchField: EditText
    private lateinit var folderField: EditText
    private lateinit var artifactField: EditText
    private lateinit var mirrorCheck: CheckBox
    private lateinit var waitCheck: CheckBox
    private lateinit var settingsBody: LinearLayout
    private lateinit var settingsToggle: TextView
    private lateinit var pickedLabel: TextView
    private lateinit var messageField: EditText
    private lateinit var uploadBtn: Button
    private lateinit var latestBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var ksBtn: Button
    private lateinit var ksBackupBtn: Button
    private lateinit var pickBtn: Button
    private lateinit var stageView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var logView: TextView
    private lateinit var installCard: LinearLayout
    private lateinit var installInfo: TextView
    private lateinit var installBtn: Button

    // ------------------------------------------------------------------ UI helpers

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun col(id: Int) = ContextCompat.getColor(this, id)

    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun label(t: String, size: Float = 14f, color: Int = col(R.color.text), bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun step(n: String, title: String, hint: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        row.addView(TextView(context).apply {
            text = n; textSize = 12f; gravity = android.view.Gravity.CENTER
            setTextColor(col(R.color.bg)); setTypeface(typeface, Typeface.BOLD)
            background = rounded(col(R.color.accent), 999)
        }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(10) })
        row.addView(label(title, 16f, bold = true))
        addView(row)
        addView(label(hint, 12f, col(R.color.muted)).apply { setPadding(0, dp(6), 0, 0) })
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(col(R.color.card), 16, col(R.color.line))
        setPadding(dp(14), dp(14), dp(14), dp(14))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }
    }

    private fun field(hint: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setHintTextColor(col(R.color.muted))
        setTextColor(col(R.color.text))
        textSize = 14f
        background = rounded(col(R.color.field), 10, col(R.color.line))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        inputType = InputType.TYPE_CLASS_TEXT or
            (if (password) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_URI)
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) }
    }

    private fun check(t: String) = CheckBox(this).apply {
        text = t
        textSize = 13f
        setTextColor(col(R.color.text))
        buttonTintList = ColorStateList.valueOf(col(R.color.accent))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) }
    }

    private fun button(t: String, primary: Boolean, onClick: () -> Unit) = Button(this).apply {
        text = t
        setAllCaps(false)
        textSize = 14f
        setTextColor(if (primary) col(R.color.bg) else col(R.color.text))
        val bg = if (primary) rounded(col(R.color.accent), 12) else rounded(0x00000000, 12, col(R.color.line))
        background = RippleDrawable(ColorStateList.valueOf((col(R.color.text) and 0x00FFFFFF) or 0x22000000), bg, null)
        stateListAnimator = null
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(48)).apply { topMargin = dp(8) }
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        buildUi()
        fillFields(store.load())
        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        maybeAutoInstall()
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) cancelToken.cancelled = true
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_BACKUP) {
            if (resultCode == RESULT_OK) data?.data?.let { writeBackup(it) }
            return
        }
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) return
        val list = ArrayList<Uri>()
        data.clipData?.let { cd -> for (i in 0 until cd.itemCount) list.add(cd.getItemAt(i).uri) }
        if (list.isEmpty()) data.data?.let { list.add(it) }
        setPicked(list)
    }

    @Suppress("DEPRECATION")
    private fun handleIncoming(i: Intent?) {
        if (i == null) return
        val uris: List<Uri> = when (i.action) {
            Intent.ACTION_SEND -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            )
            Intent.ACTION_SEND_MULTIPLE ->
                (if (Build.VERSION.SDK_INT >= 33) i.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else i.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)).orEmpty()
            else -> emptyList()
        }
        if (uris.isNotEmpty()) setPicked(uris)
    }

    // ------------------------------------------------------------------ UI

    private fun buildUi() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(col(R.color.bg))
            clipToPadding = false
            isFillViewport = true
            addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(scroll)

        content.addView(label("GITLS Uploader", 26f, bold = true))
        content.addView(label("Kirim kode ke GitHub, tunggu build, lalu pasang sebagai update. Tidak perlu hapus app lama.", 13f, col(R.color.muted)).apply {
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(label("Cara pakai & syarat update  ›", 13f, col(R.color.text), true).apply {
            setPadding(0, dp(10), 0, dp(2))
            setOnClickListener { showHelp() }
        })

        // --- Pengaturan
        val settings = card()
        settingsToggle = label("Pengaturan GitHub (isi sekali saja)  ▾", 15f, col(R.color.text), true).apply {
            setPadding(0, dp(2), 0, dp(2))
            setOnClickListener {
                settingsBody.visibility = if (settingsBody.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        settings.addView(settingsToggle)
        settingsBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tokenField = field("Token GitHub", password = true)
        repoField = field("Repo tujuan, contoh: budi/gitls")
        branchField = field("Branch (kosong = otomatis)")
        folderField = field("Folder tujuan (kosong = root repo)")
        artifactField = field("Nama artifact APK (bawaan: installable-debug)")
        mirrorCheck = check("Samakan repo dengan ZIP (file yang tak ada di ZIP dihapus)")
        waitCheck = check("Setelah upload, tunggu build lalu buka pemasangan")
        settingsBody.addView(tokenField)
        settingsBody.addView(label("Token perlu izin: Contents (tulis), Actions (baca), Workflows (tulis), Secrets (tulis, untuk tombol keystore).", 11f, col(R.color.muted)).apply {
            setPadding(dp(4), dp(4), 0, 0)
        })
        settingsBody.addView(repoField)
        settingsBody.addView(branchField)
        settingsBody.addView(folderField)
        settingsBody.addView(artifactField)
        settingsBody.addView(mirrorCheck)
        settingsBody.addView(waitCheck)
        settingsBody.addView(button("Simpan pengaturan", primary = false) {
            val s = readFields()
            store.save(s)
            toast("Tersimpan")
            if (s.isComplete) settingsBody.visibility = View.GONE
        })
        settings.addView(settingsBody)
        content.addView(settings)

        // --- Keystore
        val ks = card()
        ks.addView(label("Keystore tetap (sekali saja)", 15f, bold = true))
        ks.addView(label(
            "Supaya update tidak ditolak Android. Membuat kunci tanda tangan di HP ini, lalu mengirimnya ke GitHub Secrets repo di atas.",
            12f, col(R.color.muted),
        ).apply { setPadding(0, dp(6), 0, 0) })
        ksBtn = button("Siapkan keystore & kirim ke GitHub", primary = false) { startKeystore(false) }
        ks.addView(ksBtn)
        ksBackupBtn = button("Simpan cadangan keystore", primary = false) { backupKeystore() }
        ks.addView(ksBackupBtn)
        content.addView(ks)

        // --- Upload
        val pick = card()
        pick.addView(step("1", "Pilih file", "Pilih ZIP proyek, atau bagikan ZIP dari file manager ke app ini."))
        pickedLabel = label("Belum ada file dipilih.", 13f, col(R.color.muted)).apply { setPadding(0, dp(10), 0, 0) }
        pick.addView(pickedLabel)
        pickBtn = button("Pilih ZIP / file", primary = false) { pickFiles() }
        pick.addView(pickBtn)
        content.addView(pick)

        val up = card()
        up.addView(step("2", "Upload & build", "Hanya file yang berubah dikirim, dalam satu commit."))
        messageField = field("Pesan commit (opsional)")
        up.addView(messageField)
        uploadBtn = button("Upload & Build", primary = true) { start(upload = true) }
        up.addView(uploadBtn)
        latestBtn = button("Ambil APK terakhir (tanpa upload)", primary = false) { start(upload = false) }
        up.addView(latestBtn)
        cancelBtn = button("Batalkan", primary = false) { cancelToken.cancelled = true }.apply { visibility = View.GONE }
        up.addView(cancelBtn)
        content.addView(up)

        // --- Pasang
        installCard = card().apply { visibility = View.GONE }
        installCard.addView(step("3", "Pasang update", "APK sudah siap. Android akan menampilkan tombol Update."))
        installInfo = label("", 13f).apply { setPadding(0, dp(10), 0, 0) }
        installCard.addView(installInfo)
        installBtn = button("Pasang sebagai update", primary = true) { onInstallClicked() }
        installCard.addView(installBtn)
        content.addView(installCard)

        // --- Status
        val st = card()
        st.addView(label("Status", 15f, bold = true))
        stageView = label("Siap. Isi pengaturan, pilih file, lalu tekan Upload & Build.", 13f, col(R.color.muted)).apply { setPadding(0, dp(6), 0, dp(6)) }
        st.addView(stageView)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = ColorStateList.valueOf(col(R.color.accent))
            indeterminateTintList = ColorStateList.valueOf(col(R.color.accent))
            layoutParams = LinearLayout.LayoutParams(MATCH, dp(6))
        }
        st.addView(progressBar)
        logView = label("", 11f, col(R.color.muted)).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(0, dp(10), 0, 0)
        }
        logView.visibility = View.GONE
        val logToggle = label("Lihat detail  ▾", 12f, col(R.color.muted), true).apply {
            setPadding(0, dp(10), 0, 0)
            setOnClickListener {
                val show = logView.visibility != View.VISIBLE
                logView.visibility = if (show) View.VISIBLE else View.GONE
                text = if (show) "Sembunyikan detail  ▴" else "Lihat detail  ▾"
            }
        }
        st.addView(logToggle)
        st.addView(logView)
        content.addView(st)
    }

    private fun fillFields(s: UploaderSettings) {
        tokenField.setText(s.token)
        repoField.setText(s.repo)
        branchField.setText(s.branch)
        folderField.setText(s.folder)
        artifactField.setText(s.artifactKeyword)
        mirrorCheck.isChecked = s.mirrorDelete
        waitCheck.isChecked = s.waitAndInstall
        settingsBody.visibility = if (s.isComplete) View.GONE else View.VISIBLE
    }

    private fun readFields() = UploaderSettings(
        token = tokenField.text.toString().trim(),
        repo = repoField.text.toString().trim()
            .removePrefix("https://github.com/").removePrefix("github.com/").removeSuffix(".git").trim('/'),
        branch = branchField.text.toString().trim(),
        folder = folderField.text.toString().trim(),
        artifactKeyword = artifactField.text.toString().trim().ifBlank { "installable-debug" },
        mirrorDelete = mirrorCheck.isChecked,
        waitAndInstall = waitCheck.isChecked,
    )

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("Cara pakai")
            .setMessage(
                "1. Isi token GitHub dan repo tujuan (sekali saja).\n" +
                "2. Pilih ZIP proyek.\n" +
                "3. Tekan Upload & Build, lalu tunggu.\n" +
                "4. Setelah APK siap, tekan Pasang sebagai update.\n\n" +
                "Agar bisa update TANPA hapus app, dua syarat harus terpenuhi:\n" +
                "• Tanda tangan APK sama: tekan \"Siapkan keystore & kirim ke GitHub\" (sekali saja), lalu simpan cadangannya.\n" +
                "• versionCode tidak lebih kecil dari yang terpasang.\n\n" +
                "Layar di bawah akan menunjukkan ✔ bila update aman."
            )
            .setPositiveButton("Mengerti", null)
            .show()
    }

    // ------------------------------------------------------------------ keystore

    private fun startKeystore(overwrite: Boolean) {
        if (worker?.isAlive == true) return
        val s = readFields()
        store.save(s)
        if (!s.isComplete) {
            toast("Isi token dan repo (owner/repo) dulu.")
            settingsBody.visibility = View.VISIBLE
            return
        }
        cancelToken = CancelToken()
        val token = cancelToken
        installCard.visibility = View.GONE
        logLines.clear()
        logView.text = ""
        setBusy(true)
        worker = Thread {
            var done = false
            var exists = false
            var cancelled = false
            var error: String? = null
            try {
                KeystoreSetup(applicationContext, s, ui, token).run(overwrite)
                done = true
            } catch (e: SecretsExistException) {
                exists = true
            } catch (e: CancelledException) {
                cancelled = true
            } catch (e: OutOfMemoryError) {
                error = "Memori tidak cukup."
            } catch (e: Throwable) {
                error = e.message ?: e.javaClass.simpleName
            }
            main.post { finishKeystore(s.repo, done, exists, error, cancelled) }
        }.apply { name = "keystore-worker"; start() }
    }

    private fun finishKeystore(repo: String, done: Boolean, exists: Boolean, error: String?, cancelled: Boolean) {
        setBusy(false)
        progressBar.isIndeterminate = false
        when {
            cancelled -> {
                stageView.text = "Dibatalkan."
                stageView.setTextColor(col(R.color.warn))
            }
            exists -> {
                stageView.text = "Secret keystore sudah ada di $repo."
                stageView.setTextColor(col(R.color.warn))
                AlertDialog.Builder(this)
                    .setTitle("Secret sudah ada")
                    .setMessage(
                        "Repo ini sudah punya secret keystore. Menimpa akan mengganti tanda tangan; " +
                            "app yang sudah terpasang dengan kunci lama akan menolak update sampai dihapus-instal sekali.\n\nTimpa?"
                    )
                    .setPositiveButton("Timpa") { _, _ -> startKeystore(true) }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            error != null -> {
                stageView.text = "Gagal: $error"
                stageView.setTextColor(col(R.color.err))
                appendLog("✖ $error")
            }
            done -> {
                progressBar.progress = 1000
                stageView.text = "Keystore siap. 4 secret terkirim ke $repo."
                stageView.setTextColor(col(R.color.ok))
                AlertDialog.Builder(this)
                    .setTitle("Simpan cadangan")
                    .setMessage(
                        "Simpan cadangan keystore sekarang. Kalau keystore hilang, app harus dihapus-instal lagi. " +
                            "File cadangan berisi kata sandi, jadi simpan di tempat pribadi."
                    )
                    .setPositiveButton("Simpan cadangan") { _, _ -> backupKeystore() }
                    .setNegativeButton("Nanti", null)
                    .show()
            }
        }
    }

    private fun backupKeystore() {
        if (KeystoreTool.load(this) == null) {
            toast("Belum ada keystore. Tekan \"Siapkan keystore\" dulu.")
            return
        }
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "gitls-keystore-backup.txt")
        }
        startActivityForResult(i, REQ_BACKUP)
    }

    private fun writeBackup(uri: Uri) {
        val m = KeystoreTool.load(this)
        if (m == null) { toast("Keystore tidak ditemukan."); return }
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(KeystoreTool.backupText(m).toByteArray(Charsets.UTF_8)) }
                ?: throw java.io.IOException("Tidak bisa menulis file.")
            toast("Cadangan tersimpan.")
        } catch (e: Exception) {
            toast("Gagal menyimpan: ${e.message}")
        }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------------ memilih file

    private fun pickFiles() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(i, REQ_PICK)
    }

    private fun setPicked(uris: List<Uri>) {
        pickedUris = uris
        val infos = UploadSource.describe(this, uris)
        val names = infos.take(3).joinToString(", ") { it.name } + if (infos.size > 3) " +${infos.size - 3} lagi" else ""
        pickedLabel.text = "${infos.size} dipilih: $names"
        pickedLabel.setTextColor(col(R.color.text))
    }

    // ------------------------------------------------------------------ menjalankan

    private val ui = object : PipelineUi {
        override fun log(msg: String) { main.post { appendLog(msg) } }
        override fun stage(text: String) { main.post { stageView.text = text; stageView.setTextColor(col(R.color.text)) } }
        override fun progress(done: Long, total: Long) {
            val now = System.currentTimeMillis()
            if (now - lastProgressAt < 120 && done != total) return
            lastProgressAt = now
            main.post {
                if (total <= 0) {
                    progressBar.isIndeterminate = true
                } else {
                    progressBar.isIndeterminate = false
                    progressBar.progress = (done * 1000 / total).coerceIn(0, 1000).toInt()
                }
            }
        }
    }

    private fun appendLog(line: String) {
        logLines.addLast(line)
        while (logLines.size > 250) logLines.removeFirst()
        logView.text = logLines.joinToString("\n")
    }

    private fun setBusy(busy: Boolean) {
        listOf(uploadBtn, latestBtn, pickBtn, ksBtn, ksBackupBtn).forEach { it.isEnabled = !busy; it.alpha = if (busy) 0.5f else 1f }
        cancelBtn.visibility = if (busy) View.VISIBLE else View.GONE
        if (busy) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun start(upload: Boolean) {
        if (worker?.isAlive == true) return
        val s = readFields()
        store.save(s)
        if (!s.isComplete) {
            toast("Isi token dan repo (owner/repo) dulu.")
            settingsBody.visibility = View.VISIBLE
            return
        }
        if (upload && pickedUris.isEmpty()) {
            toast("Pilih ZIP/file dulu.")
            return
        }
        val uris = pickedUris
        val msg = messageField.text.toString().trim().ifBlank {
            "Update via GITLS Uploader (" + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) + ")"
        }

        cancelToken = CancelToken()
        val token = cancelToken
        readyApk = null
        inspection = null
        installCard.visibility = View.GONE
        logLines.clear()
        logView.text = ""
        setBusy(true)

        worker = Thread {
            var outcome: Outcome? = null
            var error: String? = null
            var cancelled = false
            try {
                val p = Pipeline(applicationContext, s, ui, token)
                outcome = if (upload) p.uploadAndBuild(uris, msg) else p.fetchLatest()
            } catch (e: CancelledException) {
                cancelled = true
            } catch (e: OutOfMemoryError) {
                error = "Memori tidak cukup untuk membaca file sebesar ini."
            } catch (e: Throwable) {
                error = e.message ?: e.javaClass.simpleName
            }
            main.post { finishRun(outcome, error, cancelled) }
        }.apply { name = "uploader-worker"; start() }
    }

    private fun finishRun(outcome: Outcome?, error: String?, cancelled: Boolean) {
        setBusy(false)
        progressBar.isIndeterminate = false
        when {
            cancelled -> {
                stageView.text = "Dibatalkan."
                stageView.setTextColor(col(R.color.warn))
                appendLog("Dibatalkan oleh pengguna.")
            }
            error != null -> {
                stageView.text = "Gagal: $error"
                stageView.setTextColor(col(R.color.err))
                appendLog("✖ $error")
            }
            outcome != null -> {
                val good = outcome.buildOk != false && outcome.apk != null
                stageView.text = outcome.note
                stageView.setTextColor(col(if (good) R.color.ok else R.color.warn))
                progressBar.progress = 1000
                appendLog(outcome.note)
                if (outcome.buildOk == false && outcome.runUrl != null) appendLog("Detail: ${outcome.runUrl}")
                outcome.apk?.let { showApk(it, outcome.buildOk == true) }
            }
        }
    }

    // ------------------------------------------------------------------ pasang APK

    private fun showApk(apk: File, autoOpen: Boolean) {
        readyApk = apk
        val ins = ApkInstaller.inspect(this, apk)
        inspection = ins
        installCard.visibility = View.VISIBLE

        if (ins == null) {
            installInfo.text = "APK terunduh (${apk.length() / 1024} KB) tetapi tidak bisa dibaca. Coba pasang manual."
            installInfo.setTextColor(col(R.color.warn))
            installBtn.text = "Pasang APK"
            return
        }

        val sb = StringBuilder(ins.pkg).append('\n')
        if (ins.oldCode != null) {
            sb.append("Terpasang ${ins.oldName} (${ins.oldCode}) → baru ${ins.newName} (${ins.newCode})\n")
        } else {
            sb.append("Belum terpasang → baru ${ins.newName} (${ins.newCode})\n")
        }
        sb.append(
            when (ins.sig) {
                ApkInstaller.Sig.MATCH -> "✔ Tanda tangan sama: bisa update tanpa hapus-instal."
                ApkInstaller.Sig.MISMATCH -> "✖ Tanda tangan BERBEDA: Android akan menolak update. Pakai keystore tetap di GitHub Secrets (lihat README)."
                ApkInstaller.Sig.NOT_INSTALLED -> "Pemasangan baru (app belum terpasang)."
                ApkInstaller.Sig.UNKNOWN -> "Tanda tangan tidak bisa diperiksa."
            }
        )
        if (ins.isDowngrade) sb.append("\n⚠ Versi lebih rendah dari yang terpasang; Android akan menolak.")
        installInfo.text = sb.toString()
        installInfo.setTextColor(col(if (ins.canInstallInPlace) R.color.text else R.color.err))
        installBtn.text = if (ins.sig == ApkInstaller.Sig.MISMATCH) "Tetap coba pasang" else
            if (ins.oldCode != null) "Pasang sebagai update" else "Pasang APK"

        pendingAutoInstall = autoOpen && ins.canInstallInPlace
        maybeAutoInstall()
    }

    private fun maybeAutoInstall() {
        if (!pendingAutoInstall || !resumed || readyApk == null) return
        pendingAutoInstall = false
        doInstall()
    }

    private fun onInstallClicked() {
        val ins = inspection
        if (ins != null && ins.sig == ApkInstaller.Sig.MISMATCH) {
            AlertDialog.Builder(this)
                .setTitle("Tanda tangan berbeda")
                .setMessage(
                    "APK ini ditandatangani kunci yang berbeda dari app yang terpasang, jadi Android akan menolak update-nya. " +
                        "Jalan keluarnya hanya hapus-instal sekali ini, lalu pasang keystore tetap agar update berikutnya selalu mulus.\n\nTetap coba?"
                )
                .setPositiveButton("Coba pasang") { _, _ -> doInstall() }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            doInstall()
        }
    }

    private fun doInstall() {
        val apk = readyApk ?: return
        if (ApkInstaller.needsUnknownSourcesPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle("Izin pemasangan")
                .setMessage("Izinkan GITLS Uploader memasang aplikasi, lalu kembali ke sini dan tekan tombol pasang lagi.")
                .setPositiveButton("Buka pengaturan") { _, _ -> ApkInstaller.openUnknownSourcesSettings(this) }
                .setNegativeButton("Batal", null)
                .show()
            return
        }
        try {
            ApkInstaller.install(this, apk)
        } catch (e: Exception) {
            toast("Tidak bisa membuka pemasang: ${e.message}")
        }
    }
}
