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


internal fun MainActivity.modularIotDashboard() {
        // Kunci landscape sebelum membangun canvas agar tidak sempat kembali ke Home
        // saat Activity menerima perubahan orientasi.
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        clearPage("IoT Dynamic Topology")
        action.text = "+"
        action.textSize = 28f
        action.setOnClickListener { showStudioWidgetPicker() }
        title.text = "IOT STUDIO"
        content.setBackgroundColor(Color.BLACK)
        content.setPadding(0, 0, 0, 0)
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = false
        content.layoutParams = content.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }

        studioWidgets = loadStudioWidgets()
        studioLinks = loadStudioLinks()
        studioSelectedLinkId = null
        studioCanvas = StudioCanvasView(this)
        studioCanvas?.setBackgroundColor(Color.BLACK)
        content.removeAllViews()
        content.addView(studioCanvas, LinearLayout.LayoutParams(-1, -1))
        studioCanvas?.setLinks(studioLinks)
        studioCanvas?.setWidgets(studioWidgets)
    }
internal fun MainActivity.showStudioWidgetPicker() {
        val options = arrayOf(
            "🔌 Relay — ON / OFF",
            "🔘 Push — tekan & tahan",
            "💡 Slider / PWM Dimmer",
            "⚙️ Atur MQTT Studio"
        )
        AlertDialog.Builder(this)
            .setTitle("Tambah Widget")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showStudioConfig("RELAY_TOGGLE")
                    1 -> showStudioConfig("PUSH_MOMENTARY")
                    2 -> showStudioConfig("PWM_SLIDER")
                    3 -> showStudioMqttConfig()
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }
internal fun MainActivity.showStudioConfig(type: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        val name = edit(if (type == "RELAY_TOGGLE") "Nama tombol, contoh Lampu Teras" else "Nama widget")
        val gpio = edit("GPIO / Relay, contoh 2")
        gpio.inputType = InputType.TYPE_CLASS_NUMBER
        box.addView(name)
        box.addView(gpio)
        if (type == "PWM_SLIDER") {
            box.addView(subLabel("Nilai PWM 0–255. Geser untuk mengatur kecerahan/kecepatan.", 11f))
        } else if (type == "PUSH_MOMENTARY") {
            box.addView(subLabel("Perintah ON dikirim saat ditekan, OFF saat dilepas.", 11f))
        } else {
            box.addView(subLabel("Tap sekali untuk ON/OFF.", 11f))
        }
        AlertDialog.Builder(this)
            .setTitle(when (type) {
                "RELAY_TOGGLE" -> "Tambah Tombol Relay"
                "PUSH_MOMENTARY" -> "Tambah Tombol Push"
                else -> "Tambah PWM Dimmer"
            })
            .setView(box)
            .setNegativeButton("BATAL", null)
            .setPositiveButton("TAMBAH") { _, _ ->
                val labelText = name.text.toString().trim().ifBlank { "GPIO" }
                val pin = gpio.text.toString().toIntOrNull()?.coerceIn(0, 99) ?: 2
                val widget = StudioWidget(
                    id = "btn_${studioNextId++}",
                    type = type,
                    label = labelText,
                    gpio = pin,
                    x = dp(24),
                    y = dp(24) + studioWidgets.size * dp(18)
                )
                studioWidgets.add(widget)
                saveStudioWidgets()
                studioCanvas?.setWidgets(studioWidgets)
            }
            .show()
    }
internal fun MainActivity.showStudioMqttConfig() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        val host = edit("Broker host, contoh 192.168.1.10")
        host.setText(prefs.getString("studio_mqtt_host", "") ?: "")
        val port = edit("Port")
        port.setText(prefs.getString("studio_mqtt_port", "1883") ?: "1883")
        port.inputType = InputType.TYPE_CLASS_NUMBER
        val topic = edit("Topic, contoh esp32/gpio")
        topic.setText(prefs.getString("studio_mqtt_topic", "esp32/gpio") ?: "esp32/gpio")
        box.addView(host); box.addView(port); box.addView(topic)
        AlertDialog.Builder(this)
            .setTitle("MQTT Studio")
            .setView(box)
            .setNegativeButton("BATAL", null)
            .setPositiveButton("SIMPAN") { _, _ ->
                prefs.edit()
                    .putString("studio_mqtt_host", host.text.toString().trim())
                    .putString("studio_mqtt_port", port.text.toString().trim())
                    .putString("studio_mqtt_topic", topic.text.toString().trim())
                    .apply()
                toast("Konfigurasi MQTT Studio disimpan")
            }
            .show()
    }
