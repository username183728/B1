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


internal fun MainActivity.showHome(filter: String = homeFilter) {
        homeFilter = filter
        clearPage("home", true)
        content.setPadding(dp(12), dp(8), dp(12), dp(18))

        // Beranda dibuat sebagai ringkasan aplikasi: kategori Tools + daftar Studio.
        // Filter lama tetap dipakai untuk menjaga perilaku pencarian/favorit.
        if (filter != "Semua") {
            val title = when (filter) {
                "Favorit" -> "Favorit"
                "Terbaru" -> "Terbaru"
                "Populer" -> "Populer"
                else -> "Tools"
            }
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(4), 0, dp(10)) }
            row.addView(label(title, 20f, true), LinearLayout.LayoutParams(0, -2, 1f))
            content.addView(row)
            val items = filteredHomeItems(filter)
            if (items.isEmpty()) {
                content.addView(subLabel("Belum ada tool pada bagian ini.", 13f))
            } else {
                items.forEach { (id, name) ->
                    content.addView(toolCard(id, name, iconFor(id)).apply {
                        layoutParams = LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(7) }
                    })
                }
            }
            return
        }

        // ==================== DASHBOARD ====================
        homeDashboardCard()
        homeQuickTools()
        // Riwayat terbaru ditampilkan ringkas di slot "Aktivitas" pada Ringkasan.
        // Tidak lagi membuat section horizontal terpisah agar Beranda tidak terlalu panjang.
        homeMostUsedTools()
        homePinnedTools()

        // ==================== TOOLS ====================
        homeSectionHeader("Tools", "Lihat Semua") { showAllTools() }
        val toolGrid = GridLayout(this).apply {
            columnCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        // Pengelompokan Beranda mengikuti fungsi tool yang sebenarnya.
        // Studio tetap dipisahkan dari Tools; kartu di bawah hanya berisi tool operasional.
        val toolCategories = listOf(
            HomeCategory("FILE & APP", "15 tools", "folder-outline", listOf(
                "filemanager", "recentfiles", "backuprestore", "zip", "githubzip", "fileconvert", "filesearch", "storage",
                "apps", "apk", "apkcompare", "duplicatefinder", "largefilefinder", "filehashcompare", "dedupe"
            )),
            HomeCategory("SYSTEM", "3 info gabungan", "cellphone-cog", listOf(
                "devicecenter"
            )),
            HomeCategory("CALCULATOR", "10 tools", "calculator-variant-outline", listOf(
                "number", "pivotcalc", "dividercalc", "dcacalc", "pwmcalc", "spritecalc", "installcalc", "powercalc", "aspectcalc", "pphcalc"
            )),
            HomeCategory("TEXT & DEV", "31 tools", "file-document-edit-outline", listOf(
                "editor", "json", "base64", "url", "regex", "uuid", "textstat", "case", "compare", "slug", "lorem", "token", "random",
                "hex", "base32", "timestamp", "unicode", "urlparser", "mime", "jsonformat", "xmlformat", "uuidbatch", "base64file",
                "textreplace", "wordfreq", "markdown", "sql", "yaml", "toml", "cron", "helpbot"
            )),
            HomeCategory("SECURITY", "20 tools", "shield-check-outline", listOf(
                "hash", "password", "passwordstrength", "jwt", "hmac", "totp", "aes", "checksum", "securitycenter", "fileencryption",
                "steganography", "passwordanalyzer", "breachchecker", "securenotes", "totpvault", "pgp", "sshkeygen", "certviewer", "virusscanner", "urlsafety"
            )),
            HomeCategory("NETWORK", "15 tools", "web", listOf(
                "dns", "rdns", "port", "publicip", "ping", "ipinfo", "ssl", "http", "httpheaders", "restclient", "websocket", "network", "networkcenter", "wifi", "webhostwifi"
            )),
            HomeCategory("MEDIA & COLOR", "2 tools", "palette-outline", listOf(
                "color", "imagestudio"
            )),
            HomeCategory("QR / OCR", "1 tool", "qrcode", listOf("qr")),
            HomeCategory("FINANCE", "2 tools", "bank-outline", listOf("financereader", "financedashboard")),
            HomeCategory("UTILITY", "3 tools", "toolbox-outline", listOf("reminder", "stopwatch", "timer")),
            HomeCategory("LAINNYA", "4 tools", "view-grid-outline", listOf("workspace", "plugincenter", "customtools", "studiocenter"))
        )

        toolCategories.forEach { category ->
            val card = homeCategoryCard(category)
            toolGrid.addView(card, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(82)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        content.addView(toolGrid, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        // ==================== STUDIO ====================
        homeSectionHeader("Studio", "Lihat Semua") { showAllStudios() }
        val studios = listOf(
            Triple("Web Project Builder", "web-box", "webproject"),
            Triple("Network Studio", "web", "networkstudio"),
            Triple("Developer Studio", "code-tags", "developerstudio"),
            Triple("File Studio", "folder-outline", "filestudio"),
            Triple("Image Studio", "image-outline", "imagestudio"),
            Triple("Finance Studio", "bank-outline", "financestudio"),
            Triple("System Studio", "cog-outline", "systemstudio"),
            Triple("Utility Studio", "toolbox-outline", "utilitystudio"),
            Triple("ESP / IoT Studio", "chip", "espstudio")
        )
        val studioGrid = GridLayout(this).apply {
            columnCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }
        studios.forEach { (name, icon, id) ->
            studioGrid.addView(homeStudioCard(name, icon, id), GridLayout.LayoutParams().apply {
                width = 0
                height = dp(76)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        content.addView(studioGrid, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
internal fun MainActivity.homeQuickTools() {
        homeSectionHeader("Akses Cepat", "Semua Tools") { showAllTools() }
        val quick = listOf(
            "filemanager" to "File",
            "color" to "Warna",
            "qr" to "QR",
            "hash" to "Hash",
            "fileconvert" to "Convert",
            "reminder" to "Reminder"
        )
        val row = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, 0, 0, dp(3))
        }
        val rail = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quick.forEach { (id, name) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(8), dp(8), dp(7))
                background = bg(if (isDarkTheme) panel2 else Color.WHITE, 16, line)
                isClickable = true
                setOnClickListener { openTool(id) }
                addPressFeedback(this)
                contentDescription = "Akses cepat $name"
            }
            card.addView(MdiIconView(this).apply {
                setIconName(iconFor(id))
                setIconSize(22f)
                setTextColor(textMain)
            }, LinearLayout.LayoutParams(dp(30), dp(30)))
            card.addView(label(name, 10.5f, true).apply { gravity = Gravity.CENTER })
            rail.addView(card, LinearLayout.LayoutParams(dp(82), dp(70)).apply { rightMargin = dp(7) })
        }
        row.addView(rail)
        content.addView(row, LinearLayout.LayoutParams(-1, dp(76)).apply { bottomMargin = dp(3) })
    }
internal fun MainActivity.homeRecentTools() {
        val recentIds = prefs.getString("recent_tools", "")?.split(',')
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()?.take(6) ?: emptyList()
        val recent = recentIds.mapNotNull { id -> homeToolMap[id]?.let { id to it } }
        if (recent.isEmpty()) return
        homeSectionHeader("Terakhir Digunakan", "Riwayat") { historyTool() }
        val row = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, 0, 0, dp(3))
        }
        val rail = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        recent.forEach { (id, name) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(9), dp(8), dp(10), dp(8))
                background = bg(if (isDarkTheme) panel2 else Color.WHITE, 16, line)
                isClickable = true
                setOnClickListener { openTool(id) }
                contentDescription = "Buka $name dari riwayat"
            }
            card.addView(MdiIconView(this).apply {
                setIconName(iconFor(id))
                setIconSize(19f)
                setTextColor(textMain)
            }, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(7) })
            card.addView(label(name, 11f, true).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(dp(92), dp(34)))
            rail.addView(card, LinearLayout.LayoutParams(dp(135), dp(54)).apply { rightMargin = dp(7) })
        }
        row.addView(rail)
        content.addView(row, LinearLayout.LayoutParams(-1, dp(60)).apply { bottomMargin = dp(3) })
    }
