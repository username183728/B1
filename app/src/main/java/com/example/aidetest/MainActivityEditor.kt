package com.example.aidetest

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.concurrent.RejectedExecutionException
import android.Manifest
import android.app.*
import android.app.usage.StorageStatsManager
import android.os.StatFs
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.*
import android.provider.Settings
import android.provider.MediaStore
import android.media.ImageReader
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.text.InputType
import android.view.*
import android.widget.*
import android.webkit.MimeTypeMap
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.MultiFormatReader
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.common.HybridBinarizer
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.*
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.roundToInt
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.airbnb.lottie.LottieListener


internal fun MainActivity.editor(file: File?, forcedMode: String? = null) {
        editorLanding = file == null && forcedMode == null && editorExternalTarget == null
        clearPage("Editor")
        if (file?.absolutePath != editorFile?.absolutePath) editorSourceUri = null
        editorFile = file
        file?.let { recordRecentFile(it) }
        editorMode = forcedMode ?: detectEditorMode(file?.name)
        if (editorLanding) renderEditorHome() else renderEditorPage()
    }
internal fun MainActivity.detectEditorMode(name: String?): String {
        val ext = name?.substringAfterLast('.', "")?.lowercase(Locale.getDefault()) ?: ""
        return when (ext) {
            "html", "htm" -> "html"
            "css" -> "css"
            "js", "mjs", "cjs" -> "js"
            "json" -> "json"
            "csv", "tsv" -> "csv"
            "base64", "b64" -> "base64"
            "py", "kt", "kts", "java", "ts", "c", "cpp", "h", "hpp", "cs", "go", "rs", "php", "sh" -> "code"
            "ini", "cfg", "conf", "properties", "yaml", "yml", "toml" -> "config"
            "xml" -> "xml"
            else -> "text"
        }
    }
internal fun MainActivity.editorModeName(mode: String): String = when (mode) {
        "html" -> "HTML"
        "css" -> "CSS"
        "js" -> "JavaScript"
        "json" -> "JSON"
        "csv" -> "CSV"
        "base64" -> "Base64"
        "utility" -> "Utilitas"
        "code" -> "Kode"
        "config" -> "Konfig"
        "xml" -> "XML"
        else -> "Teks"
    }

internal fun MainActivity.editorDefaultName(mode: String): String = when (mode) {
        "html" -> "index.html"
        "css" -> "style.css"
        "js" -> "script.js"
        "json" -> "untitled.json"
        "csv" -> "untitled.csv"
        "base64" -> "untitled.txt"
        "utility" -> "untitled.txt"
        "code" -> "untitled.py"
        "config" -> "config.ini"
        "xml" -> "untitled.xml"
        else -> "untitled.txt"
    }

internal fun MainActivity.editorModeDescription(mode: String): String = when (mode) {
        "html" -> "Edit HTML dan preview halaman web"
        "css" -> "Edit stylesheet CSS"
        "js" -> "Edit JavaScript"
        "json" -> "Edit, validasi, format, dan konversi JSON"
        "csv" -> "Lihat dan konversi data tabel"
        "base64" -> "Encode dan decode Base64"
        "utility" -> "Utilitas teks dan perhitungan"
        "code" -> "Edit kode program"
        "config" -> "Edit file konfigurasi"
        "xml" -> "Edit dan rapikan XML"
        else -> "Edit teks dan catatan"
    }

internal fun MainActivity.renderEditorHome() {
        // Landing tetap memakai navigasi utama. Hanya tiga mode web-code yang tampil
        // langsung; format lain dipindahkan ke tombol + agar layar tetap bersih.
        title.text = "Editor"
        subtitle.visibility = View.GONE
        action.visibility = View.VISIBLE
        action.text = "+"
        action.textSize = 28f
        back.visibility = View.VISIBLE
        configureActionForPage("Editor")
        editorBox = null
        editorLanding = true
        editorBottomBar.visibility = View.GONE
        editorBottomBar.removeAllViews()
        editorMore.visibility = View.GONE
        topBarVisibility(true)
        content.post { syncEditorBottomBar() }

        content.setPadding(dp(10), dp(4), dp(10), dp(12))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = bg(Color.rgb(246, 248, 250), 18, Color.rgb(231, 235, 239))
        }
        hero.addView(MdiIconView(this).apply {
            setIconName("file-document-edit-outline")
            setIconSize(38f)
            setTextColor(textMain)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply { bottomMargin = dp(8) }
        })
        hero.addView(TextView(this).apply {
            text = "Pilih mode editor"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textMain)
            gravity = Gravity.CENTER
        })
        hero.addView(TextView(this).apply {
            text = "HTML, CSS, dan JavaScript dalam satu editor. Format lain ada di (+)."
            textSize = 12f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        })
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(136)).apply { bottomMargin = dp(12) })
        animateEditorItem(hero, 0L, 10f)

        val modes = listOf(
            Triple("language-html5", "HTML", "Edit halaman HTML dan preview web.") to "html",
            Triple("language-css3", "CSS", "Edit stylesheet dan tampilan web.") to "css",
            Triple("language-javascript", "JavaScript", "Edit logic dan interaksi halaman web.") to "js"
        )
        modes.forEachIndexed { index, pair ->
            val (item, mode) = pair
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(7), dp(10), dp(7))
                background = bg(Color.WHITE, 16, Color.rgb(226, 231, 235))
                isClickable = true
                setOnClickListener {
                    animateEditorPress(this)
                    it.postDelayed({ editorExternalTarget = null; editorExternalMode = null; editor(null, mode) }, 70L)
                }
            }
            val (iconName, name, desc) = item
            row.addView(MdiIconView(this).apply {
                setIconName(iconName)
                setIconSize(24f)
                setTextColor(textMain)
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(40)).apply { rightMargin = dp(8) }
            })
            val textBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            textBox.addView(TextView(this).apply {
                text = name
                textSize = 14f
                setTextColor(textMain)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            textBox.addView(TextView(this).apply {
                text = desc
                textSize = 11f
                setTextColor(textMuted)
                setPadding(0, dp(2), 0, 0)
            })
            row.addView(textBox)
            row.addView(TextView(this).apply {
                text = "›"
                textSize = 25f
                setTextColor(textMuted)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(42))
            })
            content.addView(row, LinearLayout.LayoutParams(-1, dp(66)).apply { bottomMargin = dp(7) })
            animateEditorItem(row, 80L + index * 45L, 12f)
        }
        animateEditorScreen()
    }