internal fun MainActivity.saveStudioWidgets() {
        val arr = JSONArray()
        studioWidgets.forEach { w ->
            arr.put(JSONObject().apply {
                put("id", w.id); put("type", w.type); put("label", w.label); put("gpio", w.gpio)
                put("posX", w.x); put("posY", w.y); put("value", w.value); put("checked", w.checked)
            })
        }
        prefs.edit().putString(studioPrefsKey, arr.toString()).apply()
    }
internal fun MainActivity.saveStudioLinks() {
        val arr = JSONArray()
        studioLinks.forEach { lk ->
            arr.put(JSONObject().apply { put("from", lk.fromId); put("to", lk.toId) })
        }
        prefs.edit().putString(studioLinksPrefsKey, arr.toString()).apply()
    }
internal fun MainActivity.loadStudioLinks(): ArrayList<StudioLink> {
        val result = ArrayList<StudioLink>()
        val raw = prefs.getString(studioLinksPrefsKey, "[]") ?: "[]"
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val from = o.optString("from", ""); val to = o.optString("to", "")
                if (from.isNotBlank() && to.isNotBlank()) result.add(StudioLink(from, to))
            }
        }
        return result
    }
internal fun MainActivity.onStudioLinkTap(id: String) {
        val sel = studioSelectedLinkId
        studioSelectedLinkId = when {
            sel == null -> id
            sel == id -> null
            else -> {
                val exists = studioLinks.any { (it.fromId == sel && it.toId == id) || (it.fromId == id && it.toId == sel) }
                if (!exists) { studioLinks.add(StudioLink(sel, id)); saveStudioLinks() }
                null
            }
        }
        studioCanvas?.setLinks(studioLinks)
        studioCanvas?.setWidgets(studioWidgets)
    }
internal fun MainActivity.loadStudioWidgets(): ArrayList<StudioWidget> {
        val result = ArrayList<StudioWidget>()
        val raw = prefs.getString(studioPrefsKey, "[]") ?: "[]"
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(StudioWidget(
                    id = o.optString("id", "btn_${i + 1}"),
                    type = o.optString("type", "RELAY_TOGGLE"),
                    label = o.optString("label", "Widget ${i + 1}"),
                    gpio = o.optInt("gpio", 2),
                    x = o.optInt("posX", dp(24)),
                    y = o.optInt("posY", dp(24)),
                    value = o.optInt("value", 0),
                    checked = o.optBoolean("checked", false)
                ))
            }
        }
        studioNextId = result.mapNotNull { it.id.substringAfter("btn_", "").toIntOrNull() }.maxOrNull()?.plus(1) ?: 1
        return result
    }
internal fun MainActivity.sendStudioCommand(widget: StudioWidget, command: String) {
        val host = prefs.getString("studio_mqtt_host", "")?.trim().orEmpty()
        val port = prefs.getString("studio_mqtt_port", "1883")?.toIntOrNull() ?: 1883
        val topic = prefs.getString("studio_mqtt_topic", "esp32/gpio")?.trim().orEmpty()
        val payload = JSONObject().apply {
            put("device", widget.label)
            put("gpio", widget.gpio)
            put("command", command)
            put("value", widget.value)
        }.toString()
        if (host.isBlank()) {
            toast("Widget ${widget.label}: ${command} • MQTT belum dikonfigurasi")
            return
        }
        toolThread {
            val result = runCatching {
                mqttPublish(host, port, "MyTools-Studio-${System.currentTimeMillis() % 100000}", topic, payload)
            }.getOrElse { "MQTT gagal: ${it.message}" }
            runOnUiThread { if (result.startsWith("MQTT gagal")) toast(result) }
        }
    }