internal fun MainActivity.homeMostUsedTools() {
        val usage = prefs.getString("tool_usage", "")
            ?.split(',')
            ?.mapNotNull { part ->
                val bits = part.split(':', limit = 2)
                if (bits.size == 2) bits[0].trim().takeIf { it.isNotEmpty() }?.let { id -> id to (bits[1].toIntOrNull() ?: 0) } else null
            }
            ?.filter { it.second > 0 }
            ?.sortedByDescending { it.second }
            ?: emptyList()
        val mostUsed = usage.mapNotNull { (id, count) -> homeToolMap[id]?.let { Triple(id, it, count) } }.take(6)
        if (mostUsed.isEmpty()) return

        // Pengguna dapat menutup section ini dengan X agar Beranda lebih ringkas.
        // Status disimpan sehingga section benar-benar hilang pada kunjungan berikutnya.
        if (prefs.getBoolean("hide_most_used_home", false)) return

        val row = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, 0, 0, dp(3))
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(6))
        }
        header.addView(MdiIconView(this).apply {
            setIconName("view-grid")
            setIconSize(21f)
            setTextColor(textMain)
        }, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(5) })
        header.addView(label("Paling Sering Digunakan", 19f, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "×"
            textSize = 22f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(dp(8), 0, dp(4), 0)
            minWidth = dp(40)
            minHeight = dp(40)
            contentDescription = "Sembunyikan Paling Sering Digunakan"
            setOnClickListener {
                prefs.edit().putBoolean("hide_most_used_home", true).apply()
                content.removeView(header)
                content.removeView(row)
            }
        })
        content.addView(header, LinearLayout.LayoutParams(-1, -2))

        val rail = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        mostUsed.forEach { (id, name, count) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(9), dp(8), dp(9), dp(7))
                background = bg(if (isDarkTheme) panel2 else Color.WHITE, 16, line)
                isClickable = true
                setOnClickListener { openTool(id) }
                addPressFeedback(this)
                contentDescription = "$name, digunakan $count kali"
            }
            card.addView(MdiIconView(this).apply {
                setIconName(iconFor(id)); setIconSize(21f); setTextColor(textMain)
            }, LinearLayout.LayoutParams(dp(30), dp(30)))
            card.addView(label(name, 10.5f, true).apply {
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(dp(94), dp(31)))
            card.addView(subLabel("$count× digunakan", 8.5f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(15)))
            rail.addView(card, LinearLayout.LayoutParams(dp(116), dp(82)).apply { rightMargin = dp(7) })
        }
        row.addView(rail)
        content.addView(row, LinearLayout.LayoutParams(-1, dp(86)).apply { bottomMargin = dp(3) })
    }
internal fun MainActivity.homePinnedTools() {
        val favorites = favoriteToolIds().mapNotNull { id -> homeToolMap[id]?.let { id to it } }.take(6)
        if (favorites.isEmpty()) return
        homeSectionHeader("Pinned / Favorit", "Kelola") { showFavorites() }
        val row = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, 0, 0, dp(4))
        }
        val rail = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        favorites.forEach { (id, name) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(9), dp(10), dp(9))
                background = bg(if (isDarkTheme) panel2 else Color.WHITE, 16, line)
                isClickable = true
                setOnClickListener { openTool(id) }
                addPressFeedback(this)
                contentDescription = "Buka favorit $name"
            }
            card.addView(MdiIconView(this).apply { setIconName(iconFor(id)); setIconSize(21f); setTextColor(textMain) }, LinearLayout.LayoutParams(dp(30), dp(30)))
            card.addView(label(name, 10.5f, true).apply { gravity = Gravity.CENTER; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(dp(92), dp(30)))
            rail.addView(card, LinearLayout.LayoutParams(dp(112), dp(70)).apply { rightMargin = dp(7) })
        }
        row.addView(rail)
        content.addView(row, LinearLayout.LayoutParams(-1, dp(76)).apply { bottomMargin = dp(5) })
    }
internal fun MainActivity.homeDashboardCard() {
    val activity = this
        val historyCount = runCatching {
            JSONArray(prefs.getString("history", "[]") ?: "[]").length()
        }.getOrDefault(0)
        val favoriteCount = favoriteToolIds().size
        val reminderCount = runCatching {
            JSONArray(prefs.getString("reminders", "[]") ?: "[]").length()
        }.getOrDefault(0)
        val toolCount = homeToolMap.size

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(10))
            contentDescription = "Ringkasan GITLS"
        }
        applyInteractiveSurface(card, 18, 1)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(MdiIconView(this).apply {
            setIconName("view-dashboard-outline")
            setIconSize(21f)
            setTextColor(textMain)
        }, LinearLayout.LayoutParams(dp(30), dp(30)).apply { rightMargin = dp(5) })
        header.addView(label("Ringkasan", 17f, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "Detail  ›"
            textSize = 11.5f
            setTextColor(textMuted)
            setPadding(dp(5), dp(7), 0, dp(7))
            setOnClickListener { showSettings() }
            contentDescription = "Buka pengaturan GITLS"
        })
        card.addView(header)

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, 0)
        }
        fun stat(icon: String, value: String, title: String, click: (() -> Unit)? = null): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(5), dp(5), dp(5), dp(5))
                contentDescription = "$title: $value"
                addView(MdiIconView(activity).apply {
                    setIconName(icon); setIconSize(19f); setTextColor(textMuted)
                }, LinearLayout.LayoutParams(dp(24), dp(24)))
                addView(label(value, 16f, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
                addView(subLabel(title, 9.5f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
                if (click != null) setOnClickListener { click() }
            }
        }
        stats.addView(stat("star", favoriteCount.toString(), "Favorit") { showFavorites() }, LinearLayout.LayoutParams(0, -2, 1f))

        // Aktivitas menampilkan tool terakhir sebagai ikon kecil langsung di dalam slot statistik.
        // Ini menggantikan section "Terakhir Digunakan" yang sebelumnya memakan banyak ruang.
        val activityStat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(5), dp(2), dp(5))
            setOnClickListener { historyTool() }
            contentDescription = "Aktivitas, $historyCount aktivitas"
        }
        val recentIds = prefs.getString("recent_tools", "")?.split(',')
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()?.take(3) ?: emptyList()
        val recentIconRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        if (recentIds.isEmpty()) {
            recentIconRow.addView(MdiIconView(this).apply {
                setIconName("history")
                setIconSize(19f)
                setTextColor(textMuted)
            }, LinearLayout.LayoutParams(dp(24), dp(24)))
        } else {
            recentIds.forEachIndexed { index, id ->
                if (homeToolMap.containsKey(id)) {
                    recentIconRow.addView(MdiIconView(this).apply {
                        setIconName(iconFor(id))
                        setIconSize(17f)
                        setTextColor(textMain)
                        contentDescription = homeToolMap[id] ?: id
                    }, LinearLayout.LayoutParams(dp(22), dp(24)).apply {
                        if (index > 0) leftMargin = dp(2)
                    })
                }
            }
        }
        activityStat.addView(recentIconRow, LinearLayout.LayoutParams(-1, dp(24)))
        activityStat.addView(subLabel("Aktivitas", 9.5f).apply { gravity = Gravity.CENTER })
        stats.addView(activityStat, LinearLayout.LayoutParams(0, -2, 1f))

        stats.addView(stat("bell-outline", reminderCount.toString(), "Reminder") { openTool("reminder") }, LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(stat("tools", toolCount.toString(), "Tools") { showAllTools() }, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(stats)

        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(7)
        })
    }