internal fun MainActivity.topBarVisibility(visible: Boolean) {
        topBar.visibility = if (visible) View.VISIBLE else View.GONE
        applyEdgeInsets(imeBottomInset)
    }
internal fun MainActivity.applyEdgeInsets(imeBottom: Int) {
        val topBarView = runCatching { topBar }.getOrNull() ?: return
        val scrollView = runCatching { scroll }.getOrNull() ?: return
        val barH = dp(60) + sysTopInset
        val tp = topBarView.layoutParams as LinearLayout.LayoutParams
        tp.height = barH
        tp.bottomMargin = -barH
        topBarView.layoutParams = tp
        topBarView.setPadding(topBarView.paddingLeft, sysTopInset, topBarView.paddingRight, 0)
        topBarView.elevation = dp(8).toFloat()
        topBarView.translationZ = dp(2).toFloat()

        val topSpace = if (topBarView.visibility == View.VISIBLE) barH else sysTopInset
        val bottomSpace = if (imeBottom > 0) 0 else sysBottomInset
        scrollView.clipToPadding = false
        scrollView.setPadding(0, topSpace, 0, bottomSpace)

        // Keyboard: sebelumnya ditangani adjustResize; di mode full layar diganti padding manual.
        mainContainer.setPadding(0, 0, 0, imeBottom)

        val nav = bottomNav.layoutParams as LinearLayout.LayoutParams
        nav.bottomMargin = dp(10) + sysBottomInset
        bottomNav.layoutParams = nav

        val eb = editorBottomBar.layoutParams
        eb.height = dp(56) + bottomSpace
        editorBottomBar.layoutParams = eb
        editorBottomBar.setPadding(editorBottomBar.paddingLeft, 0, editorBottomBar.paddingRight, bottomSpace)

        loginScreen.setPadding(dp(28), dp(24) + sysTopInset, dp(28), dp(36) + sysBottomInset)
    }
internal fun MainActivity.showEditorModePicker() {
    val activity = this
        var menuDialog: AlertDialog? = null
        // Tampilan menu sengaja dibuat seperti sheet pada screenshot: tiga kartu besar
        // untuk operasi file, lalu pilihan format berada di dalam "Buat file".
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(4), dp(28), dp(18))
            background = bg(Color.WHITE, 28, Color.TRANSPARENT)
        }
        panel.addView(TextView(this).apply {
            text = "Tambah"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(textMain)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(14))
        })

        fun sheetRow(iconName: String, titleText: String, action: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(8), dp(16), dp(8))
                background = bg(Color.rgb(247, 247, 248), 18, Color.TRANSPARENT)
                isClickable = true
                setOnClickListener { action() }
                addView(MdiIconView(activity).apply {
                    setIconName(iconName)
                    setIconSize(24f)
                    setTextColor(textMain)
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(48)).apply { rightMargin = dp(8) }
                })
                addView(TextView(activity).apply {
                    text = titleText
                    textSize = 15f
                    setTextColor(textMain)
                    gravity = Gravity.CENTER_VERTICAL
                }, LinearLayout.LayoutParams(0, dp(48), 1f))
            }
        }

        panel.addView(sheetRow("file-outline", "Buat file") {
            menuDialog?.dismiss()
            showEditorCreateFilePicker()
        }, LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(12) })
        panel.addView(sheetRow("folder-plus-outline", "Buat folder") {
            menuDialog?.dismiss()
            createEditorFolder()
        }, LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(12) })
        panel.addView(sheetRow("folder-open-outline", "Buka file") {
            menuDialog?.dismiss()
            editorExternalTarget = null
            editorExternalMode = null
            pickFileForEditor()
        }, LinearLayout.LayoutParams(-1, dp(72)))

        val dialog = AlertDialog.Builder(this).setView(panel).create()
        menuDialog = dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            dialog.window?.setDimAmount(0.46f)
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes = dialog.window?.attributes?.apply {
            width = (resources.displayMetrics.widthPixels - dp(32))
        }
    }
