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


internal fun MainActivity.ensureToolLoadingOverlay() {
        if (toolLoadingOverlay != null) return
        val rootFrameView = runCatching { rootFrame }.getOrNull() ?: return
        val overlay = FrameLayout(this).apply {
            setPadding(dp(14), dp(8), dp(16), dp(8))
            elevation = dp(10).toFloat()
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(if (isDarkTheme) 0xEE202124.toInt() else 0xEEFFFFFF.toInt())
            }
        }
        val loadingTint = if (isDarkTheme) Color.WHITE else Color.rgb(32, 33, 36)
        val spinner = ProgressBar(this).apply {
            isIndeterminate = true
            // Loading indicator mengikuti tema: gelap pada light mode, putih pada dark mode.
            indeterminateTintList = ColorStateList.valueOf(loadingTint)
            layoutParams = FrameLayout.LayoutParams(dp(26), dp(26)).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = dp(2)
            }
        }
        val label = TextView(this).apply {
            text = "Memproses…"
            textSize = 13f
            setTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
            maxLines = 1
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(24)
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = dp(36)
                rightMargin = dp(2)
            }
        }
        overlay.addView(spinner)
        overlay.addView(label)
        overlay.visibility = View.GONE
        rootFrameView.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, dp(48)
        ).apply {
            // Letakkan loading dekat pusat layar, bukan menempel di bawah status/top bar.
            gravity = Gravity.CENTER
            topMargin = dp(18)
        })
        toolLoadingOverlay = overlay
        toolLoadingSpinner = spinner
        toolLoadingText = label
    }
internal fun MainActivity.showToolLoading() {
        if (isFinishing) return
        ensureToolLoadingOverlay()
        val overlay = toolLoadingOverlay ?: return
        // Batalkan fade-out yang masih berjalan agar tidak menyembunyikan overlay yang baru muncul.
        overlay.animate().cancel()
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.translationY = dp(-10).toFloat()
        overlay.scaleX = 0.96f
        overlay.scaleY = 0.96f
        overlay.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
        toolLoadingSpinner?.let { Motion.startLoading(it) }
    }
internal fun MainActivity.hideToolLoading() {
        val overlay = toolLoadingOverlay ?: return
        if (overlay.visibility != View.VISIBLE) return
        toolLoadingSpinner?.let { Motion.stopLoading(it) }
        overlay.animate().cancel()
        overlay.animate().alpha(0f).setDuration(120L).withEndAction {
            // Jangan sembunyikan bila ada task baru yang sudah menampilkan loading lagi.
            if (activeToolTasks.get() <= 0) {
                overlay.visibility = View.GONE
                overlay.alpha = 1f
            }
        }.start()
    }
internal fun MainActivity.toolThread(label: String = "Memproses…", showLoading: Boolean = true, block: () -> Unit) {
        activeToolTasks.incrementAndGet()
        val showRunnable = Runnable {
            if (activeToolTasks.get() > 0 && !isFinishing) {
                ensureToolLoadingOverlay()
                toolLoadingText?.text = label
                showToolLoading()
            }
        }
        if (showLoading) {
            toolLoadingHandler.postDelayed(showRunnable, 280L)
        }
        // Shared bounded pool — avoids creating unlimited threads under load.
        try {
            ToolPerformance.toolExecutor.execute {
                try {
                    block()
                } finally {
                    toolLoadingHandler.removeCallbacks(showRunnable)
                    runOnUiThread {
                        if (activeToolTasks.decrementAndGet() <= 0) {
                            activeToolTasks.set(0)
                            hideToolLoading()
                        }
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            toolLoadingHandler.removeCallbacks(showRunnable)
            if (activeToolTasks.decrementAndGet() <= 0) activeToolTasks.set(0)
            toast("Terlalu banyak proses aktif. Coba lagi sebentar.")
            hideToolLoading()
        }
    }
internal fun MainActivity.isTouchInsideNonPagingArea(rawX: Float, rawY: Float): Boolean {
        // Do not hijack swipes that start on the fixed toolbar/bottom navigation
        // or inside a horizontal filter row (for example the category chips).
        fun contains(v: View): Boolean {
            if (v.visibility != View.VISIBLE) return false
            val loc = IntArray(2)
            v.getLocationOnScreen(loc)
            return rawX >= loc[0] && rawX <= loc[0] + v.width &&
                    rawY >= loc[1] && rawY <= loc[1] + v.height
        }

        if (contains(topBar) || contains(bottomNav)) return true

        fun hasHorizontalScroller(v: View): Boolean {
            if (v.visibility != View.VISIBLE) return false
            if (v is HorizontalScrollView && contains(v)) return true
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) {
                    if (hasHorizontalScroller(v.getChildAt(i))) return true
                }
            }
            return false
        }
        return hasHorizontalScroller(rootFrame)
    }
internal fun MainActivity.swipeRootPage(next: Boolean) {
        val index = rootPageOrder.indexOf(currentPage)
        if (index < 0) return
        val targetIndex = if (next) index + 1 else index - 1
        if (targetIndex !in rootPageOrder.indices) {
            // Small edge feedback instead of silently doing nothing.
            if (animationsEnabled()) {
                content.animate().cancel()
                content.animate().translationX(if (next) -dp(10).toFloat() else dp(10).toFloat())
                    .setDuration(70L)
                    .withEndAction {
                        content.animate().translationX(0f).setDuration(120L).start()
                    }.start()
            }
            return
        }

        val target = rootPageOrder[targetIndex]
        val render: () -> Unit = {
            when (target) {
                "home" -> showHome()
                "all" -> showAllTools()
                "favorites" -> showFavorites()
                "settings" -> showSettings()
            }
        }

        // Re-render first, then slide the new page in from the swipe direction.
        // This keeps the existing navigation/state code as the single source of truth.
        navigateRoot(render = render)
        if (!animationsEnabled()) {
            content.translationX = 0f
            content.alpha = 1f
            return
        }
        Motion.pageSlide(content, if (next) 42f else -42f)
    }
internal fun MainActivity.isEditorWorkspace(): Boolean =
        !editorLanding && (currentPage == "Editor" || currentPage.startsWith("Editor - ")) && editorBox != null

internal fun MainActivity.syncEditorBottomBar() {
        val editorBottomBarView = runCatching { editorBottomBar }.getOrNull() ?: return
        val show = isEditorWorkspace()
        editorBottomBarView.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) {
            editorBottomBarView.alpha = 1f
            editorBottomBarView.translationY = 0f
        }
    }
