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


internal fun MainActivity.apkInspector() {
        clearPage("APK Inspector")
        content.addView(button("Pilih APK") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type="application/vnd.android.package-archive"; addCategory(Intent.CATEGORY_OPENABLE)
            },1002)
        })
        content.addView(label("Menampilkan daftar isi APK/ZIP. Parsing AndroidManifest binary XML penuh memerlukan parser tambahan."))
    }
internal fun MainActivity.inspectZipOrApk(uri: Uri) {
        toolThread {
            val sb=StringBuilder()
            contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zis ->
                    var count=0
                    while(true) {
                        val e=zis.nextEntry ?: break
                        sb.append(e.name).append('\n')
                        if(++count>=300) { sb.append("..."); break }
                    }
                }
            }
            runOnUiThread { clearPage("APK Inspector"); output(sb.toString()) }
        }
    }
internal fun MainActivity.qrSourceLabel(id: String) = when (id) { "file" -> "File"; "foto" -> "Foto & Scan"; else -> "Teks / Link" }

internal fun MainActivity.qrSourceIcon(id: String) = when (id) { "file" -> "▤"; "foto" -> "▧"; else -> "✎" }

internal fun MainActivity.qrTool() {
        clearPage("QR Scanner")
        qrSourceExpanded = false
        qrSelectedSource = null
        qrPickedUri = null
        qrPickedName = null
        qrScanBusy = false
        qrScanResult = null
        renderQrScanner()
    }
internal fun MainActivity.renderQrScanner() {
        content.removeAllViews()
        // Tombol kanan atas khusus untuk langsung membuka pemindai QR kamera.
        action.text = "⌗"
        action.textSize = 21f
        action.contentDescription = "Scan QR"
        action.setOnClickListener { scanQrWithCamera() }
        if (qrSelectedSource == null) content.addView(qrHeroBox())
        content.addView(qrSourceSelectorRow())
        if (qrSourceExpanded) {
            content.addView(qrSourceOptionsBox())
        } else if (qrSelectedSource != null) {
            qrSelectedSource?.let { content.addView(qrSourcePanel(it)) }
        }
    }
internal fun MainActivity.qrHeroBox(): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(34), dp(24), dp(30))
        background = bg(panel2, 18)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        addView(TextView(activity).apply {
            text = "⛶"; textSize = 32f; gravity = Gravity.CENTER; setTextColor(textMuted)
        }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(14) })
        addView(label("QR Scanner", 17f, true).apply { gravity = Gravity.CENTER })
        addView(subLabel("Scan kode QR atau buat QR sendiri.", 12f).apply { gravity = Gravity.CENTER })
    }

}

internal fun MainActivity.qrSourceSelectorRow(): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = bg(panel2, 14, line)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = if (qrSourceExpanded) dp(6) else dp(14) }
        isClickable = true
        setOnClickListener { qrSourceExpanded = !qrSourceExpanded; renderQrScanner() }
        addView(TextView(activity).apply {
            text = if (qrSelectedSource == null) "▦" else qrSourceIcon(qrSelectedSource ?: "")
            textSize = 15f; setTextColor(textMuted)
        }, LinearLayout.LayoutParams(dp(24), -2))
        addView(label(if (qrSelectedSource == null) "Pilih sumber input" else qrSourceLabel(qrSelectedSource ?: ""), 14f).apply {
            setPadding(dp(6), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(activity).apply {
            text = if (qrSourceExpanded) "⌃" else "⌄"; textSize = 13f; setTextColor(textMuted)
        })
    }

}

internal fun MainActivity.qrSourceOptionsBox(): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        background = bg(panel2, 14, line)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        qrSources.forEachIndexed { i, (id, icon, name) ->
            addView(qrOptionRow(id, icon, name, when (id) {
                "file" -> "Pilih file gambar (PNG, JPG, dll)."
                "foto" -> "Ambil foto langsung dari kamera atau galeri."
                else -> "Masukkan teks atau link untuk dibuat QR."
            }))
            if (i != qrSources.lastIndex) addView(View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(dp(10), dp(2), dp(10), dp(2)) }
                setBackgroundColor(line)
            })
        }
    }

}

internal fun MainActivity.qrOptionRow(id: String, icon: String, titleText: String, desc: String): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        isClickable = true
        setOnClickListener {
            qrSelectedSource = id; qrSourceExpanded = false
            qrPickedUri = null; qrPickedName = null; qrScanResult = null; qrScanBusy = false
            renderQrScanner()
        }
        addView(TextView(activity).apply {
            text = icon; textSize = 16f; gravity = Gravity.CENTER; setTextColor(textMain)
            background = bg(Color.rgb(235, 236, 239), 10)
        }, LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(12) })
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(titleText, 14f, true).apply { setPadding(dp(2), 0, dp(2), dp(1)) })
            addView(subLabel(desc, 11f))
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }

}

internal fun MainActivity.qrSourcePanel(source: String): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (source == "teks") {
            val input = edit("Masukkan teks atau link…", true)
            addView(input)
            addView(qrDarkButton("Buat QR") {
                val text = input.text.toString().trim()
                if (text.isEmpty()) { toast("Teks / link tidak boleh kosong"); return@qrDarkButton }
                generateQr(activity, text)?.let { bmp ->
                    addView(qrResultCard(bmp = bmp, resultText = null, onShareText = { text }))
                } ?: toast("Gagal membuat QR")
            })
        } else {
            addView(qrUploadBox(source))
            addView(TextView(activity).apply {
                text = "ⓘ  Format yang didukung: JPG, PNG, WEBP\n    Maksimal ukuran: 10 MB"
                textSize = 11f; setTextColor(textMuted)
                setPadding(dp(2), dp(10), dp(2), dp(4))
            })
            if (qrScanBusy) {
                addView(subLabel("Memindai QR…", 12f))
            } else if (qrPickedUri != null) {
                addView(qrResultCard(bmp = null, resultText = qrScanResult, onShareText = { qrScanResult ?: "" }))
            }
        }
    }

}

