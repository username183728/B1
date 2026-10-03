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


internal fun MainActivity.batteryStatusIntent(): Intent? {
        val now = SystemClock.elapsedRealtime()
        val cached = batteryCache
        if (cached != null && now - batteryCacheAt < 1000L) return cached
        val fresh = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryCache = fresh
        batteryCacheAt = now
        return fresh
    }
internal fun MainActivity.showGhBitProcessError(errorTitle: String, errorMessage: String) {
        val host = ghStage ?: return
        val assistant = bitProcessErrorView ?: return
        val face = bitProcessFace

        val safe = errorMessage.replace("\n", " ").trim().take(150)
        // Step 13: Bit receives only the sanitized process context, never credentials.
        val chatMessage = BitRuntimeContext.errorContextForBit()
            ?: "Proses GitHub gagal: $errorTitle. $safe"
        bitPendingChatMessage = chatMessage
        BitRuntimeContext.onToolError(chatMessage)

        // Error reaction uses the separate bit_error.json asset (720..770).
        bitAnim?.playError()

        val label = assistant.findViewWithTag<TextView>("bit_error_label")
        label?.text = "Error"
        label?.contentDescription = "Error — tekan untuk abrir Bit"
        label?.visibility = View.VISIBLE
        assistant.layoutParams = (assistant.layoutParams ?: FrameLayout.LayoutParams(dp(104), dp(58))).apply { width = dp(104); height = dp(58) }
        assistant.background = rippleBg(if (isDarkTheme) Color.rgb(43,43,48) else Color.WHITE, 24)
        assistant.elevation = dp(4).toFloat()
        assistant.visibility = View.VISIBLE
        assistant.alpha = 0f
        assistant.translationX = dp(18).toFloat()
        assistant.translationY = dp(8).toFloat()
        assistant.animate()
            .alpha(1f)
            .translationX(-dp(4).toFloat())
            .translationY(-dp(4).toFloat())
            .setDuration(360L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Small one-time pulse draws attention without constantly covering the process UI.
        assistant.animate().setStartDelay(360L).scaleX(1.06f).scaleY(1.06f).setDuration(130L).withEndAction {
            assistant.animate().scaleX(1f).scaleY(1f).setDuration(130L).start()
        }.start()
    }
internal fun MainActivity.createGhBitProcessAssistant(): FrameLayout {
        val wrapper = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(dp(0), dp(0), dp(0), dp(0))
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            elevation = 0f
            visibility = View.VISIBLE
        }
        val faceHost = ghBitProcessAssistant()
        wrapper.addView(faceHost, FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER))

        val label = TextView(this).apply {
            tag = "bit_error_label"
            text = ""
            contentDescription = "Error — tekan untuk bertanya kepada Bit"
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(if (isDarkTheme) Color.WHITE else Color.rgb(30, 35, 40))
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(2), 0, dp(10), 0)
        }
        wrapper.addView(label, FrameLayout.LayoutParams(dp(48), dp(52), Gravity.CENTER_VERTICAL or Gravity.END))
        wrapper.setOnClickListener {
            playBitThinking()
            wrapper.postDelayed({ showBitChat() }, 120L)
        }
        addPressFeedback(wrapper)
        bitProcessErrorView = wrapper
        return wrapper
    }
internal fun MainActivity.zipPath(src: File, out: File, onBytes: ((Long) -> Unit)? = null) {
        val srcCanonical = src.canonicalFile
        val outCanonical = out.canonicalFile
        if (srcCanonical == outCanonical) throw IOException("File sumber dan ZIP tujuan tidak boleh sama")
        val buffer = ByteArray(64 * 1024)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outCanonical))).use { zos ->
            fun addFile(f: File, name: String) {
                zos.putNextEntry(ZipEntry(name))
                f.inputStream().use { input ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        zos.write(buffer, 0, n)
                        onBytes?.invoke(n.toLong())
                    }
                }
                zos.closeEntry()
            }
            if (src.isFile) {
                addFile(src, src.name)
            } else {
                val base = src.parentFile?.toPath() ?: src.toPath()
                src.walkTopDown().filter { it.isFile }.forEach { f ->
                    if (f.canonicalFile == outCanonical) return@forEach
                    val name = base.relativize(f.toPath()).toString().replace(File.separatorChar, '/')
                    addFile(f, name)
                }
            }
        }
    }
internal fun MainActivity.unzipSafe(zip: File, dest: File, onBytes: ((Long) -> Unit)? = null) {
        val destCanonical = dest.canonicalFile
        var totalBytes = 0L
        var entries = 0
        val maxEntries = 5000
        val maxTotalBytes = 256L * 1024L * 1024L
        val maxEntryBytes = 64L * 1024L * 1024L
        ZipInputStream(BufferedInputStream(FileInputStream(zip))).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                entries++
                if (entries > maxEntries) throw IOException("ZIP terlalu banyak entry")
                val target = File(destCanonical, e.name).canonicalFile
                if (!target.path.startsWith(destCanonical.path + File.separator)) throw SecurityException("ZIP entry di luar folder tujuan: ${e.name}")
                if (e.isDirectory) {
                    if (!target.mkdirs() && !target.isDirectory) throw IOException("Gagal membuat folder: ${e.name}")
                } else {
                    target.parentFile?.mkdirs()
                    var entryBytes = 0L
                    FileOutputStream(target).use { out ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = zis.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            if (entryBytes > maxEntryBytes || totalBytes > maxTotalBytes) throw IOException("ZIP melebihi batas ekstraksi aman")
                            out.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
            }
        }
    }
internal fun MainActivity.jsonTool() {
        clearPage("JSON Tools")
        addToolHeader("JSON Tools", "Validasi, rapikan, kecilkan, atau escape JSON.", "{}")
        content.addView(toolSection("INPUT", "Tempel JSON yang ingin diproses."))
        val e = edit("Tempel JSON di sini", true); content.addView(e)
        content.addView(toolSection("ACTIONS"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Validasi", "Pretty", "Minify").forEachIndexed { i, txt ->
            val b = button(txt) {
                val s=e.text.toString().trim()
                when(i) {
                    0 -> output(runCatching {
                        if (s.startsWith("{")) JSONObject(s) else if (s.startsWith("[")) JSONArray(s) else error("JSON harus dimulai dengan { atau [")
                        "JSON valid."
                    }.getOrElse { "JSON tidak valid: ${it.message}" })
                    1 -> output(runCatching { prettyJson(e.text.toString()) }.getOrElse { "JSON tidak valid: ${it.message}" })
                    else -> output(runCatching { minifyJson(e.text.toString()) }.getOrElse { "JSON tidak valid: ${it.message}" })
                }
            }
            row.addView(b, LinearLayout.LayoutParams(0, dp(50), 1f).apply { if(i>0) leftMargin=dp(5) })
        }
        content.addView(row)
        content.addView(button("Escape String") { output(JSONObjectLite.escape(e.text.toString())) })
        content.addView(button("Preview Lottie") {
            val s = e.text.toString()
            if (LottieJson.looksLikeLottie(s)) showLottiePreview(s) else toast("Ini bukan JSON animasi Lottie")
        })
    }