internal fun MainActivity.enableImmersiveFullscreen() {
        // oldt.py explicitly keeps Android system bars visible.
        // Full layar: konten digambar sampai belakang status bar & gesture bar.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = EDGE_FLAGS or
                (if (Build.VERSION.SDK_INT >= 23) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0)
    }
internal fun MainActivity.showStartupFallback(error: Throwable) {
        // Last-resort launch screen: if a future UI change breaks showHome(),
        // keep the APK open instead of letting the Activity crash back to launcher.
        runCatching {
            loginScreen.visibility = View.GONE
            mainContainer.visibility = View.VISIBLE
            bottomNav.visibility = View.GONE
            search.visibility = View.GONE
            content.removeAllViews()
            content.setPadding(dp(20), dp(24), dp(20), dp(24))
            content.addView(label("GITLS", 26f, true), LinearLayout.LayoutParams(-1, -2))
            content.addView(subLabel("Aplikasi berhasil dibuka, tetapi halaman utama gagal dimuat.", 14f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            content.addView(subLabel("Coba buka ulang aplikasi. Data lokal tidak dihapus.", 12f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            val retry = Button(this).apply {
                text = "Coba Muat Ulang"
                setOnClickListener {
                    runCatching { enterApp() }
                        .onFailure { android.util.Log.e("GITLS", "Retry startup failed", it) }
                }
            }
            content.addView(retry, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(20) })
        }.onFailure {
            android.util.Log.e("GITLS", "Startup fallback also failed", it)
            Toast.makeText(this, "GITLS gagal memuat tampilan utama.", Toast.LENGTH_LONG).show()
        }
    }
internal fun MainActivity.setupDynamicShortcuts() {
        if (Build.VERSION.SDK_INT < 25) return
        val sm = getSystemService(ShortcutManager::class.java) ?: return
        val icon = android.graphics.drawable.Icon.createWithResource(this, R.mipmap.app_icon)
        val shortcuts = listOf(
            ShortcutInfo.Builder(this, "expense")
                .setShortLabel("+ Pengeluaran")
                .setLongLabel("Tambah pengeluaran")
                .setIcon(icon)
                .setIntent(Intent(this, MainActivity::class.java).putExtra("quick_expense", true))
                .build(),
            ShortcutInfo.Builder(this, "income")
                .setShortLabel("+ Pemasukan")
                .setLongLabel("Tambah pemasukan")
                .setIcon(icon)
                .setIntent(Intent(this, MainActivity::class.java).putExtra("quick_income", true))
                .build(),
            ShortcutInfo.Builder(this, "finance")
                .setShortLabel("Keuangan")
                .setLongLabel("Finance Dashboard")
                .setIcon(icon)
                .setIntent(Intent(this, MainActivity::class.java).putExtra("open_finance", true))
                .build(),
            ShortcutInfo.Builder(this, "iot")
                .setShortLabel("ESP Studio")
                .setLongLabel("ESP Studio & Visual Wiring")
                .setIcon(icon)
                .setIntent(Intent(this, MainActivity::class.java).putExtra("open_iot", true))
                .build()
        )
        sm.dynamicShortcuts = shortcuts
    }
internal fun MainActivity.scheduleFinanceMaintenance() {
        val request = PeriodicWorkRequest.Builder(FinanceMaintenanceWorker::class.java, 24, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("mytools_finance_maintenance", ExistingPeriodicWorkPolicy.KEEP, request)
    }
internal fun MainActivity.applySystemTheme() {
        val forced = runCatching { prefs.getString("theme_mode", "system") ?: "system" }.getOrDefault("system")
        val ui = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        isDarkTheme = when (forced) {
            "dark" -> true
            "light" -> false
            else -> ui != Configuration.UI_MODE_NIGHT_NO
        }
        if (!isDarkTheme) {
            dark = Color.rgb(248, 248, 250); panel = Color.rgb(255, 255, 255); panel2 = Color.rgb(242, 242, 246)
            textMain = Color.rgb(24, 24, 28); textMuted = Color.rgb(100, 100, 108); line = Color.rgb(215, 215, 222)
        } else {
            dark = Color.rgb(10, 10, 11); panel = Color.rgb(22, 22, 24); panel2 = Color.rgb(28, 28, 31)
            textMain = Color.rgb(245, 245, 247); textMuted = Color.rgb(155, 155, 160); line = Color.rgb(48, 48, 52)
        }

        // Apply the selected theme to Android system bars immediately.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 23) {
            var flags = EDGE_FLAGS or (if (isDarkTheme) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            if (Build.VERSION.SDK_INT >= 26 && !isDarkTheme) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }
internal fun MainActivity.applyBottomNavShape() {
        // Keep the navigation container rounded. Calling setBackgroundColor() here
        // would replace the rounded drawable with a sharp rectangle.
        val backgroundColor = if (isDarkTheme) panel else Color.WHITE
        bottomNav.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            cornerRadius = dp(30).toFloat()
            setStroke(dp(1), if (isDarkTheme) line else Color.rgb(225, 230, 235))
        }
        bottomNav.clipToOutline = true
        if (Build.VERSION.SDK_INT >= 21) bottomNav.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(30).toFloat())
            }
        }
    }
internal fun MainActivity.applyUiColors() {
        mainContainer.setBackgroundColor(dark)
        content.setBackgroundColor(dark)

        // Search bar harus hanya punya SATU background berbentuk pill.
        // Jangan gunakan setBackgroundColor() karena itu mengganti drawable
        // rounded menjadi kotak biasa.
        search.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(panel)
            cornerRadius = dp(24).toFloat()
            setStroke(dp(1), line)
        }
        search.setTextColor(textMain)
        search.setHintTextColor(textMuted)
        styleTopFabs()
        search.setPadding(dp(16), 0, dp(16), 0)

        applyBottomNavShape()
    }