internal fun MainActivity.qrUploadBox(source: String): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(20), dp(30), dp(20), dp(26))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(panel2); cornerRadius = dp(16).toFloat()
            setStroke(dp(1), line)
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) }
        if (qrPickedUri != null) {
            addView(ImageView(activity).apply {
                setImageURI(qrPickedUri)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }, LinearLayout.LayoutParams(dp(140), dp(140)).apply { bottomMargin = dp(10) })
            addView(subLabel(qrPickedName ?: "Gambar terpilih", 11f).apply { gravity = Gravity.CENTER })
        } else {
            addView(TextView(activity).apply {
                text = "▧"; textSize = 30f; setTextColor(textMuted); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(10) })
            addView(subLabel(
                if (source == "foto") "Pilih foto atau scan QR dengan kamera" else "Ketuk untuk memilih file gambar",
                12f
            ).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(14)) })
        }
        addView(qrDarkButton(if (source == "foto") "Foto & Scan" else "Pilih File") {
            if (source == "foto") pickQrPhoto() else pickQrFile()
        })
    }

}

internal fun MainActivity.qrDarkButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.WHITE)
        background = bg(Color.rgb(17, 17, 19), 24)
        minHeight = dp(46)
        setPadding(dp(24), dp(2), dp(24), dp(2))
        setStateListAnimator(null)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-2, dp(46)).apply { gravity = Gravity.CENTER; bottomMargin = dp(6) }
    }

internal fun MainActivity.qrResultCard(bmp: Bitmap?, resultText: String?, onShareText: () -> String): LinearLayout = run {
    val activity = this
 LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = bg(panel2, 14, line)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        addView(label(if (bmp != null) "QR berhasil dibuat" else if (resultText != null) "Hasil pindaian" else "Tidak terdeteksi kode QR", 14f, true))
        if (bmp != null) {
            // Preview QR mengikuti referensi: kotak putih untuk QR, teks/link tepat di bawahnya.
            val preview = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = bg(Color.WHITE, 2)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(10); bottomMargin = dp(12)
                }
                addView(ImageView(activity).apply {
                    setImageBitmap(bmp); adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_INSIDE
                }, LinearLayout.LayoutParams(dp(250), dp(250)).apply { gravity = Gravity.CENTER })
                addView(subLabel(onShareText(), 13f).apply {
                    setTextColor(Color.rgb(45, 45, 48)); gravity = Gravity.CENTER
                    setPadding(dp(8), dp(10), dp(8), dp(4))
                })
            }
            addView(preview)
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(button("Download") { saveQrBitmap(bmp) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
            row.addView(button("Bagikan") { shareQrBitmap(bmp, onShareText()) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) })
            addView(row)
        } else if (resultText != null) {
            addView(subLabel(resultText, 13f).apply { setTextColor(textMain); setPadding(dp(2), dp(10), dp(2), dp(10)) })
            addView(qrDarkButton("Bagikan") { shareText(onShareText()) })
        }
    }

}

internal fun MainActivity.saveQrBitmap(bitmap: Bitmap) {
        runCatching {
            val name = "MyTools_QR_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyTools")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Penyimpanan tidak tersedia")
            contentResolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) error("Gagal menulis QR")
            } ?: error("Tidak bisa membuka penyimpanan")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            toast("QR disimpan ke Pictures/MyTools")
        }.onFailure { toast("Download QR gagal: ${it.message}") }
    }
internal fun MainActivity.shareQrBitmap(bitmap: Bitmap, text: String) {
        runCatching {
            val file = File(cacheDir, "MyTools_QR_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Bagikan QR"))
        }.onFailure { toast("Gagal membagikan QR: ${it.message}") }
    }
internal fun MainActivity.generateQr(ctx: Context, text: String): Bitmap? = runCatching {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 600, 600)
        val bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        for (x in 0 until 600) for (y in 0 until 600) bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        bmp
    }.getOrNull()

internal fun MainActivity.scanQrWithCamera() {
        runCatching {
            val photoFile = File(cacheDir, "qr_scan_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            qrCameraOutUri = uri
            qrSelectedSource = "foto"
            qrSourceExpanded = false
            qrPickedUri = null
            qrPickedName = null
            qrScanResult = null
            qrScanBusy = false

            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (camera.resolveActivity(packageManager) == null) {
                toast("Kamera tidak tersedia")
                return
            }
            startActivityForResult(camera, 1043)
        }.onFailure {
            toast("Tidak bisa membuka kamera: ${it.message}")
        }
    }
internal fun MainActivity.pickQrFile() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE)
        }, 1041)
    }