internal fun MainActivity.showEditorCreateFilePicker() {
        val labels = arrayOf("HTML (.html)", "CSS (.css)", "JavaScript (.js)", "JSON (.json)", "CSV (.csv)", "Base64 (.txt)", "Teks (.txt)", "Konfigurasi (.ini)", "XML (.xml)", "Utilitas Teks")
        val keys = arrayOf("html", "css", "js", "json", "csv", "base64", "text", "config", "xml", "utility")
        AlertDialog.Builder(this)
            .setTitle("Buat file")
            .setItems(labels) { _, which ->
                editorExternalTarget = null
                editorExternalMode = null
                editorFile = null
                editorSourceUri = null
                editor(null, keys[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }
internal fun MainActivity.createEditorFolder() {
        val name = edit("Nama folder")
        AlertDialog.Builder(this)
            .setTitle("Buat folder")
            .setView(name)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Buat") { _, _ ->
                val folder = safeChildFile(filesDir, name.text.toString())
                if (folder == null) toast("Nama folder tidak valid")
                else if (folder.exists() || !folder.mkdirs()) toast("Folder gagal dibuat")
                else toast("Folder dibuat: ${folder.name}")
            }.show()
    }
internal fun MainActivity.showEditorMoreMenu() {
        showEditorModePicker()
    }
internal fun MainActivity.renderEditorPage() {
        // Saat sudah masuk workspace editor, sembunyikan AppBar agar area kode bersih
        // seperti editor pada screenshot. Tombol + dipindah ke kartu nama file.
        topBarVisibility(false)
        subtitle.visibility = View.GONE
        homeMenu.visibility = View.GONE
        homeProfile.visibility = View.GONE
        action.visibility = View.GONE
        back.visibility = View.GONE
        editorMore.visibility = View.GONE
        editorBottomBar.visibility = View.GONE

        content.setPadding(dp(8), dp(6), dp(8), dp(4))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val fileCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(8), dp(8))
            background = bg(Color.rgb(246, 246, 247), 20, Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, dp(66), 1f)
        }
        fileCard.addView(MdiIconView(this).apply {
            setIconName(if (editorMode == "html") "language-html5" else if (editorMode == "css") "language-css3" else if (editorMode == "js") "language-javascript" else "file-document-outline")
            setIconSize(25f)
            setTextColor(textMain)
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(48)).apply { rightMargin = dp(8) }
        })
        editorNameLabel = TextView(this).apply {
            text = editorFile?.name ?: if (editorExternalMode != null) editorDefaultName(editorMode) else "Tanpa judul"
            textSize = 15f
            setTextColor(textMain)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        fileCard.addView(editorNameLabel)
        fileCard.addView(TextView(this).apply {
            text = "✎"
            textSize = 22f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(48))
            setOnClickListener { renameEditorFile() }
        })
        header.addView(fileCard)

        // Hamburger/project-tree sengaja tidak ditampilkan di workspace editor.
        // Menu drawer (homeMenu) tetap tersedia di Beranda saja.

        val console = TextView(this).apply {
            text = "›_"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(textMain)
            background = bg(if (isDarkTheme) panel2 else Color.rgb(242,244,246), 16, Color.TRANSPARENT)
            contentDescription = "Console"
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { leftMargin = dp(5) }
            setOnClickListener { showEditorConsole() }
        }
        header.addView(console)

        val add = TextView(this).apply {
            text = "+"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = bg(Color.rgb(16,16,16), 16, Color.TRANSPARENT)
            contentDescription = "New file"
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(44)).apply { leftMargin = dp(5) }
            elevation = dp(3).toFloat()
            setOnClickListener { showEditorModePicker() }
        }
        header.addView(add)
        content.addView(header, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(5) })

        // File tabs: compact, horizontally scrollable, and visually closer to a mobile IDE.
        val tabsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dp(4))
        }
        val modeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tabModes = listOf("HTML" to "html", "CSS" to "css", "JS" to "js")
        if (editorMode !in tabModes.map { it.second }) {
            tabModes.plus(editorModeName(editorMode) to editorMode).forEach { (labelText, mode) ->
                modeBar.addView(editorTab(labelText, mode), LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(5) })
            }
        } else {
            tabModes.forEach { (labelText, mode) ->
                modeBar.addView(editorTab(labelText, mode), LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(5) })
            }
        }
        tabsScroll.addView(modeBar)
        content.addView(tabsScroll, LinearLayout.LayoutParams(-1, dp(42)))

        val work = edit(when (editorMode) {
            "html" -> "Ketik HTML...   ! + Tab/Enter = Emmet"
            "css" -> "Ketik CSS..."
            "js" -> "Ketik JavaScript..."
            "json" -> "Ketik JSON di sini..."
            "csv" -> "Ketik data CSV di sini..."
            "base64" -> "Masukkan teks atau Base64..."
            else -> "Ketik teks atau kode di sini..."
        }, true).apply {
            minLines = 1
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { bottomMargin = dp(3) }
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = bg(Color.WHITE, 18, Color.rgb(225, 225, 225))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        editorBox = work

        // Quick actions: copy/share + search & replace.
        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun quickButton(textValue: String, click: () -> Unit): TextView = TextView(this).apply {
            text = textValue; textSize = 11f; gravity = Gravity.CENTER; setTextColor(textMain)
            background = bg(if (isDarkTheme) panel2 else Color.rgb(242,244,246), 12, line)
            isClickable = true; setOnClickListener { click() }
        }
        quick.addView(quickButton("Copy") { val clip = android.content.ClipData.newPlainText("Editor", work.text.toString()); (getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip); toast("Teks disalin") }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(3) })
        quick.addView(quickButton("Share") { shareText(work.text.toString()) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        quick.addView(quickButton("Find") { showEditorSearchDialog(work, false) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        quick.addView(quickButton("Replace") { showEditorSearchDialog(work, true) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        quick.addView(quickButton("Cek") { showEditorDiagnostics() }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(3) })
        content.addView(quick, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(4) })

        // Nomor baris tetap sinkron dengan jumlah baris teks.
        val lineNumbers = TextView(this).apply {
            text = "1"; textSize = 12f; typeface = android.graphics.Typeface.MONOSPACE; gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setTextColor(textMuted); setPadding(dp(4), dp(18), dp(4), dp(18)); background = bg(if (isDarkTheme) panel2 else Color.rgb(246,247,248), 18, Color.TRANSPARENT)
        }
        val externalText = editorExternalTarget?.text?.toString()
        when {
            editorFile != null -> work.setText(runCatching { editorFile?.readText().orEmpty() }.getOrDefault(""))
            externalText != null -> work.setText(externalText)
        }
        val initialText = work.text.toString()
        lineNumbers.text = (1..(initialText.count { it == '\n' } + 1)).joinToString("\n")
        val workRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
        workRow.addView(lineNumbers, LinearLayout.LayoutParams(dp(38), -1).apply { rightMargin = dp(4) })
        workRow.addView(work, LinearLayout.LayoutParams(0, -1, 1f))
        content.addView(workRow, LinearLayout.LayoutParams(-1, 0, 1f).apply { bottomMargin = dp(3) })

        editorStatusLabel = TextView(this).apply {
            text = "Baris 1, Kolom 1  |  ${work.text.length} karakter"
            textSize = 11f
            setTextColor(textMuted)
            setPadding(dp(4), dp(3), dp(4), dp(3))
        }
        content.addView(editorStatusLabel)
        work.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                val txt = s?.toString().orEmpty()
                val lines = txt.count { it == '\n' } + 1
                lineNumbers.text = (1..lines).joinToString("\n")
                editorStatusLabel?.text = "Baris $lines, Kolom ${txt.substringAfterLast('\n').length + 1}  |  ${txt.length} karakter${EditorDiagnostics.summaryTail()}"
            }
            override fun afterTextChanged(e: android.text.Editable?) {}
        })
        // Pewarnaan sintaks (HTML / CSS / JS / JSON) ala VS Code.
        EditorSyntax.attach(work, editorMode)
        // Step 19: deteksi error (garis bawah merah/kuning) untuk JS / HTML / CSS / JSON.
        EditorDiagnostics.attach(work, { editorMode }) { refreshEditorStatus(work) }
        work.setTextColor(android.graphics.Color.parseColor("#24292F"))

        // Tool-specific actions tetap bisa dipanggil dari toolbar bawah, tetapi daftar
        // mode/file tambahan tidak lagi memenuhi area editor.
        editorContextActions = null
        renderEditorBottomBar(work)
        animateEditorScreen()
    }