internal fun MainActivity.enterApp() {
        loginScreen.visibility = View.GONE
        mainContainer.visibility = View.VISIBLE
        // Do not force Light here: the Theme setting must be respected on every
        // entry into the app, including after login, shortcuts, and deep links.
        applySystemTheme()
        applyUiColors()
        showHome()
    }
internal fun MainActivity.applyLightAppTheme() {
        isDarkTheme = false
        dark = Color.rgb(255, 255, 255)
        panel = Color.rgb(248, 250, 252)
        panel2 = Color.rgb(244, 246, 248)
        textMain = Color.rgb(15, 15, 16)
        textMuted = Color.rgb(123, 135, 148)
        line = Color.rgb(225, 230, 235)
        mainContainer.setBackgroundColor(Color.WHITE)
        content.setBackgroundColor(Color.WHITE)
        applyBottomNavShape()
        search.setBackgroundResource(com.example.aidetest.R.drawable.bg_search_light)
        search.setTextColor(textMain)
        search.setHintTextColor(textMuted)
        styleTopFabs()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility = EDGE_FLAGS or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        }
    }
internal fun MainActivity.visualTheme(name: String = currentPage): ToolVisualTheme {
        // MyTools uses one consistent monochrome UI. Tool categories may still
        // have different labels, but never introduce colored buttons/accent panels.
        return ToolVisualTheme(
            textMain,
            panel2,
            line,
            when {
                name.contains("esp", true) || name.contains("iot", true) -> "HARDWARE"
                name.contains("jaringan", true) || name.contains("network", true) || name.contains("dns", true) || name.contains("ping", true) || name.contains("port", true) || name.contains("http", true) || name.contains("ssl", true) -> "NETWORK"
                name.contains("security", true) || name.contains("password", true) || name.contains("token", true) || name.contains("aes", true) || name.contains("hmac", true) || name.contains("jwt", true) || name.contains("hash", true) -> "SECURITY"
                name.contains("keuangan", true) || name.contains("finance", true) || name.contains("dca", true) || name.contains("loan", true) || name.contains("margin", true) || name.contains("discount", true) || name.contains("bunga", true) -> "FINANCE"
                name.contains("file", true) || name.contains("zip", true) || name.contains("apk", true) || name.contains("storage", true) || name.contains("folder", true) -> "FILES"
                name.contains("color", true) || name.contains("pipet", true) || name.contains("sprite", true) || name.contains("qr", true) || name.contains("ocr", true) -> "VISUAL"
                name.contains("editor", true) || name.contains("json", true) || name.contains("xml", true) || name.contains("regex", true) || name.contains("base64", true) || name.contains("text", true) || name.contains("unicode", true) -> "DEVELOPER"
                name.contains("battery", true) || name.contains("device", true) || name.contains("system", true) -> "SYSTEM"
                else -> "UTILITY"
            },
            textMain,
            if (isDarkTheme) Color.rgb(15, 15, 16) else Color.WHITE
        )
    }