internal fun MainActivity.pickQrPhoto() {
        val gallery = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        val chooser = Intent.createChooser(gallery, "Pilih sumber foto")
        runCatching {
            val photoFile = File(cacheDir, "qr_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            qrCameraOutUri = uri
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            if (camera.resolveActivity(packageManager) != null) {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
            }
        }
        startActivityForResult(chooser, 1042)
    }
internal fun MainActivity.decodeQrFromUri(uri: Uri) {
        qrScanBusy = true; qrPickedUri = uri; qrScanResult = null
        renderQrScanner()
        toolThread {
            val text = runCatching {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                    ?: error("Gambar tidak bisa dibuka")
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Gambar tidak valid")
                var sample = 1
                while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                val opts = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                    ?: error("Gambar tidak bisa dibuka")
                try {
                    val pixels = IntArray(bmp.width * bmp.height)
                    bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    val source = RGBLuminanceSource(bmp.width, bmp.height, pixels)
                    val binary = BinaryBitmap(HybridBinarizer(source))
                    MultiFormatReader().decode(binary).text
                } finally {
                    bmp.recycle()
                }
            }
            runOnUiThread {
                qrScanBusy = false
                qrScanResult = text.getOrNull() ?: run {
                    if (text.exceptionOrNull() !is NotFoundException) toast("Gagal membaca gambar")
                    null
                }
                if (qrScanResult == null) toast("Tidak ditemukan kode QR pada gambar ini")
                renderQrScanner()
            }
        }
    }
internal fun MainActivity.webProjectBuilderTool() {
    val activity = this
        // ---------- WEB PROJECT BUILDER ----------

        fun sectionTitle(textValue: String, icon: String): View = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(12), dp(4), dp(8))
            addView(MdiIconView(activity).apply {
                setIconName(icon); setIconSize(20f); setTextColor(textMain)
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { rightMargin = dp(6) }
            })
            addView(TextView(activity).apply {
                text = textValue; textSize = 13f; setTextColor(textMain)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        }

        val intro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = bg(if (isDarkTheme) panel else Color.rgb(246,248,250), 18, if (isDarkTheme) line else Color.rgb(225,230,234))
        }
        val introRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        introRow.addView(MdiIconView(activity).apply {
            setIconName("web"); setIconSize(30f); setTextColor(textMain)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(10) }
        })
        val introText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        introText.addView(TextView(activity).apply {
            text = "Web Project Builder"; textSize = 17f; setTextColor(textMain)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        introText.addView(TextView(activity).apply {
            text = "HTML + CSS + JavaScript → Build → Preview → Host"
            textSize = 11.5f; setTextColor(textMuted); setPadding(0, dp(3), 0, 0)
        })
        introRow.addView(introText, LinearLayout.LayoutParams(0,-2,1f))
        intro.addView(introRow)
        content.addView(intro, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val name = edit("Nama Project", false).apply { hint = "my-website" }
        content.addView(sectionTitle("PROJECT", "folder-outline"))
        content.addView(name)

        val html = edit("HTML", true)
        val css = edit("CSS", true)
        val js = edit("JavaScript", true)
        val files = listOf(
            Triple("HTML", "language-html5", html),
            Triple("CSS", "language-css3", css),
            Triple("JavaScript", "language-javascript", js)
        )
        files.forEach { (labelText, iconName, editorTarget) ->
            editorTarget.visibility = View.GONE
            val row = settingRowClickable(labelText, "Editor ${labelText.lowercase()}", "Edit source $labelText", iconName) {
                webCodeEditor(labelText, editorTarget)
            }
            content.addView(row, LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(6) })
        }

        content.addView(sectionTitle("IMPORT", "file-import-outline"))
        val importRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 3f }
        listOf("HTML" to WEB_HTML_PICK_REQUEST, "CSS" to WEB_CSS_PICK_REQUEST, "JS" to WEB_JS_PICK_REQUEST).forEach { (labelText, request) ->
            val b = button(labelText) {
                webImportTarget = when (request) {
                    WEB_HTML_PICK_REQUEST -> html
                    WEB_CSS_PICK_REQUEST -> css
                    else -> js
                }
                val type = when (request) {
                    WEB_HTML_PICK_REQUEST -> "text/html"
                    WEB_CSS_PICK_REQUEST -> "text/css"
                    else -> "text/javascript"
                }
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { this.type = type; addCategory(Intent.CATEGORY_OPENABLE) }, request)
            }
            importRow.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
        }
        content.addView(importRow)

        content.addView(sectionTitle("BUILD PIPELINE", "source-branch-check"))
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = bg(if (isDarkTheme) panel2 else Color.WHITE, 16, if (isDarkTheme) line else Color.rgb(225,230,234))
        }
        val statusIcon = MdiIconView(this).apply {
            setIconName("circle-small"); setIconSize(24f); setTextColor(textMuted)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(8) }
        }
        statusCard.addView(statusIcon)
        val status = TextView(this).apply {
            text = "BELUM BUILD"
            textSize = 13f; setTextColor(textMain); setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        webBuildStatusView = status
        statusCard.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(statusCard, LinearLayout.LayoutParams(-1, dp(58)))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f }
        val buildButton = button("Build") {
            buildWebProject(name.text.toString(), html.text.toString(), css.text.toString(), js.text.toString())
        }
        webHostButton = button("Host Wi-Fi") { hostHomeWifiProject() }.apply { isEnabled = false; alpha = 0.45f }
        actions.addView(buildButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(5); topMargin = dp(8) })
        actions.addView(webHostButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(5); topMargin = dp(8) })
        content.addView(actions)

        content.addView(sectionTitle("OUTPUT", "monitor-dashboard"))
        val outputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f }
        outputRow.addView(button("Preview") { previewWebProject() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(5) })
        outputRow.addView(button("Project Files") { openWebFolder() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(5) })
        content.addView(outputRow)
        content.addView(subLabel("Build membuat folder project lokal. Setelah status SUCCESS, Preview dan Host Wi-Fi dapat digunakan.", 11f).apply { setPadding(dp(3), dp(7), dp(3), 0) })
    }
internal fun MainActivity.pickWebFile(target: EditText, requestCode: Int) {
        webImportTarget = target
        val type = when(requestCode) { WEB_HTML_PICK_REQUEST -> "text/html"; WEB_CSS_PICK_REQUEST -> "text/css"; else -> "text/javascript" }
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { this.type=type; addCategory(Intent.CATEGORY_OPENABLE) }, requestCode)
    }