internal fun MainActivity.showEditorSearchDialog(work: EditText, replaceMode: Boolean) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), 0) }
        val find = edit("Cari teks")
        val start = work.selectionStart.coerceAtLeast(0).coerceAtMost(work.length())
        val end = work.selectionEnd.coerceAtLeast(0).coerceAtMost(work.length())
        if (start < end) find.setText(work.text.subSequence(start, end).toString())
        box.addView(find)
        val replace = if (replaceMode) edit("Ganti dengan") else null
        replace?.let { box.addView(it) }
        AlertDialog.Builder(this)
            .setTitle(if (replaceMode) "Cari & Ganti" else "Cari")
            .setView(box)
            .setNegativeButton("Batal", null)
            .setPositiveButton(if (replaceMode) "Ganti Semua" else "Cari") { _, _ ->
                val q = find.text.toString()
                if (q.isBlank()) { toast("Teks pencarian kosong"); return@setPositiveButton }
                val source = work.text.toString()
                if (replaceMode) {
                    val r = replace?.text?.toString().orEmpty()
                    val result = source.replace(q, r)
                    work.setText(result)
                    work.setSelection(result.length)
                    toast("${source.split(q).size - 1} kemunculan diganti")
                } else {
                    val index = source.indexOf(q, work.selectionStart.coerceAtLeast(0))
                    if (index >= 0) { work.requestFocus(); work.setSelection(index, index + q.length) } else toast("Teks tidak ditemukan")
                }
            }.show()
    }
internal fun MainActivity.editorTab(labelText: String, mode: String): View = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), 0, dp(10), 0)
        background = bg(
            if (editorMode == mode) (if (isDarkTheme) panel2 else Color.rgb(232,236,240)) else Color.TRANSPARENT,
            12,
            if (editorMode == mode) (if (isDarkTheme) line else Color.rgb(215,220,224)) else Color.TRANSPARENT
        )
        isClickable = true
        isFocusable = true
        contentDescription = "Buka tab $labelText"
        setOnClickListener {
            if (editorMode != mode) {
                editorExternalTarget = null
                editorExternalMode = mode
                editor(null, mode)
            }
        }
        addView(MdiIconView(activity).apply {
            setIconName(when (mode) {
                "html" -> "language-html5"
                "css" -> "language-css3"
                "js" -> "language-javascript"
                else -> "file-document-outline"
            })
            setIconSize(16f)
            setTextColor(if (editorMode == mode) textMain else textMuted)
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(24)).apply { rightMargin = dp(5) }
        })
        addView(TextView(activity).apply {
            text = labelText
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(if (editorMode == mode) textMain else textMuted)
            includeFontPadding = false
        })
    }

}

