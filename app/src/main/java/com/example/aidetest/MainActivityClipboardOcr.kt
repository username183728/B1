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


internal fun MainActivity.espTools() {
        clearPage("ESP Tools")
        content.addView(label("ESP Tools", 22f, true))
        content.addView(subLabel("Toolkit untuk ESP32 / ESP8266: hitung nilai, cek pin, dan siapkan parameter proyek.", 12f))

        sectionTitle("Hardware")
        content.addView(button("GPIO Reference") {
            output("ESP32 GPIO reference:\n\nGPIO 0  • Boot/strapping\nGPIO 1  • UART0 TX\nGPIO 3  • UART0 RX\nGPIO 6–11 • Umumnya terhubung flash internal — hindari\nGPIO 34–39 • Input only\n\nCatatan: fungsi pin dapat berbeda menurut board. Periksa pinout board sebelum memasang hardware.")
        })
        content.addView(button("Pinout ESP32 / ESP8266") {
            output("ESP32 umum: GPIO0–39 (beberapa GPIO tidak tersedia pada semua board).\nESP8266 NodeMCU: D0=GPIO16, D1=GPIO5, D2=GPIO4, D3=GPIO0, D4=GPIO2, D5=GPIO14, D6=GPIO12, D7=GPIO13, D8=GPIO15.\n\nBoot pins dan pin flash memiliki batasan khusus.")
        })
        content.addView(button("LED Resistor Calculator") {
            espLedResistorCalculator()
        })
        content.addView(button("💡 LED Canvas + Animation Studio") {
            espLedStudio()
        })
        content.addView(button("Voltage Divider Calculator") {
            openTool("dividercalc")
        })

        sectionTitle("ADC / PWM")
        content.addView(button("ADC → Voltage") {
            espAdcCalculator()
        })
        content.addView(button("PWM / Duty Cycle") {
            openTool("pwmcalc")
        })

        sectionTitle("Serial / Network")
        content.addView(button("UART / Serial Settings") {
            output("Baud rate umum:\n9600 • 19200 • 38400 • 57600 • 115200\n\nFormat umum: 8 data bit, No parity, 1 stop bit (8N1).\n\nPastikan baud rate ESP dan perangkat lawan sama.")
        })
        content.addView(button("Wi-Fi Info") {
            openTool("wifi")
        })
        content.addView(button("Power / Current Helper") {
            openTool("powercalc")
        })

        sectionTitle("ESP Control & Network")
        content.addView(button("🔎 Auto-Discovery ESP (mDNS/NSD)") { espAutoDiscovery() })
        content.addView(button("📡 Device Manager") { espDeviceManager() })
        content.addView(button("🎛 GPIO Controller") { espGpioController() })
        content.addView(button("📊 Sensor Dashboard") { espSensorDashboard() })
        content.addView(button("📶 Wi-Fi Manager") { espWifiManager() })
        content.addView(button("🔄 OTA Firmware") { espOtaFirmware() })
        content.addView(button("🌐 HTTP / API Tester") { espHttpApiTester() })
        content.addView(button("📬 MQTT Client") { espMqttClient() })
        content.addView(button("🔌 USB / OTG Info") { espUsbInfo() })
        content.addView(button("🖥 TCP Serial Monitor") { espTcpSerialMonitor() })

        sectionTitle("Quick Notes")
        val notes = listOf(
            "⚠ 3.3V logic: jangan langsung memberi 5V ke GPIO ESP32.",
            "⚠ GPIO 34–39 pada ESP32 klasik adalah input-only.",
            "⚠ Hindari GPIO strapping saat boot jika rangkaian eksternal mengubah levelnya.",
            "✓ Gunakan resistor seri untuk LED dan pembagi tegangan untuk input analog yang melebihi batas ADC."
        )
        notes.forEach { content.addView(subLabel(it, 13f).apply { setPadding(dp(6), dp(5), dp(6), dp(5)) }) }
    }
internal fun MainActivity.espLedResistorCalculator() {
        clearPage("LED Resistor")
        content.addView(label("LED Resistor Calculator", 22f, true))
        content.addView(subLabel("R = (Vsupply − Vled) / Iled", 12f))
        val vs = calcDisplay("Tegangan supply, contoh 3.3")
        val vf = calcDisplay("Forward voltage LED, contoh 2.0")
        val ma = calcDisplay("Arus LED (mA), contoh 10")
        content.addView(vs); content.addView(vf); content.addView(ma)
        content.addView(button("Hitung Resistor") {
            val supply = vs.numberValue()
            val led = vf.numberValue()
            val currentMa = ma.numberValue()
            if (supply == null || led == null || currentMa == null || currentMa <= 0.0) {
                toast("Masukkan angka yang valid")
            } else {
                val r = (supply - led) / (currentMa / 1000.0)
                if (r <= 0.0) output("VLED harus lebih kecil dari Vsupply")
                else output("Resistor ideal ≈ ${"%.1f".format(Locale.US, r)} Ω\nNilai praktis terdekat: ${preferredResistor(r)} Ω")
            }
        })
    }