internal fun MainActivity.webCodeEditor(mode: String = "HTML", target: EditText? = null) {
        val normalized = when (mode.uppercase(Locale.getDefault())) {
            "HTML" -> "html"
            "CSS" -> "css"
            "JAVASCRIPT", "JS" -> "js"
            else -> "code"
        }
        editorExternalTarget = target
        editorExternalMode = normalized
        editorFile = null
        editorSourceUri = null
        editor(null, normalized)
    }
internal fun MainActivity.buildWebProject(projectName:String, html:String, css:String, js:String) {
        val h=html.trim(); val c=css.trim(); val j=js.trim()
        webBuildReady=false
        webHostButton?.isEnabled=false; webHostButton?.alpha=0.45f
        webBuildStatusView?.text="MEMERIKSA FILE…"
        if (h.isBlank()) { webBuildStatusView?.text="GAGAL • HTML wajib diisi"; toast("HTML wajib diisi"); return }
        if (h.isBlank() && (c.isNotBlank() || j.isNotBlank())) { webBuildStatusView?.text="GAGAL • HTML wajib ada"; return }

        val localCss=Regex("""(?i)(?:href|src)\s*=\s*[\"']([^\"']+\.css(?:\?[^\"']*)?)[\"']""").findAll(h).map { it.groupValues[1].substringBefore('?') }.filter { !it.startsWith("http://") && !it.startsWith("https://") && !it.startsWith("//") && !it.startsWith("data:") }.toList()
        val localJs=Regex("""(?i)<script[^>]+src\s*=\s*[\"']([^\"']+\.js(?:\?[^\"']*)?)[\"']""").findAll(h).map { it.groupValues[1].substringBefore('?') }.filter { !it.startsWith("http://") && !it.startsWith("https://") && !it.startsWith("//") && !it.startsWith("data:") }.toList()
        val warnings=mutableListOf<String>()
        if (localCss.isNotEmpty() && c.isBlank()) warnings.add("HTML memanggil CSS lokal: ${localCss.joinToString(", ")}, tetapi file CSS belum diisi.")
        if (localJs.isNotEmpty() && j.isBlank()) warnings.add("HTML memanggil JavaScript lokal: ${localJs.joinToString(", ")}, tetapi file JS belum diisi.")
        if (warnings.isNotEmpty()) {
            webBuildStatusView?.text="GAGAL • Dependency belum lengkap"
            AlertDialog.Builder(this).setTitle("Project belum lengkap").setMessage(warnings.joinToString("\n\n") + "\n\nIsi file yang kurang lalu Build lagi.").setPositiveButton("OK",null).show()
            return
        }

        val safe=(projectName.trim().ifBlank{"website"}).replace(Regex("[^A-Za-z0-9_-]"),"_")
        val dir=File(filesDir,"web_projects/$safe").apply { mkdirs() }
        runCatching {
            var htmlBody=h
            val full=h.contains("<html",true)
            if (!full) {
                htmlBody="<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"style.css\"></head><body>$h<script src=\"script.js\"></script></body></html>"
            } else {
                if (c.isNotBlank() && !Regex("(?i)<link[^>]+href\\s*=\\s*[\"'](?:./)?style\\.css").containsMatchIn(h)) {
                    htmlBody=if (Regex("(?i)</head>").containsMatchIn(htmlBody)) htmlBody.replace(Regex("(?i)</head>"),"<link rel=\"stylesheet\" href=\"style.css\"></head>") else "<link rel=\"stylesheet\" href=\"style.css\">"+htmlBody
                }
                if (j.isNotBlank() && !Regex("(?i)<script[^>]+src\\s*=\\s*[\"'](?:./)?script\\.js").containsMatchIn(h)) {
                    htmlBody=if (Regex("(?i)</body>").containsMatchIn(htmlBody)) htmlBody.replace(Regex("(?i)</body>"),"<script src=\"script.js\"></script></body>") else htmlBody+"<script src=\"script.js\"></script>"
                }
            }
            File(dir,"index.html").writeText(htmlBody, StandardCharsets.UTF_8)
            if (c.isNotBlank()) {
                File(dir,"style.css").writeText(c, StandardCharsets.UTF_8)
                localCss.map { File(it).name }.filter { it.isNotBlank() && it != "style.css" }.distinct().forEach { File(dir,it).writeText(c, StandardCharsets.UTF_8) }
            } else File(dir,"style.css").delete()
            if (j.isNotBlank()) {
                File(dir,"script.js").writeText(j, StandardCharsets.UTF_8)
                localJs.map { File(it).name }.filter { it.isNotBlank() && it != "script.js" }.distinct().forEach { File(dir,it).writeText(j, StandardCharsets.UTF_8) }
            } else File(dir,"script.js").delete()
            prefs.edit().putString("last_web_project",dir.absolutePath).putBoolean("last_web_build_ok",true).apply()
            webBuildReady=true
            webBuildStatusView?.text="SUCCESS • BUILD BERHASIL • ${dir.name}"
            webHostButton?.isEnabled=true; webHostButton?.alpha=0.98f
            toast("Build berhasil: ${dir.name}")
        }.onFailure {
            prefs.edit().putBoolean("last_web_build_ok",false).apply()
            webBuildStatusView?.text="GAGAL • ${it.message ?: "kesalahan build"}"
            toast("Build gagal: ${it.message ?: "kesalahan file"}")
        }
    }
internal fun MainActivity.previewWebProject() {
        val path=prefs.getString("last_web_project","") ?: ""
        if(path.isBlank() || !File(path,"index.html").isFile || !prefs.getBoolean("last_web_build_ok",false)){toast("Build website dulu sampai SUCCESS");return}
        previewHtmlText(File(path,"index.html").readText(StandardCharsets.UTF_8),"HTML")
    }