internal fun MainActivity.showEditorProjectTree() {
        val root = editorFile?.parentFile ?: prefs.getString("last_workspace", null)?.let { File(it) }
        val files = root?.listFiles()?.filter { it.isFile && it.name != "workspace.json" }?.sortedBy { it.name.lowercase(Locale.getDefault()) } ?: emptyList()
        if (files.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Project Files")
                .setMessage("Belum ada file di workspace ini. Buat file baru dari tombol + di editor.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val names = files.map { if (it == editorFile) "✓  ${it.name}" else it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Project Files • ${files.size}")
            .setItems(names) { _, which -> editor(files[which]) }
            .setNegativeButton("Tutup", null)
            .show()
    }
internal fun MainActivity.appendEditorConsole(line: String) {
        val stamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
        synchronized(editorConsoleLog) {
            if (editorConsoleLog.length > 120_000) {
                editorConsoleLog.delete(0, editorConsoleLog.length - 80_000)
            }
            editorConsoleLog.append('[').append(stamp).append("] ").append(line).append('\n')
        }
        runOnUiThread {
            editorConsoleView?.text = synchronized(editorConsoleLog) { editorConsoleLog.toString() }
        }
    }
internal fun MainActivity.clearEditorConsole() {
        synchronized(editorConsoleLog) { editorConsoleLog.setLength(0) }
        editorConsoleView?.text = "Console kosong. Jalankan Preview untuk menangkap console.log / error JS.\n"
    }
internal fun MainActivity.showEditorConsole() {
        clearPage("Console")
        content.setPadding(dp(12), dp(8), dp(12), dp(16))
        content.addView(label("Console", 22f, true))
        content.addView(subLabel("Menampilkan log JavaScript (console.log, warn, error) dari Preview, plus status editor.", 12f))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(button("Clear") { clearEditorConsole() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(6) })
        row.addView(button("Salin") {
            val t = synchronized(editorConsoleLog) { editorConsoleLog.toString() }
            if (t.isBlank()) toast("Console kosong") else copyText(t)
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        row.addView(button("Preview+Log") {
            val src = editorBox?.text?.toString().orEmpty()
            if (src.isBlank()) toast("Editor kosong") else {
                appendEditorConsole("Menjalankan Preview…")
                previewUnifiedEditor(editorBox ?: return@button)
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(6) })
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        if (editorMode == "js") {
            val jsInput = edit("Jalankan ekspresi JS (opsional)", true)
            content.addView(jsInput)
            content.addView(button("Eval JS") {
                val code = jsInput.text.toString().trim()
                if (code.isBlank()) { toast("Ketik kode JS"); return@button }
                appendEditorConsole("> $code")
                val encoded = android.util.Base64.encodeToString(code.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
                val wrapped = """
                    <!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>
                    <script>
                    (function(){
                      try {
                        var src = atob("$encoded");
                        var __r = (0, eval)(src);
                        if (typeof __r !== "undefined") console.log(String(__r));
                        else console.log("(undefined)");
                      } catch(e) {
                        console.error(e && e.stack ? e.stack : e);
                      }
                    })();
                    </script>
                    <pre>Eval selesai — buka kembali Console untuk melihat log</pre>
                    </body></html>
                """.trimIndent()
                previewHtmlText(wrapped, "HTML")
            }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(6); bottomMargin = dp(10) })
        }

        val logView = TextView(this).apply {
            text = synchronized(editorConsoleLog) {
                if (editorConsoleLog.isEmpty()) {
                    "Console kosong.\n• Tekan Preview di editor untuk menangkap console.log\n• Error JS akan muncul di sini\n• Mode JS: gunakan Eval JS di atas"
                } else editorConsoleLog.toString()
            }
            textSize = 12f
            setTextColor(if (isDarkTheme) Color.rgb(200, 220, 200) else Color.rgb(30, 40, 30))
            setTypeface(android.graphics.Typeface.MONOSPACE)
            setTextIsSelectable(true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = bg(if (isDarkTheme) Color.rgb(18, 20, 22) else Color.rgb(248, 250, 248), 14, line)
            minimumHeight = dp(280)
        }
        editorConsoleView = logView
        content.addView(logView, LinearLayout.LayoutParams(-1, -2))

        content.addView(subLabel("Kembali dengan tombol ‹ di toolbar. Hamburger menu hanya di Beranda.", 11f).apply {
            setPadding(0, dp(12), 0, 0)
        })
    }
internal fun MainActivity.renameEditorFile() {
        val input = edit("Nama file")
        input.setText(editorNameLabel?.text?.toString()?.removePrefix("Tanpa judul") ?: editorDefaultName(editorMode))
        AlertDialog.Builder(this).setTitle("Nama file").setView(input)
            .setNegativeButton("Batal", null)
            .setPositiveButton("OK") { _, _ ->
                val n = safeFileName(input.text.toString())
                editorNameLabel?.text = n
                val currentFile = editorFile
                if (currentFile != null && currentFile.name != n) {
                    val next = safeChildFile(currentFile.parentFile ?: filesDir, n)
                    if (next != null) runCatching { currentFile.renameTo(next); editorFile = next }
                }
            }.show()
    }
internal fun MainActivity.renderEditorBottomBar(work: EditText) {
        val bar = editorBottomBar
        bar.removeAllViews()
        bar.visibility = View.VISIBLE
        bar.alpha = 1f
        bar.translationY = 0f
        val actions = listOf(
            "file-plus-outline" to ("Baru" to { showEditorCreateFilePicker() }),
            "folder-open-outline" to ("Buka" to { pickFileForEditor() }),
            "content-save-outline" to ("Simpan" to { saveEditorCurrent() }),
            "undo" to ("Undo" to { work.undoSafe() }),
            "redo" to ("Redo" to { work.redoSafe() }),
            "magnify" to ("Cari" to { showEditorFindDialog(false) }),
            "web" to ("Preview" to { previewUnifiedEditor(work) }),
            "code-tags" to ("Emmet" to { applySimpleEmmet(work) }),
            "select-all" to ("Pilih" to { work.selectAll() })
        )
        actions.forEach { (iconName, item) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener { animateEditorPress(it); it.postDelayed({ item.second() }, 40L) }
                layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
                setPadding(dp(1), dp(3), dp(1), dp(3))
            }
            cell.addView(MdiIconView(this).apply {
                setIconName(iconName)
                setIconSize(21f)
                setTextColor(textMain)
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(27))
            })
            cell.addView(TextView(this).apply {
                text = item.first
                textSize = 10f
                setTextColor(textMain)
                gravity = Gravity.CENTER
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(-2, dp(18))
            })
            bar.addView(cell)
        }
    }
internal fun MainActivity.previewUnifiedEditor(work: EditText) {
        // Preview adalah mode tampilan penuh: toolbar aksi (Baru, Buka, Simpan,
        // Undo, Redo, Cari, Preview, Emmet, Pilih) hanya diperlukan saat mengedit.
        // Sembunyikan sebelum halaman Preview dibuat agar tidak tetap menempel di bawah.
        editorBottomBar.visibility = View.GONE
        val text = work.text.toString()
        when (editorMode) {
            "html" -> previewHtmlText(text, "HTML")
            "css" -> previewHtmlText("<style>${text.htmlEsc()}</style><body><h3>CSS Preview</h3><p>Gunakan HTML untuk melihat hasil styling secara langsung.</p></body>", "HTML")
            "js" -> previewHtmlText("<script>${text}</script><body><h3>JavaScript Preview</h3></body>", "HTML")
            "json" -> if (LottieJson.looksLikeLottie(text)) showLottiePreview(text) else output(text)
            else -> output(text)
        }
    }
internal fun MainActivity.setEditorMode(mode: String) {
        val currentFile = editorFile
        editorExternalTarget = null
        editorExternalMode = null
        editor(currentFile, mode)
    }
internal fun MainActivity.refreshEditorContextActions() {
        val row = editorContextActions ?: return
        row.removeAllViews()
        val actions: List<Pair<String, () -> Unit>> = when (editorMode) {
            "json" -> listOf(
                "✦\nFormat" to { transformEditorJson(true) },
                "ϟ\nMinify" to { transformEditorJson(false) },
                "✓\nValidasi" to { validateEditorJson() },
                "▦\nKe CSV" to { jsonToCsvEditor() },
                "▱\nBuka" to { pickFileForEditor() }
            )
            "csv" -> listOf(
                "✦\nRapikan" to { normalizeCsvEditor() },
                "{}\nKe JSON" to { csvToJsonEditor() },
                "▦\nTabel" to { showCsvInfo() },
                "▱\nBuka" to { pickFileForEditor() },
                "▣\nSimpan" to { saveEditorCurrent() }
            )
            "base64" -> listOf(
                "↑\nEncode" to { encodeBase64Editor() },
                "↓\nDecode" to { decodeBase64Editor() },
                "⌫\nBersihkan" to { editorBox?.setText("") },
                "▣\nSimpan" to { saveEditorCurrent() },
                "▱\nBuka" to { pickFileForEditor() }
            )
            "utility" -> listOf(
                "Aa\nCase" to { showEditorCaseDialog() },
                "64\nBase64" to { showEditorBase64Dialog() },
                "#\nHitung" to { showTextCount() },
                "⌕\nCari" to { showEditorFindDialog(false) },
                "↔\nGanti" to { showEditorFindDialog(true) }
            )
            "code" -> listOf(
                "⌕\nCari" to { showEditorFindDialog(false) },
                "↔\nGanti" to { showEditorFindDialog(true) },
                "Aa\nCase" to { showEditorCaseDialog() },
                "64\nBase64" to { showEditorBase64Dialog() },
                "▱\nBuka" to { pickFileForEditor() }
            )
            "config" -> listOf(
                "≡\nFormat" to { formatConfigEditor() },
                "⌕\nCari" to { showEditorFindDialog(false) },
                "↔\nGanti" to { showEditorFindDialog(true) },
                "↺\nReset" to { editorBox?.setText("") },
                "▣\nSimpan" to { saveEditorCurrent() }
            )
            "xml" -> listOf(
                "✓\nValidasi" to { validateXmlEditor() },
                "≡\nFormat" to { formatXmlEditor() },
                "⌕\nCari" to { showEditorFindDialog(false) },
                "↔\nGanti" to { showEditorFindDialog(true) },
                "▣\nSimpan" to { saveEditorCurrent() }
            )
            else -> listOf(
                "▱\nBuka" to { pickFileForEditor() },
                "▣\nSimpan" to { saveEditorCurrent() },
                "Aa\nCase" to { showEditorCaseDialog() },
                "64\nBase64" to { showEditorBase64Dialog() },
                "▢\nCopy" to { copyEditorText() }
            )
        }
        actions.forEach { (txt, click) ->
            val parts = txt.split("\n")
            val v = TextView(this).apply {
                text = "${parts[0]}\n${parts[1]}"
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(textMain)
                background = bg(panel2, 13, line)
                setOnClickListener { click() }
                layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) }
            }
            row.addView(v)
        }
    }
internal fun MainActivity.copyEditorText() {
        val clip = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clip.setPrimaryClip(android.content.ClipData.newPlainText("Editor", editorBox?.text?.toString().orEmpty()))
        toast("Teks disalin")
    }
internal fun MainActivity.selectedOrAllText(): String {
        val box = editorBox ?: return ""
        val a = minOf(box.selectionStart, box.selectionEnd)
        val b = maxOf(box.selectionStart, box.selectionEnd)
        return if (a != b) box.text.substring(a, b) else box.text.toString()
    }
internal fun MainActivity.replaceSelectedOrAll(value: String) {
        val box = editorBox ?: return
        val a = minOf(box.selectionStart, box.selectionEnd)
        val b = maxOf(box.selectionStart, box.selectionEnd)
        if (a != b) {
            box.text.replace(a, b, value)
            box.setSelection(a + value.length)
        } else {
            box.setText(value)
            box.setSelection(box.length())
        }
    }
internal fun MainActivity.showEditorCaseDialog() {
        val items = arrayOf("UPPERCASE", "lowercase", "Title Case", "Slug / URL", "Hitung kata & karakter")
        AlertDialog.Builder(this).setTitle("Text Case & Utility").setItems(items) { _, which ->
            when (which) {
                0 -> replaceSelectedOrAll(selectedOrAllText().uppercase(Locale.getDefault()))
                1 -> replaceSelectedOrAll(selectedOrAllText().lowercase(Locale.getDefault()))
                2 -> replaceSelectedOrAll(selectedOrAllText().lowercase(Locale.getDefault()).split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ") { word -> if (word.isEmpty()) word else word.substring(0, 1).uppercase(Locale.getDefault()) + word.substring(1) })
                3 -> replaceSelectedOrAll(selectedOrAllText().trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "-").trim('-'))
                4 -> showTextCount()
            }
        }.setNegativeButton("Batal", null).show()
    }
internal fun MainActivity.showTextCount() {
        val s = selectedOrAllText()
        val words = s.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }
        toast("$words kata • ${s.length} karakter")
    }