internal fun MainActivity.homeSectionHeader(titleText: String, actionText: String, actionClick: () -> Unit) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(6))
        }
        val gridIcon = MdiIconView(this).apply {
            setIconName("view-grid")
            setIconSize(21f)
            setTextColor(textMain)
        }
        row.addView(gridIcon, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(5) })
        row.addView(label(titleText, 19f, true), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = actionText + "  ›"
            textSize = 12f
            setTextColor(textMuted)
            setPadding(dp(6), dp(8), 0, dp(8))
            setOnClickListener { actionClick() }
            contentDescription = "$actionText $titleText"
        })
        content.addView(row, LinearLayout.LayoutParams(-1, -2))
    }
internal fun MainActivity.homeCategoryCard(category: HomeCategory): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            contentDescription = "${category.name}, ${category.count}"
        }
        applyInteractiveSurface(card, 18, 1)
        card.setOnClickListener {
            // SYSTEM langsung membuka halaman gabungan Device Info + System Center + Battery Info.
            // Tidak perlu menampilkan tombol/menu perantara lagi.
            if (category.name.equals("SYSTEM", ignoreCase = true)) {
                deviceSystemCenterTool()
            } else {
                val ids = category.ids.filter { homeToolMap.containsKey(it) }
                showCategory(category.name, ids)
            }
        }

        // Ikon kategori dibuat langsung di atas card tanpa kotak/badge warna tambahan.
        // Hasilnya lebih ringan dan konsisten dengan ikon hitam pada screenshot referensi.
        card.addView(MdiIconView(this).apply {
            setIconName(category.icon)
            setIconSize(24f)
            setTextColor(textMain)
        }, LinearLayout.LayoutParams(dp(42), dp(42)))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(4), 0)
        }
        texts.addView(label(category.name, 12.5f, true))
        texts.addView(subLabel(category.count, 10.5f))
        card.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(TextView(this).apply {
            text = "›"
            textSize = 22f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(20), dp(42)))
        return card
    }
internal fun MainActivity.homeStudioCard(name: String, iconName: String, id: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            contentDescription = "$name. Buka Studio"
        }
        applyInteractiveSurface(card, 18, 1)
        card.setOnClickListener { openTool(id) }

        card.addView(MdiIconView(this).apply {
            setIconName(iconName)
            setIconSize(23f)
            setTextColor(textMain)
        }, LinearLayout.LayoutParams(dp(42), dp(42)))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(6), 0)
        }
        texts.addView(label(name, 13.5f, false))
        texts.addView(subLabel("Buka Studio", 10.5f))
        card.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(TextView(this).apply {
            text = "›"
            textSize = 22f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(22), dp(42)))
        return card
    }
internal fun MainActivity.showAllStudios() {
        clearPage("all", true)
        suppressSearch = true
        search.setText("")
        suppressSearch = false
        content.addView(label("Semua Studio", 22f, true))
        content.addView(subLabel("Ruang kerja untuk membuat dan mengelola proyek.", 12f).apply {
            setPadding(0, 0, 0, dp(10))
        })
        val studios = listOf(
            Triple("Web Project Builder", "web-box", "webproject"),
            Triple("Network Studio", "web", "networkstudio"),
            Triple("Developer Studio", "code-tags", "developerstudio"),
            Triple("File Studio", "folder-outline", "filestudio"),
            Triple("Image Studio", "image-outline", "imagestudio"),
            Triple("Finance Studio", "bank-outline", "financestudio"),
            Triple("System Studio", "cog-outline", "systemstudio"),
            Triple("Utility Studio", "toolbox-outline", "utilitystudio"),
            Triple("ESP / IoT Studio", "chip", "espstudio")
        )
        studios.forEach { (name, icon, id) ->
            content.addView(homeStudioCard(name, icon, id), LinearLayout.LayoutParams(-1, dp(76)).apply {
                bottomMargin = dp(7)
            })
        }
    }
internal fun MainActivity.filteredHomeItems(filter: String): List<Pair<String, String>> {
        val base = homeToolMap
        return when (filter) {
            "Favorit" -> favoriteToolIds().mapNotNull { id -> base[id]?.let { id to it } }
            "Terbaru" -> {
                val updated = latestUpdatedToolIds()
                val recent = prefs.getString("recent_tools", "")
                    ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                (updated + recent).distinct().mapNotNull { id -> base[id]?.let { id to it } }
                    .ifEmpty { defaultPreferredTools().take(12).mapNotNull { id -> base[id]?.let { id to it } } }
            }
            "Populer" -> defaultPreferredTools().take(20).mapNotNull { id -> base[id]?.let { id to it } }
            else -> defaultPreferredTools().mapNotNull { id -> base[id]?.let { id to it } }
        }
    }
internal fun MainActivity.defaultPreferredTools(): List<String> = listOf(
        "filemanager", "editor", "reminder", "zip", "githubzip", "http", "json", "hash", "base64", "url", "regex", "uuid", "color",
        "number", "textstat", "case", "dedupe", "compare", "slug", "lorem", "password", "token", "jwt", "hmac", "totp", "aes",
        "random", "checksum", "hex", "base32", "dns", "rdns", "port", "publicip", "ping", "ipinfo", "ssl", "apk", "qr", "system",
        "timestamp", "unicode", "urlparser", "mime", "jsonformat", "xmlformat", "uuidbatch", "base64file", "httpheaders", "textreplace",
        "wordfreq", "devicecenter", "storage", "apps", "network", "filesearch", "pivotcalc", "dividercalc", "dcacalc",
        "pwmcalc", "spritecalc", "installcalc", "powercalc", "aspectcalc", "pphcalc", "financereader", "financedashboard",
        "securitycenter", "clipboard", "ocr", "unitconverter", "apkanalyzer", "netscanner",
        "webproject", "networkstudio", "developerstudio", "filestudio", "imagestudio",
        "systemstudio", "financestudio", "utilitystudio", "espstudio", "whois", "traceroute", "subnetcalc"
    )

internal fun MainActivity.favoriteToolIds(): List<String> = prefs.getString("favorite_tools", "")
        ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