internal fun MainActivity.espAdcCalculator() {
        clearPage("ESP ADC")
        content.addView(label("ADC → Voltage", 22f, true))
        content.addView(subLabel("V = ADC / (2^bits − 1) × Vref", 12f))
        val adc = calcDisplay("Nilai ADC")
        val bits = calcDisplay("Resolusi bit, contoh 12")
        val vref = calcDisplay("Vref, contoh 3.3")
        content.addView(adc); content.addView(bits); content.addView(vref)
        content.addView(button("Hitung Tegangan") {
            val a = adc.numberValue(); val b = bits.numberValue(); val v = vref.numberValue()
            if (a == null || b == null || v == null || b <= 0.0) toast("Masukkan angka yang valid")
            else {
                val max = Math.pow(2.0, b) - 1.0
                output("Tegangan ≈ ${"%.4f".format(Locale.US, a / max * v)} V")
            }
        })
    }
internal fun EditText.numberValue(): Double? = text.toString().trim().replace(',', '.').toDoubleOrNull()

internal fun MainActivity.preferredResistor(value: Double): Int {
        val e24 = doubleArrayOf(10.0, 11.0, 12.0, 13.0, 15.0, 16.0, 18.0, 20.0, 22.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 43.0, 47.0, 51.0, 56.0, 62.0, 68.0, 75.0, 82.0, 91.0)
        if (value <= 0.0) return 0
        val decade = Math.pow(10.0, Math.floor(Math.log10(value)))
        val normalized = value / decade
        val nearest = e24.minByOrNull { Math.abs(it - normalized) } ?: normalized
        return Math.round(nearest * decade).toInt()
    }