internal fun MainActivity.toolAccentStrip(name: String): View {
    val activity = this
        val t = visualTheme(name)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = bg(t.surface, 14, t.border)
            addView(View(activity).apply { background = bg(t.accent, 3) }, LinearLayout.LayoutParams(dp(5), dp(30)))
            addView(TextView(activity).apply {
                text = t.chip
                textSize = 10f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(t.accent)
                setPadding(dp(10), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(activity).apply {
                text = "● READY"
                textSize = 10f
                setTextColor(Color.rgb(105, 110, 116))
            })
        }
    }
internal fun MainActivity.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

internal fun MainActivity.animationsEnabled(): Boolean = Motion.enabled

internal fun MainActivity.systemReduceMotion(): Boolean = runCatching {
        android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

internal fun MainActivity.animateEditorItem(view: View, delay: Long = 0L, distance: Float = 18f) =
        Motion.enter(view, delay, distance, 240L)

internal fun MainActivity.animateEditorPress(view: View) = Motion.tap(view, 0.96f)

internal fun MainActivity.animateEditorScreen() = Motion.enter(content, 0L, 8f, 220L)

internal fun MainActivity.navigateRoot(targetPage: String? = null, render: () -> Unit) {
        if (restoringSnapshot) return
        if (targetPage != null &&
            currentPage == targetPage &&
            content.childCount > 0 &&
            !resettingRootNavigation
        ) {
            selectBottomNav(targetPage)
            return
        }
        pageBackStack.clear()
        resettingRootNavigation = true
        try { render() } finally { resettingRootNavigation = false }
    }
internal fun MainActivity.clearPage(name: String = currentPage, pushBack: Boolean = true) {
        if (!restoringSnapshot && pushBack && !resettingRootNavigation) {
            saveCurrentPageSnapshot()
        }
        content.removeAllViews()
        modernUiRunnable?.let { modernUiHandler.removeCallbacks(it) }
        modernUiRunnable = null
        currentPage = name
        if (name != "Bit Assistant") {
            bitChatTopFace?.let { face ->
                (face.parent as? ViewGroup)?.removeView(face)
            }
            bitChatTopFace = null
        }
        // Top bar (hamburger, cari, profil) disembunyikan oleh workspace Editor lewat
        // topBarVisibility(false). Kembalikan setiap halaman baru dibuka; renderEditorPage()
        // akan menyembunyikannya lagi sendiri bila memang perlu.
        if (topBar.visibility != View.VISIBLE) topBarVisibility(true)

        val root = name == "home" || name == "all" || name == "favorites" || name == "settings"
        val isHome = name == "home"
        // Halaman "Semua Tools" tidak memakai search bar. Search di sini dulu
        // ikut terhubung ke listener scroll sehingga saat pengguna naik-turun
        // halaman, bar bisa snap berulang dan membuat tampilan terasa meloncat.
        // Search tersedia di Beranda dan Favorit. Semua Tools memakai filter chip
        // sendiri agar tidak membuat dua sistem pencarian saling bertabrakan.
        val showRootSearch = name == "home" || name == "favorites"

        title.text = when (name) {
            "home" -> "GITLS"
            "all" -> "Semua Tools"
            "favorites" -> "Favorit"
            "settings" -> "Pengaturan"
            else -> name
        }
        // GitHub Publisher memakai judul global yang ringkas di antara tombol
        // kembali (<) dan menu (⋮), sehingga tidak membutuhkan kartu header besar.
        findViewById<View>(R.id.headerTitleBox)?.let { header ->
            header.visibility = if (name == "GitHub Publisher") View.VISIBLE else View.GONE
        }
        if (name == "GitHub Publisher") {
            title.text = "GITLS Publisher"
            title.textSize = 16f
            title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
        }
        subtitle.visibility = if (isHome) View.VISIBLE else View.GONE
        homeMenu.visibility = if (isHome) View.VISIBLE else View.GONE
        homeSearch.visibility = if (showRootSearch) View.VISIBLE else View.GONE
        homeProfile.visibility = if (isHome) View.VISIBLE else View.GONE
        back.visibility = if (isHome) View.GONE else View.VISIBLE
        action.visibility = if (isHome) View.GONE else View.VISIBLE

        searchSnapEnabled = showRootSearch
        resetSearchSnap(showRootSearch)
        // Toolbar aksi editor hanya boleh hidup ketika benar-benar berada di editor.
        // Sebelumnya toolbar tetap terlihat setelah keluar dari Preview karena
        // editorBottomBar adalah view global di layout utama dan clearPage()
        // tidak mengubah visibility-nya.
        // Landing Editor juga bukan workspace edit — toolbar bawah harus hilang.
        if (name != "Editor" || editorLanding) {
            editorBottomBar.visibility = View.GONE
            editorBottomBar.alpha = 1f
            editorBottomBar.translationY = 0f
        }
        bottomNav.visibility = if (root && !imeVisible) View.VISIBLE else View.GONE
        bottomNav.translationY = 0f
        if (root) selectBottomNav(name)
        configureActionForPage(name)
        // Normalize after the page renderer has finished adding its content.
        // This removes any page-local title that duplicates the global toolbar title.
        content.post { normalizeToolContentHeader() }
        scroll.post { scroll.scrollTo(0, 0) }
    }
internal fun MainActivity.addPressFeedback(view: View) = Motion.press(view)

internal fun MainActivity.applyInteractiveSurface(view: View, radius: Int = Ds.RADIUS_LG, elevationDp: Int = 1) {
        view.background = rippleBg(if (isDarkTheme) panel2 else Color.WHITE, radius, line)
        if (elevationDp > 0) view.elevation = dp(elevationDp).toFloat()
        view.clipToOutline = true
    }
internal fun MainActivity.label(text: String, sizeSp: Float = 15f, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(textMain)
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
            includeFontPadding = true
        }

internal fun MainActivity.subLabel(text: String, sizeSp: Float = 12f): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(textMuted)
            includeFontPadding = true
        }

internal fun MainActivity.toolCard(id: String, name: String, iconName: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            contentDescription = "$name. Buka tool"
            minimumHeight = dp(64)
        }
        applyInteractiveSurface(card, Ds.RADIUS_LG, 1)
        card.setOnClickListener { openToolWithPress(id, card) }

        val iconBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = bg(if (isDarkTheme) Color.rgb(42, 42, 46) else Color.rgb(238, 242, 245), 13)
        }
        iconBox.addView(MdiIconView(this).apply {
            setIconName(iconName)
            setIconSize(21f)
            setTextColor(if (isDarkTheme) Color.WHITE else Color.rgb(70, 80, 90))
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        card.addView(iconBox, LinearLayout.LayoutParams(dp(42), dp(42)))
        // Sentuhan kartu: scale kecil + shadow turun + ikon bereaksi sesuai karakter Tool.
        val toolCharacter = Motion.characterFor(id)
        Motion.press(card, 0.98f, shadowDp = -1f, onDown = {
            Motion.characterIcon(iconBox.getChildAt(0) ?: iconBox, toolCharacter)
        })

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(6), 0)
        }
        texts.addView(label(name, 13.5f, true))
        texts.addView(subLabel(toolHelp(id, name).purpose, 10.5f).apply {
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(MdiIconView(this).apply {
            setIconName("information-outline")
            setIconSize(18f)
            setTextColor(textMuted)
            contentDescription = "Info $name"
            setOnClickListener { showToolHelpDialog(id) }
        }, LinearLayout.LayoutParams(dp(36), dp(48)))
        card.addView(TextView(this).apply {
            text = "›"
            textSize = 24f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            contentDescription = "Buka"
        }, LinearLayout.LayoutParams(dp(28), dp(48)))
        return card
    }
internal fun MainActivity.bg(color: Int, radius: Int = 16, stroke: Int? = null): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

internal fun MainActivity.keepShapeFill(v: View, color: Int) {
        val d = v.background
        if (d is android.graphics.drawable.GradientDrawable) { d.mutate(); (v.background as android.graphics.drawable.GradientDrawable).setColor(color) }
        else v.setBackgroundColor(color)
    }
internal fun MainActivity.rippleBg(fill: Int, radius: Int = Ds.RADIUS_MD, stroke: Int? = null): Drawable {
        val base = bg(fill, radius, stroke)
        val rippleColor = ColorStateList.valueOf(
            if (isDarkTheme) Color.argb(48, 255, 255, 255) else Color.argb(36, 0, 0, 0)
        )
        val mask = bg(Color.WHITE, radius)
        return RippleDrawable(rippleColor, base, mask)
    }
internal fun MainActivity.statusColor(state: Ds.State): Int = Ds.statusColor(state, isDarkTheme)

internal fun MainActivity.isSecondaryAction(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        return listOf(
            "salin", "copy", "bagikan", "share", "bersihkan", "clear", "hapus", "reset",
            "batal", "cancel", "tutup", "close", "kembali", "back", "acak ulang"
        ).any { t == it || t.startsWith("$it ") }
    }
internal fun MainActivity.styleAsPrimary(b: TextView) {
        val theme = visualTheme()
        b.setTextColor(theme.onButton)
        b.background = rippleBg(theme.button, Ds.RADIUS_MD, theme.button)
    }
internal fun MainActivity.styleAsSecondary(b: TextView) {
        b.setTextColor(textMain)
        b.background = rippleBg(panel, Ds.RADIUS_MD, line)
    }
internal fun MainActivity.styleAsTertiary(b: TextView) {
        b.setTextColor(textMain)
        b.background = rippleBg(Color.TRANSPARENT, Ds.RADIUS_SM)
    }
internal fun MainActivity.secondaryButton(text: String, onClick: () -> Unit): Button =
        button(text, onClick).also { styleAsSecondary(it) }

internal fun MainActivity.tertiaryButton(text: String, onClick: () -> Unit): Button =
        button(text, onClick).also { styleAsTertiary(it) }

internal fun MainActivity.stateCard(
        state: Ds.State,
        title: String,
        message: String = "",
        onRetry: (() -> Unit)? = null
    ): LinearLayout {
        val tint = statusColor(state)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(Ds.SPACE_LG), dp(Ds.SPACE_XL), dp(Ds.SPACE_LG), dp(Ds.SPACE_XL))
            background = bg(panel2, Ds.RADIUS_LG, line)
            contentDescription = if (message.isBlank()) title else "$title. $message"
        }
        if (state == Ds.State.LOADING) {
            card.addView(ProgressBar(this).apply { isIndeterminate = true },
                LinearLayout.LayoutParams(dp(Ds.TOUCH_MIN), dp(Ds.TOUCH_MIN)))
        } else {
            card.addView(MdiIconView(this).apply {
                setIconName(Ds.stateIcon(state)); setIconSize(28f); setTextColor(tint)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
        }
        card.addView(label(title, 15f, true).apply { gravity = Gravity.CENTER; setTextColor(tint) })
        if (message.isNotBlank()) card.addView(subLabel(message, 13f).apply { gravity = Gravity.CENTER })
        if (onRetry != null && state == Ds.State.ERROR) {
            val retry = secondaryButton("Coba lagi", onRetry)
            card.addView(retry, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(Ds.SPACE_MD) })
        }
        return card
    }