internal fun MainActivity.showStudioEditDialog(widget: StudioWidget) {
        val options = arrayOf("Ubah nama / GPIO", "Hapus widget")
        AlertDialog.Builder(this)
            .setTitle(widget.label)
            .setItems(options) { _, which ->
                if (which == 0) {
                    val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(4), dp(20), 0) }
                    val name = edit("Nama"); name.setText(widget.label)
                    val gpio = edit("GPIO"); gpio.inputType = InputType.TYPE_CLASS_NUMBER; gpio.setText(widget.gpio.toString())
                    box.addView(name); box.addView(gpio)
                    AlertDialog.Builder(this).setTitle("Edit Widget").setView(box)
                        .setNegativeButton("BATAL", null)
                        .setPositiveButton("SIMPAN") { _, _ ->
                            widget.label = name.text.toString().trim().ifBlank { widget.label }
                            widget.gpio = gpio.text.toString().toIntOrNull()?.coerceIn(0, 99) ?: widget.gpio
                            saveStudioWidgets(); studioCanvas?.setWidgets(studioWidgets)
                        }.show()
                } else {
                    studioWidgets.removeAll { it.id == widget.id }
                    studioLinks.removeAll { it.fromId == widget.id || it.toId == widget.id }
                    if (studioSelectedLinkId == widget.id) studioSelectedLinkId = null
                    saveStudioWidgets(); saveStudioLinks()
                    studioCanvas?.setLinks(studioLinks); studioCanvas?.setWidgets(studioWidgets)
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }
internal fun MainActivity.ledSectionCard(title: String, subtitleText: String, icon: String = "•"): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = bg(panel2, 18, line)
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = icon; textSize = 21f; gravity = Gravity.CENTER; setTextColor(textMain)
            background = bg(panel, 12, line)
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(12) })
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(label(title, 15f, true))
        labels.addView(subLabel(subtitleText, 11f))
        top.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(top)
        return box
    }