internal fun MainActivity.showEditorBase64Dialog() {
        AlertDialog.Builder(this).setTitle("Base64").setItems(arrayOf("Encode", "Decode")) { _, which ->
            if (which == 0) encodeBase64Editor() else decodeBase64Editor()
        }.setNegativeButton("Batal", null).show()
    }
internal fun MainActivity.encodeBase64Editor() {
        val bytes = selectedOrAllText().toByteArray(StandardCharsets.UTF_8)
        replaceSelectedOrAll(android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
    }
internal fun MainActivity.decodeBase64Editor() {
        runCatching { String(Base64.getDecoder().decode(selectedOrAllText().trim()), StandardCharsets.UTF_8) }
            .onSuccess { replaceSelectedOrAll(it) }
            .onFailure { toast("Base64 tidak valid") }
    }
internal fun MainActivity.normalizeCsvEditor() {
        val lines = editorBox?.text?.toString()?.lines()?.filter { it.isNotBlank() }.orEmpty()
        if (lines.isEmpty()) return
        val delim = if (lines.first().count { it == ';' } > lines.first().count { it == ',' }) ';' else ','
        val out = lines.joinToString("\n") { csvParseLine(it, delim).joinToString(",") { cell -> csvEscape(cell.trim()) } }
        editorBox?.setText(out)
        editorBox?.setSelection(editorBox?.length() ?: 0)
    }
internal fun MainActivity.csvParseLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>(); val cur = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (quoted && i + 1 < line.length && line[i + 1] == '"') { cur.append('"'); i++ } else quoted = !quoted
            } else if (c == delimiter && !quoted) { out.add(cur.toString()); cur.setLength(0) } else cur.append(c)
            i++
        }
        out.add(cur.toString()); return out
    }
internal fun MainActivity.csvEscape(s: String): String = if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"${s.replace("\"", "\"\"")}\"" else s

internal fun MainActivity.csvToJsonEditor() {
        val lines = editorBox?.text?.toString()?.lines()?.filter { it.isNotBlank() }.orEmpty()
        if (lines.size < 1) return
        val delimiter = if (lines.first().count { it == ';' } > lines.first().count { it == ',' }) ';' else ','
        val headers = csvParseLine(lines.first(), delimiter)
        val arr = JSONArray()
        lines.drop(1).forEach { line ->
            val cells = csvParseLine(line, delimiter); val obj = JSONObject()
            headers.forEachIndexed { i, h -> obj.put(h.trim(), cells.getOrElse(i) { "" }) }
            arr.put(obj)
        }
        editorBox?.setText(prettyJson(arr.toString()))
        editorBox?.setSelection(editorBox?.length() ?: 0)
    }