internal fun MainActivity.isFavorite(id: String): Boolean = favoriteToolIds().contains(id)

internal fun MainActivity.toggleFavorite(id: String) {
        val old = favoriteToolIds().toMutableList()
        if (old.contains(id)) old.remove(id) else old.add(0, id)
        val nowFavorite = old.contains(id)
        prefs.edit().putString("favorite_tools", old.distinct().take(30).joinToString(",")).apply()
        toast(if (nowFavorite) "Ditambahkan ke favorit" else "Dihapus dari favorit")
    }
internal fun MainActivity.toolCategory(id: String): String {
        val n = (homeToolMap[id] ?: id).lowercase(Locale.getDefault())
        return when {
            n.contains("esp") || n.contains("iot") || n.contains("gpio") || n.contains("sensor") -> "ESP / IoT"
            n.contains("network") || n.contains("wifi") || n.contains("dns") || n.contains("ping") || n.contains("http") || n.contains("ssl") || n.contains("port") || n.contains("ip ") -> "Network"
            n.contains("password") || n.contains("hash") || n.contains("token") || n.contains("aes") || n.contains("hmac") || n.contains("jwt") || n.contains("totp") || n.contains("encryption") || n.contains("steganography") || n.contains("breach") || n.contains("secure notes") || n.contains("pgp") || n.contains("ssh key") || n.contains("certificate") || n.contains("virus scanner") || n.contains("url safety") -> "Security"
            n.contains("finance") || n.contains("keuangan") || n.contains("dca") || n.contains("bunga") || n.contains("pajak") || n.contains("margin") || n.contains("diskon") -> "Finance"
            n.contains("file") || n.contains("zip") || n.contains("apk") || n.contains("storage") || n.contains("folder") -> "File"
            n.contains("text") || n.contains("json") || n.contains("xml") || n.contains("regex") || n.contains("base64") || n.contains("unicode") || n.contains("slug") -> "Developer"
            n.contains("calc") || n.contains("kalkulator") || n.contains("converter") || n.contains("konversi") || n.contains("aspect") -> "Calculator"
            else -> "Utility"
        }
    }
internal fun MainActivity.syncToolUpdates() {
        val stored = prefs.getString("tool_update_versions", "")
            ?.split("|")?.mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size == 2 && pieces[0].isNotBlank()) pieces[0] to pieces[1] else null
            }?.toMap()?.toMutableMap() ?: mutableMapOf()
        val latest = prefs.getString("latest_tool_updates", "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()

        var changed = false
        toolUpdateCatalog.forEach { (id, version) ->
            if (stored[id] != version) {
                latest.remove(id)
                latest.add(0, id)
                stored[id] = version
                changed = true
            }
        }
        if (changed) {
            prefs.edit()
                .putString("tool_update_versions", stored.entries.joinToString("|") { "${it.key}=${it.value}" })
                .putString("latest_tool_updates", latest.distinct().take(20).joinToString(","))
                .apply()
        }
    }
internal fun MainActivity.latestUpdatedToolIds(): List<String> {
        val ids = prefs.getString("latest_tool_updates", "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        return ids.filter { id -> homeToolMap.containsKey(id) }.take(6)
    }
internal fun MainActivity.latestToolVersion(id: String): String = toolUpdateCatalog[id] ?: "2.19.8"

internal fun MainActivity.recordRecentTool(id: String) {
        val old = prefs.getString("recent_tools", "")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val updated = (listOf(id) + old.filter { it != id }).take(20)

        // Track lightweight local usage counts so the Home dashboard can surface
        // tools the user actually relies on. This stays on-device and never needs
        // an account or network connection.
        val usage = prefs.getString("tool_usage", "")
            ?.split(',')
            ?.mapNotNull { part ->
                val bits = part.split(':', limit = 2)
                if (bits.size == 2) bits[0].trim().takeIf { it.isNotEmpty() }?.let { key -> key to (bits[1].toIntOrNull() ?: 0) } else null
            }
            ?.toMap()
            ?.toMutableMap() ?: mutableMapOf()
        usage[id] = (usage[id] ?: 0) + 1
        val encodedUsage = usage.entries
            .sortedByDescending { it.value }
            .take(30)
            .joinToString(",") { "${it.key}:${it.value}" }

        prefs.edit()
            .putString("recent_tools", updated.joinToString(","))
            .putString("tool_usage", encodedUsage)
            .apply()
    }
internal fun MainActivity.latestToolCard(id: String, name: String): LinearLayout {
    val activity = this
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(9), dp(8), dp(7))
            contentDescription = "$name. Tool terbaru"
        }
        applyInteractiveSurface(card, 18, 2)
        addPressFeedback(card)
        card.setOnClickListener { openToolWithPress(id, card) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MdiIconView(activity).apply {
            setIconName(iconFor(id)); setIconSize(21f); setTextColor(Color.WHITE)
            setPadding(dp(5), dp(5), dp(5), dp(5)); background = bg(Color.rgb(25,25,27), 11)
        }, LinearLayout.LayoutParams(dp(34), dp(34)))
        top.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(TextView(this).apply {
            text = "UPDATE"; textSize = 7.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(90,90,95)); gravity = Gravity.CENTER; setPadding(dp(5), dp(3), dp(5), dp(3))
            background = bg(Color.rgb(242,242,242), 8)
        }, LinearLayout.LayoutParams(-2, dp(24)))
        card.addView(top)
        card.addView(TextView(this).apply {
            text = name; textSize = 12.5f; setTextColor(Color.rgb(17,17,17))
            setTypeface(typeface, android.graphics.Typeface.BOLD); maxLines = 2
            setPadding(0, dp(7), 0, 0)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        card.addView(TextView(this).apply {
            text = "Versi ${latestToolVersion(id)}"; textSize = 8.5f; setTextColor(Color.rgb(120,120,125))
        }, LinearLayout.LayoutParams(-1, dp(15)))
        return card
    }
internal fun MainActivity.mainPyToolCard(id: String, name: String): LinearLayout {
        val theme = visualTheme(name)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(10), dp(9), dp(9))
            contentDescription = "$name. Tool"
        }
        applyInteractiveSurface(card, 18, 2)
        addPressFeedback(card)
        card.setOnClickListener { openToolWithPress(id, card) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val accent = View(this).apply { background = bg(if (isDarkTheme) Color.rgb(90,90,96) else Color.rgb(25,25,27), 3) }
        top.addView(accent, LinearLayout.LayoutParams(dp(4), dp(38)).apply { rightMargin = dp(8) })
        val icon = MdiIconView(this).apply {
            setIconName(iconFor(id))
            setIconSize(22f)
            setTextColor(Color.WHITE)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = bg(if (isDarkTheme) Color.rgb(42,42,46) else Color.rgb(25,25,27), 12)
        }
        top.addView(icon, LinearLayout.LayoutParams(dp(38), dp(38)))
        top.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(TextView(this).apply {
            text = if (isFavorite(id)) "★" else "☆"; textSize = 19f; gravity = Gravity.CENTER
            setTextColor(textMain); setPadding(dp(2),0,dp(2),0)
            contentDescription = if (isFavorite(id)) "Hapus $name dari favorit" else "Tambahkan $name ke favorit"
            setOnClickListener { toggleFavorite(id); text = if (isFavorite(id)) "★" else "☆"; contentDescription = if (isFavorite(id)) "Hapus $name dari favorit" else "Tambahkan $name ke favorit" }
        }, LinearLayout.LayoutParams(dp(30), dp(38)))
        top.addView(MdiIconView(this).apply {
            setIconName("information-outline")
            setIconSize(17f)
            setTextColor(textMuted)
            contentDescription = "Info $name"
            setOnClickListener { showToolHelpDialog(id) }
        }, LinearLayout.LayoutParams(dp(28), dp(38)))
        top.addView(TextView(this).apply {
            text = "›"
            textSize = 22f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(24), dp(38)))
        card.addView(top)
        card.addView(TextView(this).apply {
            text = name
            textSize = 13.5f
            setTextColor(textMain)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 2
            setPadding(0, dp(8), 0, dp(2))
        }, LinearLayout.LayoutParams(-1, dp(40)))
        card.addView(TextView(this).apply {
            text = toolHelp(id, name).purpose
            textSize = 8.5f
            setTextColor(textMuted)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(-1, dp(30)))
        return card
    }
internal fun MainActivity.homeHeroCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(18), dp(18), dp(18))
            background = bg(Color.rgb(242, 245, 247), 22)
            setOnClickListener { showAllTools() }
        }
        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textBox.addView(label("Tempat widget", 24f, true))
        textBox.addView(subLabel("Deskripsi singkat tentang\naplikasi atau fitur utama.", 14f).apply { setPadding(dp(2), 0, 0, 0) })
        val spacer = Space(this)
        val imageBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val image = MdiIconView(this).apply {
            setIconName("view-grid")
            setIconSize(34f)
            setTextColor(Color.rgb(80, 90, 100))
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = bg(Color.rgb(232, 236, 240), 18)
            alpha = 0.7f
        }
        imageBox.addView(image, LinearLayout.LayoutParams(dp(70), dp(70)))
        val dots = TextView(this).apply {
            text = "●  •  •"
            textSize = 11f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        imageBox.addView(dots, LinearLayout.LayoutParams(dp(76), dp(24)))
        card.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(imageBox, LinearLayout.LayoutParams(dp(86), -1))
        return card
    }