internal fun MainActivity.espLedStudio() {
    val activity = this
        stopLedPlayback()
        clearPage("ESP LED Studio")
        content.addView(label("ESP LED Studio", 22f, true))
        content.addView(subLabel("Buat pola LED, pilih susunan, atur jarak dan animasi, lalu kirim langsung ke ESP32.", 12f))

        // Jumlah LED — kontrol +/− lebih cepat daripada spinner.
        val countCard = ledSectionCard("Jumlah LED", "Maksimum 50 LED", "💡")
        val countRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val minus = Button(this).apply {
            text = "−"; textSize = 22f; setTextColor(textMain); background = bg(panel, 14, line); setStateListAnimator(null)
            setOnClickListener { setLedCount(ledCount - 1) }
        }
        ledCountLabel = TextView(this).apply {
            text = ledCount.toString(); textSize = 22f; gravity = Gravity.CENTER; setTextColor(textMain)
        }
        val plus = Button(this).apply {
            text = "+"; textSize = 22f; setTextColor(textMain); background = bg(panel, 14, line); setStateListAnimator(null)
            setOnClickListener { setLedCount(ledCount + 1) }
        }
        countRow.addView(minus, LinearLayout.LayoutParams(dp(52), dp(48)))
        countRow.addView(ledCountLabel, LinearLayout.LayoutParams(0, dp(48), 1f))
        countRow.addView(plus, LinearLayout.LayoutParams(dp(52), dp(48)))
        countCard.addView(countRow)
        content.addView(countCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Bentuk/susunan tidak lagi mengambil ruang besar. Tap kartu untuk membuka pilihan.
        val layoutCard = ledSectionCard("Bentuk / Susunan LED", "Tap untuk memilih pola susunan", "▦")
        ledLayoutLabel = TextView(this).apply {
            text = "Grid"
            textSize = 14f
            setTextColor(textMain)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = bg(panel, 13, line)
        }
        val layoutRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
            addView(ledLayoutLabel, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(TextView(activity).apply {
                text = "›"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(textMuted)
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            setOnClickListener { showLedLayoutPicker() }
        }
        layoutCard.addView(layoutRow)
        content.addView(layoutCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Preview utama.
        val previewCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = bg(panel2, 18, line)
        }
        val previewTitle = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        previewTitle.addView(label("Preview LED", 15f, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        previewTitle.addView(TextView(this).apply {
            text = "LIVE"; textSize = 11f; gravity = Gravity.CENTER; setTextColor(textMain); background = bg(panel, 12, line)
            setPadding(dp(12), 0, dp(12), 0)
        }, LinearLayout.LayoutParams(dp(64), dp(34)))
        previewCard.addView(previewTitle)
        ledCanvas = LedCanvasView(this).apply {
            setLedConfig(ledCount, ledLayout)
            onLedClicked = { index ->
                val frame = ledFrames.getOrNull(ledFrameIndex)
                if (frame != null && index in frame.states.indices) {
                    frame.states[index] = !frame.states[index]
                    setStates(frame.states)
                    updateLedFrameInfo()
                    renderLedFrames()
                }
            }
        }
        previewCard.addView(ledCanvas, LinearLayout.LayoutParams(-1, dp(330)).apply { topMargin = dp(6) })
        content.addView(previewCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Jarak LED.
        val gapCard = ledSectionCard("Jarak antar LED", "0 dp = paling rapat", "↔")
        ledGapSeek = SeekBar(this).apply {
            max = 20; progress = ledGapDp
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    ledGapDp = progress; ledCanvas?.setLedGap(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        gapCard.addView(ledGapSeek, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(8) })
        content.addView(gapCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Pattern.
        val patternCard = ledSectionCard("Pattern", "Simpan pola dan gunakan lagi kapan saja", "◉")
        ledNameEdit = edit("Nama pattern, contoh LOVE")
        patternCard.addView(ledNameEdit, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        ledFrameInfo = label("Frame 1 / 1 • 0/${ledCount} LED menyala", 12f, true)
        patternCard.addView(ledFrameInfo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        content.addView(patternCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Timeline frame dan durasi.
        val animCard = ledSectionCard("Animasi", "Buat beberapa frame dan atur kecepatan", "◷")
        val frameActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        frameActions.addView(button("+ Frame") { addLedFrame() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(4) })
        frameActions.addView(button("Duplikat") { duplicateLedFrame() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        frameActions.addView(button("Hapus") { deleteLedFrame() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(4) })
        animCard.addView(frameActions)
        ledFrameStrip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val frameScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(ledFrameStrip)
        }
        animCard.addView(frameScroll, LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(6) })
        ledSpeedInfo = subLabel("Durasi frame: 300 ms", 11f)
        animCard.addView(ledSpeedInfo)
        ledSpeedSeek = SeekBar(this).apply {
            max = 1950; progress = 250
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val ms = (progress + 50).toLong()
                    ledFrames.getOrNull(ledFrameIndex)?.durationMs = ms
                    ledSpeedInfo?.text = "Durasi frame: ${ms} ms"
                    updateLedFrameInfo()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        animCard.addView(ledSpeedSeek, LinearLayout.LayoutParams(-1, dp(42)))
        val playRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        playRow.addView(button("▶ Putar") { playLedAnimation() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(4) })
        playRow.addView(button("■ Stop") { stopLedPlayback(); updateLedFrameInfo() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(4) })
        animCard.addView(playRow)
        content.addView(animCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Simpan / reset.
        val saveRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        saveRow.addView(button("Simpan Pattern") { saveLedPattern() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(4) })
        saveRow.addView(button("Reset") { resetLedFrames(); renderLedFrames() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(4) })
        content.addView(saveRow)

        content.addView(label("Pattern tersimpan", 15f, true).apply { setPadding(0, dp(14), 0, dp(6)) })
        renderSavedLedPatterns()

        // Upload tetap pada halaman yang sama.
        val uploadTitle = label("Upload ke ESP32", 15f, true).apply {
            setPadding(0, dp(14), 0, dp(5)); tag = "led_upload_title"
        }
        content.addView(uploadTitle)
        content.addView(subLabel("Endpoint HTTP POST ESP32. Contoh: http://192.168.4.1/api/led/pattern", 11f))
        ledEndpointEdit = edit("URL endpoint ESP32")
        ledEndpointEdit?.setText(prefs.getString("led_endpoint", "http://192.168.4.1/api/led/pattern") ?: "")
        content.addView(ledEndpointEdit)
        content.addView(button("Upload Pattern") { uploadLedPattern() })
        content.addView(button("Salin JSON Pattern") { copyText(buildLedPatternJson().toString(2)) })

        resetLedFrames()
        renderLedFrames()
    }
internal fun MainActivity.setLedCount(value: Int) {
        val newCount = value.coerceIn(1, 50)
        if (newCount == ledCount) return
        ledCount = newCount
        ledCountLabel?.text = ledCount.toString()
        ledFrames.forEach { frame ->
            val oldStates = frame.states
            frame.states = BooleanArray(ledCount).also { next ->
                for (i in 0 until minOf(oldStates.size, next.size)) next[i] = oldStates[i]
            }
        }
        if (ledFrames.isEmpty()) resetLedFrames()
        ledCanvas?.setLedConfig(ledCount, ledLayout)
        ledCanvas?.setStates(ledFrames.getOrNull(ledFrameIndex)?.states ?: BooleanArray(ledCount))
        updateLedFrameInfo()
        renderLedFrames()
    }
internal fun MainActivity.showLedLayoutPicker() {
        val values = arrayOf("Grid", "Lingkaran", "Strip", "Spiral")
        val current = values.indexOf(ledLayout).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Bentuk / Susunan LED")
            .setSingleChoiceItems(values, current) { dialog, which ->
                ledLayout = values[which]
                ledLayoutLabel?.text = ledLayout
                ledCanvas?.setLedConfig(ledCount, ledLayout)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
internal fun MainActivity.resetLedFrames() {
        stopLedPlayback()
        ledFrames.clear()
        ledFrames.add(LedFrameData(BooleanArray(ledCount), 300L))
        ledFrameIndex = 0
        ledCanvas?.setLedConfig(ledCount, ledLayout)
        ledCanvas?.setLedGap(ledGapDp)
        ledCanvas?.setStates(ledFrames[0].states)
        ledSpeedSeek?.progress = 250
        ledSpeedInfo?.text = "Durasi frame: 300 ms"
        updateLedFrameInfo()
    }
internal fun MainActivity.addLedFrame() {
        if (ledFrames.isEmpty()) resetLedFrames()
        val source = ledFrames[ledFrameIndex]
        ledFrames.add(LedFrameData(source.states.copyOf(), source.durationMs))
        ledFrameIndex = ledFrames.lastIndex
        selectLedFrame(ledFrameIndex)
    }
internal fun MainActivity.duplicateLedFrame() {
        if (ledFrames.isEmpty()) resetLedFrames()
        val source = ledFrames[ledFrameIndex]
        ledFrames.add(ledFrameIndex + 1, LedFrameData(source.states.copyOf(), source.durationMs))
        ledFrameIndex += 1
        selectLedFrame(ledFrameIndex)
    }
internal fun MainActivity.deleteLedFrame() {
        if (ledFrames.size <= 1) { toast("Minimal harus ada 1 frame"); return }
        ledFrames.removeAt(ledFrameIndex)
        ledFrameIndex = ledFrameIndex.coerceAtMost(ledFrames.lastIndex)
        selectLedFrame(ledFrameIndex)
    }
internal fun MainActivity.selectLedFrame(index: Int) {
        if (ledFrames.isEmpty()) return
        ledFrameIndex = index.coerceIn(0, ledFrames.lastIndex)
        val frame = ledFrames[ledFrameIndex]
        ledCanvas?.setStates(frame.states)
        ledSpeedSeek?.progress = (frame.durationMs.coerceIn(50L, 2000L) - 50L).toInt()
        ledSpeedInfo?.text = "Durasi frame: ${frame.durationMs} ms"
        updateLedFrameInfo()
        renderLedFrames()
    }
internal fun MainActivity.updateLedFrameInfo() {
        val frame = ledFrames.getOrNull(ledFrameIndex) ?: return
        val on = frame.states.count { it }
        ledFrameInfo?.text = "Frame ${ledFrameIndex + 1} / ${ledFrames.size} • $on/${ledCount} LED menyala${if (ledPlaying) " • Playing" else ""}"
    }
internal fun MainActivity.renderLedFrames() {
        val strip = ledFrameStrip ?: return
        strip.removeAllViews()
        ledFrames.forEachIndexed { index, frame ->
            val b = Button(this).apply {
                text = "${index + 1}\n${frame.states.count { it }} ON"
                textSize = 10f
                setTextColor(textMain)
                background = bg(if (index == ledFrameIndex) panel else panel2, 12, if (index == ledFrameIndex) textMain else line)
                setOnClickListener { selectLedFrame(index) }
                setStateListAnimator(null)
            }
            strip.addView(b, LinearLayout.LayoutParams(dp(78), dp(54)).apply { rightMargin = dp(5) })
        }
        ledCanvas?.setStates(ledFrames.getOrNull(ledFrameIndex)?.states ?: BooleanArray(ledCount))
        updateLedFrameInfo()
    }
internal fun MainActivity.playLedAnimation() {
        if (ledFrames.isEmpty()) return
        stopLedPlayback()
        ledPlaying = true
        var index = ledFrameIndex
        val run = object : Runnable {
            override fun run() {
                if (!ledPlaying || ledFrames.isEmpty()) return
                index %= ledFrames.size
                ledFrameIndex = index
                val frame = ledFrames[index]
                ledCanvas?.setStates(frame.states)
                ledSpeedSeek?.progress = (frame.durationMs.coerceIn(50L, 2000L) - 50L).toInt()
                ledSpeedInfo?.text = "Durasi frame: ${frame.durationMs} ms"
                renderLedFrames()
                index++
                ledPlayHandler.postDelayed(this, frame.durationMs.coerceIn(50L, 10000L))
            }
        }
        ledPlayRunnable = run
        ledPlayHandler.post(run)
    }
internal fun MainActivity.stopLedPlayback() {
        ledPlaying = false
        ledPlayRunnable?.let { ledPlayHandler.removeCallbacks(it) }
        ledPlayRunnable = null
    }
internal fun MainActivity.buildLedPatternJson(name: String? = null): JSONObject {
        val root = JSONObject()
        root.put("type", "mytools_esp_led_pattern")
        root.put("version", 2)
        root.put("name", name ?: ledNameEdit?.text?.toString()?.trim().orEmpty().ifBlank { "Untitled" })
        root.put("led_count", ledCount)
        root.put("layout", ledLayout)
        root.put("gap_dp", ledGapDp)
        val framesJson = JSONArray()
        ledFrames.forEachIndexed { index, frame ->
            val f = JSONObject()
            f.put("frame", index + 1)
            f.put("duration_ms", frame.durationMs)
            val states = JSONArray()
            frame.states.forEach { states.put(if (it) 1 else 0) }
            f.put("leds", states)
            framesJson.put(f)
        }
        root.put("frames", framesJson)
        root.put("loop", true)
        return root
    }
internal fun MainActivity.saveLedPattern() {
        val name = ledNameEdit?.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) { toast("Masukkan nama pattern"); return }
        val saved = runCatching { JSONArray(prefs.getString("led_patterns", "[]") ?: "[]") }.getOrElse { JSONArray() }
        val item = buildLedPatternJson(name).apply { put("saved_at", System.currentTimeMillis()) }
        val next = JSONArray(); next.put(item)
        for (i in 0 until saved.length()) {
            val old = saved.optJSONObject(i) ?: continue
            if (!old.optString("name").equals(name, true)) next.put(old)
        }
        while (next.length() > 30) next.remove(next.length() - 1)
        prefs.edit().putString("led_patterns", next.toString()).apply()
        toast("Pattern \"$name\" disimpan")
        renderSavedLedPatterns()
    }
internal fun MainActivity.renderSavedLedPatterns() {
        val marker = content.findViewWithTag<View>("led_saved_container")
        if (marker != null) (marker.parent as? ViewGroup)?.removeView(marker)
        val box = LinearLayout(this).apply { tag = "led_saved_container"; orientation = LinearLayout.VERTICAL }
        val saved = runCatching { JSONArray(prefs.getString("led_patterns", "[]") ?: "[]") }.getOrElse { JSONArray() }
        if (saved.length() == 0) {
            box.addView(subLabel("Belum ada pattern tersimpan.", 12f))
        } else {
            for (i in 0 until saved.length()) {
                val obj = saved.optJSONObject(i) ?: continue
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(8), dp(6), dp(8)); background = bg(panel2, 14, line) }
                val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                val count = obj.optInt("led_count", 0)
                info.addView(label(obj.optString("name", "Pattern"), 14f, true))
                info.addView(subLabel("$count LED • ${obj.optString("layout", "Grid")}", 11f))
                row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(Button(this).apply {
                    text = "LOAD"; setTextColor(textMain); background = bg(panel, 10, line); setStateListAnimator(null); setOnClickListener { loadLedPattern(obj) }
                }, LinearLayout.LayoutParams(dp(78), dp(44)).apply { rightMargin = dp(4) })
                row.addView(Button(this).apply {
                    text = "×"; textSize = 18f; setTextColor(textMain); background = bg(panel, 10, line); setStateListAnimator(null); setOnClickListener { deleteLedPattern(obj.optString("name")) }
                }, LinearLayout.LayoutParams(dp(48), dp(44)))
                box.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }
        val uploadIndex = findContentChildIndexByTag("led_upload_title")
        if (uploadIndex >= 0) content.addView(box, uploadIndex) else content.addView(box)
    }
internal fun MainActivity.findContentChildIndexByTag(tagValue: String): Int {
        for (i in 0 until content.childCount) if (content.getChildAt(i).tag == tagValue) return i
        return -1
    }
internal fun MainActivity.deleteLedPattern(name: String) {
        val saved = runCatching { JSONArray(prefs.getString("led_patterns", "[]") ?: "[]") }.getOrElse { JSONArray() }
        val next = JSONArray()
        for (i in 0 until saved.length()) {
            val obj = saved.optJSONObject(i) ?: continue
            if (!obj.optString("name").equals(name, true)) next.put(obj)
        }
        prefs.edit().putString("led_patterns", next.toString()).apply()
        toast("Pattern dihapus")
        renderSavedLedPatterns()
    }
internal fun MainActivity.loadLedPattern(obj: JSONObject) {
        stopLedPlayback()
        ledCount = obj.optInt("led_count", 10).coerceIn(1, 50)
        ledLayout = when (obj.optString("layout", "Grid")) {
            "Kotak" -> "Grid"
            else -> obj.optString("layout", "Grid")
        }.let { if (it in arrayOf("Grid", "Lingkaran", "Strip", "Spiral")) it else "Grid" }
        ledGapDp = obj.optInt("gap_dp", 0).coerceIn(0, 20)
        ledFrames.clear()
        val frames = obj.optJSONArray("frames")
        if (frames != null) for (i in 0 until frames.length()) {
            val f = frames.optJSONObject(i) ?: continue
            val arr = f.optJSONArray("leds")
            val states = BooleanArray(ledCount)
            if (arr != null) for (j in 0 until minOf(ledCount, arr.length())) states[j] = arr.optInt(j, 0) != 0
            ledFrames.add(LedFrameData(states, f.optLong("duration_ms", 300L).coerceIn(50L, 10000L)))
        }
        if (ledFrames.isEmpty()) ledFrames.add(LedFrameData(BooleanArray(ledCount), 300L))
        ledFrameIndex = 0
        ledNameEdit?.setText(obj.optString("name", "Pattern"))
        ledCountLabel?.text = ledCount.toString()
        ledLayoutLabel?.text = ledLayout
        ledCanvas?.setLedConfig(ledCount, ledLayout)
        ledCanvas?.setLedGap(ledGapDp)
        ledGapSeek?.progress = ledGapDp
        renderLedFrames()
        toast("Pattern dimuat")
    }
internal fun MainActivity.uploadLedPattern() {
        if (ledFrames.isEmpty()) { toast("Belum ada frame"); return }
        val endpoint = ledEndpointEdit?.text?.toString()?.trim().orEmpty()
        if (endpoint.isBlank()) { toast("Masukkan URL endpoint ESP"); return }
        runCatching {
            val uri = Uri.parse(endpoint)
            if (uri.scheme != "http" && uri.scheme != "https") error("URL harus http:// atau https://")
        }.onFailure { toast(it.message ?: "URL tidak valid"); return }
        prefs.edit().putString("led_endpoint", endpoint).apply()
        val json = buildLedPatternJson()
        toast("Mengirim pattern ke ESP…")
        toolThread {
            val result = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 7000; readTimeout = 7000; doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8"); setRequestProperty("Accept", "application/json")
                }
                conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }?.take(500).orEmpty()
                conn.disconnect()
                if (code !in 200..299) error("HTTP $code ${body.ifBlank { "ESP menolak request" }}")
                "Berhasil • HTTP $code${if (body.isBlank()) "" else "\nESP: $body"}"
            }.getOrElse { "Upload gagal: ${it.message ?: it.javaClass.simpleName}" }
            runOnUiThread {
                if (result.startsWith("Berhasil")) toast(result) else AlertDialog.Builder(this).setTitle("Upload ESP").setMessage(result).setPositiveButton("OK", null).show()
            }
        }
    }