internal fun MainActivity.addEmptyState(title: String = "Belum ada hasil", message: String = "Jalankan tool untuk melihat hasilnya") {
        content.addView(stateCard(Ds.State.EMPTY, title, message),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(Ds.SPACE_MD); bottomMargin = dp(Ds.SPACE_MD) })
    }
internal fun MainActivity.saveCurrentPageSnapshot() {
        if (restoringSnapshot || content.childCount == 0) return

        val rootPage = currentPage == "home" || currentPage == "all" ||
                currentPage == "favorites" || currentPage == "settings"

        // Root pages can contain dozens/hundreds of Views. Moving the entire View tree
        // into the Back stack caused visible jank and retained a lot of memory. Keep only
        // the page identity for roots and rebuild them on demand when Back is pressed.
        if (rootPage) {
            pageBackStack.addLast(
                PageSnapshot(
                    name = currentPage,
                    children = mutableListOf(),
                    scrollY = scroll.scrollY,
                    searchText = search.text?.toString() ?: "",
                    searchVisible = if (searchOpen) View.VISIBLE else View.GONE,
                    lightweight = true
                )
            )
            while (pageBackStack.size > 8) pageBackStack.removeFirst()
            return
        }

        val children = ArrayList<View>(content.childCount)
        while (content.childCount > 0) {
            children.add(content.getChildAt(0))
            content.removeViewAt(0)
        }
        pageBackStack.addLast(
            PageSnapshot(
                name = currentPage,
                children = children,
                scrollY = scroll.scrollY,
                searchText = search.text?.toString() ?: "",
                searchVisible = if (searchOpen) View.VISIBLE else View.GONE
            )
        )
        while (pageBackStack.size > 8) pageBackStack.removeFirst()
    }