internal fun MainActivity.homeChip(textValue: String, active: Boolean, onClick: () -> Unit): TextView = TextView(this).apply {
        text = textValue
        textSize = 11f
        gravity = Gravity.CENTER
        minHeight = dp(38)
        setTextColor(if (active) Color.WHITE else textMain)
        val fill = if (active) (if (isDarkTheme) Color.WHITE else Color.rgb(15,15,16)) else (if (isDarkTheme) Color.rgb(42,42,46) else Color.rgb(241,244,246))
        background = bg(fill, 22)
        setTextColor(if (active) (if (isDarkTheme) Color.BLACK else Color.WHITE) else textMain)
        isClickable = true
        isFocusable = true
        contentDescription = "Filter $textValue${if (active) ", aktif" else ""}"
        setOnClickListener { onClick() }
    }

internal fun MainActivity.homeToolCard(id: String, name: String, desc: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(12), dp(10))
            background = bg(Color.WHITE, 18, Color.rgb(238, 241, 244))
            isClickable = true
            setOnClickListener { if (id == "settings") showSettings() else openTool(id) }
        }
        val iconRes = when (id) {
            "filemanager" -> R.drawable.ic_folder
            "number" -> R.drawable.ic_calculator
            "editor" -> R.drawable.ic_code
            "qr" -> R.drawable.ic_qr
            "reminder" -> R.drawable.ic_bell
            "zip" -> R.drawable.ic_archive
            else -> R.drawable.ic_settings
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = bg(Color.rgb(242, 245, 247), 14)
        }
        card.addView(icon, LinearLayout.LayoutParams(dp(52), dp(52)))
        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(8), 0)
        }
        textBox.addView(label(name, 15f, true).apply { setPadding(0, 0, 0, dp(2)) })
        textBox.addView(subLabel(desc, 11f).apply { setPadding(0, 0, 0, 0) })
        card.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(ImageView(this).apply { setImageResource(R.drawable.ic_arrow_right) }, LinearLayout.LayoutParams(dp(28), dp(28)))
        return card
    }
internal fun MainActivity.showFavorites() {
        if (currentPage == "favorites" && content.childCount > 0) { selectBottomNav("favorites"); return }
        clearPage("favorites", false)
        suppressSearch = true
        search.setText("")
        suppressSearch = false
        content.setPadding(dp(12), dp(8), dp(12), dp(18))
        val favorites = favoriteToolIds().mapNotNull { id -> homeToolMap[id]?.let { id to it } }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(10)) }
        header.addView(label("Favorit", 22f, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "${favorites.size}/30"
            textSize = 11f
            setTextColor(textMuted)
            background = bg(if (isDarkTheme) Color.rgb(42,42,46) else Color.rgb(241,244,246), 14)
            setPadding(dp(9), dp(5), dp(9), dp(5))
        })
        content.addView(header)
        content.addView(subLabel(if (favorites.isEmpty()) "Tool yang kamu tandai akan muncul di sini." else "Akses cepat ke tool yang paling sering kamu gunakan.", 12f).apply { setPadding(dp(2), 0, 0, dp(14)) })
        if (favorites.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(38), dp(24), dp(38))
                contentDescription = "Belum ada tool favorit"
            }
            applyInteractiveSurface(empty, 20, 1)
            empty.addView(MdiIconView(this).apply { setIconName("star"); setIconSize(38f); setTextColor(textMuted) }, LinearLayout.LayoutParams(-1, dp(50)))
            empty.addView(label("Belum ada favorit", 16f, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(2)) })
            empty.addView(subLabel("Tekan ☆ pada kartu tool untuk menyimpannya.", 11f).apply { gravity = Gravity.CENTER })
            content.addView(empty, LinearLayout.LayoutParams(-1, dp(170)).apply { topMargin = dp(6) })
            return
        }
        favorites.forEach { (id, name) ->
            content.addView(toolCard(id, name, iconFor(id)).apply {
                layoutParams = LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(8) }
            })
        }
    }
internal fun MainActivity.setupBitFace() {
        val host = findViewById<FrameLayout>(R.id.navBotAnimationContainer)
        val label = findViewById<TextView>(R.id.navBotLabel)
        label.setTextColor(if (isDarkTheme) Color.rgb(155, 155, 160) else Color.rgb(138, 150, 163))

        val view = LottieAnimationView(this).apply {
            setAnimation("bit/bit_idle.json")
            repeatMode = LottieDrawable.RESTART
            repeatCount = LottieDrawable.INFINITE
            setMinAndMaxFrame(0, 450)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = false
            isFocusable = false
            contentDescription = "Bit"
        }
        host.removeAllViews()
        host.addView(view, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER))
        bitFace = view
        applyBitFaceTheme()
        bitAnim?.attach(view)

        navBot.setOnClickListener {
            bitAnim?.onTap(view, openChat = true)
        }
        addPressFeedback(navBot)

        // Normal state: blink/look + sleep/wake only. Special reactions are
        // controlled by BitAnimationController.
        view.post { runCatching { bitAnim?.playIdleAll() } }
    }
