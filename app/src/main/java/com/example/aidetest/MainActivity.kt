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

class MainActivity : Activity() {

    internal lateinit var content: LinearLayout

    // --- File tools state (Batch 2) ---
    internal var fileSortMode = 0
    internal var fileFilterText = ""


    internal val convCategories = listOf(
        ConvCategory(
            "arsip", "◫", "Arsip & Kompresi", "ZIP, RAR, 7Z, TAR, dll.",
            "Kompresi dan ekstrak file arsip dengan mudah.",
            "Kompresi dan ekstrak file arsip dengan mudah.",
            listOf("RAR", "7Z", "TAR", "GZ", "ISO", "Folder Normal (Extract)"), "*/*"
        ),
        ConvCategory(
            "dokumen", "▤", "Dokumen & Teks", "DOCX, PDF, XLSX, dll.",
            "Word, PDF, Excel, TXT, dll.",
            "Konversi dokumen, lembar kerja, presentasi, dan e-book.",
            listOf("PDF", "DOCX", "XLSX", "PPTX", "TXT", "RTF"), "*/*"
        ),
        ConvCategory(
            "gambar", "▧", "Gambar & Desain", "PNG, JPG, SVG, PSD, dll.",
            "PNG, JPG, JPEG, WEBP, BMP, GIF, HEIC, dll.",
            "Konversi gambar langsung di HP ke PNG, JPG, WEBP, atau BMP.",
            listOf("PNG", "JPG", "WEBP", "BMP"), "image/*"
        ),
        ConvCategory(
            "audio", "♪", "Audio & Musik", "MP3, WAV, FLAC, dll.",
            "MP3, WAV, FLAC, dll.",
            "Konversi antar format audio serta ekstrak kualitas.",
            listOf("MP3", "WAV", "OGG", "M4A"), "audio/*"
        ),
        ConvCategory(
            "video", "▶", "Video", "MP4, MKV, AVI, GIF, dll.",
            "MP4, MKV, AVI, GIF, dll.",
            "Konversi format video atau ekstrak audio.",
            listOf("MP4", "MKV", "AVI", "WEBM"), "video/*"
        )
    )

    internal var convCategory: String? = null
    internal var convStage = "form" // "form" | "pickfile" | "progress" | "done"
    internal var convToExpanded = false
    internal var convPickedUri: Uri? = null
    internal var convPickedName: String? = null
    internal var convFromFormat = "Otomatis terdeteksi"
    internal var convToFormat: String? = null
    internal var convStepIndex = 0
    internal var convResultUri: Uri? = null
    internal var convResultName: String? = null
    internal var convResultSizeText: String? = null

    internal lateinit var scroll: ScrollView
    internal lateinit var title: TextView
    internal lateinit var subtitle: TextView
    internal lateinit var back: TextView
    internal lateinit var action: TextView
    internal lateinit var homeMenu: ImageButton
    internal lateinit var homeSearch: ImageButton
    internal lateinit var homeProfile: ImageButton
    internal lateinit var search: EditText
    internal lateinit var searchFill: View
    internal var searchOpen = false
    // Search bar "Snap / Enter Always": a small scroll gesture is enough to hide/show it.
    internal var searchSnapEnabled = false          // halaman aktif memang punya search bar
    internal var searchSnapHidden = false           // sedang disembunyikan oleh snap
    internal var searchSnapFraction = 1f            // 1 = tampil penuh, 0 = tertutup
    internal var searchSnapAnimator: android.animation.ValueAnimator? = null
    internal var searchSnapTouchY = 0f
    internal var searchSnapTracking = false
    internal var searchSnapGesture = 0f
    internal var searchSnapGestureTriggered = false
    internal var lastToolOpenId: String? = null
    internal var lastToolOpenAt = 0L
    internal lateinit var bottomNav: LinearLayout
    internal lateinit var navFavorite: View
    internal lateinit var navBot: View
    internal var bitFace: LottieAnimationView? = null
    internal var bitInteractionToken = 0
    internal lateinit var bitAnimationController: BitAnimationController
    // `::lateinitProp.isInitialized` hanya boleh dipanggil di dalam class pemilik properti.
    // File extension (MainActivityHome/Github/GithubScreens) wajib lewat accessor aman ini.
    internal val bitAnim: BitAnimationController?
        get() = if (::bitAnimationController.isInitialized) bitAnimationController else null
    // Bit mini-assistant on process screens. It reuses the same Lottie face as the bottom nav.
    internal var bitProcessFace: LottieAnimationView? = null
    internal var bitProcessErrorView: View? = null
    internal var bitPendingChatMessage: String? = null
    internal lateinit var loginScreen: LinearLayout
    internal lateinit var mainContainer: LinearLayout
    internal lateinit var topBar: LinearLayout
    internal lateinit var rootFrame: FrameLayout

    // Loading proses panjang: muncul otomatis hanya jika background task > 280 ms.
    // Tidak mengunci UI; pengguna tetap bisa membatalkan/navigasi bila task memang mengizinkan.
    internal val activeToolTasks = AtomicInteger(0)
    internal val toolLoadingHandler = Handler(Looper.getMainLooper())
    internal var toolLoadingOverlay: FrameLayout? = null
    internal var toolLoadingSpinner: ProgressBar? = null
    internal var toolLoadingText: TextView? = null


    /**
     * Semua pekerjaan background memakai helper ini. Loading baru muncul setelah 280 ms,
     * sehingga operasi cepat tidak menampilkan "flash" spinner.
     */

    internal var drawerOverlay: FrameLayout? = null
    internal var drawerOpen = false
    // Saat keyboard/IME terbuka, bottom navigation disembunyikan agar tidak
    // ikut naik dan menempel di atas keyboard. Setelah keyboard ditutup, nav
    // kembali ke posisi bawah seperti semula.
    internal var imeVisible = false
    internal var imeBottomInset = 0
    internal var sysTopInset = 0
    internal var sysBottomInset = 0
    internal val EDGE_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

    internal var currentPage = "home"

    // Horizontal root-page navigation: Home <-> Tools <-> Favorit <-> Pengaturan.
    // Vertical swipes remain owned by the ScrollView; only a clear horizontal
    // gesture changes the root page.
    internal var rootSwipeDownX = 0f
    internal var rootSwipeDownY = 0f
    internal var rootSwipeTracking = false
    internal var rootSwipeBlocked = false
    internal var rootSwipePageAtDown = "home"
    internal var rootSwipeHandled = false
    internal val rootPageOrder = listOf("home", "all", "favorites", "settings")