internal fun MainActivity.clipboardManagerTool() {
        clearPage("Clipboard Manager")
        content.addView(label("Clipboard Manager", 22f, true))
        content.addView(subLabel("Clipboard Hub: teks, URL, JSON, kode, IP, dan gambar bisa langsung diteruskan ke tool GITLS lain. Data tetap diproses lokal.", 12f))

        val imageUri = clipboardImageUri()
        val current = clipboardText()

        if (imageUri != null) {
            content.addView(label("Clipboard saat ini • Gambar", 13f, true))
            val imageCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = bg(panel2, 14, line)
            }
            val preview = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                adjustViewBounds = true
                setImageURI(imageUri)
                contentDescription = "Gambar dari clipboard"
                minimumHeight = dp(150)
            }
            imageCard.addView(preview, LinearLayout.LayoutParams(-1, dp(180)))
            imageCard.addView(subLabel("Gambar terdeteksi dari clipboard", 11f))
            val imageActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            imageActions.addView(button("OCR Gambar") { openClipboardImageInOcr(imageUri) }, LinearLayout.LayoutParams(0, -2, 1f))
            imageActions.addView(button("Kirim ke…") { showClipboardSendToDialog(null, imageUri) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            imageActions.addView(button("Simpan / Buka") {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, imageUri)) }
                    .onFailure { toast("Tidak ada aplikasi untuk membuka gambar") }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            imageCard.addView(imageActions)
            content.addView(imageCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }

        if (current != null) {
            saveClipboard(current)
            content.addView(label("Clipboard saat ini • Teks", 13f, true))
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = bg(panel2, 14, line)
            }
            card.addView(label(current.take(4000), 14f))
            card.addView(subLabel("${current.length} karakter", 10f))
            content.addView(card)

            val type = clipboardType(current)
            val typeCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = bg(panel2, 14, line)
            }
            typeCard.addView(label("Terdeteksi: $type", 14f, true))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(button("Salin") { copyText(current) }, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(button("Edit") { openClipboardInEditor(current) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            actions.addView(button("Kirim ke…") { showClipboardSendToDialog(current, null) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            typeCard.addView(actions)

            if (isUrlText(current)) {
                typeCard.addView(button("Buka URL") {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(current.trim()))) }
                        .onFailure { toast("URL tidak bisa dibuka") }
                })
            }
            if (looksLikeJson(current)) {
                typeCard.addView(button("Format JSON") {
                    runCatching { openClipboardInEditor(prettyJson(current), "json") }
                        .onFailure { toast("JSON tidak valid: ${it.message}") }
                })
            }
            if (looksLikeCode(current)) {
                typeCard.addView(button("Buka sebagai Kode") { openClipboardInEditor(current, "code") })
            }
            if (looksLikeIp(current)) {
                typeCard.addView(subLabel("IP terdeteksi. Gunakan hasil ini untuk Network / Ping.", 10.5f))
            }
            content.addView(typeCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(10) })
        }

        content.addView(button("Ambil Clipboard Sekarang") {
            val value = clipboardText()
            val image = clipboardImageUri()
            if (value == null && image == null) {
                toast("Clipboard kosong atau tipe data belum didukung")
            } else {
                value?.let { saveClipboard(it) }
                clipboardManagerTool()
            }
        })
        content.addView(button("Hapus Riwayat Clipboard") {
            prefs.edit().remove("clipboard_history").apply()
            clipboardManagerTool()
        })

        content.addView(label("Riwayat", 15f, true))
        val arr = runCatching { JSONArray(prefs.getString("clipboard_history", "[]") ?: "[]") }.getOrElse { JSONArray() }
        if (arr.length() == 0) content.addView(subLabel("Belum ada riwayat clipboard.", 12f))
        for (i in 0 until arr.length()) {
            val value = arr.optString(i)
            if (value.isBlank()) continue
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = bg(panel2, 14, line)
            }
            card.addView(label(value.take(700), 13f))
            card.addView(subLabel("${clipboardType(value)} • ${value.length} karakter", 10f))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(button("Salin") { copyText(value) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(button("Edit") { openClipboardInEditor(value) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            card.addView(row)
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
        startClipboardMonitor()
    }
internal fun MainActivity.showClipboardSendToDialog(value: String?, imageUri: Uri?) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        if (imageUri != null) {
            options += "OCR & Table" to { openClipboardImageInOcr(imageUri) }
            options += "Color Tools" to { openTool("color") }
            options += "File Manager" to { openTool("filemanager") }
        }
        if (!value.isNullOrBlank()) {
            val t = value.trim()
            options += "Editor" to { openClipboardInEditor(value) }
            options += "JSON Formatter" to { if (looksLikeJson(t)) openClipboardInEditor(prettyJson(t), "json") else toast("Data clipboard bukan JSON") }
            options += "Hash Generator" to { openTool("hash") }
            options += "Base64" to { openTool("base64") }
            if (isUrlText(t)) options += "URL Tools" to { openTool("url") }
            if (looksLikeIp(t)) options += "Network / Ping" to { openTool("network") }
            if (Regex("^-?\\d+(?:[.,]\\d+)?$").matches(t)) options += "Calculator" to { openTool("number") }
        }
        if (options.isEmpty()) {
            toast("Tidak ada aksi yang cocok untuk clipboard ini")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Kirim ke tool")
            .setItems(options.map { it.first }.toTypedArray()) { _, which -> options[which].second.invoke() }
            .setNegativeButton("Batal", null)
            .show()
    }
internal fun MainActivity.clipboardImageUri(): Uri? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (!cm.hasPrimaryClip()) return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val desc = clip.description
        val isImage = desc != null && desc.hasMimeType("image/*")
        val uri = clip.getItemAt(0).uri
        if (isImage && uri != null) return uri
        return null
    }
internal fun MainActivity.clipboardType(value: String): String {
        val t = value.trim()
        return when {
            isUrlText(t) -> "URL"
            looksLikeJson(t) -> "JSON"
            looksLikeIp(t) -> "IP Address"
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(t) -> "Email"
            Regex("^\\+?[0-9 ()-]{7,20}$").matches(t) -> "Nomor"
            looksLikeCode(t) -> "Kode / Teks teknis"
            else -> "Teks"
        }
    }
internal fun MainActivity.isUrlText(value: String): Boolean {
        return Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(value.trim())
    }
internal fun MainActivity.looksLikeIp(value: String): Boolean {
        val t = value.trim()
        return Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$").matches(t) &&
            t.split('.').all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
    }
internal fun MainActivity.looksLikeJson(value: String): Boolean {
        val t = value.trim()
        if (!(t.startsWith("{") && t.endsWith("}") || t.startsWith("[") && t.endsWith("]"))) return false
        return runCatching {
            if (t.startsWith("{")) JSONObject(t) else JSONArray(t)
            true
        }.getOrDefault(false)
    }
internal fun MainActivity.looksLikeCode(value: String): Boolean {
        val t = value.trim()
        if (t.length < 8) return false
        val markers = listOf("{", "}", ";", "=>", "fun ", "class ", "import ", "const ", "let ", "def ", "#include", "<html", "</")
        return markers.count { t.contains(it, ignoreCase = true) } >= 2
    }
internal fun MainActivity.openClipboardInEditor(value: String, forcedMode: String? = null) {
        val dir = File(filesDir, "clipboard_imports").apply { mkdirs() }
        val ext = when (forcedMode) {
            "json" -> ".json"
            "code" -> ".txt"
            else -> ".txt"
        }
        val file = File(dir, "clipboard_${System.currentTimeMillis()}$ext")
        runCatching {
            file.writeText(value, StandardCharsets.UTF_8)
            editor(file, forcedMode)
        }.onFailure { toast("Gagal membuka editor: ${it.message}") }
    }
internal fun MainActivity.openClipboardImageInOcr(uri: Uri) {
        pendingOcrUri = uri
        ocrTool()
        pendingOcrUri = uri
        pendingOcrPreview?.setImageURI(uri)
        ocrPreviewStatus?.text = "Gambar dari clipboard siap diproses"
        toast("Gambar clipboard dikirim ke OCR")
    }
internal fun MainActivity.clipboardText(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (!cm.hasPrimaryClip()) return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()?.takeIf { it.isNotBlank() }
    }
internal fun MainActivity.saveClipboard(value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) return
        val arr = runCatching { JSONArray(prefs.getString("clipboard_history", "[]") ?: "[]") }.getOrElse { JSONArray() }
        val next = JSONArray()
        next.put(clean.take(10000))
        for (i in 0 until arr.length()) {
            val old = arr.optString(i)
            if (old.isNotBlank() && old != clean && next.length() < 50) next.put(old)
        }
        prefs.edit().putString("clipboard_history", next.toString()).apply()
    }
internal fun MainActivity.startClipboardMonitor() {
        stopClipboardMonitor()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            runOnUiThread { clipboardText()?.let { saveClipboard(it) } }
        }
        clipboardManager = cm
        clipboardListener = listener
        cm.addPrimaryClipChangedListener(listener)
    }
internal fun MainActivity.stopClipboardMonitor() {
        val cm = clipboardManager
        val listener = clipboardListener
        if (cm != null && listener != null) runCatching { cm.removePrimaryClipChangedListener(listener) }
        clipboardManager = null
        clipboardListener = null
    }