internal fun MainActivity.showBitChat() {
        clearPage("Bit Assistant", true)
        bottomNav.visibility = View.GONE
        editorBottomBar.visibility = View.GONE

        // Reuse the global toolbar, but make it look like a dedicated chat header.
        findViewById<View>(R.id.headerTitleBox)?.visibility = View.VISIBLE
        title.text = "Bit"
        title.textSize = 17f
        title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
        subtitle.text = "Asisten MyTools • offline"
        subtitle.visibility = View.VISIBLE
        homeMenu.visibility = View.GONE
        homeSearch.visibility = View.GONE
        homeProfile.visibility = View.GONE
        action.visibility = View.GONE
        back.visibility = View.VISIBLE
        setupBitChatTopFace()

        content.setPadding(dp(12), dp(8), dp(12), dp(14))
        if (bitChatMessages.isEmpty()) {
            addBitMessage("Halo! Aku Bit. Kamu bisa tanya cara memakai fitur MyTools atau kirim pesan error. Kalau aku tidak punya jawabannya, aku bisa bantu mencarikannya di Google.", false)
        } else {
            bitChatMessages.forEach { addBitMessageView(it) }
        }
        bitPendingChatMessage?.let { pending ->
            if (bitChatMessages.none { !it.fromUser && it.text == pending }) {
                bitChatMessages.add(BitChatMessage(pending, false))
                addBitMessageView(bitChatMessages.last())
            }
            bitPendingChatMessage = null
        }
        setupBitChatInput()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
internal fun MainActivity.setupBitChatTopFace() {
    bitChatTopFace?.let { existing ->
        if (existing.parent != null) return
    }
    val top = topBar
    val view = LottieAnimationView(this).apply {
        setAnimation("bit/bit_idle.json")
        repeatMode = LottieDrawable.RESTART
        repeatCount = LottieDrawable.INFINITE
        setMinAndMaxFrame(0, 450)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        isClickable = false
        isFocusable = false
        contentDescription = "Bit status"
    }
    top.addView(view, LinearLayout.LayoutParams(dp(46), dp(46)).apply {
        leftMargin = dp(2)
        rightMargin = dp(2)
    })
    bitChatTopFace = view
    applyBitFaceTheme(view)
    bitAnim?.attach(view)
    view.setOnClickListener { bitAnim?.onTap(view, openChat = false) }
}

internal fun MainActivity.setupBitChatInput() {
    editorBottomBar.removeAllViews()
    editorBottomBar.visibility = View.VISIBLE
    editorBottomBar.background = if (isDarkTheme) bg(panel, 0) else bg(Color.WHITE, 0)
    editorBottomBar.setPadding(dp(8), dp(6), dp(8), dp(6))
    editorBottomBar.orientation = LinearLayout.VERTICAL
    editorBottomBar.layoutParams = editorBottomBar.layoutParams.apply {
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    val attachmentRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = if (bitAttachmentUri != null) View.VISIBLE else View.GONE
        setPadding(dp(4), 0, dp(4), dp(5))
    }
    val attachmentLabel = TextView(this).apply {
        text = "📎  ${bitAttachmentName.ifBlank { "Lampiran" }}"
        textSize = 12f
        setTextColor(textMain)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f)
    }
    val removeAttachment = TextView(this).apply {
        text = "×"
        textSize = 20f
        gravity = Gravity.CENTER
        setTextColor(textMuted)
        contentDescription = "Hapus lampiran"
        background = rippleBg(if (isDarkTheme) panel2 else Color.rgb(238,240,243), 16)
        setPadding(dp(8), 0, dp(8), 0)
        setOnClickListener {
            bitAttachmentUri = null
            bitAttachmentName = ""
            bitAttachmentMime = ""
            setupBitChatInput()
        }
    }
    attachmentRow.addView(attachmentLabel)
    attachmentRow.addView(removeAttachment, LinearLayout.LayoutParams(dp(36), dp(32)))
    editorBottomBar.addView(attachmentRow)

    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val plus = TextView(this).apply {
        text = "+"
        textSize = 27f
        gravity = Gravity.CENTER
        setTextColor(if (isDarkTheme) Color.WHITE else Color.rgb(30, 40, 50))
        background = rippleBg(if (isDarkTheme) Color.rgb(55,55,60) else Color.rgb(235,238,242), 22)
        contentDescription = "Tambah lampiran"
        isClickable = true
        isFocusable = true
        setOnClickListener { showBitAttachmentMenu(this) }
    }

    val input = EditText(this).apply {
        hint = "Tulis pertanyaan..."
        textSize = 14f
        isSingleLine = true
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        setTextColor(textMain)
        setHintTextColor(textMuted)
        background = rippleBg(if (isDarkTheme) panel2 else Color.rgb(245,247,249), 22)
        setPadding(dp(16), 0, dp(16), 0)
    }
    bitChatInput = input

    val send = TextView(this).apply {
        text = "➤"
        textSize = 21f
        gravity = Gravity.CENTER
        setTextColor(if (isDarkTheme) Color.WHITE else Color.rgb(30, 40, 50))
        background = rippleBg(if (isDarkTheme) Color.rgb(55,55,60) else Color.rgb(235,238,242), 22)
        contentDescription = "Kirim pertanyaan"
        isClickable = true
        isFocusable = true
    }

    row.addView(plus, LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(6) })
    row.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(8) })
    row.addView(send, LinearLayout.LayoutParams(dp(48), dp(48)))
    editorBottomBar.addView(row)

    fun submit() {
        val q = input.text.toString().trim()
        if (q.isEmpty() && bitAttachmentUri == null) return

        BitRuntimeContext.onCommand(q.ifBlank { "Lampiran: $bitAttachmentName" })
        input.setText("")

        if (bitAttachmentUri != null && q.contains("github", true) &&
            (q.contains("upload", true) || q.contains("unggah", true) || q.contains("kirim", true))) {
            val uri = bitAttachmentUri!!
            val name = bitAttachmentName.ifBlank { "upload.bin" }
            bitChatMessages.add(BitChatMessage(q.ifBlank { "Upload $name ke GitHub" }, true))
            addBitMessageView(bitChatMessages.last())
            bitChatMessages.add(BitChatMessage("Siap. Aku akan meng-upload **$name** ke GitHub. Tentukan nama/path file di repository.", false))
            addBitMessageView(bitChatMessages.last())
            showBitGithubUploadDialog(uri, name)
            return
        }

        if (q.isBlank() && bitAttachmentUri != null) {
            bitChatMessages.add(BitChatMessage("Lampiran: $bitAttachmentName", true))
            addBitMessageView(bitChatMessages.last())
            bitChatMessages.add(BitChatMessage("File sudah dipilih. Kamu bisa memberi perintah, misalnya: \"upload file ini ke GitHub\".", false))
            addBitMessageView(bitChatMessages.last())
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            return
        }

        bitChatMessages.add(BitChatMessage(q, true))
        addBitMessageView(bitChatMessages.last())
        val result = answerBitLocally(q)
        bitChatMessages.add(BitChatMessage(result.text, false, result.actions))
        addBitMessageView(bitChatMessages.last())
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        result.openToolId?.let { toolId ->
            input.postDelayed({
                if (!isFinishing) {
                    val action = BitActionBridge.forTool(toolId)
                    BitActionBridge.execute(this, action)
                }
            }, 350L)
        }
        if (result.openGoogle) {
            input.postDelayed({ openBitGoogleSearch(q) }, 450L)
        }
    }

    send.setOnClickListener { submit() }
    input.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
            submit()
            true
        } else false
    }
}