internal fun MainActivity.previewHtmlText(html: String, mode: String) {
        clearPage("Preview")
        content.setPadding(0, 0, 0, 0)
        editorBottomBar.visibility = View.GONE
        appendEditorConsole("Preview dibuka (${mode})")
        val w = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    val msg = consoleMessage ?: return true
                    val level = when (msg.messageLevel()) {
                        android.webkit.ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                        android.webkit.ConsoleMessage.MessageLevel.WARNING -> "WARN"
                        android.webkit.ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                        else -> "LOG"
                    }
                    appendEditorConsole("$level: ${msg.message()}  (${msg.sourceId()}:${msg.lineNumber()})")
                    return true
                }
            }
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    appendEditorConsole("ERROR: page $description ($errorCode)")
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    appendEditorConsole("Page finished")
                }
            }
            val body = if (mode == "HTML") html else "<pre>${html.htmlEsc()}</pre>"
            loadDataWithBaseURL(null, body, "text/html", "UTF-8", null)
        }
        content.addView(w, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(button("Buka Console") { showEditorConsole() }, LinearLayout.LayoutParams(-1, dp(48)).apply {
            setMargins(dp(12), dp(8), dp(12), dp(12))
        })
    }
internal fun MainActivity.hostWebProject() = hostHomeWifiProject()

internal fun MainActivity.hostHomeWifiProject() {
        val path=prefs.getString("last_web_project","") ?: ""
        if(path.isBlank() || !File(path,"index.html").isFile || !prefs.getBoolean("last_web_build_ok",false)) { toast("Build harus SUCCESS sebelum hosting"); return }
        wifiHtmlHostingTool()
    }
internal fun MainActivity.openWebFolder() { val p=prefs.getString("last_web_project","") ?: ""; if(p.isBlank()){toast("Belum ada project");return}; clearPage("Project Files"); File(p).listFiles()?.forEach{content.addView(settingRowClickable(it.name, "${it.length()} bytes", "File project", "file-outline"){ if(it.extension.equals("html",true)||it.extension.equals("htm",true)) previewHtmlText(it.readText(StandardCharsets.UTF_8),"HTML") else output(it.readText(StandardCharsets.UTF_8)) })} }
internal fun MainActivity.saveWebEditor(mode:String,text:String){ editorPendingTarget?.setText(text); val ext=when(mode){"HTML"->"html";"CSS"->"css";"JavaScript"->"js";else->"txt"}; val f=File(filesDir,"web_editor");f.mkdirs();File(f,"untitled.$ext").writeText(text);toast("Disimpan: untitled.$ext") }
internal fun MainActivity.findInEditor(e:EditText){ val q=EditText(this); q.hint="Cari"; AlertDialog.Builder(this).setTitle("Cari").setView(q).setPositiveButton("Cari"){_,_->val i=e.text.toString().indexOf(q.text.toString()); if(i>=0){e.requestFocus();e.setSelection(i,i+q.text.length)}else toast("Tidak ditemukan")}.setNegativeButton("Batal",null).show() }
internal fun MainActivity.applySimpleEmmet(e: EditText) {
        val t = e.text.toString().trim()
        val x = when (t) {
            "!" -> "<!doctype html>\n<html>\n<head><meta charset=\"UTF-8\"></head>\n<body>\n</body>\n</html>"
            "div" -> "<div></div>"
            "p" -> "<p></p>"
            "h1" -> "<h1></h1>"
            "h2" -> "<h2></h2>"
            "button" -> "<button></button>"
            "img" -> "<img src=\"\" alt=\"\">"
            "a" -> "<a href=\"\"></a>"
            "ul" -> "<ul>\n  <li></li>\n</ul>"
            else -> null
        }
        if (x != null) e.setText(x) else toast("Emmet: gunakan !, div, p, h1, h2, button, img, a, ul")
    }
internal fun MainActivity.openTextFileIntoEditor(e:EditText){ val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="text/*";addCategory(Intent.CATEGORY_OPENABLE)}; startActivityForResult(i,9811); editorPendingTarget=e }
internal fun String.htmlEsc()=replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")

internal fun MainActivity.workspaceRoot(): File = File(filesDir, "workspaces").apply { mkdirs() }

internal fun MainActivity.workspaceCard(dir: File): View {
        val files = dir.listFiles()?.filter { it.name != "workspace.json" } ?: emptyList()
        val modified = dir.lastModified()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(13), dp(12), dp(13), dp(10)); background = bg(panel2, 18, line)
            isClickable = true; isFocusable = true; contentDescription = "Workspace ${dir.name}"
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(MdiIconView(this).apply { setIconName("folder-outline"); setIconSize(25f); setTextColor(textMain); background = bg(panel, 13, line); setPadding(dp(9), dp(9), dp(9), dp(9)) }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(10) })
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(dir.name, 15f, true))
        texts.addView(subLabel("${files.size} item  •  ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(modified))}", 10f))
        top.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply { text = "›"; textSize = 27f; setTextColor(textMuted); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(34), dp(44)) })
        card.addView(top)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, 0) }
        fun small(text: String, action: () -> Unit) = TextView(this).apply { this.text = text; textSize = 11f; gravity = Gravity.CENTER; setTextColor(textMain); background = bg(panel, 10, line); isClickable = true; isFocusable = true; setPadding(dp(10), 0, dp(10), 0); setOnClickListener { action() } }
        actions.addView(small("Buka") { prefs.edit().putString("last_workspace", dir.absolutePath).apply(); workspaceDetailTool(dir) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(5) })
        actions.addView(small("Editor") { dir.listFiles()?.firstOrNull { it.isFile && it.name != "workspace.json" }?.let { editor(it) } ?: toast("Belum ada file") }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(5) })
        card.addView(actions)
        card.setOnClickListener { prefs.edit().putString("last_workspace", dir.absolutePath).apply(); workspaceDetailTool(dir) }
        return card.apply { layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) } }
    }