internal fun MainActivity.restoreSnapshot(snapshot: PageSnapshot) {
        restoringSnapshot = true
        try {
            // Re-render root pages instead of restoring a huge retained View tree.
            // This keeps Back navigation smooth and prevents the app from accumulating
            // hundreds of detached card Views in memory.
            if (snapshot.lightweight) {
                when (snapshot.name) {
                    "home" -> showHome(homeFilter)
                    "all" -> showAllTools()
                    "favorites" -> showFavorites()
                    "settings" -> showSettings()
                    else -> showHome()
                }
                return
            }

            content.removeAllViews()
            snapshot.children.forEach { content.addView(it) }
            currentPage = snapshot.name
            if (snapshot.name == "Editor" && !editorLanding) {
                // Console/Preview temporarily shows the global toolbar so it has
                // its own Back/menu actions. When returning to the editor workspace,
                // those global actions must disappear again; otherwise the Console
                // toolbar remains visually stuck on top of the editor.
                topBarVisibility(false)
                subtitle.visibility = View.GONE
                homeMenu.visibility = View.GONE
                homeSearch.visibility = View.GONE
                homeProfile.visibility = View.GONE
                back.visibility = View.GONE
                action.visibility = View.GONE
                editorMore.visibility = View.GONE
            } else if (snapshot.name != "Editor" && topBar.visibility != View.VISIBLE) topBarVisibility(true)
            // ESP Studio memakai landscape. Saat tombol kembali ditekan, kembalikan
            // orientasi ke portrait agar layar benar-benar kembali ke posisi semula.
            if (snapshot.name != "IoT Dynamic Topology") {
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            val root = snapshot.name == "home" || snapshot.name == "all" || snapshot.name == "favorites" || snapshot.name == "settings"
            title.text = when (snapshot.name) {
                "home" -> "GITLS"
                "all" -> "Semua Tools"
                "favorites" -> "Favorit"
                "settings" -> "Pengaturan"
                else -> snapshot.name
            }
            subtitle.visibility = if (snapshot.name == "home") View.VISIBLE else View.GONE
            homeMenu.visibility = if (snapshot.name == "home") View.VISIBLE else View.GONE
            homeSearch.visibility = if (snapshot.name == "home" || snapshot.name == "favorites") View.VISIBLE else View.GONE
            homeProfile.visibility = if (snapshot.name == "home") View.VISIBLE else View.GONE
            action.visibility = if (snapshot.name == "home") View.GONE else View.VISIBLE
            back.visibility = if (snapshot.name == "home") View.GONE else View.VISIBLE
            resetSearchSnap(snapshot.name == "home" || snapshot.name == "favorites")
            suppressSearch = true
            search.setText(snapshot.searchText)
            suppressSearch = false
            if (searchSnapEnabled && (snapshot.searchVisible == View.VISIBLE || snapshot.searchText.isNotEmpty())) setSearchBoxOpen(true)
            bottomNav.visibility = if (root && !imeVisible) View.VISIBLE else View.GONE
            bottomNav.translationY = 0f
            // Toolbar aksi editor hanya tampil saat benar-benar berada di mode edit.
            // Saat halaman Preview dibuka toolbar disembunyikan, lalu dimunculkan lagi
            // ketika snapshot editor dipulihkan lewat tombol kembali.
            editorBottomBar.visibility = if (snapshot.name == "Editor" && !editorLanding) View.VISIBLE else View.GONE
            if (root) selectBottomNav(snapshot.name)
            configureActionForPage(snapshot.name)
            scroll.post { scroll.scrollTo(0, snapshot.scrollY) }
        } finally {
            restoringSnapshot = false
        }
    }
internal fun MainActivity.navigateBack() {
        if (drawerOpen) {
            closeDrawer()
            return
        }
        if (currentPage == "Editor" && !editorLanding) {
            editorExternalTarget = null
            editorExternalMode = null
        }
        if (pageBackStack.isEmpty()) {
            if (currentPage != "home") {
                // Safety fallback for a page created before the stack was populated.
                showHome()
            } else {
                invokeSuperOnBackPressed()
            }
            return
        }
        val snapshot = pageBackStack.removeLast()
        restoreSnapshot(snapshot)
    }
internal fun MainActivity.handleBack() {
        if (currentPage == "Konversi File" && convCategory != null) { convGoBackStage(); return }
        navigateBack()
    }
internal fun MainActivity.registerBackCallback() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                android.window.OnBackInvokedCallback { handleBack() }
            )
        }
    }