internal fun MainActivity.jsonToCsvEditor() {
        val s = editorBox?.text?.toString()?.trim().orEmpty()
        runCatching {
            val arr = if (s.startsWith("[")) JSONArray(s) else JSONArray().put(JSONObject(s))
            if (arr.length() == 0) return@runCatching ""
            val keys = linkedSetOf<String>()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.keys()?.forEach { keys.add(it) }
            val header = keys.joinToString(",") { csvEscape(it) }
            val rows = (0 until arr.length()).map { i ->
                val o = arr.optJSONObject(i) ?: JSONObject()
                keys.joinToString(",") { k -> csvEscape(o.opt(k)?.toString() ?: "") }
            }
            (listOf(header) + rows).joinToString("\n")
        }.onSuccess { editorBox?.setText(it); editorBox?.setSelection(editorBox?.length() ?: 0) }
            .onFailure { toast("JSON tidak valid: ${it.message}") }
    }
internal fun MainActivity.showCsvInfo() {
        val lines = editorBox?.text?.toString()?.lines()?.filter { it.isNotBlank() }.orEmpty()
        val delimiter = lines.firstOrNull()?.let { if (it.count { c -> c == ';' } > it.count { c -> c == ',' }) ';' else ',' } ?: ','
        val cols = lines.firstOrNull()?.let { csvParseLine(it, delimiter).size } ?: 0
        toast("${lines.size} baris • $cols kolom")
    }
internal fun MainActivity.saveEditorCurrent() {
        val box = editorBox ?: return
        editorExternalTarget?.let {
            it.setText(box.text.toString())
            toast("Diterapkan ke ${editorMode.uppercase(Locale.getDefault())}")
            return
        }

        // Tombol Simpan harus benar-benar bisa membuat file di penyimpanan HP.
        // Jika file sudah dibuka dari Storage Access Framework, timpa file aslinya.
        // Jika belum punya URI, langsung tampilkan pemilih lokasi/nama file Android.
        if (editorSourceUri != null) {
            saveEditorQuick()
        } else {
            saveEditorAs()
        }
    }
internal fun MainActivity.saveEditorQuick() {
        val box = editorBox ?: return
        val src = editorSourceUri
        if (src != null) {
            runCatching {
                contentResolver.openOutputStream(src, "wt")?.use { it.write(box.text.toString().toByteArray(StandardCharsets.UTF_8)) }
                    ?: error("Tidak bisa menulis file")
            }.onSuccess { toast("Tersimpan: ${queryName(src) ?: "file"}") }
                .onFailure { toast("Gagal menimpa file asli: ${it.message}. Coba Simpan sebagai.") }
            return
        }
        saveEditorInternal()
    }
internal fun MainActivity.saveEditorAs() {
        val box = editorBox ?: return
        editorSaveAsText = box.text.toString()
        val name = safeFileName((editorNameLabel?.text?.toString() ?: "").trim().ifEmpty { editorDefaultName(editorMode) })
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "text/plain"
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, name)
        }, 1031)
    }
internal fun MainActivity.saveEditorInternal() {
        val box = editorBox ?: return
        val name = safeFileName((editorNameLabel?.text?.toString() ?: "").trim().ifEmpty { editorDefaultName(editorMode) })
        val target = editorFile ?: safeChildFile(filesDir, name)
        if (target == null) { toast("Nama file tidak valid"); return }
        runCatching {
            target.parentFile?.mkdirs()
            target.writeText(box.text.toString())
            editorFile = target
            editorNameLabel?.text = target.name
        }.onSuccess { toast("Tersimpan: ${target.name}") }
            .onFailure { toast("Gagal menyimpan: ${it.message}") }
    }
internal fun MainActivity.validateEditorJson() {
        val s = editorBox?.text?.toString()?.trim().orEmpty()
        val result = runCatching {
            if (s.startsWith("{")) JSONObject(s) else if (s.startsWith("[")) JSONArray(s) else error("JSON harus dimulai dengan { atau [")
            "JSON valid"
        }.getOrElse { "JSON tidak valid: ${it.message}" }
        toast(result)
    }
internal fun MainActivity.refreshEditorStatus(work: EditText) {
        val txt = work.text.toString()
        val lines = txt.count { it == '\n' } + 1
        editorStatusLabel?.text = "Baris $lines, Kolom ${txt.substringAfterLast('\n').length + 1}  |  ${txt.length} karakter${EditorDiagnostics.summaryTail()}"
    }
internal fun MainActivity.showEditorDiagnostics() {
        val box = editorBox
        if (box == null) { toast("Buka file di Text Editor dulu"); return }
        if (!CodeDiagnostics.supports(editorMode)) {
            toast("Pemeriksaan error tersedia untuk JS, HTML, CSS, dan JSON")
            return
        }
        val issues = EditorDiagnostics.analyzeNow(box.text.toString(), editorMode)
        refreshEditorStatus(box)
        if (issues.isEmpty()) { toast("✓ Tidak ada error terdeteksi"); return }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        var dialog: AlertDialog? = null
        issues.take(50).forEach { issue ->
            list.addView(TextView(this).apply {
                val mark = if (issue.severity == CodeDiagnostics.Severity.ERROR) "✖" else "⚠"
                text = "$mark Baris ${issue.line}:${issue.column} — ${issue.message}" +
                    (issue.fix?.let { "\n   ↳ ${it.label}" } ?: "")
                textSize = 13f
                setTextColor(textMain)
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    jumpToEditorIssue(box, issue)
                    dialog?.dismiss()
                }
            })
        }
        val fixable = issues.count { it.fix != null }
        val builder = AlertDialog.Builder(this)
            .setTitle("${issues.size} masalah ditemukan")
            .setView(android.widget.ScrollView(this).apply { addView(list) })
            .setNegativeButton("Tutup", null)
        if (fixable > 0) builder.setPositiveButton("Perbaiki ($fixable)") { _, _ -> showEditorFixPreview() }
        dialog = builder.show()
    }