internal fun MainActivity.workspaceDetailTool(dir: File) {
        clearPage("Workspace: ${dir.name}")
        val files = dir.listFiles()?.filter { it.name != "workspace.json" }?.sortedBy { it.name.lowercase(Locale.getDefault()) } ?: emptyList()
        content.addView(compactButtonRow(
            "+ File" to {
                val n = edit("Nama file", false)
                AlertDialog.Builder(this).setTitle("File baru").setView(n).setNegativeButton("Batal", null).setPositiveButton("Buat") { _, _ ->
                    val name = n.text.toString().trim()
                    if (name.isBlank()) return@setPositiveButton
                    runCatching { File(dir, name).writeText("", StandardCharsets.UTF_8); workspaceDetailTool(dir) }.onFailure { toast("Gagal: ${it.message}") }
                }.show()
            },
            "File Manager" to { fileManager(dir) }
        ))
        content.addView(toolSection("PROJECT FILES", "Ketuk file untuk membuka editor."))
        if (files.isEmpty()) content.addView(subLabel("Belum ada file. Buat file pertama dari tombol + File.", 12f))
        files.forEach { f ->
            val row = fileManagerCard(f, dir)
            content.addView(row)
        }
        content.addView(button("←  Kembali ke Workspace") { workspaceCenterTool() })
    }
internal fun MainActivity.pluginCenterTool() {
        clearPage("Plugin Center")
        content.addView(label("Plugin Center", 22f, true))
        content.addView(subLabel("Plugin lokal berbasis manifest JSON. Plugin tidak dijalankan otomatis dan tidak diberi akses khusus.", 12f))
        val root = File(filesDir, "plugins").apply { mkdirs() }
        content.addView(button("Buat Template Plugin") {
            val f = File(root, "plugin_${System.currentTimeMillis()}.json")
            f.writeText(JSONObject().apply {
                put("id", f.nameWithoutExtension); put("name", "My Plugin"); put("version", "1.0");
                put("description", "Local tool manifest"); put("enabled", false); put("entry", "")
            }.toString(2), StandardCharsets.UTF_8)
            toast("Template plugin dibuat"); pluginCenterTool()
        })
        val files = root.listFiles()?.filter { it.extension.equals("json", true) } ?: emptyList()
        if (files.isEmpty()) content.addView(subLabel("Belum ada manifest plugin.", 13f))
        files.forEach { f ->
            val j = runCatching { JSONObject(f.readText(StandardCharsets.UTF_8)) }.getOrNull()
            val name = j?.optString("name", f.nameWithoutExtension) ?: f.nameWithoutExtension
            val ver = j?.optString("version", "?") ?: "?"
            content.addView(settingRowClickable(name, "v$ver", f.absolutePath, "tools") { output(f.readText(StandardCharsets.UTF_8)) })
        }
    }
internal fun MainActivity.toolCustomizationTool() {
        clearPage("Tool Customization")
        content.addView(label("Tool Customization", 22f, true))
        content.addView(subLabel("Pin tool ke Beranda atau sembunyikan tool tertentu. Pengaturan disimpan lokal.", 12f))
        val pinned = prefs.getStringSet("pinned_tools", emptySet()) ?: emptySet()
        content.addView(button("Reset Kustomisasi") { prefs.edit().remove("pinned_tools").remove("hidden_tools").apply(); toolCustomizationTool() })
        homeTools.forEach { (id, name) ->
            val isPinned = pinned.contains(id)
            val row = settingRowClickable(name, if (isPinned) "Pinned" else "Tidak dipin", "ID: $id", "tools") {
                val now = prefs.getStringSet("pinned_tools", emptySet())?.toMutableSet() ?: mutableSetOf()
                if (now.contains(id)) now.remove(id) else now.add(id)
                prefs.edit().putStringSet("pinned_tools", now).apply(); toolCustomizationTool()
            }
            content.addView(row)
        }
    }
internal fun MainActivity.studioCenterTool() {
        clearPage("Studio Center")
        content.addView(label("Studio Center", 22f, true))
        content.addView(subLabel("Workspace terpadu untuk File, Network, Developer, System, Finance, Utility, dan Web.", 12f))
        val studios = listOf(
            "File Studio" to "filestudio", "Network Studio" to "networkstudio", "Developer Studio" to "developerstudio",
            "System Studio" to "systemstudio", "Finance Studio" to "financestudio", "Utility Studio" to "utilitystudio",
            "Web Project Builder" to "webproject", "Workspace Center" to "workspace"
        )
        studios.forEach { (n,id) -> content.addView(settingRowClickable(n, "Buka workspace", "Studio terpadu", iconFor(id)) { openTool(id) }) }
    }