internal fun MainActivity.configureActionForPage(name: String) {
        when (name) {
            "IoT Dynamic Topology" -> {
                action.text = "+"; action.textSize = 28f; action.setOnClickListener { showStudioWidgetPicker() }
                editorMore.visibility = View.VISIBLE; editorMore.text = "⋮"; editorMore.textSize = 25f
                editorMore.setOnClickListener { showToolMenu(editorMore) }
            }
            "Pengelola Keuangan" -> {
                action.text = "+"; action.textSize = 28f; action.setOnClickListener { showFinanceActions() }
                editorMore.visibility = View.VISIBLE; editorMore.text = "⋮"; editorMore.textSize = 25f
                editorMore.setOnClickListener { showToolMenu(editorMore) }
            }
            in rmAllPages -> {
                editorMore.visibility = View.GONE
                action.text = "⋮"; action.textSize = 25f
                action.setOnClickListener { rmMenu() }
            }
            else -> {
                if (name == "Editor" || name.startsWith("Editor - ")) {
                    action.text = "+"
                    action.textSize = 28f
                    action.setOnClickListener { showEditorModePicker() }
                    editorMore.visibility = if (name == "Editor" && !editorLanding) View.VISIBLE else View.GONE
                    editorMore.text = "⋮"
                    editorMore.textSize = 25f
                    editorMore.setOnClickListener { showEditorMoreMenu() }
                } else {
                    editorMore.visibility = View.GONE
                    action.text = "⋮"
                    action.textSize = 25f
                    action.setOnClickListener { showToolMenu(action) }
                }
            }
        }
    }