    internal var modernUiHandler = Handler(Looper.getMainLooper())
    internal var modernUiRunnable: Runnable? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Let Android/ScrollView handle the gesture normally first. We only
        // observe it and schedule a root-page change after ACTION_UP, so a
        // horizontal swipe never breaks ordinary vertical scrolling/clicks.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Home search is on-demand: when there is no query, the first
                // touch anywhere outside the search controls dismisses the
                // expanded field. It never reopens automatically while scrolling.
                if (searchSnapEnabled && searchOpen && search.text.isNullOrEmpty()) {
                    fun inside(v: View?): Boolean {
                        if (v == null || v.visibility != View.VISIBLE) return false
                        val loc = IntArray(2)
                        v.getLocationOnScreen(loc)
                        return event.rawX >= loc[0] && event.rawX <= loc[0] + v.width &&
                                event.rawY >= loc[1] && event.rawY <= loc[1] + v.height
                    }
                    if (!inside(search) && !inside(homeSearch)) setSearchBoxOpen(false)
                }
                rootSwipeDownX = event.rawX
                rootSwipeDownY = event.rawY
                rootSwipeTracking = currentPage in rootPageOrder && !imeVisible
                rootSwipeBlocked = !rootSwipeTracking ||
                        isTouchInsideNonPagingArea(event.rawX, event.rawY)
                rootSwipePageAtDown = currentPage
                rootSwipeHandled = false
            }
            MotionEvent.ACTION_UP -> {
                if (rootSwipeTracking && !rootSwipeBlocked && !rootSwipeHandled) {
                    val dx = event.rawX - rootSwipeDownX
                    val dy = event.rawY - rootSwipeDownY
                    val distance = kotlin.math.abs(dx)
                    val vertical = kotlin.math.abs(dy)
                    if (distance >= dp(72) && distance > vertical * 1.25f) {
                        rootSwipeHandled = true
                        val page = rootSwipePageAtDown
                        window.decorView.post {
                            if (!isFinishing && currentPage == page && currentPage in rootPageOrder) {
                                swipeRootPage(dx < 0f)
                            }
                        }
                    }
                }
                rootSwipeTracking = false
            }
            MotionEvent.ACTION_CANCEL -> {
                rootSwipeTracking = false
                rootSwipeHandled = false
            }
        }
        return super.dispatchTouchEvent(event)
    }


    internal var wifiHistory = mutableListOf<Int>()
    internal var batteryHistory = mutableListOf<Int>()

    // Navigation + UI state preservation. Each rendered page is kept as an actual View tree,
    // so EditText contents, selections, toggle states and ScrollView position survive Back.
    internal val pageBackStack = ArrayDeque<PageSnapshot>()
    internal var restoringSnapshot = false
    internal var resettingRootNavigation = false
    internal var editorFile: File? = null
    internal var editorSourceUri: Uri? = null   // file asli di HP (dibuka lewat Buka)
    internal var editorSaveAsText: String? = null
    @set:JvmName("setEditorModeValue") internal var editorMode = "text"
    internal var editorLastSelection = ""
    internal var editorBox: EditText? = null
    internal var editorNameLabel: TextView? = null
    internal var editorStatusLabel: TextView? = null
    internal var editorContextActions: LinearLayout? = null
    internal lateinit var editorBottomBar: LinearLayout
    internal var editorLanding = false

    /** Toolbar bawah editor hanya saat workspace edit aktif (bukan landing / preview / halaman lain). */


    internal var editorPendingTarget: EditText? = null
    internal var editorExternalTarget: EditText? = null
    internal var editorExternalMode: String? = null
    internal lateinit var editorMore: TextView
    // Semua kalkulator dirender dalam satu workspace; perpindahan mode tidak membuka halaman baru.
    internal var embeddedCalculatorRender = false
    internal var calculatorSelectedMode = "basiccalc"
    internal var homeFilter = "Semua"
    internal var server: ServerSocket? = null
    internal var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    internal var hostingStatusView: TextView? = null
    internal var hostingUrlView: TextView? = null
    internal var hostingCredentialsView: TextView? = null
    internal var hostingQrView: ImageView? = null
    internal val HOTSPOT_PERMISSION_REQUEST = 9901
    internal var pendingHostingPort = 8080
    internal var pendingHostingRoot: File? = null
    internal var webImportTarget: EditText? = null
    internal var webBuildReady = false
    /** Buffer log console editor (JS console.log, error, status). */
    internal val editorConsoleLog = StringBuilder()
    internal var editorConsoleView: TextView? = null
    internal var webHostButton: Button? = null
    internal var webBuildStatusView: TextView? = null
    internal var webHostingToken = ""
    internal val WEB_HTML_PICK_REQUEST = 9821
    internal val WEB_CSS_PICK_REQUEST = 9822
    internal val WEB_JS_PICK_REQUEST = 9823
    internal var suppressSearch = false
    // Animasi daftar tool hanya diputar sekali saat sesi aplikasi dimulai.
    // Setelah pengguna masuk ke tool lalu kembali ke Beranda, daftar tetap stabil tanpa replay.
    internal var initialToolAnimationPlayed = false
    internal lateinit var prefs: android.content.SharedPreferences

    internal var isDarkTheme = false
    internal var clipboardManager: android.content.ClipboardManager? = null
    internal var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null
    internal var networkScanStop = AtomicBoolean(false)
    internal val COLOR_PICKER_CAPTURE_REQUEST = 7421
    internal val COLOR_PHOTO_PICK_REQUEST = 7422
    internal val COLOR_PHOTO_CAMERA_REQUEST = 7423
    internal val COLOR_PALETTE_EXPORT_REQUEST = 7424
    internal var pendingColorPaletteExportFormat = "json"
    internal var currentPhotoPalette = mutableListOf<Pair<Int, Int>>()
    internal var colorPhotoView: ImageView? = null
    internal var colorPhotoBitmap: Bitmap? = null
    internal var colorPhotoMarker: View? = null
    internal var colorPhotoSelected = Color.rgb(25, 25, 27)
    internal var colorPhotoStatus: TextView? = null
    internal var colorPhotoHex: TextView? = null
    internal var colorPhotoRgb: TextView? = null
    internal var colorPhotoHsl: TextView? = null
    internal var colorPhotoPalette: LinearLayout? = null
    internal var colorPhotoCameraUri: Uri? = null
    internal var colorPhotoSelectionUpdater: ((Int) -> Unit)? = null
    internal var colorPhotoPlaceholder: View? = null
    internal var colorPickerUiUpdater: ((Int) -> Unit)? = null
    internal val colorPickerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ColorPickerService.ACTION_COLOR_PICKED) return
            val color = intent.getIntExtra(ColorPickerService.EXTRA_COLOR, Color.WHITE)
            colorPickerUiUpdater?.invoke(color)
        }
    }


    internal var pendingOtaEndpoint = ""
    internal val FINANCE_EXPORT_CREATE = 2003
    internal val FINANCE_BACKUP_CREATE = 2004
    internal val FINANCE_BACKUP_OPEN = 2005
    internal val GITHUB_ZIP_PICK_REQUEST = 12801
    internal val GITHUB_FOLDER_PICK_REQUEST = 12802
    internal val BIT_ATTACHMENT_PICK_REQUEST = 12901
    internal var bitAttachmentUri: Uri? = null
    internal var bitAttachmentName: String = ""
    internal var bitAttachmentMime: String = ""
    internal var bitChatTopFace: com.airbnb.lottie.LottieAnimationView? = null
    internal var githubFolderUri: Uri? = null
    internal var githubFolderLabel: TextView? = null
    internal var githubFolderPreview: TextView? = null
    internal var githubZipUri: Uri? = null
    internal var githubZipLabel: TextView? = null
    internal var githubUploadStatus: TextView? = null
    internal var githubZipRoot = ""
    internal val githubZipExcluded = linkedSetOf<String>()
    internal var githubZipPreviewFiles = emptyList<String>()
    internal var githubZipPreviewDirs = emptyList<String>()
    internal var espSensorPolling = false
    internal var espSensorHandler: Handler? = null
    internal var espSensorRunnable: Runnable? = null
    internal var nsdDiscoveryManager: NsdManager? = null
    internal var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    internal var colorPickerProjectionResultCode = 0
    internal var colorPickerProjectionData: Intent? = null


    internal val homeTools = listOf(
        "workspace" to "Workspace Center", "plugincenter" to "Plugin Center", "clipboard" to "Clipboard Hub", "ocr" to "OCR & Table", "filemanager" to "File Manager", "recentfiles" to "Recent Files", "backuprestore" to "Backup / Restore", "editor" to "Editor", "reminder" to "Notifikasi", "zip" to "ZIP / UNZIP", "githubzip" to "GitHub Publisher",
        "json" to "JSON Tools", "hash" to "Hash Generator",
        "base64" to "Base64", "url" to "URL Tools", "regex" to "Regex Tester",
        "uuid" to "UUID Generator", "color" to "Color Tools", "number" to "Kalkulator Lengkap",
        "textstat" to "Statistik Teks", "case" to "Case Converter", "dedupe" to "Hapus Duplikat",
        "compare" to "Bandingkan Teks", "slug" to "Slug Generator", "lorem" to "Lorem Ipsum",
        "password" to "Password Generator", "token" to "Token Acak", "jwt" to "JWT Decoder",
        "hmac" to "HMAC Generator", "totp" to "TOTP Generator", "aes" to "AES Encrypt / Decrypt",
        "random" to "Random Bytes", "checksum" to "Checksum File", "hex" to "Hex Converter",
        "base32" to "Base32", "dns" to "DNS Lookup", "rdns" to "Reverse DNS",
        "port" to "Port Checker", "publicip" to "IP Publik", "ping" to "Ping",
        "ipinfo" to "IP Address Info", "ssl" to "SSL Certificate", "apk" to "APK Inspector",
        "qr" to "QR Scanner", "http" to "HTTP Server", "webhostwifi" to "HTML Hosting Wi-Fi",
        "fileconvert" to "Konversi File",
        "timestamp" to "Timestamp Converter", "unicode" to "Unicode Inspector",
        "urlparser" to "URL Parser", "mime" to "MIME Type Lookup", "jsonformat" to "JSON Formatter",
        "xmlformat" to "XML Formatter", "uuidbatch" to "UUID Batch Generator", "base64file" to "Base64 File Tool",
        "httpheaders" to "HTTP Headers", "textreplace" to "Find & Replace", "wordfreq" to "Word Frequency",
        "devicecenter" to "Device & System", "storage" to "Storage Analyzer", "apps" to "App Manager",
        "network" to "Network Info", "filesearch" to "File Search",
        "pivotcalc" to "Pivot Point", "dividercalc" to "Voltage Divider", "dcacalc" to "Averaging Down & DCA",
        "pwmcalc" to "PWM & Duty Cycle", "spritecalc" to "Sprite Sheet Grid", "installcalc" to "Bunga Flat vs Anuitas",
        "powercalc" to "Konsumsi Listrik", "aspectcalc" to "Aspect Ratio", "pphcalc" to "PPN & PPh Final",
        "financereader" to "Pengelola Keuangan", "financedashboard" to "Finance Dashboard", "securitycenter" to "Security Center", "helpbot" to "HelpBot Offline",
        "filehashcompare" to "File Hash Compare", "markdown" to "Markdown Viewer",
        "sql" to "SQL Tools", "yaml" to "YAML Formatter", "toml" to "TOML Inspector",
        "cron" to "Cron Helper", "passwordstrength" to "Password Strength",
        "fileencryption" to "File Encryption", "steganography" to "Steganography", "passwordanalyzer" to "Password Strength Analyzer", "breachchecker" to "Data Breach Checker", "securenotes" to "Secure Notes", "totpvault" to "2FA Manager (TOTP)", "pgp" to "PGP Encrypt / Decrypt", "sshkeygen" to "SSH Key Generator", "certviewer" to "Certificate Viewer", "virusscanner" to "Virus Scanner", "urlsafety" to "URL Safety Checker",
        "stopwatch" to "Stopwatch", "timer" to "Timer",
        "imagestudio" to "Image Studio",
        "restclient" to "REST / API Client", "websocket" to "WebSocket Client",
        "networkcenter" to "Network Center",
        "apkcompare" to "APK Compare", "duplicatefinder" to "Duplicate Finder",
        "largefilefinder" to "Large File Finder",
        "customtools" to "Tool Customization", "studiocenter" to "Studio Center"
    )

    // Cached indexes: avoid repeated O(n) scans/toMap() while the user scrolls/searches.
    // Registry invariant: one stable ID maps to one tool definition.
    // This prevents duplicate entries from leaking into search/customization even if a
    // future edit accidentally repeats an ID in homeTools.
    internal val homeToolMap: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) { homeTools.associate { it.first to it.second } }
    internal val homeToolSearchIndex by lazy(LazyThreadSafetyMode.NONE) {
        homeTools.distinctBy { it.first }.map { it.first to it.second.lowercase(Locale.getDefault()) }
    }

    internal var dark = Color.rgb(10, 10, 11)
    internal var panel = Color.rgb(22, 22, 24)
    internal var panel2 = Color.rgb(28, 28, 31)
    internal var textMain = Color.rgb(245, 245, 247)
    internal var textMuted = Color.rgb(155, 155, 160)
    internal var line = Color.rgb(48, 48, 52)


    override fun onCreate(state: Bundle?) {
        // Splash screen resmi: harus dipasang sebelum super.onCreate().
        runCatching { installSplashScreen() }
        super.onCreate(state)
        runCatching { registerBackCallback() }
            .onFailure { android.util.Log.w("GITLS", "Back callback skipped", it) }
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("mytools_prefs", MODE_PRIVATE)
        Motion.refresh(prefs, contentResolver)
        // Startup maintenance must never be able to close the Activity.
        // A malformed old preference/history entry should not turn into a launch crash.
        runCatching { syncToolUpdates() }
            .onFailure { android.util.Log.w("GITLS", "Startup tool-sync skipped", it) }
        // Android 13+ requires an explicit export flag for dynamically registered receivers.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                colorPickerReceiver,
                IntentFilter(ColorPickerService.ACTION_COLOR_PICKED),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(colorPickerReceiver, IntentFilter(ColorPickerService.ACTION_COLOR_PICKED))
        }
        runCatching { sanitizeSensitiveHistory() }
            .onFailure { android.util.Log.w("GITLS", "History cleanup skipped", it) }
        runCatching { applySystemTheme() }
            .onFailure { android.util.Log.w("GITLS", "Theme restore skipped", it) }
        runCatching { enableImmersiveFullscreen() }
            .onFailure { android.util.Log.w("GITLS", "System-bar setup skipped", it) }
        window.decorView.postDelayed({
            runCatching { AppUpdateManager(this).checkForUpdate() }
                .onFailure { android.util.Log.w("GITLS", "Update check skipped", it) }
        }, 900L)

        rootFrame = findViewById(android.R.id.content)
        content = findViewById(R.id.content)
        scroll = findViewById(R.id.scroll)
        // Semua Tools berisi banyak kartu kategori. Gunakan ScrollView (sesuai layout) agar
        // gesture vertikal tetap diteruskan dengan mulus dan tidak mudah tersangkut
        // oleh HorizontalScrollView filter di bagian atas.
        scroll.isNestedScrollingEnabled = true
        scroll.isSmoothScrollingEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        title = findViewById(R.id.tvTitle)
        subtitle = findViewById(R.id.tvSubtitle)
        back = findViewById(R.id.btnBack)
        action = findViewById(R.id.btnAction)
        homeMenu = findViewById(R.id.homeMenu)
        homeSearch = findViewById(R.id.homeSearch)
        homeProfile = findViewById(R.id.homeProfile)
        search = findViewById(R.id.searchBox)
        searchFill = findViewById(R.id.searchFill)
        bottomNav = findViewById(R.id.bottomNav)
        editorBottomBar = findViewById(R.id.editorBottomBar)
        editorMore = findViewById(R.id.editorMore)
        loginScreen = findViewById(R.id.loginScreen)
        mainContainer = findViewById(R.id.mainContainer)
        topBar = findViewById(R.id.topBar)

        // Keyboard-safe bottom navigation: keep the nav anchored below the keyboard
        // instead of letting it float directly above the IME when adjustResize runs.
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { _, insets ->
            imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            sysTopInset = sys.top
            sysBottomInset = sys.bottom
            applyEdgeInsets(if (imeVisible) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0)
            val rootPage = currentPage == "home" || currentPage == "all" ||
                    currentPage == "favorites" || currentPage == "settings"

            // Keep the navigation bar anchored to the app's bottom. When the keyboard
            // opens, adjustResize moves the parent bottom upward; translating the nav
            // by the IME inset pushes it back down behind the keyboard instead of making
            // it float directly above the keyboard.
            imeBottomInset = if (imeVisible) {
                insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            } else 0
            bottomNav.translationY = 0f
            bottomNav.visibility = if (rootPage && !imeVisible) View.VISIBLE else View.GONE
            insets
        }
        ViewCompat.requestApplyInsets(mainContainer)
        runCatching { applyUiColors() }
            .onFailure { android.util.Log.w("GITLS", "Initial colors skipped", it) }
        // Splash "GitLis": pada launch baru, animasi harus benar-benar selesai
        // sebelum Home ditampilkan. Sebelumnya enterApp() dipanggil lebih dulu sehingga
        // UI utama sudah aktif di belakang splash dan transisi dapat terlihat terpotong.
        // Untuk restore/rotasi, jangan mengulang splash.
        var startupUiShown = false
        fun showStartupUi() {
            if (startupUiShown) return
            startupUiShown = true
            runCatching { enterApp() }
                .onFailure { error ->
                    android.util.Log.e("GITLS", "Startup UI failed; showing safe fallback", error)
                    showStartupFallback(error)
                }
        }

        if (state == null) {
            runCatching {
                val splash = GitlisSplashView(this)
                (findViewById<View>(android.R.id.content) as ViewGroup).addView(
                    splash, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                )
                // enterApp() hanya dipanggil dari callback akhir animasi. Dengan begitu
                // splash tidak pernah menghilang di tengah animasi masuk.
                splash.start { showStartupUi() }
            }.onFailure {
                android.util.Log.w("GITLS", "Splash skipped; opening app immediately", it)
                showStartupUi()
            }
        } else {
            showStartupUi()
        }

        // Maintenance dijalankan secara defensif agar tidak pernah menggagalkan startup.
        runCatching { scheduleFinanceMaintenance() }
            .onFailure { android.util.Log.w("GITLS", "Finance maintenance skipped", it) }

        back.setOnClickListener { navigateBack() }
        action.setOnClickListener { showAbout() }
        // Search must never rebuild the whole page on every keystroke.
        // IME composition on Android can emit several text events per character;
        // debouncing keeps the EditText responsive and lets the keyboard finish its
        // composing transaction before the result list is rendered.
        search.addTextChangedListener(DebouncedSearchWatcher { query ->
            updateSearchClearButton()
            if (!suppressSearch) filterCurrent(query)
        })
        // Clear button berada langsung di sisi kanan kotak pencarian.
        // Saat ada teks, X muncul; menekannya menghapus query, menutup keyboard,
        // lalu mengembalikan workspace ke Beranda tanpa membangun ulang top bar.
        search.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && search.compoundDrawables[2] != null) {
                val clear = search.compoundDrawables[2]
                val hitLeft = search.width - search.paddingEnd - clear.intrinsicWidth - dp(12)
                if (event.x >= hitLeft) {
                    search.setText("")
                    search.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(search.windowToken, 0)
                    if (currentPage == "home") showHome(homeFilter)
                    return@setOnTouchListener true
                }
            }
            false
        }
        updateSearchClearButton()
        // Keyboard-safe root navigation: the bottom navigation must never become a
        // second toolbar above the keyboard while the user is typing/searching.
        search.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bottomNav.visibility = View.GONE
            } else {
                search.postDelayed({
                    val rootPage = currentPage == "home" || currentPage == "all" ||
                            currentPage == "favorites" || currentPage == "settings"
                    if (rootPage && !imeVisible) bottomNav.visibility = View.VISIBLE
                }, 120L)
            }
        }
        search.setOnEditorActionListener { _, actionId, event ->
            val submit = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER)
            if (submit) {
                search.clearFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(search.windowToken, 0)
                true
            } else false
        }
        // Header fades with normal scrolling. Snap Search is handled separately
        // from the scroll position so a tiny finger gesture can show/hide it even
        // when the ScrollView is far from the top.
        scroll.setOnScrollChangeListener { _, scrollY, _, _, _ ->
            val progress = (scrollY / dp(220).toFloat()).coerceIn(0f, 1f)
            title.alpha = 1f - (progress * 0.08f)
            subtitle.alpha = 1f - (progress * 0.18f)
            homeMenu.alpha = 1f
            homeSearch.alpha = 1f
            homeProfile.alpha = 1f
        }

        // Snap / Enter Always: read the user's finger movement directly. Do not
        // wait for scrollY to change by a large amount and do not require reaching
        // scrollY == 0. Returning false keeps normal ScrollView scrolling intact.
        findViewById<View>(R.id.navHome).setOnClickListener { navigateRoot("home") { showHome() } }
        findViewById<View>(R.id.navTools).setOnClickListener { navigateRoot("all") { showAllTools() } }
        navBot = findViewById(R.id.navBot)
        navFavorite = findViewById(R.id.navFavorite)
        navFavorite.setOnClickListener { navigateRoot("favorites") { showFavorites() } }
        findViewById<View>(R.id.navSettings).setOnClickListener { navigateRoot("settings") { showSettings() } }
        listOf(R.id.navHome, R.id.navTools, R.id.navFavorite, R.id.navSettings).forEach { id ->
            addPressFeedback(findViewById(id))
        }
        bitAnimationController = BitAnimationController(this)
        bitAnimationController.startShakeDetection()
        setupBitFace()
        homeMenu.setOnClickListener { toggleDrawer() }
        homeSearch.setOnClickListener {
            if (!searchSnapEnabled) return@setOnClickListener
            if (searchOpen) {
                // Tekan lagi: kosongkan pencarian lalu sembunyikan kotak.
                if (search.text.isNotEmpty()) search.setText("")
                setSearchBoxOpen(false)
            } else {
                setSearchBoxOpen(true, focus = true)
            }
        }
        homeProfile.setOnClickListener { showProfile() }
        // Ghost button: tanpa container; ikon scale kecil + shadow tipis turun lalu kembali halus.
        Motion.press(homeMenu, 0.86f, shadowDp = -1f)
        Motion.press(homeSearch, 0.86f, shadowDp = -1f, onRelease = { Motion.icon(homeSearch, Motion.Icon.SEARCH) })
        Motion.press(homeProfile, 0.86f, shadowDp = -1f)
        Motion.press(back, 0.88f)
        Motion.press(action, 0.88f)

        // Login actions: all visible controls now have a real action.
        findViewById<Button>(R.id.btnMasuk).setOnClickListener { enterApp() }
        findViewById<Button>(R.id.btnDaftar).setOnClickListener { showLocalRegistration() }
        findViewById<TextView>(R.id.tvLanjut).setOnClickListener { enterApp() }
        addPressFeedback(homeMenu)
        addPressFeedback(homeSearch)
        addPressFeedback(homeProfile)
        styleTopFabs()
        // Fitur tambahan tidak boleh membuat aplikasi mental ke Home bila ada masalah device/API.
        runCatching { setupDynamicShortcuts() }
        runCatching { MyToolsWidget.update(this) }
        if (intent?.getBooleanExtra("open_finance", false) == true) { enterApp(); financeReaderTool() }
        else if (intent?.getBooleanExtra("open_iot", false) == true) { enterApp(); openTool("espstudio") }
        else if (intent?.getBooleanExtra("open_github_upload", false) == true) { enterApp(); openTool("githubzip") }
        else if (intent?.getBooleanExtra("quick_expense", false) == true) { enterApp(); financeReaderTool(); showAddTxDialog(FinanceDb(this), false) }
        else if (intent?.getBooleanExtra("quick_income", false) == true) { enterApp(); financeReaderTool(); showAddTxDialog(FinanceDb(this), true) }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent); setIntent(intent)
        if (intent?.getBooleanExtra("open_finance", false) == true) {
            enterApp(); financeReaderTool()
        } else if (intent?.getBooleanExtra("open_iot", false) == true) {
            enterApp(); openTool("iotdashboard")
        } else if (intent?.getBooleanExtra("open_github_upload", false) == true) {
            enterApp(); openTool("githubzip")
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val selectedUri = data?.data
        if (resultCode == RESULT_OK && selectedUri != null) {
            when (requestCode) {
                BIT_ATTACHMENT_PICK_REQUEST -> {
                    bitAttachmentUri = selectedUri
                    bitAttachmentName = queryName(selectedUri) ?: selectedUri.lastPathSegment ?: "attachment"
                    bitAttachmentMime = contentResolver.getType(selectedUri).orEmpty()
                    if (currentPage == "Bit Assistant") setupBitChatInput()
                    toast("Lampiran dipilih: $bitAttachmentName")
                    return
                }
                1212 -> {
                    imageStudioResult?.invoke(selectedUri)
                    return
                }
                SECURITY_FILE_PICK -> {
                    securityFileUri = selectedUri
                    securityFileName = queryName(selectedUri) ?: selectedUri.lastPathSegment ?: "file"
                    if (currentPage == "Enkripsi File" || currentPage == "File Encryption") renderFileEncryptionUi()
                    else toast("File dipilih")
                    return
                }
                SECURITY_OPEN_PICK -> {
                    val u = selectedUri
                    val pw = lockedOpenPassword; lockedOpenPassword = null
                    if (pw.isNullOrEmpty()) toast("Password wajib diisi")
                    else openLockedFile(displayNameOf(u), pw) { contentResolver.openInputStream(u) }
                    return
                }
                STEGO_ENCODE_PICK -> { stegoImageUri = selectedUri; toast("Gambar dipilih untuk encode"); return }
                STEGO_DECODE_PICK -> { stegoImageUri = selectedUri; decodeStegoFromUri(selectedUri); return }
                CERT_PICK -> { certFileUri = selectedUri; viewCertificate(selectedUri); return }
                GITHUB_FOLDER_PICK_REQUEST -> {
                    githubFolderUri = selectedUri
                    run {
                        val uri = selectedUri
                        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, uri)
                        githubFolderLabel?.text = doc?.name ?: "Folder dipilih"
                        githubFolderPreview?.text = "Memindai isi folder…"
                        toolThread {
                            val names = mutableListOf<String>()
                            fun scan(d: androidx.documentfile.provider.DocumentFile, prefix: String) {
                                d.listFiles().forEach { child ->
                                    val n = child.name ?: return@forEach
                                    if (n == ".git" || n == "__MACOSX" || n == ".DS_Store" || n == "Thumbs.db") return@forEach
                                    val rel = if (prefix.isBlank()) n else "$prefix/$n"
                                    if (child.isDirectory) scan(child, rel) else if (child.isFile) names.add(rel)
                                }
                            }
                            runCatching { if (doc != null) scan(doc, "") }
                            runOnUiThread {
                                val shown = names.take(12).joinToString("\n")
                                githubFolderPreview?.text = "${names.size} file ditemukan" + if (shown.isNotBlank()) "\n$shown" + if (names.size > 12) "\n… dan ${names.size - 12} file lainnya" else "" else "\nFolder kosong atau tidak bisa dibaca"
                                githubUploadStatus?.text = "Folder dipilih. Periksa daftar file, lalu tekan Simpan & Upload."
                            }
                        }
                    }
                    return
                }
                GITHUB_ZIP_PICK_REQUEST -> {
                    val uri = selectedUri
                    githubZipUri = uri
                    // Simpan URI + nama agar pilihan ZIP tidak hilang ketika Activity
                    // direcreate (misalnya setelah file picker menutup atau konfigurasi berubah).
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val name = queryName(uri)
                        ?: androidx.documentfile.provider.DocumentFile.fromSingleUri(this, uri)?.name
                        ?: uri.lastPathSegment?.substringAfterLast('/')
                        ?: "ZIP dipilih"
                    val zipSize = runCatching {
                        contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
                    }.getOrNull() ?: -1L
                    prefs.edit()
                        .putString("gh_selected_zip_uri", uri.toString())
                        .putString("gh_selected_zip_name", name)
                        .apply()

                    githubZipExcluded.clear()
                    githubZipRoot = ""
                    githubZipPreviewFiles = emptyList()
                    githubZipPreviewDirs = emptyList()
                    githubZipLabel?.text = if (zipSize > 0) {
                        "$name • ${ghFormatBytes(zipSize)}"
                    } else name
                    githubUploadStatus?.text = "Menganalisis struktur ZIP..."
                    prepareGithubZipPreview(uri)
                    return
                }
                WEB_HTML_PICK_REQUEST, WEB_CSS_PICK_REQUEST, WEB_JS_PICK_REQUEST -> {
                    val uri = selectedUri
                    val target = webImportTarget
                    if (target == null) {
                        toast("Target editor tidak tersedia")
                        return
                    }
                    toolThread("Membaca file web…") {
                        val result = runCatching {
                            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                                ?: error("File tidak dapat dibaca")
                        }
                        runOnUiThread {
                            result.onSuccess { text ->
                                target.setText(text)
                                webBuildReady = false
                                webBuildStatusView?.text = "BELUM BUILD • File berhasil dimuat, tekan Build untuk validasi"
                                webHostButton?.isEnabled = false
                                webImportTarget = null
                                toast("File web berhasil dimuat")
                            }.onFailure { toast("File web gagal dibaca: ${it.message}") }
                        }
                    }
                    return
                }
            }
        }
        if (requestCode == COLOR_PICKER_CAPTURE_REQUEST) {
            // Screen capture flow disabled for privacy/policy compliance.
            toast("Pipet layar tidak tersedia di versi ini")
            return
        }
        if (requestCode == COLOR_PHOTO_PICK_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                decodeSampledBitmap(uri, 2048) ?: error("Foto tidak dapat dibaca")
            }.onSuccess { loadColorPhoto(it) }
             .onFailure { toast("Foto gagal dibaca: ${it.message}") }
            return
        }
        if (requestCode == COLOR_PALETTE_EXPORT_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val palette = currentPhotoPalette.toList()
            val text = if (pendingColorPaletteExportFormat == "json") {
                val arr = JSONArray()
                palette.forEach { (color, percent) ->
                    val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
                    arr.put(JSONObject().apply {
                        put("hex", "#%02X%02X%02X".format(Locale.US, r, g, b))
                        put("rgb", JSONArray().put(r).put(g).put(b))
                        put("percent", percent)
                    })
                }
                JSONObject().apply { put("source", "MyTools Color Tools"); put("colors", arr) }.toString(2)
            } else {
                palette.joinToString("\n") { (color, percent) ->
                    "#%02X%02X%02X\t$percent%%".format(Locale.US, Color.red(color), Color.green(color), Color.blue(color))
                }
            }
            runCatching { contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(StandardCharsets.UTF_8)) } }
                .onSuccess { toast("Palet berhasil diekspor") }
                .onFailure { toast("Ekspor palet gagal: ${it.message}") }
            return
        }
        if (requestCode == COLOR_PHOTO_CAMERA_REQUEST && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) loadColorPhoto(bitmap) else toast("Foto kamera tidak tersedia")
            return
        }
        if (requestCode == 1030 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            uploadOtaUri(uri, pendingOtaEndpoint)
            return
        }
        if (requestCode == FINANCE_EXPORT_CREATE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { contentResolver.openOutputStream(uri)?.use { out ->
                val db = FinanceDb(this)
                val bytes = if (pendingFinanceExportJson) financeJson(db).toString(2).toByteArray(StandardCharsets.UTF_8) else financeCsv(db).toByteArray(StandardCharsets.UTF_8)
                out.write(bytes)
            } }.onSuccess { toast("Ekspor berhasil") }.onFailure { toast("Ekspor gagal: ${it.message}") }
            return
        }
        if (requestCode == FINANCE_BACKUP_CREATE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { contentResolver.openOutputStream(uri)?.use { out -> out.write(financeJson(FinanceDb(this)).toString(2).toByteArray(StandardCharsets.UTF_8)) } }
                .onSuccess { toast("Backup berhasil disimpan") }.onFailure { toast("Backup gagal: ${it.message}") }
            return
        }
        if (requestCode == FINANCE_BACKUP_OPEN && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { JSONObject(it.readText()) } }
                .onSuccess { root ->
                    if (root == null) { toast("Backup kosong"); return@onSuccess }
                    AlertDialog.Builder(this).setTitle("Ganti data keuangan?")
                        .setMessage("Restore akan mengganti data keuangan lokal saat ini dengan isi backup. Buat backup saat ini terlebih dahulu jika masih diperlukan.")
                        .setNegativeButton("Batal", null)
                        .setPositiveButton("Restore") { _, _ -> restoreFinanceJson(FinanceDb(this), root) }.show()
                }.onFailure { toast("Restore gagal: ${it.message}") }
            return
        }
        if (requestCode == 3025 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { writeAppBackup(uri) }
                .onSuccess { toast("Backup V2.32 berhasil disimpan") }
                .onFailure { toast("Backup gagal: ${it.message}") }
            return
        }
        if (requestCode == 3026 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            AlertDialog.Builder(this).setTitle("Restore Backup V2.32?")
                .setMessage("Pengaturan, history, dan Recent Files dari backup akan diterapkan. File kerja tidak dihapus otomatis.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Restore") { _, _ ->
                    runCatching { readAppBackup(uri) }
                        .onSuccess { toast("Restore selesai. Buka ulang tool jika diperlukan.") }
                        .onFailure { toast("Restore gagal: ${it.message}") }
                }.show()
            return
        }
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val file = File(filesDir, "imports").apply { mkdirs() }
            val out = File(file, safeFileName(queryName(uri) ?: "import.txt"))
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { input.copyTo(it) }
            }
            val canWrite = runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }.isSuccess
            editor(out)
            editorSourceUri = if (canWrite) uri else null
        }
        if (requestCode == 1031 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val text = editorSaveAsText ?: editorBox?.text?.toString().orEmpty()
            runCatching {
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(StandardCharsets.UTF_8)) }
                    ?: error("Tidak bisa menulis file")
            }.onSuccess {
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                editorSourceUri = uri
                queryName(uri)?.let { editorNameLabel?.text = it }
                toast("Tersimpan di HP: ${queryName(uri) ?: "file"}")
            }.onFailure { toast("Gagal menyimpan: ${it.message}") }
            return
        }
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            inspectZipOrApk(uri)
        }
        if (requestCode == 1301 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            apkCompareFirstUri = uri
            toast("APK A dipilih. Pilih APK B.")
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/vnd.android.package-archive"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1302)
            return
        }
        if (requestCode == 1302 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val first = apkCompareFirstUri
            if (first == null) { toast("APK A belum dipilih"); return }
            compareApks(first, uri)
            return
        }
        if (requestCode == 9811 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.readText() }
                .onSuccess { editorPendingTarget?.setText(it) }
                .onFailure { toast("File gagal dibuka: ${it.message}") }
            editorPendingTarget = null
            return
        }
        if (requestCode == 1020 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            pendingOcrUri = uri
            pendingOcrPreview?.setImageURI(uri)
            pendingOcrView?.setText("")
            ocrResultMeta?.text = "Belum ada hasil"
            ocrDetectedRow?.removeAllViews()
            ocrDetectedRow?.addView(subLabel("Belum ada data terdeteksi", 11f))
            ocrPreviewStatus?.text = "Gambar siap diproses"
            toast("Gambar dipilih")
            return
        }
        if (requestCode == OCR_CAMERA_REQUEST && resultCode == RESULT_OK) {
            val uri = pendingOcrCameraUri
            if (uri == null) { toast("Foto kamera tidak tersedia"); return }
            pendingOcrUri = uri
            pendingOcrPreview?.setImageURI(uri)
            pendingOcrView?.setText("")
            ocrResultMeta?.text = "Membaca foto…"
            ocrPreviewStatus?.text = "Foto kamera siap • OCR otomatis"
            toast("Foto diterima, sedang mengubah ke teks…")
            runOcr(uri) { text ->
                pendingOcrView?.setText(text)
                pendingOcrView?.setSelection(pendingOcrView?.text?.length ?: 0)
                val lines = text.lineSequence().count { it.isNotBlank() }
                val chars = text.length
                ocrResultMeta?.text = if (text.isBlank()) "Tidak ada teks terdeteksi" else "$lines baris • $chars karakter"
                updateOcrDetected(text, ocrDetectedRow, if (isDarkTheme) Color.WHITE else Color.rgb(18,18,20), if (isDarkTheme) Color.rgb(30,30,33) else Color.rgb(242,242,245), if (isDarkTheme) Color.rgb(62,62,66) else Color.rgb(210,210,214))
                if (text.isBlank()) toast("Tidak ada teks yang terdeteksi") else toast("Foto berhasil diubah menjadi teks")
            }
            runOcrTable(uri) { table ->
                pendingOcrTable = table
                renderOcrTablePreview(table, ocrTablePreview, ocrTableMeta, if (isDarkTheme) Color.rgb(20,20,22) else Color.rgb(250,250,251), if (isDarkTheme) Color.rgb(30,30,33) else Color.rgb(242,242,245), if (isDarkTheme) Color.WHITE else Color.rgb(18,18,20), if (isDarkTheme) Color.rgb(175,175,180) else Color.rgb(92,92,98), if (isDarkTheme) Color.rgb(62,62,66) else Color.rgb(210,210,214))
            }
            return
        }
        if (requestCode == 1023 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            if (pendingOcrTable.isEmpty()) { toast("Belum ada tabel"); return }
            runCatching {
                contentResolver.openOutputStream(uri)?.use { writeMinimalXlsx(pendingOcrTable, it) }
                    ?: error("Tidak bisa menulis file")
            }.onSuccess { toast("XLSX tersimpan") }
                .onFailure { toast("Gagal menyimpan XLSX: ${it.message}") }
            return
        }
        if (requestCode == 1024 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            if (pendingOcrTable.isEmpty()) { toast("Belum ada tabel"); return }
            runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(tableToCsv(pendingOcrTable).toByteArray(StandardCharsets.UTF_8)) }
                    ?: error("Tidak bisa menulis file")
            }.onSuccess { toast("CSV tersimpan") }
                .onFailure { toast("Gagal menyimpan CSV: ${it.message}") }
            return
        }
        if (requestCode == 1022 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val text = pendingOcrSaveText ?: return
            runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(StandardCharsets.UTF_8)) }
                    ?: error("Tidak bisa menulis file")
            }.onSuccess {
                pendingOcrSaveText = null
                toast("TXT tersimpan")
            }.onFailure { toast("Gagal menyimpan TXT: ${it.message}") }
            return
        }
        if (requestCode == 1021 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            pendingApkUri = uri
            analyzeApk(uri)
        }
        if (requestCode == 1010 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            toolThread {
                val r = runCatching {
                    val size = contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: -1L
                    require(size <= 8L * 1024 * 1024 || size < 0) { "File terlalu besar. Batas 8 MB." }
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Tidak bisa membaca file")
                    "Base64:\n" + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }.getOrElse { "Base64 file error: ${it.message}" }
                runOnUiThread { output(r) }
            }
        }
        if (requestCode == 1050 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            convPickedUri = uri
            convPickedName = queryName(uri) ?: "file"
            val ext = convPickedName?.substringAfterLast('.', "")?.uppercase(Locale.getDefault())
            convFromFormat = if (ext.isNullOrBlank()) "Otomatis terdeteksi" else ext
            convStage = "form"
            renderConv()
        }
        if (requestCode == 1041 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            qrPickedName = queryName(uri)
            decodeQrFromUri(uri)
        }
        if (requestCode == 1042 && resultCode == RESULT_OK) {
            // Gallery pick returns data.data; camera capture writes to qrCameraOutUri instead.
            val uri = data?.data ?: qrCameraOutUri ?: return
            qrPickedName = if (data?.data != null) queryName(uri) else "Foto kamera"
            decodeQrFromUri(uri)
        }
        if (requestCode == 1043 && resultCode == RESULT_OK) {
            val uri = qrCameraOutUri ?: return
            qrPickedName = "Hasil scan kamera"
            decodeQrFromUri(uri)
        }
        if (requestCode == 1003 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            toolThread {
                val r = runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val md5 = MessageDigest.getInstance("MD5")
                        val sha1 = MessageDigest.getInstance("SHA-1")
                        val sha256 = MessageDigest.getInstance("SHA-256")
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            md5.update(buf,0,n); sha1.update(buf,0,n); sha256.update(buf,0,n)
                        }
                        "MD5  ${md5.digest().joinToString("") { "%02x".format(it) }}\n" +
                        "SHA1 ${sha1.digest().joinToString("") { "%02x".format(it) }}\n" +
                        "SHA256 ${sha256.digest().joinToString("") { "%02x".format(it) }}"
                    } ?: "Tidak bisa membaca file"
                }.getOrElse { "Error: ${it.message}" }
                runOnUiThread { output(r) }
            }
        }
        if ((requestCode == 1201 || requestCode == 1202) && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            if (requestCode == 1201) {
                fileHashUriA = uri
                fileHashCompareLabelA?.text = "File A: ${queryName(uri) ?: uri.lastPathSegment ?: uri}"
            } else {
                fileHashUriB = uri
                fileHashCompareLabelB?.text = "File B: ${queryName(uri) ?: uri.lastPathSegment ?: uri}"
            }
            return
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // IoT Dynamic tetap berada di halaman yang sama saat HP berputar ke landscape.
        // Activity tidak dibuat ulang, jadi canvas, posisi widget, dan koneksi tidak hilang.
        if (currentPage == "IoT Dynamic Topology") {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            content.post {
                studioCanvas?.requestLayout()
                studioCanvas?.invalidate()
            }
        }
    }


    override fun onDestroy() {
        runCatching { if (::bitAnimationController.isInitialized) bitAnimationController.stopShakeDetection() }
        // Bit (Lottie) cleanup: stop animator and release listeners.
        bitFace?.let {
            it.removeAllAnimatorListeners()
            it.cancelAnimation()
        }
        bitFace = null
        bitProcessFace?.let {
            it.removeAllAnimatorListeners()
            it.cancelAnimation()
        }
        bitProcessFace = null
        bitProcessErrorView = null
        runCatching { unregisterReceiver(colorPickerReceiver) }
        stopClipboardMonitor()
        modernUiRunnable?.let { modernUiHandler.removeCallbacks(it) }
        modernUiRunnable = null
        toolLoadingHandler.removeCallbacksAndMessages(null)
        ghHandler.removeCallbacksAndMessages(null)
        ghActionPoll?.removeCallbacksAndMessages(null)
        ghActionPoll = null
        ghActionRunnable = null
        ghActionPollWanted = false
        toolLoadingSpinner?.let { Motion.stopLoading(it) }
        networkScanStop.set(true)
        stopEspDiscovery()
        stopEspSensorPolling()
        stopLedPlayback()
        server?.close()
        server = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { hotspotReservation?.close() }
        }
        hotspotReservation = null
        super.onDestroy()
    }


    // Lightweight native animations: no extra dependency, tuned for Android phones.

    // Hormati pengaturan sistem "Skala durasi animator" = 0 (Reduce Motion / hemat baterai).

    // Helper lama kini hanya mendelegasikan ke Motion agar hanya ada satu sistem animasi.


    // ---- Restored navigation / UI helpers ----
    // These small helpers are intentionally kept local to MainActivity so older
    // screens and newer GitHub screens can share the same rendering primitives.
    /**
     * Navigasi tab root (Beranda / Tools / Favorit / Settings).
     * Jika sudah di [targetPage] dan konten masih ada, jangan rebuild UI
     * (menghindari jank: removeAllViews → layout → scroll=0).
     */


    /** Ganti warna isi tanpa menghilangkan sudut tumpul (setBackgroundColor akan membuatnya kotak tajam). */


    // ---- Design system helpers (lihat DesignSystem.kt) ----


    /** Level 1: aksi utama (terisi). */


    /** Level 2: aksi pendukung (outline). */


    /** Level 3: aksi kecil (teks saja, tetap 48dp). */


    /**
     * Komponen state reusable: Loading / Success / Error / Empty / Info / Warning.
     * onRetry (opsional) menampilkan tombol "Coba lagi".
     */


    /** Logika Back tunggal: dipakai oleh onBackPressed (API < 33) dan OnBackInvokedCallback (API 33+). */


    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBack()
    }

    @Suppress("DEPRECATION")
    internal fun invokeSuperOnBackPressed() {
        super.onBackPressed()
    }

    /** Android 16 (targetSdk 36) tidak lagi memanggil onBackPressed() untuk gestur/tombol Back sistem. */


    /** Ghost floating controls: no visible container, only a subtle icon shadow. */
    /**
     * Semua tombol top bar (menu, kembali, cari, profil, titik tiga/aksi) tampil sebagai
     * Floating Action Button mini: lingkaran, bayangan lembut, tanpa kotak pembungkus.
     */


    /**
     * Dipanggil setiap halaman baru dibuka. [show] = halaman ini punya tombol cari.
     * Kotak cari hanya muncul lewat tombol cari (di sebelahnya, di top bar).
     */


    /** Update ikon X di dalam search field hanya ketika ada query. */


    /** Tampilkan / sembunyikan kotak cari di samping tombol cari. */


    // Registry update tool: setiap kali versi tool berubah, tool otomatis masuk ke bagian "Terbaru".
    // Untuk rilis berikutnya cukup naikkan versi pada entry terkait.
    internal val toolUpdateCatalog = linkedMapOf(
        "githubzip" to "2.30.0",
        "webhostwifi" to "2.19.8",
        "webproject" to "2.19.8",
        "webeditor" to "2.19.8",
        "reminder" to "2.19.8",
        "espstudio" to "2.19.8",
        "networkstudio" to "2.19.8",
        "clipboard" to "2.19.8",
        "apkanalyzer" to "2.19.8",
        "fileencryption" to "2.20.0", "steganography" to "2.20.0", "passwordanalyzer" to "2.20.0", "breachchecker" to "2.20.0", "securenotes" to "2.20.0", "totpvault" to "2.20.0", "pgp" to "2.20.0", "sshkeygen" to "2.20.0", "certviewer" to "2.20.0", "virusscanner" to "2.20.0", "urlsafety" to "2.20.0"
    )


    /**
     * Bit is a small animated assistant placed between Tools and Favorit.
     * It is intentionally only visual for now; the real Q&A function can be wired
     * later without changing the navigation layout.
     */


    // ===== Bit Assistant / Local QA + Google fallback =====
    internal val bitChatMessages = mutableListOf<BitChatMessage>()
    internal var bitChatInput: EditText? = null


    /**
     * Warna Bit mengikuti tema aplikasi (tanpa kotak latar):
     * - Gelap : badan putih, mata gelap, Z putih
     * - Terang: badan gelap, mata putih, Z gelap
     */


    /**
     * User-facing micro documentation for every registered tool.
     * Keep this intentionally short: the card explains WHAT, the dialog explains WHEN/EXAMPLE.
     */


    // Shared full-width input used by the tools.
    // Normal fields are deliberately taller and multiline fields get substantially
    // more vertical space so text is edited in a real work area instead of a tiny box.


    // Shared modern utility layout. It keeps the monochrome identity while giving
    // each tool a clearer visual hierarchy instead of the old input-button-output stack.


    // Setiap tool wajib punya ikon: pakai nama ikon jika valid, kalau tidak cari dari nama tool.


    // V4: workspace helpers for complex tools. These keep domain logic untouched while
    // giving network/file/security/system tools a consistent mobile workspace hierarchy.


    // Shared controls for every tool: status, history and a contextual help panel.
    // Domain-specific controls remain inside each tool so the layout stays fast on mobile.


    // Menu titik tiga kanan atas untuk semua tool: Riwayat, Info, dan Tentang.


    // ===================== 2.14 FINANCE DASHBOARD =====================

    // financeDashboardTool() moved to feature/finance/FinanceScreen.kt


    // ---------- V2.26: NETWORK / SYSTEM / APK / STORAGE TOOLS ----------


    // ---------- CUSTOM DASHBOARD BUILDER / STUDIO MODE ----------
    // Studio landscape: canvas hitam, widget bebas diposisikan, tersimpan lokal.


    internal var studioWidgets = ArrayList<StudioWidget>()
    internal var studioLinks = ArrayList<StudioLink>()
    internal var studioSelectedLinkId: String? = null
    internal var studioCanvas: StudioCanvasView? = null
    internal val studioPrefsKey = "studio_widgets_v1"
    internal val studioLinksPrefsKey = "studio_links_v1"
    internal var studioNextId = 1


    // Tap satu widget untuk memilihnya (menyala), lalu tap widget lain untuk menyambung.
    // Tap widget yang sama lagi untuk membatalkan pilihan.


    internal inner class StudioCanvasView(context: Context) : ViewGroup(context) {
        @set:JvmName("setWidgetsValue") internal var widgets: List<StudioWidget> = emptyList()
        @set:JvmName("setLinksValue") internal var links: List<StudioLink> = emptyList()
        internal val cardWidth = dp(170)
        internal val cardHeight = dp(92)
        internal val linePaint = Paint().apply {
            color = Color.rgb(120, 120, 128)
            strokeWidth = dp(2).toFloat()
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        // Grid titik dibuat sengaja sangat samar agar canvas tidak terasa polos,
        // tetapi tetap nyaman untuk melihat widget dan garis koneksi.
        internal val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(58, 115, 115, 120)
            style = Paint.Style.FILL
        }
        internal val dotSpacing = dp(34).coerceAtLeast(dp(20))
        internal val dotRadius = 1.35f * resources.displayMetrics.density

        init {
            setWillNotDraw(false)
            setBackgroundColor(Color.BLACK)
        }

        fun setWidgets(list: List<StudioWidget>) {
            widgets = list.toList()
            removeAllViews()
            widgets.forEach { addView(createWidgetView(it)) }
            requestLayout()
            invalidate()
        }

        fun setLinks(list: List<StudioLink>) {
            links = list.toList()
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Pola titik gelap-samar seperti canvas desain/ESP Studio.
            // Digambar sebelum link supaya garis koneksi tetap jelas.
            var y = dotSpacing / 2f
            while (y < height) {
                var x = dotSpacing / 2f
                while (x < width) {
                    canvas.drawCircle(x.toFloat(), y.toFloat(), dotRadius, dotPaint)
                    x += dotSpacing
                }
                y += dotSpacing
            }

            links.forEach { lk ->
                val a = widgets.find { it.id == lk.fromId } ?: return@forEach
                val b = widgets.find { it.id == lk.toId } ?: return@forEach
                val ax = a.x + cardWidth.toFloat()
                val ay = a.y + cardHeight / 2f
                val bx = b.x.toFloat()
                val by = b.y + cardHeight / 2f
                canvas.drawLine(ax, ay, bx, by, linePaint)
            }
        }

        internal fun createWidgetView(widget: StudioWidget): View {
            val outer = FrameLayout(context)
            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = bg(Color.rgb(28, 28, 30), 14, Color.rgb(65, 65, 70))
            }
            val title = TextView(context).apply {
                text = widget.label
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val dragRow = FrameLayout(context).apply {
                setPadding(0, 0, 0, 0)
            }
            dragRow.addView(title, FrameLayout.LayoutParams(-1, dp(36)))
            root.addView(dragRow, LinearLayout.LayoutParams(-1, dp(36)))
            // Area geser dibuat lebih besar supaya widget mudah dipindahkan di layar HP.
            // Kontrol ON/OFF, TEKAN, dan slider tetap bisa disentuh normal.
            dragRow.setOnTouchListener(object : View.OnTouchListener {
                var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
                var moved = false
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.rawX; downY = event.rawY
                            startX = widget.x; startY = widget.y
                            moved = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                            if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) moved = true
                            val maxX = (width - cardWidth).coerceAtLeast(0)
                            val maxY = (height - cardHeight).coerceAtLeast(0)
                            widget.x = (startX + dx).coerceIn(0, maxX)
                            widget.y = (startY + dy).coerceIn(0, maxY)
                            root.x = widget.x.toFloat(); root.y = widget.y.toFloat()
                            invalidate()
                            return true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            saveStudioWidgets()
                            if (!moved) onStudioLinkTap(widget.id)
                            return true
                        }
                    }
                    return true
                }
            })

            when (widget.type) {
                "RELAY_TOGGLE" -> {
                    val toggle = Switch(context).apply {
                        isChecked = widget.checked
                        text = if (widget.checked) "ON" else "OFF"
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        setOnCheckedChangeListener { _, checked ->
                            widget.checked = checked
                            text = if (checked) "ON" else "OFF"
                            sendStudioCommand(widget, if (checked) "ON" else "OFF")
                            saveStudioWidgets()
                        }
                    }
                    root.addView(toggle, LinearLayout.LayoutParams(-1, dp(42)))
                }
                "PUSH_MOMENTARY" -> {
                    val push = TextView(context).apply {
                        text = "TEKAN"
                        textSize = 13f
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                        background = bg(Color.rgb(55, 55, 58), 10)
                        isClickable = true
                        setOnTouchListener { v, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> { sendStudioCommand(widget, "ON"); v.performClick() }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> sendStudioCommand(widget, "OFF")
                            }
                            true
                        }
                    }
                    root.addView(push, LinearLayout.LayoutParams(-1, dp(38)))
                }
                "PWM_SLIDER" -> {
                    val slider = SeekBar(context).apply {
                        max = 255
                        progress = widget.value.coerceIn(0, 255)
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                widget.value = progress
                                if (fromUser) sendStudioCommand(widget, "PWM:$progress")
                            }
                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar?) { saveStudioWidgets() }
                        })
                    }
                    root.addView(slider, LinearLayout.LayoutParams(-1, dp(40)))
                }
            }

            root.setOnLongClickListener {
                showStudioEditDialog(widget)
                true
            }
            outer.addView(root, FrameLayout.LayoutParams(-1, -1))

            // Titik sambung: tap satu widget lalu tap widget lain untuk menghubungkan.
            val selected = studioSelectedLinkId == widget.id
            val linkDot = TextView(context).apply {
                text = "\u2295"
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(if (selected) Color.BLACK else Color.WHITE)
                background = bg(if (selected) Color.WHITE else Color.rgb(45, 45, 47), 20, Color.rgb(95, 95, 100))
                setOnClickListener { onStudioLinkTap(widget.id) }
            }
            val dotSize = dp(26)
            val dotLp = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(2); rightMargin = dp(2)
            }
            outer.addView(linkDot, dotLp)
            return outer
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
            for (i in 0 until childCount) {
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(cardHeight, MeasureSpec.EXACTLY))
            }
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            for (i in 0 until childCount) {
                val w = widgets.getOrNull(i) ?: continue
                val child = getChildAt(i)
                val x = w.x.coerceIn(0, (width - cardWidth).coerceAtLeast(0))
                val y = w.y.coerceIn(0, (height - cardHeight).coerceAtLeast(0))
                child.layout(x, y, x + cardWidth, y + cardHeight)
            }
        }
    }


    // ---------- ESP LED STUDIO ----------
    // Editor visual LED addressable. Layout selector dibuat ringkas/tersembunyi
    // di dalam kartu dan seluruh pengaturan tetap berada pada satu halaman.

    @set:JvmName("setLedCountValue") internal var ledCount = 10
    internal var ledLayout = "Grid"
    internal val ledFrames = ArrayList<LedFrameData>()
    internal var ledFrameIndex = 0
    internal var ledCanvas: LedCanvasView? = null
    internal var ledFrameStrip: LinearLayout? = null
    internal var ledFrameInfo: TextView? = null
    internal var ledSpeedInfo: TextView? = null
    internal var ledSpeedSeek: SeekBar? = null
    internal var ledNameEdit: EditText? = null
    internal var ledEndpointEdit: EditText? = null
    internal var ledGapSeek: SeekBar? = null
    internal var ledGapDp = 0
    internal var ledPlaying = false
    internal var ledLayoutLabel: TextView? = null
    internal var ledCountLabel: TextView? = null
    internal val ledPlayHandler = Handler(Looper.getMainLooper())
    internal var ledPlayRunnable: Runnable? = null


    internal inner class LedCanvasView(context: Context) : View(context) {
        internal var count = 10
        internal var layoutMode = "Grid"
        @set:JvmName("setStatesValue") internal var states = BooleanArray(count)
        internal val positions = ArrayList<android.graphics.PointF>()
        internal var gapDp = 0
        internal val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var onLedClicked: ((Int) -> Unit)? = null

        init { setLayerType(View.LAYER_TYPE_SOFTWARE, null); isClickable = true }

        fun setLedConfig(newCount: Int, newLayout: String) {
            count = newCount.coerceIn(1, 50)
            layoutMode = when (newLayout) { "Kotak" -> "Grid"; "Lingkaran", "Strip", "Spiral" -> newLayout; else -> "Grid" }
            if (states.size != count) {
                val next = BooleanArray(count)
                for (i in 0 until minOf(states.size, count)) next[i] = states[i]
                states = next
            }
            recalcPositions(width, height); invalidate()
        }

        fun setStates(newStates: BooleanArray) { states = newStates.copyOf(count); invalidate() }
        fun setLedGap(gap: Int) { gapDp = gap.coerceIn(0, 20); recalcPositions(width, height); invalidate() }
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { recalcPositions(w, h) }

        internal fun recalcPositions(w: Int, h: Int) {
            positions.clear()
            if (w <= 0 || h <= 0) return
            val cx = w / 2f; val cy = h / 2f
            val margin = dp(12).toFloat(); val extra = dp(gapDp).toFloat()
            when (layoutMode) {
                "Grid" -> {
                    val cols = min(10, Math.ceil(Math.sqrt(count.toDouble())).toInt().coerceAtLeast(1))
                    val rows = Math.ceil(count.toDouble() / cols).toInt().coerceAtLeast(1)
                    val stepX = ((w - margin * 2f - extra * (cols - 1)) / cols.coerceAtLeast(1)).coerceAtLeast(dp(26).toFloat())
                    val stepY = ((h - margin * 2f - extra * (rows - 1)) / rows.coerceAtLeast(1)).coerceAtLeast(dp(26).toFloat())
                    val totalW = (cols - 1) * (stepX + extra); val totalH = (rows - 1) * (stepY + extra)
                    val sx = cx - totalW / 2f; val sy = cy - totalH / 2f
                    for (i in 0 until count) {
                        val row = i / cols; val col = i % cols
                        positions.add(android.graphics.PointF(sx + col * (stepX + extra), sy + row * (stepY + extra)))
                    }
                }
                "Lingkaran" -> {
                    val r = (min(w, h) / 2f - dp(34)).coerceAtLeast(dp(24).toFloat())
                    if (count == 1) positions.add(android.graphics.PointF(cx, cy)) else for (i in 0 until count) {
                        val a = -Math.PI / 2 + i * (2 * Math.PI / count)
                        positions.add(android.graphics.PointF(cx + Math.cos(a).toFloat() * r, cy + Math.sin(a).toFloat() * r))
                    }
                }
                "Strip" -> {
                    val step = ((w - margin * 2f - extra * (count - 1)) / count.coerceAtLeast(1)).coerceAtLeast(dp(26).toFloat())
                    val total = (count - 1) * (step + extra)
                    val sx = cx - total / 2f
                    for (i in 0 until count) positions.add(android.graphics.PointF(sx + i * (step + extra), cy))
                }
                "Spiral" -> {
                    val maxR = (min(w, h) / 2f - dp(24)).coerceAtLeast(dp(20).toFloat())
                    for (i in 0 until count) {
                        val t = if (count <= 1) 0f else i.toFloat() / (count - 1).toFloat()
                        val r = maxR * t
                        val a = -Math.PI / 2 + i * (Math.PI * 2.2 / 10.0)
                        positions.add(android.graphics.PointF(cx + Math.cos(a).toFloat() * r, cy + Math.sin(a).toFloat() * r))
                    }
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(panel)
            if (positions.size != count) recalcPositions(width, height)
            val radius = (min(width, height) * 0.04f).coerceIn(dp(10).toFloat(), dp(17).toFloat())
            positions.forEachIndexed { index, p ->
                val on = states.getOrNull(index) == true
                if (on) {
                    glowPaint.color = Color.rgb(120, 120, 120)
                    glowPaint.setShadowLayer(radius * 0.9f, 0f, 0f, Color.argb(150, 52, 132, 255))
                    if (layoutMode == "Grid") canvas.drawRoundRect(p.x-radius, p.y-radius, p.x+radius, p.y+radius, radius*.25f, radius*.25f, glowPaint)
                    else canvas.drawCircle(p.x, p.y, radius*1.05f, glowPaint)
                    glowPaint.clearShadowLayer()
                    paint.color = Color.rgb(170, 170, 170)
                } else paint.color = Color.rgb(65, 70, 78)
                if (layoutMode == "Grid") canvas.drawRoundRect(p.x-radius*.82f, p.y-radius*.82f, p.x+radius*.82f, p.y+radius*.82f, radius*.22f, radius*.22f, paint)
                else canvas.drawCircle(p.x, p.y, radius, paint)
                paint.color = if (on) Color.WHITE else Color.rgb(165, 170, 178)
                paint.textSize = dp(8).toFloat(); paint.textAlign = Paint.Align.CENTER
                canvas.drawText((index + 1).toString(), p.x, p.y + dp(3), paint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (positions.size != count) recalcPositions(width, height)
            if (event.action == MotionEvent.ACTION_DOWN) {
                var nearest = -1; var dist = Float.MAX_VALUE
                val hit = (min(width, height) * .04f).coerceIn(dp(12).toFloat(), dp(20).toFloat()) * 2f
                positions.forEachIndexed { i, p ->
                    val dx = event.x-p.x; val dy = event.y-p.y; val d = Math.sqrt((dx*dx+dy*dy).toDouble()).toFloat()
                    if (d <= hit && d < dist) { nearest=i; dist=d }
                }
                if (nearest >= 0) onLedClicked?.invoke(nearest)
                performClick(); return true
            }
            return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
    }


    // ---------- ESP / IoT TOOLKIT ----------


    // ---------- NEW TOOLS: CLIPBOARD / OCR / UNIT / APK / NETWORK SCANNER ----------


    /** Universal bridge: routes clipboard data into existing GITLS tools without duplicating data. */


    internal var pendingOcrView: EditText? = null
    internal var pendingOcrPreview: ImageView? = null
    internal var pendingOcrUri: Uri? = null
    internal var ocrPreviewStatus: TextView? = null
    internal var ocrResultMeta: TextView? = null
    internal var ocrDetectedRow: LinearLayout? = null
    internal var ocrButtonTextState: Button? = null
    internal var pendingOcrSaveText: String? = null
    internal var pendingOcrTable: List<List<String>> = emptyList()
    internal var pendingOcrCameraUri: Uri? = null
    internal val OCR_CAMERA_REQUEST = 1025
    internal val OCR_CAMERA_PERMISSION_REQUEST = 1026
    internal var ocrTableMeta: TextView? = null
    internal var ocrTablePreview: LinearLayout? = null


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == OCR_CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) openOcrCamera()
            else toast("Izin kamera diperlukan untuk memotret dokumen")
        }
    }


    internal var pendingApkUri: Uri? = null
    internal var apkCompareFirstUri: Uri? = null
    internal var wsSocket: Socket? = null
    internal var wsInput: InputStream? = null
    internal var wsOutput: OutputStream? = null
    internal var pendingApkOutput: TextView? = null


    // ---------- NATIVE DEVICE / STORAGE / APP / NETWORK TOOLS ----------


    // ---------- FILE MANAGER / EDITOR ----------

    /** 0=skip, 1=replace, 2=rename, 3=cancel. Dialog runs on UI thread before worker. */


    // ===================== Notifikasi / Pengingat Terpadu =====================
    internal var reminderFilter = "all"
    internal var reminderEditing: JSONObject? = null
    internal var reminderDraftMessage = ""
    internal var reminderDraftBody = ""
    internal var reminderDraftNote = ""
    internal var reminderDraftHour = 13
    internal var reminderDraftMinute = 0
    internal var reminderDraftCategory = "kegiatan"
    internal var reminderDraftRepeat = "daily"
    internal var reminderDraftEnabled = true
    internal var reminderDraftPayload = JSONObject()

    // ---- Nama halaman Notifikasi (dipakai clearPage untuk menyembunyikan strip bawaan tool) ----
    internal val rmPickerPages = setOf("Pilih Waktu", "Pilih Tanggal / Pengulangan", "Pilih Hari")
    internal val rmEditorPages = setOf("Tambah Notifikasi", "Tambah Notifikasi - Detail", "Edit Notifikasi")
    internal val rmSubPages = rmPickerPages + rmEditorPages + "Detail Notifikasi"
    internal val rmAllPages = rmSubPages + "Notifikasi"

    // ---- Warna sesuai desain ----
    internal val rmDark = Color.rgb(38, 51, 61)
    internal val rmCardBg = Color.rgb(247, 249, 250)
    internal val rmCardLine = Color.rgb(229, 234, 238)
    internal val rmGray = Color.rgb(145, 154, 161)

    // ===================== Helper tampilan =====================


    // ===================== Navigasi internal Notifikasi =====================

    /** Kembali ke daftar Notifikasi (segar) tanpa menumpuk riwayat halaman. */


    /** Kembali ke form (Tambah/Edit) setelah memilih waktu/pengulangan/hari. */


    // ===================== 1. Daftar Notifikasi =====================


    // ===================== Kustom: pembuat percakapan notifikasi =====================


    


    // ===================== 2 / 6 / 7. Tambah, Tambah - Detail, Edit =====================


    // ===================== 5. Pilih Waktu =====================


    // ===================== 3. Pilih Tanggal / Pengulangan =====================


    // ===================== 4. Pilih Hari =====================


    // ===================== 8. Detail Notifikasi =====================


    /**
     * Layout full layar: header (menu/cari/profil) melayang DI ATAS konten, tidak lagi memakai
     * baris penampung sendiri. Konten ScrollView digambar sampai ke belakang status bar dan
     * gesture bar; padding atas/bawah hanya menjaga posisi awal agar item pertama tidak tertutup.
     */


    /** Simpan: timpa file asli di HP bila dibuka dari HP, selain itu simpan salinan internal. */


    /** Simpan sebagai: buka pemilih lokasi Android (ACTION_CREATE_DOCUMENT). */


    /** Daftar masalah di editor; ketuk satu untuk melompat ke barisnya. */


    /** Pratinjau perubahan; kode baru diterapkan hanya setelah pengguna menekan Terapkan. */


    // ---------- GITHUB ZIP PUBLISHER ----------
    // Tiga layar dalam satu halaman: Pengaturan -> Proses Upload -> Upload Selesai.


    internal var ghStage: FrameLayout? = null
    internal var ghSource = 0
    internal var ghBranch = "main"
    internal var ghSaveToken = true
    internal var ghPrivateRepo = true
    internal var ghRunning = false
    internal var ghStartedAt = 0L
    internal var ghCurrentStep = 0
    internal var ghFileTotal = 0
    internal var ghRetry: (() -> Unit)? = null
    internal var ghLastResult: GhResult? = null
    internal var ghActionPoll: Handler? = null
    internal var ghActionRunnable: Runnable? = null
    internal var ghActionLastFailedJob: GhActionJob? = null
    internal var ghUserValue = ""
    internal var ghRepoValue = ""
    internal var ghTokenValue = ""
    internal var ghCommitValue = ""
    @Volatile internal var ghCancelled = false
    internal var ghPendingResult: GhResult? = null
    internal var ghPendingError: Throwable? = null
    internal var ghFinishedSec = -1
    internal var ghPct = 0
    internal var ghAppForeground = true
    internal val ghStepLast = arrayOfNulls<GhProgress>(5)
    internal var ghCancelBtn: LinearLayout? = null
    internal var ghCancelLabel: TextView? = null
    internal var ghRing: ProgressRingView? = null
    internal var ghPercentText: TextView? = null
    internal var ghElapsedText: TextView? = null
    internal var ghNoteBox: LinearLayout? = null
    internal var ghErrorHost: LinearLayout? = null
    internal val ghStepViews = ArrayList<StepStateView>()
    internal val ghStepTitles = ArrayList<TextView>()
    internal val ghStepDetails = ArrayList<TextView>()
    internal val ghHandler = Handler(Looper.getMainLooper())
    internal val ghTicker = object : Runnable {
        override fun run() {
            if (!ghRunning) return
            val sec = ((SystemClock.elapsedRealtime() - ghStartedAt) / 1000L).toInt()
            ghElapsedText?.text = "Berjalan %02d:%02d".format(sec / 60, sec % 60)
            ghHandler.postDelayed(this, 1000L)
        }
    }

    internal val ghInk: Int get() = ghc(0xFFF4F4F6, 0xFF15161A)
    internal val ghOnInk: Int get() = ghc(0xFF15161A, 0xFFFFFFFF)
    internal val ghCard: Int get() = ghc(0xFF1B1D22, 0xFFFFFFFF)
    internal val ghStroke: Int get() = ghc(0xFF34373F, 0xFFE3E5EA)
    internal val ghMuted: Int get() = ghc(0xFF9DA0A9, 0xFF6C717C)
    internal val ghSoft: Int get() = ghc(0xFF23262C, 0xFFF0F1F4)
    internal val ghDanger: Int get() = 0xFFD9534F.toInt()

    // ----- komponen form -----

    override fun onStart() { super.onStart(); ghAppForeground = true }
    override fun onStop() { ghAppForeground = false; super.onStop() }

    // ----- Polling hanya berjalan saat Activity terlihat -----
    internal var batteryCache: Intent? = null
    internal var batteryCacheAt = 0L

    /** Intent baterai (sticky) di-cache 1 detik agar tidak registerReceiver berulang-ulang. */


    internal var ghActionPollWanted = false

    override fun onPause() {
        espSensorRunnable?.let { espSensorHandler?.removeCallbacks(it) }
        modernUiRunnable?.let { modernUiHandler.removeCallbacks(it) }
        ghHandler.removeCallbacks(ghTicker)
        ghActionRunnable?.let { ghActionPoll?.removeCallbacks(it) }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (espSensorPolling) espSensorRunnable?.let { r -> espSensorHandler?.let { h -> h.removeCallbacks(r); h.post(r) } }
        modernUiRunnable?.let { modernUiHandler.removeCallbacks(it); modernUiHandler.post(it) }
        if (ghRunning) { ghHandler.removeCallbacks(ghTicker); ghHandler.post(ghTicker) }
        val poll = ghActionRunnable
        if (ghActionPollWanted && poll != null) { ghActionPoll?.removeCallbacks(poll); ghActionPoll?.post(poll) }
    }


    // ---------- ZIP ----------


    // ---------- SIMPLE TOOLS ----------


    internal inner class ColorWheelView(context: Context) : View(context) {
        internal val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal var selectedHue = 0f
        var onColorChanged: ((Int) -> Unit)? = null
        init { isClickable = true; setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx=width/2f; val cy=height/2f; val radius=(min(width,height)*.39f).coerceAtLeast(dp(55).toFloat())
            for (i in 0 until 360) {
                paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(18).toFloat(); paint.color=Color.HSVToColor(floatArrayOf(i.toFloat(),1f,1f))
                canvas.drawArc(cx-radius,cy-radius,cx+radius,cy+radius,i.toFloat(),1.4f,false,paint)
            }
            paint.style=Paint.Style.FILL; paint.color=Color.WHITE; paint.setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),0x55000000)
            canvas.drawCircle(cx,cy,dp(38).toFloat(),paint); paint.clearShadowLayer()
            val center=Color.HSVToColor(floatArrayOf(selectedHue,1f,1f)); paint.color=center; canvas.drawCircle(cx,cy,dp(30).toFloat(),paint)
            paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(3).toFloat(); paint.color=Color.WHITE
            val a=Math.toRadians(selectedHue.toDouble()); val sx=cx+Math.cos(a).toFloat()*radius; val sy=cy+Math.sin(a).toFloat()*radius
            canvas.drawCircle(sx,sy,dp(11).toFloat(),paint)
        }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if(event.action!=MotionEvent.ACTION_DOWN && event.action!=MotionEvent.ACTION_MOVE && event.action!=MotionEvent.ACTION_UP) return true
            val cx=width/2f; val cy=height/2f; val dx=event.x-cx; val dy=event.y-cy; val d=Math.sqrt((dx*dx+dy*dy).toDouble()).toFloat(); val r=(min(width,height)*.39f).coerceAtLeast(dp(55).toFloat())
            if(d >= r-dp(24) && d <= r+dp(24)) { selectedHue=((Math.toDegrees(Math.atan2(dy.toDouble(),dx.toDouble()))+360)%360).toFloat(); onColorChanged?.invoke(Color.HSVToColor(floatArrayOf(selectedHue,1f,1f))); invalidate() }
            performClick(); return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
    }


    /** Layer penuh untuk melihat seluruh warna hasil ekstraksi tanpa terpotong. */


    // ==================== CALCULATOR SUITE ====================


    internal val calculatorModes = listOf(
        CalculatorMode("basiccalc", "Dasar", "Utama"),
        CalculatorMode("scicalc", "Ilmiah", "Utama"),
        CalculatorMode("percentcalc", "Persentase", "Matematika"),
        CalculatorMode("fractioncalc", "Pecahan", "Matematika"),
        CalculatorMode("ratiocalc", "Rasio & Proporsi", "Matematika"),
        CalculatorMode("equationcalc", "Persamaan", "Matematika"),
        CalculatorMode("basecalc", "Basis Angka", "Matematika"),
        CalculatorMode("unitcalc", "Konverter Satuan", "Konversi"),
        CalculatorMode("datacalc", "Ukuran Data", "Konversi"),
        CalculatorMode("speedcalc", "Kecepatan", "Konversi"),
        CalculatorMode("pressurecalc", "Tekanan", "Konversi"),
        CalculatorMode("timecalc", "Durasi", "Tanggal & Waktu"),
        CalculatorMode("datecalc", "Tanggal & Umur", "Tanggal & Waktu"),
        CalculatorMode("worktimecalc", "Jam Kerja", "Tanggal & Waktu"),
        CalculatorMode("areacalc", "Luas & Keliling", "Geometri"),
        CalculatorMode("volumecalc", "Volume", "Geometri"),
        CalculatorMode("riskcalc", "Risk-Reward", "Finansial"),
        CalculatorMode("compoundcalc", "Compound & Tabungan", "Finansial"),
        CalculatorMode("margincalc", "Margin & Pajak", "Finansial"),
        CalculatorMode("discountcalc", "Diskon Bertingkat", "Finansial"),
        CalculatorMode("loancalc", "Cicilan Pinjaman", "Finansial"),
        CalculatorMode("fuelcalc", "Konsumsi BBM", "Finansial"),
        CalculatorMode("pivotcalc", "Pivot Point", "Trading"),
        CalculatorMode("dcacalc", "Averaging / DCA", "Trading"),
        CalculatorMode("installcalc", "Flat vs Anuitas", "Finansial"),
        CalculatorMode("pphcalc", "PPN & PPh", "Finansial"),
        CalculatorMode("dividercalc", "Voltage Divider", "Teknik"),
        CalculatorMode("pwmcalc", "PWM & Duty Cycle", "Teknik"),
        CalculatorMode("powercalc", "Konsumsi Listrik", "Teknik"),
        CalculatorMode("aspectcalc", "Aspect Ratio", "Developer"),
        CalculatorMode("spritecalc", "Sprite Sheet Grid", "Developer")
    )


    // ==================== EXTRA CALCULATORS 2.4 ====================


    /**
     * Calculator hub entry for UI color conversion.
     * Reuses the screen color picker implementation to avoid duplicating
     * color parsing/conversion state and UI logic.
     */


    // ===================== Pengelola Keuangan =====================
    // All finance UI (reader, dialogs, components) extracted to:
    //   feature/finance/FinanceScreen.kt
    //   feature/finance/FinanceDialogs.kt
    //   feature/finance/FinanceComponents.kt
    // Vars kept here for shared state:
    internal var financeSearchQuery = ""
    internal var financeCategoryFilter = "Semua"
    internal var financeWalletFilter = "Semua"
    internal var pendingFinanceExportJson = false
    // ==============================================================


    // ---------- ESP DEVICE / CONTROL TOOLKIT ----------


    // ---------- NETWORK ----------


    // ---------- APK / QR / SYSTEM ----------


    // ---------- QR SCANNER (tampilan 3 langkah: awal, pilih sumber, panel sumber) ----------

    internal var qrSourceExpanded = false
    internal var qrSelectedSource: String? = null // "file" | "foto" | "teks"
    internal var qrPickedUri: Uri? = null
    internal var qrPickedName: String? = null
    internal var qrScanBusy = false
    internal var qrScanResult: String? = null
    internal var qrCameraOutUri: Uri? = null
    internal val qrSources = listOf(
        Triple("file", "▤", "File"),
        Triple("foto", "▧", "Foto & Scan"),
        Triple("teks", "✎", "Teks / Link")
    )


    // ---------- V2.27: WORKSPACE / PLUGIN / CUSTOMIZATION ----------


    // ---------- NEW TOOLS 2.0 ----------


    // ---------- CRYPTO / HELPERS ----------


    // ---------- COMPLETE TOOL EXPANSION ----------


    internal var fileHashUriA: Uri? = null
    internal var fileHashUriB: Uri? = null
    internal var fileHashCompareLabelA: TextView? = null
    internal var fileHashCompareLabelB: TextView? = null


    internal var securityFileUri: Uri? = null
    internal var securityFileName: String? = "file"
    // State File Encryption (dipakai SecurityTools.kt)
    internal var encStage: String = "form"
    internal var encProgressPct: Int = 0
    internal var encResultPath: String? = null
    internal var encResultSize: Long = 0L
    internal var encErrorMessage: String = ""
    internal val encCancelFlag = java.util.concurrent.atomic.AtomicBoolean(false)
    internal var encModeEncrypt: Boolean = true
    internal var encPassword: String = ""
    internal var encShowPassword: Boolean = false
    internal var encOutSameAsSource: Boolean = true
    internal var encOverwrite: Boolean = false
    internal var encDeleteSource: Boolean = false
    internal var encCompress: Boolean = false
    internal var stegoImageUri: Uri? = null
    internal var certFileUri: Uri? = null
    internal val SECURITY_FILE_PICK = 1301
    internal val SECURITY_OPEN_PICK = 1305
    internal var lockedOpenPassword: String? = null
    internal val STEGO_ENCODE_PICK = 1302
    internal val STEGO_DECODE_PICK = 1303
    internal val CERT_PICK = 1304

    /** Daftar file .mytools.enc hasil enkripsi di aplikasi + opsi memilih dari penyimpanan. */


    /** Dekripsi ke cache lalu buka dengan aplikasi yang cocok (tipe dari ekstensi asli). */


    /** Decode gambar dengan inSampleSize agar foto besar tidak memenuhi RAM. */


    // Steganografi harus memakai resolusi penuh (sampling akan merusak bit tersembunyi),
    // jadi optimasinya: decode langsung mutable (tanpa copy) dan proses piksel per baris.


    /** Baca header "MYTOOLS-STG1:<panjang>:" lalu tepat <panjang> karakter; berhenti begitu pesan lengkap. */


    internal var imageStudioResult: ((Uri)->Unit)? = null
}