internal fun MainActivity.studioHub(titleText:String, subtitleText:String, tools:List<Pair<String,String>>){ clearPage(titleText); content.addView(subLabel(subtitleText,13f)); tools.forEach{(n,id)->content.addView(settingRowClickable(n,"Buka tool", "", iconFor(id)){openTool(id)})} }
internal fun MainActivity.networkStudioTool(){ studioHub("Network Studio","Semua alat jaringan dalam satu workspace.",listOf("Ping" to "ping","Port Checker" to "port","DNS Lookup" to "dns","Reverse DNS" to "rdns","Whois" to "whois","Traceroute" to "traceroute","HTTP Headers" to "httpheaders","SSL Certificate" to "ssl","Network Scanner" to "netscanner","Subnet Calculator" to "subnetcalc")) }
internal fun MainActivity.developerStudioTool(){ studioHub("Developer Studio","Editor dan formatter untuk developer.",listOf("Web Project Builder" to "webproject","JSON Formatter" to "jsonformat","XML Formatter" to "xmlformat","Regex Tester" to "regex","Timestamp Converter" to "timestamp","Base64" to "base64","JWT Decoder" to "jwt","UUID Generator" to "uuid","Hash Generator" to "hash")) }
internal fun MainActivity.systemStudioTool(){ studioHub("System Studio","Informasi perangkat dan sistem.",listOf("Device & System" to "devicecenter","Storage Analyzer" to "storage","Network Info" to "network","App Manager" to "apps")) }
internal fun MainActivity.financeStudioTool(){ studioHub("Finance Studio","Kalkulator dan dashboard keuangan.",listOf("Kalkulator Lengkap" to "number","Finance Dashboard" to "financedashboard","Pengelola Keuangan" to "financereader","Pivot Point" to "pivotcalc","Averaging Down & DCA" to "dcacalc","Voltage Divider" to "dividercalc","PWM" to "pwmcalc","Konsumsi Listrik" to "powercalc")) }
internal fun MainActivity.utilityStudioTool(){ studioHub("Utility Studio","Utilitas sehari-hari.",listOf("Calculator" to "number","Clipboard Manager" to "clipboard","Unit Converter" to "unitconverter","QR Scanner" to "qr","OCR" to "ocr","Password Generator" to "password","Notes / Notifikasi" to "reminder")) }
internal fun MainActivity.systemInfo() {
        clearPage("Sistem")
        modernToolListRow("Sistem", "Informasi dan pengaturan sistem", "cog-outline") { showSystemDetailMenu() }
        modernToolListRow("System Center", "Kontrol layanan sistem", "view-dashboard-outline") { systemCenterTool() }
        modernToolListRow("Device Info", "Informasi perangkat lengkap", "cellphone-information") { deviceInfoTool() }
        modernToolListRow("Battery Info", "Status dan statistik baterai", "battery-high") { batteryInfoTool() }
        modernToolListRow("Wi-Fi Info", "Jaringan dan koneksi Wi-Fi", "wifi") { wifiInfo() }
    }
internal fun MainActivity.showSystemDetailMenu() {
        // clearPage() already owns the global toolbar/back navigation.
        // Do not add another page title with a second back button here;
        // that created a duplicated/nested header on the System detail screen.
        clearPage("Sistem")
        val headerSpacer = Space(this)
        content.addView(headerSpacer, LinearLayout.LayoutParams(1, dp(10)))
        val b = batteryStatusIntent()
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (scale > 0 && level >= 0) level * 100 / scale else -1
        modernMetricGrid(
            listOf(
                "Android" to "${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}",
                "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "CPU cores" to Runtime.getRuntime().availableProcessors().toString(),
                "Battery" to if (pct >= 0) "$pct%" else "Tidak tersedia"
            )
        )
        modernActionRow("Buka Pengaturan Aplikasi", "cog-outline") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
internal fun MainActivity.httpServer() {
        clearPage("HTTP Server")
        addToolHeader("HTTP Server", "Server HTTP lokal untuk file/project web.", "WEB")
        val root = File(prefs.getString("last_web_project", "") ?: "")
        val port = edit("Port", false).apply { setText("8080") }
        content.addView(port)
        val path = label(if (root.isDirectory) "Root: ${root.absolutePath}" else "Root belum dipilih", 12f)
        content.addView(path)
        val status = label(if (server != null && !(server?.isClosed ?: true)) "RUNNING" else "STOPPED", 16f, true)
        content.addView(status)
        content.addView(button("Gunakan Project Web terakhir") {
            val p = File(prefs.getString("last_web_project", "") ?: "")
            if (!p.isDirectory || !File(p, "index.html").isFile) {
                toast("Build Web Project dulu")
                return@button
            }
            path.text = "Root: ${p.absolutePath}"
        })
        content.addView(button("Start Static Server") {
            val p = File(prefs.getString("last_web_project", "") ?: "")
            val prt = port.text.toString().toIntOrNull()
            if (!p.isDirectory || !File(p, "index.html").isFile) {
                toast("Build Web Project dulu")
                return@button
            }
            if (prt == null || prt !in 1024..65535) {
                toast("Port harus 1024-65535")
                return@button
            }
            startStaticWebServer(p, prt, status)
        })
        content.addView(button("Stop Server") {
            stopStaticWebServer()
            status.text = "STOPPED"
        })
        content.addView(subLabel("Melayani index.html, CSS, JS, gambar, font, JSON, SVG, dan file project lain. Path traversal di luar folder project ditolak.", 11f))
    }
internal fun MainActivity.startStaticWebServer(root: File, port: Int, status: TextView? = null): Boolean {
        if (server != null && !(server?.isClosed ?: true)) {
            toast("Server sudah berjalan")
            return false
        }
        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: run {
            toast("Folder project tidak valid")
            return false
        }
        val socket = runCatching { ServerSocket(port) }.getOrElse {
            toast("Port $port gagal dibuka: ${it.message}")
            return false
        }
        server = socket
        status?.text = "RUNNING :$port"
        thread(name = "mytools-http-$port") {
            try {
                while (!socket.isClosed) {
                    val client = socket.accept()
                    thread(name = "mytools-http-client") { serveStaticClient(client, canonicalRoot) }
                }
            } catch (_: SocketException) {
                // Normal when Stop closes the ServerSocket.
            } catch (t: Throwable) {
                runOnUiThread { status?.text = "ERROR: ${t.message}" }
            } finally {
                runOnUiThread {
                    if (server === socket) {
                        server = null
                        if (status != null) status.text = "STOPPED"
                    }
                }
                runCatching { socket.close() }
            }
        }
        return true
    }
internal fun MainActivity.serveStaticClient(socket: Socket, root: File) {
        socket.soTimeout = 8000
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
            val requestLine = reader.readLine() ?: return
            var headerCount = 0
            while (headerCount++ < 100) {
                val h = reader.readLine() ?: break
                if (h.isEmpty()) break
            }
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                writeHttpResponse(socket, 400, "text/plain; charset=utf-8", "Bad Request")
                return
            }
            val method = parts[0].uppercase(Locale.US)
            if (method != "GET" && method != "HEAD") {
                writeHttpResponse(socket, 405, "text/plain; charset=utf-8", "Method Not Allowed", method == "HEAD")
                return
            }
            val rawPath = runCatching { URLDecoder.decode(parts[1].substringBefore('?'), "UTF-8") }.getOrElse { "/" }
            val relative = rawPath.removePrefix("/").ifBlank { "index.html" }
            val requested = File(root, relative).canonicalFile
            if (requested != root && !requested.path.startsWith(root.path + File.separator)) {
                writeHttpResponse(socket, 403, "text/plain; charset=utf-8", "Forbidden", method == "HEAD")
                return
            }
            val file = if (requested.isDirectory) File(requested, "index.html") else requested
            if (!file.isFile) {
                writeHttpResponse(socket, 404, "text/plain; charset=utf-8", "Not Found", method == "HEAD")
                return
            }
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase(Locale.US))
                ?: when (file.extension.lowercase(Locale.US)) {
                    "html", "htm" -> "text/html"
                    "css" -> "text/css"
                    "js", "mjs" -> "text/javascript"
                    "json" -> "application/json"
                    "svg" -> "image/svg+xml"
                    "wasm" -> "application/wasm"
                    else -> "application/octet-stream"
                }
            writeHttpFileResponse(socket, "$mime; charset=utf-8", file, method == "HEAD")
        } catch (_: Throwable) {
            runCatching { writeHttpResponse(socket, 500, "text/plain; charset=utf-8", "Server Error") }
        } finally {
            runCatching { socket.close() }
        }
    }