internal fun MainActivity.jumpToEditorIssue(box: EditText, issue: CodeDiagnostics.Issue) {
        val len = box.text.length
        val s = issue.start.coerceIn(0, len)
        box.requestFocus()
        box.setSelection(s, issue.end.coerceIn(s, len))
    }
internal fun MainActivity.showEditorFixPreview() {
        val box = editorBox
        if (box == null) { toast("Buka file di Text Editor dulu"); return }
        if (!isEditorWorkspace() && editorFile == null) {
            toast("Buka Text Editor dulu untuk menerapkan perbaikan")
            return
        }
        val original = box.text.toString()
        val issues = EditorDiagnostics.analyzeNow(original, editorMode)
        val fixable = issues.filter { it.fix != null }
        if (fixable.isEmpty()) { toast("Tidak ada perbaikan otomatis yang tersedia"); return }
        val sb = StringBuilder()
        fixable.take(30).forEach { issue ->
            sb.append("Baris ${issue.line} — ${issue.fix?.label}\n")
            CodeDiagnostics.previewOf(original, issue)?.let { (before, after) ->
                sb.append("  − $before\n  + $after\n")
            }
            sb.append('\n')
        }
        if (fixable.size > 30) sb.append("…dan ${fixable.size - 30} perbaikan lainnya.\n")
        val tv = TextView(this).apply {
            text = sb.toString().trimEnd()
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(textMain)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Pratinjau ${fixable.size} perbaikan")
            .setView(android.widget.ScrollView(this).apply { addView(tv) })
            .setPositiveButton("Terapkan") { _, _ -> applyEditorFixes(box, issues) }
            .setNegativeButton("Batal", null)
            .show()
    }
internal fun MainActivity.applyEditorFixes(box: EditText, issues: List<CodeDiagnostics.Issue>) {
        val original = box.text.toString()
        val (fixed, _) = CodeDiagnostics.applyAllFixes(original, issues)
        if (fixed == original) { toast("Tidak ada perubahan"); return }
        val caret = box.selectionStart.coerceAtLeast(0)
        box.text.replace(0, box.text.length, fixed)
        box.setSelection(caret.coerceAtMost(box.text.length))
        // Dari layar selain editor (mis. chat Bit), simpan ke file agar tidak hilang saat editor dibuka ulang.
        var saved = ""
        if (!isEditorWorkspace()) {
            editorFile?.let { f -> if (runCatching { f.writeText(fixed) }.isSuccess) saved = " dan disimpan ke ${f.name}" }
        }
        val remaining = EditorDiagnostics.analyzeNow(fixed, editorMode)
        refreshEditorStatus(box)
        BitRuntimeContext.onToolResult("fixed", "Perbaikan diterapkan, sisa ${remaining.size} masalah")
        toast(if (remaining.isEmpty()) "Perbaikan diterapkan$saved ✓" else "Diterapkan$saved; ${remaining.size} masalah perlu dicek manual")
    }
internal fun MainActivity.transformEditorJson(pretty: Boolean) {
        val box = editorBox ?: return
        runCatching {
            box.setText(if (pretty) prettyJson(box.text.toString()) else minifyJson(box.text.toString()))
            box.setSelection(box.length())
        }.onFailure { toast("JSON tidak valid: ${it.message}") }
    }
internal fun MainActivity.showEditorFindDialog(replace: Boolean) {
        val find = edit("Cari")
        val repl = if (replace) edit("Ganti dengan") else null
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        box.addView(find); repl?.let { box.addView(it) }
        AlertDialog.Builder(this).setTitle(if (replace) "Cari & Ganti" else "Cari")
            .setView(box)
            .setNegativeButton("Batal", null)
            .setPositiveButton(if (replace) "Ganti" else "Cari") { _, _ ->
                val source = editorBox?.text?.toString().orEmpty()
                val q = find.text.toString()
                if (q.isEmpty()) { toast("Teks pencarian kosong"); return@setPositiveButton }
                if (replace) editorBox?.setText(source.replace(q, repl?.text?.toString().orEmpty()))
                else toast(if (source.contains(q)) "Ditemukan" else "Tidak ditemukan")
            }.show()
    }
internal fun MainActivity.formatConfigEditor() {
        val box = editorBox ?: return
        val out = box.text.toString().lines().joinToString("\n") { line ->
            line.trim().replace(Regex("\\s*=\\s*"), " = ")
        }.trim()
        box.setText(out)
    }
internal fun MainActivity.validateXmlEditor() {
        val s = editorBox?.text?.toString().orEmpty()
        runCatching {
            val f = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            f.newDocumentBuilder().parse(org.xml.sax.InputSource(StringReader(s)))
            "XML valid"
        }.onSuccess { toast(it) }.onFailure { toast("XML tidak valid: ${it.message}") }
    }
internal fun MainActivity.formatXmlEditor() {
        val box = editorBox ?: return
        runCatching {
            val f = javax.xml.transform.TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            }
            val sw = StringWriter()
            f.transform(javax.xml.transform.stream.StreamSource(StringReader(box.text.toString())), javax.xml.transform.stream.StreamResult(sw))
            box.setText(sw.toString())
        }.onFailure { toast("XML tidak valid: ${it.message}") }
    }
internal fun EditText.undoSafe() {
        runCatching {
            val m = java.lang.reflect.Method::class
            val field = EditText::class.java.getDeclaredField("mEditor")
            field.isAccessible = true
            val editorObj = field.get(this)
            val undo = editorObj.javaClass.getMethod("undo")
            undo.invoke(editorObj)
        }.onFailure { Toast.makeText(context, "Undo tidak tersedia pada perangkat ini", Toast.LENGTH_SHORT).show() }
    }
internal fun EditText.redoSafe() {
        runCatching {
            val field = EditText::class.java.getDeclaredField("mEditor")
            field.isAccessible = true
            val editorObj = field.get(this)
            val redo = editorObj.javaClass.getMethod("redo")
            redo.invoke(editorObj)
        }.onFailure { Toast.makeText(context, "Redo tidak tersedia pada perangkat ini", Toast.LENGTH_SHORT).show() }
    }
internal fun MainActivity.shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "*/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Bagikan file"))
    }