internal fun MainActivity.ocrTool() {
        clearPage("OCR")

        // OCR memakai bahasa visual GITLS: monochrome, ringan, fokus pada isi.
        val black = Color.rgb(18, 18, 20)
        val white = Color.WHITE
        val surface = if (isDarkTheme) Color.rgb(20, 20, 22) else Color.rgb(250, 250, 251)
        val soft = if (isDarkTheme) Color.rgb(30, 30, 33) else Color.rgb(242, 242, 245)
        val ink = if (isDarkTheme) white else black
        val muted = if (isDarkTheme) Color.rgb(175, 175, 180) else Color.rgb(92, 92, 98)
        val border = if (isDarkTheme) Color.rgb(62, 62, 66) else Color.rgb(210, 210, 214)

        fun monoButton(text: String, primary: Boolean, onClick: () -> Unit): Button = Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(if (primary) (if (isDarkTheme) black else white) else ink)
            minHeight = dp(52)
            stateListAnimator = null
            background = bg(if (primary) ink else surface, 16, if (primary) ink else border)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            contentDescription = text
            setOnClickListener {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                if (animationsEnabled()) {
                    animate().scaleX(.98f).scaleY(.98f).setDuration(60).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                }
                onClick()
            }
        }

        fun sectionTitle(title: String, count: String = "") {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(2), dp(8), dp(2), dp(6))
            }
            row.addView(label(title, 12f, true), LinearLayout.LayoutParams(0, -2, 1f))
            if (count.isNotBlank()) row.addView(subLabel(count, 11f))
            content.addView(row)
        }

        fun infoCard(title: String, body: String): View = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = bg(surface, 18, border)
            addView(label(title, 16f, true))
            addView(subLabel(body, 11f).apply { setTextColor(muted) })
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        }

        addToolHeader("OCR", "Gambar → teks atau tabel yang bisa diedit dan diekspor.", "OCR")

        sectionTitle("GAMBAR")
        val previewCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = bg(soft, 18, border)
        }
        val preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = bg(surface, 14, border)
            contentDescription = "Preview gambar OCR"
        }
        previewCard.addView(preview, LinearLayout.LayoutParams(-1, dp(190)))
        val previewStatus = subLabel("Belum ada gambar", 11f).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
            setTextColor(muted)
        }
        previewCard.addView(previewStatus)
        content.addView(previewCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val imageActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val pickButton = monoButton("Pilih Gambar", true) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1020)
        }
        val cameraButton = monoButton("Kamera", true) {
            openOcrCamera()
        }
        val clearButton = monoButton("Hapus", false) {
            pendingOcrUri = null
            pendingOcrTable = emptyList()
            preview.setImageDrawable(null)
            previewStatus.text = "Belum ada gambar"
            pendingOcrView?.setText("")
            ocrResultMeta?.text = "Belum ada hasil"
            ocrDetectedRow?.removeAllViews()
            ocrDetectedRow?.addView(subLabel("Belum ada data terdeteksi", 11f))
            ocrTableMeta?.text = "Belum ada tabel"
            ocrTablePreview?.removeAllViews()
            ocrTablePreview?.addView(subLabel("Belum ada tabel terdeteksi", 11f).apply { setTextColor(muted) })
        }
        imageActions.addView(cameraButton, LinearLayout.LayoutParams(0, -2, 1f))
        imageActions.addView(pickButton, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        imageActions.addView(clearButton, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        content.addView(imageActions, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val ocrButton = monoButton("OCR Gambar", true) {
            val uri = pendingOcrUri
            if (uri == null) { toast("Pilih gambar terlebih dahulu"); return@monoButton }
            ocrButtonTextState?.text = "Membaca gambar…"
            runOcr(uri) { text ->
                pendingOcrView?.setText(text)
                pendingOcrView?.setSelection(pendingOcrView?.text?.length ?: 0)
                val lines = text.lineSequence().count { it.isNotBlank() }
                val chars = text.length
                ocrResultMeta?.text = if (text.isBlank()) "Tidak ada teks terdeteksi" else "$lines baris • $chars karakter"
                updateOcrDetected(text, ocrDetectedRow, ink, soft, border)
                ocrButtonTextState?.text = "OCR Gambar"
                if (text.isBlank()) toast("Tidak ada teks yang terdeteksi")
            }
            runOcrTable(uri) { table ->
                pendingOcrTable = table
                renderOcrTablePreview(table, ocrTablePreview, ocrTableMeta, surface, soft, ink, muted, border)
            }
        }
        ocrButtonTextState = ocrButton
        content.addView(ocrButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        sectionTitle("HASIL OCR")
        val resultCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = bg(surface, 18, border)
        }
        val resultHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        resultHeader.addView(label("Teks hasil", 15f, true), LinearLayout.LayoutParams(0, -2, 1f))
        ocrResultMeta = subLabel("Belum ada hasil", 10.5f).apply { gravity = Gravity.CENTER_VERTICAL }
        resultHeader.addView(ocrResultMeta)
        resultCard.addView(resultHeader)
        val resultBoxLocal = edit("Belum ada teks. Jalankan OCR untuk mengisi hasil.", true).apply {
            setTextColor(ink)
            setHintTextColor(muted)
            background = bg(surface, 14, border)
            setTypeface(android.graphics.Typeface.MONOSPACE)
            textSize = 14f
        }
        resultCard.addView(resultBoxLocal)
        content.addView(resultCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        sectionTitle("TABEL TERDETEKSI", "FOTO → XLSX / CSV")
        val tableMeta = subLabel("Belum ada tabel", 11f).apply { setTextColor(muted) }
        ocrTableMeta = tableMeta
        content.addView(tableMeta, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        val tableScroll = HorizontalScrollView(this).apply { isFillViewport = false }
        val tableCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = bg(surface, 16, border)
            minimumWidth = dp(300)
        }
        tableCard.addView(subLabel("Belum ada tabel terdeteksi", 11f).apply { setTextColor(muted) })
        tableScroll.addView(tableCard)
        ocrTablePreview = tableCard
        content.addView(tableScroll, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val tableActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tableActions.addView(monoButton("Export XLSX", true) {
            if (pendingOcrTable.isEmpty()) { toast("Jalankan OCR pada foto tabel terlebih dahulu"); return@monoButton }
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "gitls_table.xlsx")
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1023)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        tableActions.addView(monoButton("Export CSV", false) {
            if (pendingOcrTable.isEmpty()) { toast("Jalankan OCR pada foto tabel terlebih dahulu"); return@monoButton }
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "gitls_table.csv")
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1024)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        content.addView(tableActions, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val bridgeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bridgeRow.addView(monoButton("Kirim ke Editor", true) {
            val text = resultBoxLocal.text.toString()
            if (text.isBlank()) toast("Belum ada hasil OCR") else openClipboardInEditor(text)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        bridgeRow.addView(monoButton("Kirim ke Clipboard", false) {
            val text = resultBoxLocal.text.toString()
            if (text.isBlank()) toast("Belum ada hasil OCR") else {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("GITLS OCR", text))
                saveClipboard(text)
                toast("Hasil OCR disalin ke Clipboard")
            }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        content.addView(bridgeRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        sectionTitle("DATA TERDETEKSI")
        val detectedCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = bg(surface, 16, border)
        }
        ocrDetectedRow = detectedCard
        detectedCard.addView(subLabel("Belum ada data terdeteksi", 11f).apply { setTextColor(muted) })
        content.addView(detectedCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        sectionTitle("AKSI")
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(monoButton("Salin", true) {
            val text = resultBoxLocal.text.toString()
            if (text.isBlank()) toast("Belum ada hasil OCR") else copyText(text)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row1.addView(monoButton("Bagikan", false) {
            val text = resultBoxLocal.text.toString()
            if (text.isBlank()) toast("Belum ada hasil OCR") else shareText(text)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        content.addView(row1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(monoButton("Simpan TXT", false) {
            val text = resultBoxLocal.text.toString()
            if (text.isBlank()) { toast("Belum ada hasil OCR"); return@monoButton }
            pendingOcrSaveText = text
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "gitls_ocr.txt")
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1022)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row2.addView(monoButton("Bersihkan", false) {
            val cleaned = resultBoxLocal.text.toString()
                .lineSequence()
                .map { it.trim().replace(Regex("[ \\t]+"), " ") }
                .filter { it.isNotBlank() }
                .joinToString("\n")
            resultBoxLocal.setText(cleaned)
            resultBoxLocal.setSelection(resultBoxLocal.text.length)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        content.addView(row2, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        content.addView(infoCard("Terhubung ke tools GITLS", "Foto tabel bisa diubah menjadi grid yang dapat diedit lalu diekspor ke XLSX atau CSV."))

        pendingOcrView = resultBoxLocal
        pendingOcrPreview = preview
        ocrPreviewStatus = previewStatus
        ocrTableMeta = tableMeta
        ocrTablePreview = tableCard
    }
internal fun MainActivity.renderOcrTablePreview(
        table: List<List<String>>, container: LinearLayout?, meta: TextView?, surface: Int,
        soft: Int, ink: Int, muted: Int, border: Int
    ) {
        if (container == null || meta == null) return
        container.removeAllViews()
        if (table.isEmpty()) {
            meta.text = "Tidak ada tabel yang cukup jelas"
            container.addView(subLabel("Coba foto lebih lurus dan pastikan garis/kolom terlihat jelas.", 11f).apply { setTextColor(muted) })
            return
        }
        val cols = table.maxOfOrNull { it.size } ?: 0
        meta.text = "${table.size} baris • $cols kolom • siap diekspor"
        table.forEachIndexed { rowIndex, row ->
            val rowView = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val count = maxOf(cols, row.size)
            for (i in 0 until count) {
                val cell = TextView(this).apply {
                    text = row.getOrNull(i).orEmpty()
                    textSize = 12f
                    setTextColor(ink)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(9), dp(8), dp(9), dp(8))
                    background = bg(if (rowIndex == 0) soft else surface, 0, border)
                    maxLines = 3
                }
                rowView.addView(cell, LinearLayout.LayoutParams(dp(112), -2))
            }
            container.addView(rowView)
        }
    }
internal fun MainActivity.runOcrTable(uri: Uri, onResult: (List<List<String>>) -> Unit) {
        toolThread {
            runCatching {
                val image = InputImage.fromFilePath(this, uri)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener { result ->
                        val cells = mutableListOf<OcrCell>()
                        result.textBlocks.forEach { block ->
                            block.lines.forEach { line ->
                                line.elements.forEach { element ->
                                    val box = element.boundingBox ?: return@forEach
                                    val value = element.text.trim()
                                    if (value.isNotBlank()) cells.add(OcrCell(value, box.left, box.top, box.right, box.bottom))
                                }
                            }
                        }
                        val table = buildOcrGrid(cells)
                        runOnUiThread { onResult(table) }
                    }
                    .addOnFailureListener { err -> runOnUiThread { toast("Deteksi tabel gagal: ${err.message}") } }
            }.onFailure { err -> runOnUiThread { toast("Gambar tidak bisa dibuka: ${err.message}") } }
        }
    }
internal fun MainActivity.buildOcrGrid(cells: List<OcrCell>): List<List<String>> {
        if (cells.isEmpty()) return emptyList()
        val sorted = cells.sortedWith(compareBy<OcrCell> { it.top }.thenBy { it.left })
        val heights = sorted.map { maxOf(1, it.bottom - it.top) }.sorted()
        val medianHeight = heights[heights.size / 2].toFloat()
        val rowTolerance = maxOf(8f, medianHeight * 0.65f)
        val rows = mutableListOf<MutableList<OcrCell>>()
        val rowCenters = mutableListOf<Float>()
        for (cell in sorted) {
            val cy = (cell.top + cell.bottom) / 2f
            var best = -1
            var bestDistance = Float.MAX_VALUE
            for (i in rowCenters.indices) {
                val d = kotlin.math.abs(rowCenters[i] - cy)
                if (d <= rowTolerance && d < bestDistance) { best = i; bestDistance = d }
            }
            if (best < 0) {
                rows.add(mutableListOf(cell)); rowCenters.add(cy)
            } else {
                rows[best].add(cell)
                rowCenters[best] = rows[best].map { (it.top + it.bottom) / 2f }.average().toFloat()
            }
        }
        rows.sortBy { rowCenters[rows.indexOf(it)] }

        val groupedRows = rows.map { row ->
            val r = row.sortedBy { it.left }
            val grouped = mutableListOf<OcrCell>()
            val gapLimit = maxOf(22f, medianHeight * 2.0f)
            for (cell in r) {
                val previous = grouped.lastOrNull()
                if (previous != null && cell.left - previous.right <= gapLimit) {
                    val mergedText = (previous.text + " " + cell.text).trim()
                    grouped[grouped.lastIndex] = OcrCell(
                        mergedText,
                        minOf(previous.left, cell.left),
                        minOf(previous.top, cell.top),
                        maxOf(previous.right, cell.right),
                        maxOf(previous.bottom, cell.bottom)
                    )
                } else grouped.add(cell)
            }
            grouped
        }.filter { it.isNotEmpty() }

        if (groupedRows.isEmpty()) return emptyList()
        val maxCells = groupedRows.maxOf { it.size }
        if (maxCells < 2) return groupedRows.map { it.map(OcrCell::text) }

        // Ambil posisi kolom dari baris yang paling padat. Header merged seperti
        // "Semester" tidak dijadikan kolom baru sehingga data tetap sejajar.
        val anchorRows = groupedRows.filter { it.size >= maxOf(2, maxCells - 1) }
        val centers = anchorRows.flatMap { it }.map { (it.left + it.right) / 2f }.sorted()
        val colCenters = mutableListOf<Float>()
        val clusterGap = maxOf(35f, medianHeight * 3.2f)
        for (x in centers) {
            val last = colCenters.lastOrNull()
            if (last == null || x - last > clusterGap) colCenters.add(x)
            else colCenters[colCenters.lastIndex] = (last + x) / 2f
        }
        if (colCenters.size < 2) return groupedRows.map { it.map(OcrCell::text) }

        return groupedRows.map { row ->
            val out = MutableList(colCenters.size) { "" }
            row.forEach { cell ->
                val cx = (cell.left + cell.right) / 2f
                val index = colCenters.indices.minByOrNull { kotlin.math.abs(colCenters[it] - cx) } ?: 0
                out[index] = if (out[index].isBlank()) cell.text else (out[index] + " " + cell.text).trim()
            }
            out
        }
    }
internal fun MainActivity.csvQuote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

internal fun MainActivity.tableToCsv(table: List<List<String>>): String =
        table.joinToString("\n") { row -> row.joinToString(",") { csvQuote(it) } }

internal fun MainActivity.xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

internal fun MainActivity.writeMinimalXlsx(table: List<List<String>>, output: OutputStream) {
        val workbook = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="OCR Table" sheetId="1" r:id="rId1"/></sheets></workbook>
        """.trimIndent()
        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>
        """.trimIndent()
        val workbookRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>
        """.trimIndent()
        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/></Types>
        """.trimIndent()
        val core = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>GITLS OCR Table</dc:title><dc:creator>GITLS</dc:creator></cp:coreProperties>
        """.trimIndent()
        val rowsXml = table.mapIndexed { rIndex, row ->
            val cells = row.mapIndexed { cIndex, value ->
                val col = StringBuilder().apply { var n = cIndex + 1; while (n > 0) { insert(0, ('A'.code + ((n - 1) % 26)).toChar()); n = (n - 1) / 26 } }.toString()
                val ref = "$col${rIndex + 1}"
                "<c r=\"$ref\" t=\"inlineStr\"><is><t>${xmlEscape(value)}</t></is></c>"
            }.joinToString("")
            "<row r=\"${rIndex + 1}\">$cells</row>"
        }.joinToString("")
        val sheet = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$rowsXml</sheetData></worksheet>
        """.trimIndent()

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            fun entry(path: String, data: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write(data.toByteArray(StandardCharsets.UTF_8)); zip.closeEntry()
            }
            entry("[Content_Types].xml", contentTypes)
            entry("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            entry("docProps/core.xml", core)
            entry("xl/workbook.xml", workbook)
            entry("xl/_rels/workbook.xml.rels", workbookRels)
            entry("xl/worksheets/sheet1.xml", sheet)
        }
    }
internal fun MainActivity.updateOcrDetected(text: String, row: LinearLayout?, ink: Int, soft: Int, border: Int) {
        row ?: return
        row.removeAllViews()
        val values = linkedMapOf(
            "URL" to Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE).findAll(text).count(),
            "Email" to Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").findAll(text).count(),
            "IP" to Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b").findAll(text).count(),
            "Angka" to Regex("\\b\\d+\\b").findAll(text).count()
        )
        values.filter { it.value > 0 }.forEach { (name, count) ->
            val chip = TextView(this).apply {
                this.text = "$name $count"
                textSize = 11f
                setTextColor(ink)
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = bg(soft, 14, border)
            }
            row.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
        }
        if (row.childCount == 0) {
            row.addView(subLabel("Belum ada data terdeteksi", 11f).apply { setTextColor(if (isDarkTheme) Color.LTGRAY else Color.DKGRAY) })
        }
    }
internal fun MainActivity.openOcrCamera() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), OCR_CAMERA_PERMISSION_REQUEST)
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) == null) { toast("Kamera tidak tersedia"); return }
        val file = File(cacheDir, "ocr_camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
        pendingOcrCameraUri = uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { startActivityForResult(intent, OCR_CAMERA_REQUEST) }
            .onFailure { pendingOcrCameraUri = null; toast("Kamera gagal dibuka: ${it.message}") }
    }
internal fun MainActivity.runOcr(uri: Uri, onResult: (String) -> Unit) {
        toolThread {
            runCatching {
                val image = InputImage.fromFilePath(this, uri)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener { result -> runOnUiThread { onResult(result.text) } }
                    .addOnFailureListener { err -> runOnUiThread { toast("OCR gagal: ${err.message}") } }
            }.onFailure { err -> runOnUiThread { toast("Gambar tidak bisa dibuka: ${err.message}") } }
        }
    }
internal fun MainActivity.unitConverterProTool() {
        clearPage("Unit Converter")
        addToolHeader("Unit Converter", "Konversi nilai dengan pasangan satuan yang jelas dan cepat.", "↔")
        val categories = arrayOf("Panjang", "Berat", "Suhu", "Luas", "Volume", "Waktu", "Kecepatan", "Tekanan", "Data", "Energi")
        val from = Spinner(this); val to = Spinner(this); val value = edit("Nilai")
        val category = Spinner(this)
        category.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        content.addView(category, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(7) })
        content.addView(value)
        content.addView(from, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(7) })
        content.addView(to, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(7) })
        val result = label("Hasil akan tampil di sini", 17f, true)
        content.addView(result)
        fun setUnits(index: Int) {
            val units = when(index) {
                0 -> arrayOf("Meter (m)", "Kilometer (km)", "Centimeter (cm)", "Millimeter (mm)", "Inch (in)", "Feet (ft)", "Yard (yd)", "Mile (mi)")
                1 -> arrayOf("Gram (g)", "Kilogram (kg)", "Milligram (mg)", "Pound (lb)", "Ounce (oz)")
                2 -> arrayOf("Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)")
                3 -> arrayOf("m²", "km²", "cm²", "ft²", "acre")
                4 -> arrayOf("Liter (L)", "Milliliter (mL)", "m³", "cm³", "gallon US")
                5 -> arrayOf("Second", "Minute", "Hour", "Day")
                6 -> arrayOf("m/s", "km/h", "mph", "knot")
                7 -> arrayOf("Pa", "kPa", "bar", "psi", "atm")
                8 -> arrayOf("Byte", "KB", "MB", "GB", "TB")
                else -> arrayOf("Joule (J)", "Kilojoule (kJ)", "calorie (cal)", "kWh", "Wh")
            }
            from.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
            to.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
            if (units.size > 1) to.setSelection(1)
        }
        setUnits(0)
        category.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { setUnits(position) }
        }
        content.addView(button("Konversi") {
            val x = value.text.toString().replace(',', '.').toDoubleOrNull()
            if (x == null) { result.text = "Nilai tidak valid"; return@button }
            val cat = category.selectedItemPosition
            val a = from.selectedItemPosition; val b = to.selectedItemPosition
            val out = convertUnits(x, cat, a, b)
            result.text = "${fmt(out)} ${to.selectedItem}"
        })
        content.addView(button("Tukar Satuan") {
            val old = from.selectedItemPosition; from.setSelection(to.selectedItemPosition); to.setSelection(old)
        })
    }
internal fun MainActivity.convertUnits(x: Double, cat: Int, a: Int, b: Int): Double {
        if (a == b) return x
        return when(cat) {
            0 -> { val f = doubleArrayOf(1.0,1000.0,0.01,0.001,0.0254,0.3048,0.9144,1609.344); x*f[a]/f[b] }
            1 -> { val f = doubleArrayOf(0.001,1.0,0.000001,0.45359237,0.028349523125); x*f[a]/f[b] }
            2 -> { val c = when(a) {0->x;1->(x-32)*5/9;else->x-273.15}; when(b){0->c;1->c*9/5+32;else->c+273.15} }
            3 -> { val f=doubleArrayOf(1.0,1e6,1e-4,0.09290304,4046.8564224); x*f[a]/f[b] }
            4 -> { val f=doubleArrayOf(1.0,0.001,1000.0,0.001,3.785411784); x*f[a]/f[b] }
            5 -> { val f=doubleArrayOf(1.0,60.0,3600.0,86400.0); x*f[a]/f[b] }
            6 -> { val f=doubleArrayOf(1.0,0.2777777778,0.44704,0.5144444444); x*f[a]/f[b] }
            7 -> { val f=doubleArrayOf(1.0,1000.0,100000.0,6894.757293,101325.0); x*f[a]/f[b] }
            8 -> { val f=doubleArrayOf(1.0,1024.0,1048576.0,1073741824.0,1099511627776.0); x*f[a]/f[b] }
            else -> { val f=doubleArrayOf(1.0,1000.0,4.184,3600000.0,3600.0); x*f[a]/f[b] }
        }
    }
internal fun MainActivity.apkAnalyzerTool() {
        clearPage("APK Analyzer Lengkap")
        toolWorkspace("APK Analyzer Lengkap", "Periksa package, SDK, permission, DEX, native library, signature, dan isi ZIP.", "android-studio")
        toolWorkspaceSection("ANALYSIS", "Pilih APK lalu jalankan analisis. Hasil muncul di bawah.")
        content.addView(button("Pilih APK") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/vnd.android.package-archive"; addCategory(Intent.CATEGORY_OPENABLE)
            }, 1021)
        })
        content.addView(button("Analisis APK terakhir") { pendingApkUri?.let { analyzeApk(it) } ?: toast("Pilih APK terlebih dahulu") })
        pendingApkOutput?.let { content.addView(it) }
    }
internal fun MainActivity.analyzeApk(uri: Uri) {
        pendingApkUri = uri
        val box = label("Menganalisis...", 13f)
        pendingApkOutput = box
        content.addView(box)
        toolThread {
            val result = runCatching { buildApkReport(uri) }.getOrElse { "APK Analyzer error: ${it.message}" }
            runOnUiThread { box.text = result }
        }
    }
internal fun MainActivity.buildApkReport(uri: Uri): String {
        val temp = File(cacheDir, "analyzer_${System.currentTimeMillis()}.apk")
        val maxApkBytes = 100L * 1024L * 1024L
        val afdLength = contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: -1L
        if (afdLength > maxApkBytes) error("APK terlalu besar. Batas analisis adalah 100 MB.")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > maxApkBytes) error("APK terlalu besar. Batas analisis adalah 100 MB.")
                    output.write(buffer, 0, n)
                }
            }
        } ?: error("Tidak bisa membaca APK")
        val pm = packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = pm.getPackageArchiveInfo(temp.absolutePath, flags)
        val sb = StringBuilder()
        sb.append("=== PACKAGE ===\n")
        sb.append("File: ${queryName(uri) ?: temp.name}\nSize: ${bytesText(temp.length())}\n")
        if (info != null) {
            sb.append("Package: ${info.packageName}\nVersion: ${info.versionName} (${info.versionCode})\n")
            val appInfo = info.applicationInfo
            if (appInfo != null) {
                if (Build.VERSION.SDK_INT >= 24) sb.append("Min SDK: ${appInfo.minSdkVersion}\nTarget SDK: ${appInfo.targetSdkVersion}\n")
                sb.append("Label: ${pm.getApplicationLabel(appInfo)}\n")
            }
            info.requestedPermissions?.let { p -> sb.append("Permissions (${p.size}):\n"); p.forEach { sb.append("  • $it\n") } }
            info.activities?.let { a -> sb.append("Activities: ${a.size}\n"); a.forEach { sb.append("  • ${it.name}\n") } }
            info.services?.let { a -> sb.append("Services: ${a.size}\n"); a.forEach { sb.append("  • ${it.name}\n") } }
            info.receivers?.let { a -> sb.append("Receivers: ${a.size}\n"); a.forEach { sb.append("  • ${it.name}\n") } }
            info.providers?.let { a -> sb.append("Providers: ${a.size}\n"); a.forEach { sb.append("  • ${it.authority}\n") } }
            sb.append("\n=== SIGNATURE ===\n")
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            signatures?.forEachIndexed { index, sig ->
                val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                sb.append("Signer ${index + 1} SHA-256: ${digest.joinToString(":") { "%02X".format(it) }}\n")
            }
        } else sb.append("PackageManager tidak dapat membaca manifest APK.\n")
        sb.append("\n=== ZIP / DEX / NATIVE ===\n")
        ZipFile(temp).use { zip ->
            var files = 0; var totalUncompressed = 0L; var dex = 0; var native = 0; var resources = false; var manifest = false
            val top = mutableListOf<String>()
            val en = zip.entries()
            while (en.hasMoreElements()) {
                val e = en.nextElement(); if (e.isDirectory) continue
                files++; totalUncompressed += e.size.coerceAtLeast(0)
                if (e.name.endsWith(".dex")) dex++
                if (e.name.startsWith("lib/") && e.name.endsWith(".so")) native++
                if (e.name == "resources.arsc") resources = true
                if (e.name == "AndroidManifest.xml") manifest = true
                if (top.size < 80) top.add("${e.name}  ${bytesText(e.size.coerceAtLeast(0))}")
            }
            sb.append("Entries: $files\nUncompressed total: ${bytesText(totalUncompressed)}\nDEX files: $dex\nNative .so: $native\nresources.arsc: $resources\nAndroidManifest.xml: $manifest\n\nTop entries:\n")
            top.forEach { sb.append("  • $it\n") }
        }
        temp.delete()
        return sb.toString()
    }