internal fun MainActivity.writeHttpResponse(socket: Socket, code: Int, contentType: String, body: String, headOnly: Boolean = false) =
        writeHttpResponse(socket, code, contentType, body.toByteArray(StandardCharsets.UTF_8), headOnly)

internal fun MainActivity.writeHttpResponse(socket: Socket, code: Int, contentType: String, body: ByteArray, headOnly: Boolean = false) {
        val reason = when (code) {
            200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; 405 -> "Method Not Allowed"; else -> "Internal Server Error"
        }
        val out = socket.getOutputStream()
        val header = "HTTP/1.1 $code $reason\r\nContent-Type: $contentType\r\nContent-Length: ${body.size}\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.ISO_8859_1))
        if (!headOnly) out.write(body)
        out.flush()
    }
internal fun MainActivity.writeHttpFileResponse(socket: Socket, contentType: String, file: File, headOnly: Boolean = false) {
        val out = socket.getOutputStream()
        val header = "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\nContent-Length: ${file.length()}\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.ISO_8859_1))
        if (!headOnly) FileInputStream(file).use { it.copyTo(out, 16 * 1024) }
        out.flush()
    }
internal fun MainActivity.stopStaticWebServer() {
        val old = server
        server = null
        runCatching { old?.close() }
    }
internal fun MainActivity.wifiHtmlHostingTool() {
        clearPage("HTML Hosting Wi-Fi")
        addToolHeader("HTML Hosting Wi-Fi", "Host website di Wi-Fi rumah yang sedang dipakai HP.", "WiFi")
        val project = File(prefs.getString("last_web_project", "") ?: "")
        content.addView(label(if (project.isDirectory && File(project, "index.html").isFile) "Project: ${project.name}" else "Belum ada project", 13f, true))
        val network = label("Jaringan: ${currentWifiSsid()}", 13f)
        content.addView(network)
        val port = edit("Port", false).apply { setText("8080") }
        content.addView(port)
        val status = label(if (server != null && !(server?.isClosed ?: true)) "RUNNING" else "STOPPED", 16f, true)
        hostingStatusView=status; content.addView(status)
        val credentials=label("SSID: ${currentWifiSsid()}\nPassword Wi-Fi: tidak diperlukan oleh server",13f); hostingCredentialsView=credentials; content.addView(credentials)
        val url=label("URL: -",13f,true); hostingUrlView=url; content.addView(url)
        val qr=ImageView(this).apply { background = bg(Color.WHITE, 16); clipToOutline = true; visibility=View.GONE; scaleType=ImageView.ScaleType.CENTER_INSIDE; layoutParams=LinearLayout.LayoutParams(dp(220),dp(220)).apply{gravity=Gravity.CENTER_HORIZONTAL;topMargin=dp(10);bottomMargin=dp(10)} }; hostingQrView=qr; content.addView(qr)
        content.addView(button("START HOSTING WI-FI RUMAH") {
            val p=File(prefs.getString("last_web_project","") ?: "")
            if(!p.isDirectory || !File(p,"index.html").isFile || !prefs.getBoolean("last_web_build_ok",false)){toast("Build website sampai SUCCESS dulu");return@button}
            val prt=port.text.toString().toIntOrNull()?.takeIf{it in 1024..65535} ?: run{toast("Port harus 1024-65535");return@button}
            pendingHostingPort=prt; pendingHostingRoot=p; startHomeWifiHosting()
        })
        content.addView(button("STOP HOSTING") { stopWifiHtmlHosting() })
        content.addView(button("COPY URL") { val text=hostingUrlView?.text?.toString()?.substringAfter("URL: ")?.lineSequence()?.firstOrNull()?.trim().orEmpty(); if(text.isBlank()||text=="-") toast("Hosting belum aktif") else copyText(text) })
        content.addView(subLabel("Semua perangkat harus terhubung ke Wi-Fi rumah yang sama. Password Wi-Fi rumah tetap dikelola router/Android dan tidak disimpan MyTools. Hanya file project hasil Build yang dilayani.",11f))
    }