internal fun MainActivity.showBitAttachmentMenu(anchor: View) {
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "Upload file")
    popup.menu.add(0, 2, 1, "Upload foto")
    popup.menu.add(0, 3, 2, "Upload text / dokumen")
    popup.setOnMenuItemClickListener {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = when (it.itemId) {
                2 -> "image/*"
                3 -> "text/*"
                else -> "*/*"
            }
            if (it.itemId == 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("*/*"))
            } else if (it.itemId == 3) {
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/*", "application/json", "application/xml"))
            }
        }
        startActivityForResult(intent, BIT_ATTACHMENT_PICK_REQUEST)
        true
    }
    popup.show()
}

internal fun MainActivity.showBitGithubUploadDialog(uri: Uri, displayName: String) {
    val pathInput = EditText(this).apply {
        setText(displayName)
        setSelectAllOnFocus(true)
        hint = "path/file.ext"
        setSingleLine(true)
        setPadding(dp(14), 0, dp(14), 0)
    }
    val box = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), 0, dp(20), 0)
        addView(pathInput, LinearLayout.LayoutParams(-1, dp(50)))
    }
    AlertDialog.Builder(this)
        .setTitle("Upload ke GitHub")
        .setMessage("File akan di-upload ke repository GitHub yang tersimpan di Pengaturan GitHub.")
        .setView(box)
        .setNegativeButton("Batal", null)
        .setPositiveButton("Upload") { _, _ ->
            val path = pathInput.text.toString().trim().trim('/')
            if (path.isBlank()) {
                toast("Nama/path file wajib diisi")
                return@setPositiveButton
            }
            bitChatMessages.add(BitChatMessage("Upload → $path", true))
            addBitMessageView(bitChatMessages.last())
            bitUploadAttachmentToGithub(uri, path)
        }
        .show()
}

internal fun MainActivity.bitUploadAttachmentToGithub(uri: Uri, path: String) {
    val token = ghTokenValue.ifBlank {
        prefs.getString("gh_token_enc", null)?.let { GithubTokenVault.decrypt(it) }.orEmpty()
    }
    if (ghUserValue.isBlank() || ghRepoValue.isBlank() || token.isBlank()) {
        bitChatMessages.add(BitChatMessage("Pengaturan GitHub belum lengkap. Buka GitHub Publisher dan simpan username, repository, branch, serta token terlebih dahulu.", false))
        addBitMessageView(bitChatMessages.last())
        return
    }

    val face = bitChatTopFace
    runCatching { face?.playAnimation() }
    bitChatMessages.add(BitChatMessage("Sedang meng-upload `$path` ke ${ghUserValue}/${ghRepoValue}…", false))
    addBitMessageView(bitChatMessages.last())
    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

    toolThread {
        val result = runCatching { ghUploadSingleFileToGithub(uri, path, token) }
        runOnUiThread {
            if (result.isSuccess) {
                bitChatMessages.add(BitChatMessage("Upload berhasil. `$path` sudah dikirim ke GitHub.", false))
            } else {
                val error = result.exceptionOrNull()
                val safe = ghDeleteError(error ?: IOException("Upload gagal"))
                bitChatMessages.add(BitChatMessage("Upload GitHub gagal:\n$safe", false))
            }
            addBitMessageView(bitChatMessages.last())
            bitAttachmentUri = null
            bitAttachmentName = ""
            bitAttachmentMime = ""
            runCatching { face?.playAnimation() }
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            setupBitChatInput()
        }
    }
}

internal fun MainActivity.answerBitLocally(question: String): BitAnswer {
        val q = question.trim()
        if (q.isBlank()) return BitAnswer("Tulis pertanyaan atau perintah dulu.", false)

        val lower = q.lowercase(Locale.ROOT)

        // Step 12: jawab pertanyaan status berdasarkan konteks proses real-time.
        val asksProcessStatus = listOf("status", "proses", "berapa persen", "sampai mana", "sedang apa", "progress", "gimana proses", "kenapa gagal")
            .any { lower.contains(it) }
        if (asksProcessStatus) {
            return BitAnswer("${BitRuntimeContext.processSummary()}. Aku hanya melihat status proses yang aman; token dan credential tidak tersedia untukku.", false)
        }

        // Step 7: gunakan konteks runtime non-rahasia untuk memahami "yang tadi",
        // "buka lagi", "coba lagi", dan perintah lanjutan lainnya.
        val contextual = listOf("yang ini", "yang tadi", "buka lagi", "jalankan lagi", "coba lagi", "ulang", "lanjutkan")
            .any { lower.contains(it) }
        if (contextual) {
            val lastId = BitRuntimeContext.lastToolId
            if (lastId != null) {
                val lastName = homeToolMap[lastId] ?: BitRuntimeContext.lastToolName ?: lastId
                val status = BitRuntimeContext.lastStatus
                val result = BitRuntimeContext.lastResultSummary
                val detail = when {
                    status == "error" -> " Status terakhir tercatat gagal${result?.let { ": $it" } ?: "."}"
                    !result.isNullOrBlank() -> " Status terakhir: $status. $result"
                    !status.isNullOrBlank() -> " Status terakhir: $status."
                    else -> ""
                }
                return BitAnswer("Aku melanjutkan tool terakhir: $lastName.$detail", false, lastId)
            }
        }

        // Step 19: diagnosis & perbaikan error kode di Text Editor (lokal, tanpa kirim kode).
        val asksEditorDiagnosis = listOf(
            "cek error", "cek kode", "periksa kode", "periksa error", "analisis kode", "analisa kode",
            "cari error", "error di editor", "error editor", "syntax error", "sintaks error",
            "kode error", "kode saya error", "perbaiki kode", "perbaiki error", "betulkan kode",
            "benerin kode", "auto fix", "autofix", "validasi json", "json error", "html error",
            "js error", "javascript error"
        ).any { lower.contains(it) }
        if (asksEditorDiagnosis) return bitEditorDiagnosis()

        // Step 6: Bit membaca registry kemampuan APK. Registry hanya berisi ID/nama tool.
        // Credential/token tetap berada di tool masing-masing dan tidak pernah dikirim ke Bit.
        val match = BitToolRegistry.find(q, homeTools)
        if (match != null) {
            val actionText = when (match.id) {
                "githubzip" -> "Aku akan membuka GitHub Publisher. Token GitHub tetap dikelola oleh fitur GitHub dan tidak ditampilkan ke Bit."
                else -> "Aku mengenali permintaan ini sebagai fitur ${match.name}. ${BitToolKnowledge.enrich(match.id, match.name)}"
            }
            // Step 11: Bit hanya mengeluarkan ID aksi. Credential tetap berada di tool.
            val action = BitActionBridge.forTool(match.id)
            val bridgeText = when (action.id) {
                "GITHUB_UPLOAD" -> " Aksi: upload GitHub."
                "ZIP" -> " Aksi: ZIP."
                "SCAN_QR" -> " Aksi: scan QR."
                else -> ""
            }
            return BitAnswer(actionText + bridgeText, false, match.id)
        }

        val error = listOf("error", "eror", "gagal", "tidak bisa", "nggak bisa", "gak bisa", "crash", "force close", "permission", "izin")
            .any { lower.contains(it) }

        return when {
            lower.contains("halo") || lower == "hai" || lower == "hi" ->
                BitAnswer("Halo. Aku Bit. Aku bisa membantu menjalankan fitur yang tersedia di MyTools.", false)
            error && lower.contains("github") -> {
                BitRuntimeContext.onToolError("Pengguna melaporkan masalah GitHub")
                BitAnswer("Kalau GitHub gagal, coba lagi dari GitHub Publisher. Bit tidak melihat token; fitur GitHub yang menangani credential.", false, if (BitRuntimeContext.lastToolId == "githubzip") "githubzip" else null)
            }
            lower.contains("cara") && (lower.contains("tool") || lower.contains("fitur") || lower.contains("mytools")) ->
                BitAnswer("Sebutkan fitur yang ingin digunakan. Aku akan mencocokkannya dengan tool yang tersedia di APK.", false)
            lower.contains("apa itu") && (lower.contains("sha") || lower.contains("hash")) ->
                BitAnswer("Hash adalah nilai ringkas yang dihasilkan dari data. SHA-256 sering digunakan untuk memeriksa integritas file.", false)
            else ->
                BitAnswer("Aku belum menemukan tool atau pengetahuan lokal yang cocok. Aku akan mencarikannya di Google.", true)
        }
    }
internal fun MainActivity.bitEditorDiagnosis(): BitAnswer {
        val box = editorBox
            ?: return BitAnswer("Belum ada file terbuka di Text Editor. Buka file .js, .html, .css, atau .json dulu, lalu minta aku memeriksanya.", false, "editor")
        val mode = editorMode
        if (!CodeDiagnostics.supports(mode)) {
            return BitAnswer("Editor sedang membuka file ${editorModeName(mode)}. Pemeriksaan error otomatis tersedia untuk JS, HTML, CSS, dan JSON.", false)
        }
        val issues = EditorDiagnostics.analyzeNow(box.text.toString(), mode)
        val errors = issues.count { it.severity == CodeDiagnostics.Severity.ERROR }
        val warns = issues.size - errors
        BitRuntimeContext.onToolResult(
            if (issues.isEmpty()) "ok" else "diagnosed",
            "Editor ${editorModeName(mode)}: $errors error, $warns peringatan"
        )
        if (issues.isEmpty()) {
            return BitAnswer("Aku memeriksa file ${editorModeName(mode)} di editor dan tidak menemukan error struktur. Ini pemeriksaan dasar (kurung, kutip, tag, koma), bukan pengganti menjalankan kodenya.", false)
        }
        val top = issues.take(5).joinToString("\n") { "• Baris ${it.line}: ${it.message}" }
        val more = if (issues.size > 5) "\n…dan ${issues.size - 5} lainnya." else ""
        val fixable = issues.count { it.fix != null }
        val fixNote = if (fixable > 0) "\n$fixable bisa diperbaiki otomatis. Kamu akan melihat pratinjau dulu sebelum perubahan diterapkan." else ""
        val actions = mutableListOf(BitActionBridge.editorDiagnose())
        if (fixable > 0) actions.add(BitActionBridge.editorFixPreview())
        return BitAnswer(
            "Aku menemukan $errors error dan $warns peringatan di file ${editorModeName(mode)}:\n$top$more$fixNote",
            false, null, actions
        )
    }
internal fun MainActivity.addBitMessage(text: String, fromUser: Boolean) {
        val message = BitChatMessage(text, fromUser)
        bitChatMessages.add(message)
        addBitMessageView(message)
    }
internal fun MainActivity.addBitMessageView(message: BitChatMessage) {
    val activity = this
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (message.fromUser) Gravity.END else Gravity.START
            setPadding(0, dp(3), 0, dp(3))
        }
        val bubble = TextView(this).apply {
            text = message.text
            textSize = 14f
            setTextColor(if (isDarkTheme) Color.WHITE else Color.rgb(35, 42, 48))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = bg(
                if (message.fromUser) {
                    if (isDarkTheme) Color.rgb(50, 75, 95) else Color.rgb(224, 240, 255)
                } else {
                    if (isDarkTheme) Color.rgb(42, 42, 46) else Color.rgb(245, 247, 249)
                }, 18
            )
            maxWidth = (resources.displayMetrics.widthPixels * 0.82f).roundToInt()
        }
        val bubbleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        bubbleColumn.addView(bubble, LinearLayout.LayoutParams(-2, -2))
        if (message.actions.isNotEmpty()) {
            val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            message.actions.forEach { act ->
                chips.addView(TextView(this).apply {
                    text = act.label ?: act.id
                    textSize = 12f
                    setTextColor(textMain)
                    setPadding(dp(12), dp(7), dp(12), dp(7))
                    background = bg(if (isDarkTheme) Color.rgb(50, 75, 95) else Color.rgb(224, 240, 255), 14)
                    isClickable = true
                    setOnClickListener { BitActionBridge.execute(activity, act) }
                }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6); topMargin = dp(6) })
            }
            bubbleColumn.addView(chips, LinearLayout.LayoutParams(-2, -2))
        }
        row.addView(bubbleColumn, LinearLayout.LayoutParams(-2, -2).apply {
            leftMargin = if (message.fromUser) dp(52) else 0
            rightMargin = if (message.fromUser) 0 else dp(52)
        })
        content.addView(row, LinearLayout.LayoutParams(-1, -2))
    }
internal fun MainActivity.openBitGoogleSearch(question: String) {
        val encoded = java.net.URLEncoder.encode(question, StandardCharsets.UTF_8.name())
        val uri = Uri.parse("https://www.google.com/search?q=$encoded")
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { Toast.makeText(this, "Tidak bisa membuka Google", Toast.LENGTH_SHORT).show() }
    }
internal fun MainActivity.applyBitFaceTheme(view: LottieAnimationView) {
        val body = if (isDarkTheme) Color.WHITE else Color.rgb(24, 28, 36)
        val bodyEdge = if (isDarkTheme) Color.rgb(209, 214, 235) else Color.rgb(24, 28, 36)
        val eyes = if (isDarkTheme) Color.rgb(20, 20, 26) else Color.WHITE
        val zzz = if (isDarkTheme) Color.WHITE else Color.rgb(24, 28, 36)
        view.addValueCallback(KeyPath("BODY", "**"), LottieProperty.COLOR, LottieValueCallback(body))
        view.addValueCallback(KeyPath("BODY", "**"), LottieProperty.STROKE_COLOR, LottieValueCallback(bodyEdge))
        view.addValueCallback(KeyPath("EYES", "**"), LottieProperty.STROKE_COLOR, LottieValueCallback(eyes))
        view.addValueCallback(KeyPath("ZZ", "**"), LottieProperty.STROKE_COLOR, LottieValueCallback(zzz))
    }
internal fun MainActivity.applyBitFaceTheme() {
        bitFace?.let { applyBitFaceTheme(it) }
    }
internal fun MainActivity.playBitThinking() {
    bitAnim?.playThinking(bitFace)
}

internal fun MainActivity.startBitAutonomousLoop() {
    bitAnim?.playIdleAll()
}