internal fun MainActivity.styleTopFabs() {
        val fill = if (isDarkTheme) panel else Color.WHITE
        val stroke = if (isDarkTheme) line else Color.rgb(232, 235, 239)
        val ink = if (isDarkTheme) textMain else Color.rgb(23, 32, 42)
        topBar.setBackgroundColor(Color.TRANSPARENT)
        // Wajib di atas ScrollView (z lebih tinggi) agar header yang menimpa konten tetap menerima sentuhan.
        topBar.elevation = dp(8).toFloat()
        topBar.translationZ = dp(2).toFloat()
        listOf<View>(homeMenu, back, homeSearch, homeProfile, action, editorMore).forEach { v ->
            v.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fill)
                setStroke(dp(1), stroke)
            }
            v.setLayerType(View.LAYER_TYPE_NONE, null)
            v.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            v.clipToOutline = false
            v.elevation = dp(4).toFloat()
            v.translationY = 0f
            when (v) {
                is ImageButton -> { v.scaleType = ImageView.ScaleType.CENTER_INSIDE; v.setColorFilter(ink) }
                is TextView -> { v.setTextColor(ink); v.includeFontPadding = false; v.gravity = Gravity.CENTER }
            }
        }
    }
internal fun MainActivity.applyGhostShadow(view: View) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        view.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                val inset = dp(8)
                outline.setOval(inset, inset, v.width - inset, v.height - inset)
                outline.alpha = 0.12f
            }
        }
        view.clipToOutline = false
        view.elevation = dp(5).toFloat()
    }
internal fun MainActivity.resetSearchSnap(show: Boolean) {
        searchSnapEnabled = show
        if (!show || (search.text.isNullOrEmpty() && !search.hasFocus())) {
            setSearchBoxOpen(false)
        } else {
            setSearchBoxOpen(true)
        }
    }
internal fun MainActivity.updateSearchClearButton() {
        val hasQuery = !search.text.isNullOrEmpty()
        val clearDrawable = if (hasQuery) {
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_clear_search)
        } else null
        search.setCompoundDrawablesWithIntrinsicBounds(null, null, clearDrawable, null)
        search.compoundDrawablePadding = dp(6)
        search.contentDescription = if (hasQuery) "Pencarian, hapus teks" else "Pencarian"
    }
internal fun MainActivity.setSearchBoxOpen(open: Boolean, focus: Boolean = false) {
        searchOpen = open
        searchFill.visibility = if (open) View.GONE else View.VISIBLE
        updateSearchClearButton()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        if (open) {
            search.visibility = View.VISIBLE
            search.alpha = 1f
            if (focus) {
                search.post {
                    search.requestFocus()
                    search.setSelection(search.text.length)
                    imm?.showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
        } else {
            if (search.hasFocus()) search.clearFocus()
            imm?.hideSoftInputFromWindow(search.windowToken, 0)
            search.visibility = View.GONE
        }
    }
