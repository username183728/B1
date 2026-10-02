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


internal fun MainActivity.toolHelp(id: String, name: String = homeToolMap[id] ?: id): ToolHelp {
        return when (id) {
            "filemanager" -> ToolHelp("Kelola file dan folder di perangkat.", "Saat ingin menyalin, memindahkan, menghapus, atau membuka file.", "Pilih folder Download lalu pindahkan file ke folder lain.", "Pilih file")
            "recentfiles" -> ToolHelp("Menampilkan file yang baru saja digunakan.", "Saat ingin cepat kembali ke file terakhir.", "Buka file yang tadi kamu edit tanpa mencarinya lagi.")
            "backuprestore" -> ToolHelp("Membuat atau memulihkan backup data aplikasi.", "Sebelum reset, pindah perangkat, atau mengembalikan data.", "Export backup → simpan → Restore saat dibutuhkan.")
            "zip" -> ToolHelp("Membuat dan mengekstrak arsip ZIP.", "Saat ingin menggabungkan file atau membuka ZIP.", "Pilih beberapa file → buat ZIP → bagikan satu file.")
            "githubzip" -> ToolHelp("Mengirim project atau ZIP ke GitHub.", "Saat ingin publish/update project dari HP.", "Pilih ZIP project → pilih repository → upload.")
            "fileconvert" -> ToolHelp("Mengonversi file antar format yang didukung.", "Saat format file perlu disesuaikan dengan aplikasi lain.", "Konversi dokumen atau gambar ke format yang dibutuhkan.")
            "filesearch" -> ToolHelp("Mencari file berdasarkan nama atau lokasi.", "Saat penyimpanan sudah besar dan file sulit ditemukan.", "Cari semua file yang mengandung kata 'project'.")
            "storage" -> ToolHelp("Menganalisis penggunaan penyimpanan.", "Saat ingin mengetahui apa yang paling banyak memakai storage.", "Lihat kategori/file terbesar sebelum membersihkan storage.")
            "apps" -> ToolHelp("Melihat dan mengelola aplikasi yang terpasang.", "Saat ingin memeriksa aplikasi di perangkat.", "Cari aplikasi tertentu lalu lihat informasi paketnya.")
            "apk" -> ToolHelp("Memeriksa informasi dan metadata APK.", "Saat ingin mengetahui package, versi, atau detail APK.", "Pilih APK → periksa package name dan version.")
            "apkcompare" -> ToolHelp("Membandingkan dua APK.", "Saat ingin melihat perbedaan versi APK.", "Bandingkan APK lama dengan APK baru sebelum release.")
            "duplicatefinder" -> ToolHelp("Mencari file yang isinya sama.", "Saat ingin menghemat storage tanpa menghapus file secara sembarang.", "Scan folder Download → review file duplikat → hapus yang tidak diperlukan.")
            "largefilefinder" -> ToolHelp("Mencari file berukuran paling besar.", "Saat storage hampir penuh.", "Cari 20 file terbesar lalu review sebelum menghapus.")
            "filehashcompare" -> ToolHelp("Membandingkan hash dua file.", "Saat ingin memastikan dua file identik.", "Bandingkan SHA-256 APK dengan checksum resmi.")
            "devicecenter" -> ToolHelp("Menampilkan informasi perangkat dan sistem.", "Saat ingin memeriksa hardware, Android, atau konfigurasi perangkat.", "Cek versi Android dan informasi perangkat.")
            "json" -> ToolHelp("Membuka workspace untuk bekerja dengan JSON.", "Saat mengedit atau memeriksa data JSON.", "Tempel response API lalu rapikan dan edit datanya.")
            "jsonformat" -> ToolHelp("Merapikan JSON menjadi format yang mudah dibaca.", "Saat JSON dari API terlihat satu baris atau berantakan.", "Tempel JSON minified → format → baca struktur object/array.")
            "base64" -> ToolHelp("Encode atau decode teks menggunakan Base64.", "Saat API atau format data membutuhkan Base64.", "Encode teks menjadi Base64 lalu gunakan hasilnya di request.")
            "base64file" -> ToolHelp("Encode atau decode file dengan Base64.", "Saat sebuah API menerima file sebagai Base64.", "Pilih gambar → encode → salin string Base64.")
            "url" -> ToolHelp("Encode atau decode bagian URL.", "Saat parameter URL memiliki karakter khusus.", "Encode 'hello world' agar aman dimasukkan ke query URL.")
            "regex" -> ToolHelp("Menguji pola regular expression.", "Saat ingin memvalidasi atau mengekstrak pola dari teks.", "Uji regex email terhadap daftar alamat.")
            "uuid" -> ToolHelp("Membuat UUID unik.", "Saat API, database, atau resource membutuhkan ID unik.", "Generate UUID lalu gunakan sebagai ID record.")
            "uuidbatch" -> ToolHelp("Membuat banyak UUID sekaligus.", "Saat membutuhkan ID unik dalam jumlah besar.", "Generate 100 UUID untuk seed data.")
            "textstat" -> ToolHelp("Menganalisis statistik teks.", "Saat ingin mengetahui jumlah karakter, kata, atau baris.", "Tempel artikel → lihat jumlah kata dan karakter.")
            "case" -> ToolHelp("Mengubah format huruf teks.", "Saat perlu uppercase, lowercase, atau variasi case.", "Ubah nama field menjadi UPPERCASE atau lowercase.")
            "dedupe" -> ToolHelp("Menghapus baris teks yang duplikat.", "Saat daftar memiliki item yang berulang.", "Tempel daftar email → hapus baris yang sama.")
            "compare" -> ToolHelp("Membandingkan dua teks.", "Saat ingin menemukan perbedaan antara dua versi.", "Bandingkan config lama dan config baru.")
            "slug" -> ToolHelp("Membuat slug ramah URL dari teks.", "Saat membuat URL dari judul.", "'Hello World 2026' → slug untuk URL.")
            "lorem" -> ToolHelp("Membuat teks placeholder Lorem Ipsum.", "Saat UI membutuhkan teks sementara.", "Generate paragraf untuk menguji layout kartu.")
            "password" -> ToolHelp("Membuat password acak.", "Saat membutuhkan credential baru yang tidak mudah ditebak.", "Generate password lalu simpan di password manager.")
            "passwordstrength" -> ToolHelp("Menilai kekuatan password.", "Saat ingin mengevaluasi password sebelum digunakan.", "Masukkan password uji → lihat indikator kekuatan.")
            "passwordanalyzer" -> ToolHelp("Menganalisis karakteristik password.", "Saat melakukan review keamanan credential.", "Periksa panjang, variasi karakter, dan pola lemah.")
            "token" -> ToolHelp("Membuat token acak.", "Saat membutuhkan identifier atau secret sementara.", "Generate token untuk pengujian API.")
            "jwt" -> ToolHelp("Membaca isi JWT tanpa mengubah signature.", "Saat debugging token autentikasi.", "Tempel JWT → lihat header dan payload.")
            "hmac" -> ToolHelp("Membuat HMAC dengan secret key.", "Saat API menggunakan signature berbasis HMAC.", "Hitung HMAC request menggunakan secret yang kamu miliki.")
            "totp" -> ToolHelp("Membuat kode TOTP untuk autentikasi dua faktor.", "Saat mengelola secret TOTP milik akun sendiri.", "Masukkan secret TOTP → lihat kode yang sedang berlaku.")
            "totpvault" -> ToolHelp("Menyimpan dan mengelola secret TOTP.", "Saat membutuhkan penyimpanan lokal untuk akun 2FA.", "Tambahkan akun → gunakan kode TOTP saat login.")
            "aes" -> ToolHelp("Enkripsi atau dekripsi data dengan AES.", "Saat ingin melindungi data yang kamu simpan atau kirim.", "Enkripsi teks dengan key lalu simpan ciphertext.")
            "random" -> ToolHelp("Menghasilkan random bytes.", "Saat membutuhkan data acak untuk testing atau crypto workflow.", "Generate 32 random bytes untuk kebutuhan pengujian.")
            "hash" -> ToolHelp("Menghasilkan hash dari input.", "Saat ingin membuat fingerprint data.", "Masukkan teks → hitung SHA-256.")
            "checksum" -> ToolHelp("Menghitung checksum/hash file.", "Saat memverifikasi integritas file.", "Pilih APK → bandingkan checksum dengan sumber resmi.")
            "hex" -> ToolHelp("Mengubah data antara teks dan representasi hexadecimal.", "Saat bekerja dengan byte atau protokol.", "Convert byte/string ke HEX untuk debugging.")
            "base32" -> ToolHelp("Encode atau decode Base32.", "Saat bekerja dengan format Base32 seperti secret tertentu.", "Decode string Base32 menjadi data asli.")
            "securitycenter" -> ToolHelp("Menyatukan beberapa pemeriksaan keamanan.", "Saat ingin melakukan pemeriksaan keamanan dasar.", "Buka Security Center lalu review status keamanan yang tersedia.")
            "fileencryption" -> ToolHelp("Melindungi file dengan enkripsi.", "Saat file lokal berisi data yang tidak ingin dibaca sembarang orang.", "Pilih file → enkripsi → simpan hasilnya.")
            "steganography" -> ToolHelp("Menyisipkan atau membaca data tersembunyi pada media yang didukung.", "Untuk eksperimen dan workflow data yang memang kamu miliki.", "Gunakan gambar milik sendiri untuk menguji proses encode/decode.")
            "breachchecker" -> ToolHelp("Memeriksa indikator kebocoran data dengan layanan yang didukung.", "Saat ingin mengecek apakah credential perlu diganti.", "Masukkan identifier yang memang boleh kamu periksa lalu review hasilnya.")
            "securenotes" -> ToolHelp("Menyimpan catatan sensitif secara lokal dengan perlindungan yang tersedia.", "Saat membutuhkan catatan privat di perangkat.", "Buat catatan → kunci → buka kembali saat diperlukan.")
            "pgp" -> ToolHelp("Mengenkripsi atau mendekripsi data menggunakan PGP.", "Saat workflow membutuhkan kriptografi PGP.", "Enkripsi pesan untuk penerima yang memiliki public key yang sesuai.")
            "sshkeygen" -> ToolHelp("Membuat pasangan kunci SSH.", "Saat menyiapkan akses SSH ke server atau Git.", "Generate key pair → pasang public key pada server.")
            "certviewer" -> ToolHelp("Melihat informasi sertifikat digital.", "Saat memeriksa issuer, validity, atau detail sertifikat.", "Buka sertifikat → cek issuer dan masa berlaku.")
            "virusscanner" -> ToolHelp("Memeriksa file dengan mekanisme scanner yang tersedia.", "Saat ingin melakukan pemeriksaan awal terhadap file.", "Pilih file hasil download → jalankan scan → review hasil.")
            "urlsafety" -> ToolHelp("Menganalisis URL untuk indikator risiko yang tersedia.", "Sebelum membuka URL yang tidak kamu kenal.", "Tempel URL → review domain, scheme, dan indikator yang tersedia.")
            "dns" -> ToolHelp("Melakukan DNS lookup.", "Saat ingin mengetahui record DNS sebuah domain.", "Masukkan example.com → lihat record yang tersedia.")
            "rdns" -> ToolHelp("Melakukan reverse DNS lookup.", "Saat ingin mencari hostname dari IP.", "Masukkan IP → lihat hostname yang terdeteksi.")
            "port" -> ToolHelp("Memeriksa konektivitas port pada host yang kamu kelola.", "Saat debugging service atau server milik sendiri.", "Cek host lokal dan port 8080 sebelum debugging API.")
            "publicip" -> ToolHelp("Menampilkan IP publik koneksi.", "Saat perlu mengetahui alamat publik koneksi saat ini.", "Buka tool → lihat IP publik yang terdeteksi.")
            "ping" -> ToolHelp("Menguji keterjangkauan host.", "Saat koneksi ke server terasa bermasalah.", "Ping server milikmu untuk melihat latency dan reachability.")
            "ipinfo" -> ToolHelp("Menampilkan informasi alamat IP.", "Saat debugging konfigurasi jaringan.", "Masukkan IP → lihat informasi yang tersedia.")
            "ssl" -> ToolHelp("Memeriksa sertifikat TLS/SSL sebuah endpoint.", "Saat mengecek konfigurasi HTTPS.", "Periksa domain → lihat issuer dan masa berlaku sertifikat.")
            "http" -> ToolHelp("Menjalankan HTTP server lokal.", "Saat ingin melayani file atau endpoint sederhana dari perangkat.", "Pilih folder → jalankan server lokal → akses dari jaringan yang diizinkan.")
            "webhostwifi" -> ToolHelp("Menyajikan halaman HTML melalui jaringan Wi-Fi lokal.", "Saat ingin preview halaman dari perangkat lain di jaringan yang sama.", "Pilih folder web → start hosting → buka alamat lokal.")
            "httpheaders" -> ToolHelp("Melihat atau menguji HTTP headers.", "Saat debugging request/response HTTP.", "Periksa Content-Type, Cache-Control, atau Authorization.")
            "restclient" -> ToolHelp("Mengirim request HTTP ke API dan melihat response.", "Saat menguji API tanpa membuat aplikasi khusus.", "GET /users → kirim request → lihat status, headers, dan body.", "Buat request")
            "websocket" -> ToolHelp("Menguji koneksi WebSocket.", "Saat debugging service realtime.", "Hubungkan endpoint WebSocket → kirim pesan → lihat event response.")
            "network" -> ToolHelp("Melihat informasi jaringan perangkat.", "Saat debugging Wi-Fi atau koneksi data.", "Periksa interface, alamat IP, dan status koneksi.")
            "networkcenter" -> ToolHelp("Menggabungkan beberapa pemeriksaan jaringan.", "Saat ingin melakukan pemeriksaan jaringan dari satu tempat.", "Buka Network Center → pilih pemeriksaan yang dibutuhkan.")
            "wifi" -> ToolHelp("Melihat informasi Wi-Fi yang tersedia untuk aplikasi.", "Saat memeriksa koneksi Wi-Fi perangkat.", "Buka tool saat terhubung ke Wi-Fi untuk melihat detailnya.")
            "qr" -> ToolHelp("Memindai QR code.", "Saat menerima QR berisi URL, teks, atau data lain.", "Arahkan kamera ke QR → baca hasilnya.")
            "color" -> ToolHelp("Memilih, menganalisis, dan mengonversi warna.", "Saat bekerja dengan UI, desain, atau asset.", "Ambil warna dari gambar → salin HEX/RGB.")
            "imagestudio" -> ToolHelp("Mengolah dan memeriksa gambar dari perangkat.", "Saat perlu melakukan pekerjaan gambar sederhana.", "Pilih gambar → gunakan tool yang tersedia → simpan hasil.")
            "number" -> ToolHelp("Membuka pusat kalkulator dengan berbagai mode.", "Saat membutuhkan perhitungan umum atau teknis.", "Pilih mode dasar, ilmiah, rasio, atau kalkulator teknis.")
            "financereader" -> ToolHelp("Mencatat dan mengelola transaksi keuangan lokal.", "Saat ingin mencatat pemasukan dan pengeluaran.", "Tambah transaksi → pilih kategori → lihat saldo.")
            "financedashboard" -> ToolHelp("Menampilkan ringkasan dan insight keuangan.", "Saat ingin melihat pola pemasukan dan pengeluaran.", "Buka dashboard → review ringkasan periode berjalan.")
            "reminder" -> ToolHelp("Membuat pengingat lokal.", "Saat ada tugas atau jadwal yang tidak boleh terlewat.", "Buat reminder untuk waktu tertentu lalu aktifkan notifikasi.")
            "stopwatch" -> ToolHelp("Mengukur durasi dengan stopwatch.", "Saat membutuhkan pengukuran waktu yang berjalan.", "Mulai → ukur durasi → pause/reset.")
            "timer" -> ToolHelp("Menjalankan hitung mundur.", "Saat membutuhkan pengingat berbasis durasi.", "Atur 10 menit → mulai → tunggu notifikasi.")
            "markdown" -> ToolHelp("Melihat Markdown dalam tampilan terformat.", "Saat membaca README atau dokumentasi Markdown.", "Tempel Markdown → lihat hasil render.")
            "sql" -> ToolHelp("Membantu membaca atau bekerja dengan query SQL.", "Saat debugging atau menyusun query database.", "Masukkan query SELECT → periksa struktur dan hasil yang didukung.")
            "yaml" -> ToolHelp("Memformat dan memeriksa YAML.", "Saat mengedit konfigurasi YAML.", "Tempel config YAML → rapikan dan periksa struktur.")
            "toml" -> ToolHelp("Memeriksa struktur TOML.", "Saat debugging file konfigurasi TOML.", "Tempel TOML → periksa section dan value.")
            "cron" -> ToolHelp("Membantu menyusun ekspresi cron.", "Saat membuat jadwal otomatis berbasis cron.", "Atur menit/jam/hari → gunakan ekspresi yang dihasilkan.")
            "helpbot" -> ToolHelp("Memberi bantuan lokal untuk penggunaan tool.", "Saat tidak yakin tool mana yang harus digunakan.", "Cari kebutuhanmu di HelpBot lalu ikuti rekomendasi.")
            "workspace" -> ToolHelp("Pusat workspace untuk workflow project.", "Saat ingin mengatur pekerjaan project dari satu tempat.", "Buka workspace → pilih project atau workflow.")
            "plugincenter" -> ToolHelp("Mengelola integrasi/plugin yang tersedia.", "Saat ingin menambah atau mengatur kemampuan terhubung.", "Buka Plugin Center → review plugin yang tersedia.")
            "customtools" -> ToolHelp("Mengatur tampilan dan perilaku tool yang bisa dikustomisasi.", "Saat ingin menyesuaikan workspace dengan kebiasaanmu.", "Pilih tool → ubah opsi yang tersedia.")
            "studiocenter" -> ToolHelp("Pusat untuk workflow Studio.", "Saat ingin masuk ke salah satu Studio khusus.", "Pilih Studio sesuai pekerjaan yang ingin dilakukan.")
            else -> {
                val lower = name.lowercase(Locale.ROOT)
                when {
                    id.endsWith("calc") -> ToolHelp("Kalkulator khusus untuk $lower.", "Saat membutuhkan perhitungan yang sesuai bidang ini.", "Masukkan nilai yang dibutuhkan → hitung → review hasil.")
                    id.contains("file") -> ToolHelp("Utility untuk bekerja dengan file terkait $lower.", "Saat sedang mengelola atau memeriksa file.", "Pilih file → jalankan operasi → review hasil.")
                    id.contains("network") || id in setOf("dns", "rdns", "port", "ping") -> ToolHelp("Utility jaringan untuk $lower.", "Saat debugging atau memeriksa koneksi yang kamu kelola.", "Masukkan target yang kamu miliki izin untuk uji → jalankan pemeriksaan.")
                    else -> ToolHelp("Utility untuk $lower.", "Saat pekerjaanmu membutuhkan fungsi ini.", "Buka tool → masukkan data yang diperlukan → jalankan aksi utama.")
                }
            }
        }
    }
internal fun MainActivity.showToolHelpDialog(id: String) {
        val name = homeToolMap[id] ?: id
        val help = toolHelp(id, name)
        val message = buildString {
            append(help.purpose)
            append("\n\nKapan digunakan?\n")
            append(help.whenUse)
            append("\n\nContoh\n")
            append(help.example)
        }
        AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(message)
            .setNegativeButton("Tutup", null)
            .setPositiveButton(help.action) { _, _ -> openTool(id) }
            .show()
    }
internal fun MainActivity.iconFor(id: String): String = when (id) {
        "filemanager" -> "folder-outline"
        "recentfiles" -> "history"
        "backuprestore" -> "backup-restore"
        "workspace" -> "view-dashboard-outline"
        "plugincenter" -> "puzzle-outline"
        "customtools" -> "tune-variant"
        "studiocenter" -> "palette-swatch-outline"
        "editor" -> "file-document-edit-outline"
        "reminder" -> "bell-outline"
        "zip" -> "folder-zip-outline"
        "githubzip" -> "rocket-launch-outline"
        "wifi" -> "wifi"
        "devicecenter" -> "cellphone-cog"
        "json" -> "code-json"
        "hash" -> "pound"
        "base64" -> "numeric-4-box-outline"
        "url" -> "link-variant"
        "regex" -> "regex"
        "uuid" -> "identifier"
        "color" -> "palette-outline"
        "number" -> "calculator-variant-outline"
        "history" -> "history"
        "uicolorcalc" -> "palette-swatch-variant"
        "basiccalc" -> "calculator"
        "scicalc" -> "function-variant"
        "percentcalc" -> "percent-outline"
        "fractioncalc" -> "division"
        "ratiocalc" -> "scale-balance"
        "unitcalc" -> "ruler"
        "areacalc" -> "vector-square"
        "volumecalc" -> "cube-outline"
        "speedcalc" -> "speedometer"
        "timecalc" -> "clock-outline"
        "datecalc" -> "calendar-range-outline"
        "loancalc" -> "bank-outline"
        "fuelcalc" -> "gas-station-outline"
        "pivotcalc" -> "chart-areaspline"
        "dividercalc" -> "sine-wave"
        "dcacalc" -> "finance"
        "pwmcalc" -> "pulse"
        "spritecalc" -> "grid"
        "installcalc" -> "cash-multiple"
        "powercalc" -> "flash-outline"
        "aspectcalc" -> "aspect-ratio"
        "pphcalc" -> "percent"
        "financereader" -> "wallet-outline"
        "financedashboard" -> "chart-line"
        "securitycenter" -> "shield-check-outline"
        "helpbot" -> "robot-outline"
        "riskcalc" -> "scale-balance"
        "compoundcalc" -> "chart-timeline-variant"
        "margincalc" -> "cash-register"
        "discountcalc" -> "tag-outline"
        "datacalc" -> "database-outline"
        "pressurecalc" -> "gauge"
        "worktimecalc" -> "briefcase-clock-outline"
        "basecalc" -> "numeric"
        "equationcalc" -> "sigma"
        "textstat" -> "format-list-numbered"
        "case" -> "format-letter-case"
        "dedupe" -> "content-duplicate"
        "compare" -> "compare"
        "slug" -> "link-box-variant-outline"
        "lorem" -> "format-align-left"
        "password" -> "form-textbox-password"
        "token" -> "key-variant"
        "jwt" -> "badge-account-outline"
        "hmac" -> "shield-key-outline"
        "totp" -> "clock-check-outline"
        "aes" -> "lock-outline"
        "random" -> "dice-multiple"
        "checksum" -> "file-check-outline"
        "hex" -> "hexadecimal"
        "base32" -> "numeric"
        "dns" -> "dns"
        "rdns" -> "lan"
        "port" -> "lan-connect"
        "publicip" -> "ip-outline"
        "ping" -> "access-point-network"
        "ipinfo" -> "ip-network"
        "ssl" -> "certificate-outline"
        "apk" -> "android"
        "qr" -> "qrcode"
        "fileconvert" -> "swap-horizontal"
        "system" -> "cog-outline"
        "http" -> "web"
        "webhostwifi" -> "wifi-star"
        "filehashcompare" -> "file-compare"
        "markdown" -> "language-markdown-outline"
        "sql" -> "database-search-outline"
        "yaml" -> "file-code-outline"
        "toml" -> "file-cog-outline"
        "cron" -> "calendar-clock-outline"
        "passwordstrength" -> "shield-lock-outline"
        "fileencryption" -> "file-lock-outline"
        "steganography" -> "image-lock-outline"
        "passwordanalyzer" -> "shield-search"
        "breachchecker" -> "shield-alert-outline"
        "securenotes" -> "note-edit-outline"
        "totpvault" -> "shield-key-outline"
        "pgp" -> "key-chain-variant"
        "sshkeygen" -> "key-plus"
        "certviewer" -> "certificate-outline"
        "virusscanner" -> "bug-outline"
        "urlsafety" -> "link-lock"
        "stopwatch" -> "timer-outline"
        "timer" -> "timer-sand"
        "imagestudio" -> "image-multiple-outline"
        "timestamp" -> "clock-time-four-outline"
        "unicode" -> "format-letter-case-upper"
        "urlparser" -> "link-variant"
        "mime" -> "file-document-outline"
        "jsonformat" -> "code-braces"
        "xmlformat" -> "xml"
        "uuidbatch" -> "identifier"
        "base64file" -> "file-code-outline"
        "httpheaders" -> "format-header-1"
        "textreplace" -> "find-replace"
        "wordfreq" -> "counter"
        "deviceinfo" -> "cellphone-information"
        "storage" -> "database"
        "apps" -> "apps"
        "network" -> "network"
        "battery" -> "battery-high"
        "filesearch" -> "file-search"
        "clipboard" -> "clipboard-text-outline"
        "esp" -> "chip"
        "espdiscover" -> "radar"
        "ledstudio" -> "led-strip"
        "espdevice" -> "devices"
        "espgpio" -> "expansion-card-variant"
        "espsensor" -> "thermometer"
        "espwifi" -> "router-wireless"
        "espota" -> "upload-network"
        "esphttp" -> "web-box"
        "espmqtt" -> "message-cog-outline"
        "espusb" -> "usb-port"
        "espserial" -> "serial-port"
        "iotdashboard" -> "view-dashboard-outline"
        "espstudio" -> "tools"
        "visualwiring" -> "vector-polyline"
        "ocr" -> "ocr"
        "unitconverter" -> "swap-horizontal-bold"
        "apkanalyzer" -> "android-studio"
        "netscanner" -> "magnify-scan"
        "webproject" -> "web-plus"
        "webeditor" -> "language-html5"
        "networkstudio" -> "lan"
        "developerstudio" -> "code-tags"
        "filestudio" -> "folder-multiple-outline"
        "imagestudio" -> "image-multiple-outline"
        "colorstudio" -> "palette"
        "systemstudio" -> "cellphone-cog"
        "financestudio" -> "cash-multiple"
        "utilitystudio" -> "toolbox-outline"
        "whois" -> "account-search-outline"
        "traceroute" -> "routes"
        "subnetcalc" -> "ip-network-outline"
        "restclient" -> "api"
        "websocket" -> "connection"
        "networkcenter" -> "lan-connect"
        "systemcenter" -> "view-dashboard-outline"
        "apkcompare" -> "compare-horizontal"
        "duplicatefinder" -> "file-multiple-outline"
        "largefilefinder" -> "file-search-outline"
        else -> "tools"
    }

internal fun MainActivity.edit(hint: String = "", multiline: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint
        textSize = 16f
        setTextColor(textMain)
        setHintTextColor(textMuted)
        setPadding(dp(Ds.SPACE_LG), dp(Ds.SPACE_MD), dp(Ds.SPACE_LG), dp(Ds.SPACE_MD))
        val theme = visualTheme()
        background = bg(theme.surface, Ds.RADIUS_MD, theme.border)
        isSingleLine = !multiline
        isFocusable = true
        isFocusableInTouchMode = true
        if (hint.isNotBlank()) contentDescription = hint
        // Focus state jelas: border 2dp memakai warna teks utama.
        setOnFocusChangeListener { view, focused ->
            view.background = bg(
                theme.surface, Ds.RADIUS_MD,
                if (focused) theme.button else theme.border
            ).also { d -> if (focused) d.setStroke(dp(2), theme.button) }
        }
        if (multiline) {
            minLines = 6
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        } else {
            minLines = 1
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT
        }
        // Tinggi responsif: ikut tinggi layar (HP kecil s/d besar, portrait/landscape).
        val multilineHeight = (resources.displayMetrics.heightPixels * 0.30f).toInt()
            .coerceIn(dp(160), dp(300))
        layoutParams = LinearLayout.LayoutParams(
            -1,
            if (multiline) multilineHeight else -2
        ).apply { bottomMargin = dp(Ds.SPACE_MD) }
        if (!multiline) minHeight = dp(56)
    }

internal fun MainActivity.button(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 14f
        minHeight = dp(Ds.TOUCH_MIN)
        setPadding(dp(Ds.SPACE_LG), dp(Ds.SPACE_XS), dp(Ds.SPACE_LG), dp(Ds.SPACE_XS))
        setStateListAnimator(null)
        isAllCaps = false
        letterSpacing = 0.01f
        contentDescription = text
        isFocusable = true
        // Hierarki otomatis: aksi utama terisi, aksi pendukung (Salin/Hapus/Reset...) outline.
        if (isSecondaryAction(text)) styleAsSecondary(this) else styleAsPrimary(this)
        setOnClickListener {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            if (animationsEnabled()) {
                animate().scaleX(0.98f).scaleY(0.98f).setDuration(Ds.ANIM_FAST / 2)
                    .withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(Ds.ANIM_FAST).start() }.start()
            }
            onClick()
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(Ds.SPACE_SM) }
    }

internal fun MainActivity.toolHeader(titleText: String, description: String, icon: String = "•"): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = bg(panel2, 18, line)
        }
        val iconView = MdiIconView(this).apply {
            setIconName(resolveToolIcon(titleText, icon)); setIconSize(22f); setTextColor(textMain)
            background = bg(panel, 14, line)
        }
        box.addView(iconView, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(12) })
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(titleText, 18f, true))
        texts.addView(subLabel(description, 11f))
        box.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        return box
    }
internal fun MainActivity.resolveToolIcon(titleText: String, icon: String): String {
        if (MdiGlyphs.has(icon)) return icon
        val exact = homeTools.firstOrNull { it.second.equals(titleText, true) }?.first
        if (exact != null) return iconFor(exact)
        val fuzzy = homeTools.firstOrNull { titleText.contains(it.second, true) || it.second.contains(titleText, true) }?.first
        return if (fuzzy != null) iconFor(fuzzy) else "tools"
    }
internal fun MainActivity.toolSection(titleText: String, subtitle: String = ""): LinearLayout {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), dp(8), dp(2), dp(5)) }
        box.addView(label(titleText, 12f, true))
        if (subtitle.isNotBlank()) box.addView(subLabel(subtitle, 10f))
        return box
    }
internal fun MainActivity.toolStatus(textValue: String, positive: Boolean = false): TextView = TextView(this).apply {
        val dotColor = statusColor(if (positive) Ds.State.SUCCESS else Ds.State.INFO)
        val sb = android.text.SpannableStringBuilder("●  $textValue")
        sb.setSpan(android.text.style.ForegroundColorSpan(dotColor), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text = sb
        textSize = 13f
        setTextColor(textMain)
        minHeight = dp(Ds.TOUCH_MIN)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(Ds.SPACE_LG), dp(Ds.SPACE_MD), dp(Ds.SPACE_LG), dp(Ds.SPACE_MD))
        background = bg(if (positive) panel else panel2, Ds.RADIUS_MD, line)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(Ds.SPACE_SM) }
    }

internal fun MainActivity.addToolHeader(titleText: String, description: String, icon: String = "•") {
        // Intro card bersama: tidak mengulang judul toolbar, tetapi memberi konteks
        // sehingga halaman tool tidak terasa seperti form kosong.
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = bg(panel2, Ds.RADIUS_LG, line)
            contentDescription = "Tentang $titleText"
        }
        val iconView = MdiIconView(this).apply {
            setIconName(resolveToolIcon(titleText, icon))
            setIconSize(21f)
            setTextColor(textMain)
            background = bg(panel, Ds.RADIUS_MD, line)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        card.addView(iconView, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(12) })
        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textBox.addView(subLabel(description, 12f).apply { setTextColor(textMain) })
        textBox.addView(subLabel("Lokal di perangkat • Hasil dapat diproses kembali", 10f))
        card.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }
internal fun MainActivity.toolWorkspace(titleText: String, description: String, icon: String = "tools") {
        // Header & control bar dihapus; Riwayat/Info ada di menu titik tiga.
    }
internal fun MainActivity.toolWorkspaceSection(titleText: String, subtitle: String = "") {
        content.addView(toolSection(titleText, subtitle))
    }
internal fun MainActivity.compactButtonRow(vararg items: Pair<String, () -> Unit>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        items.forEachIndexed { index, item ->
            val b = button(item.first, item.second)
            if (index == 0) styleAsPrimary(b) else styleAsSecondary(b)
            row.addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (index > 0) leftMargin = dp(Ds.SPACE_SM)
            })
        }
        return row
    }
internal fun MainActivity.toolControlBar(name: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = bg(panel2, 14, line)
        }
        val status = TextView(this).apply {
            text = "●  Siap digunakan"
            textSize = 11f
            setTextColor(textMain)
            gravity = Gravity.CENTER_VERTICAL
        }
        box.addView(status, LinearLayout.LayoutParams(0, dp(40), 1f))
        fun actionText(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
            this.text = text
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(textMain)
            background = bg(panel, 10, line)
            setPadding(dp(9), 0, dp(9), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        box.addView(actionText("Riwayat") { historyTool() }, LinearLayout.LayoutParams(dp(76), dp(36)).apply { rightMargin = dp(5) })
        box.addView(actionText("Info") {
            AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(toolDescription(name))
                .setPositiveButton("OK", null)
                .show()
        }, LinearLayout.LayoutParams(dp(54), dp(36)))
        return box
    }
internal fun MainActivity.showToolMenu(anchor: View) {
        val name = currentPage
        val pm = PopupMenu(this, anchor)
        pm.menu.add(0, 1, 0, "Riwayat")
        pm.menu.add(0, 2, 1, "Info")
        pm.menu.add(0, 3, 2, "Tentang GITLS")
        pm.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> historyTool()
                2 -> AlertDialog.Builder(this)
                    .setTitle(name)
                    .setMessage(toolDescription(name))
                    .setPositiveButton("OK", null)
                    .show()
                3 -> showAbout()
            }
            true
        }
        pm.show()
    }
internal fun MainActivity.toolDescription(name: String): String = when {
        name.contains("JSON", true) -> "Validasi, rapikan, kecilkan, dan proses JSON. Hasil dapat disalin atau dibagikan."
        name.contains("Converter", true) -> "Pilih input, tentukan format tujuan, proses file, lalu buka atau bagikan hasil."
        name.contains("Network", true) || name in setOf("Ping", "DNS Lookup", "Reverse DNS", "Port Checker", "SSL Certificate", "REST / API Client", "WebSocket Client", "Network Center") -> "Tool jaringan untuk diagnosis, validasi koneksi, API, WebSocket, dan pemeriksaan host."
        name.contains("ESP", true) || name.contains("IoT", true) -> "Koneksi, kontrol, monitoring, diagnosis, dan pengujian perangkat ESP/IoT."
        name.contains("Finance", true) || name.contains("Keuangan", true) -> "Pencatatan lokal, transaksi, ringkasan, anggaran, insight, dan ekspor."
        name.contains("Calculator", true) || name.contains("Kalkulator", true) || name in setOf("Voltage Divider", "Pivot Point", "PWM & Duty Cycle") -> "Masukkan parameter, hitung, lalu salin atau bagikan hasil. Input divalidasi sebelum perhitungan."
        name.contains("File", true) || name.contains("ZIP", true) -> "Kelola, baca, konversi, kompres, atau ekstrak file dengan hasil yang dapat diproses kembali."
        else -> "Tool GITLS dengan fungsi utama, validasi input, hasil, salin, bagikan, dan riwayat lokal bila relevan."
    }

internal fun MainActivity.sanitizeSensitiveHistory() {
        val arr = runCatching { JSONArray(prefs.getString("history", "[]") ?: "[]") }.getOrElse { JSONArray() }
        val cleaned = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (!isSensitiveTool(item.optString("tool"))) cleaned.put(item)
        }
        if (cleaned.length() != arr.length()) prefs.edit().putString("history", cleaned.toString()).apply()
    }
internal fun MainActivity.isSensitiveTool(name: String = currentPage): Boolean = name in setOf(
        "Password Generator", "AES Encrypt / Decrypt", "HMAC Generator", "TOTP Generator",
        "Token Acak", "Random Bytes", "JWT Decoder"
    )

internal fun MainActivity.output(text: String) {
        val safe = text.ifBlank { "(kosong)" }
        if (!isSensitiveTool()) saveHistory(currentPage, safe)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(Ds.SPACE_LG), dp(Ds.SPACE_MD), dp(Ds.SPACE_LG), dp(Ds.SPACE_SM))
            background = bg(panel2, Ds.RADIUS_LG, line)
            contentDescription = "Hasil $currentPage"
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = MdiIconView(this).apply {
            setIconName(Ds.stateIcon(Ds.State.SUCCESS))
            setIconSize(18f)
            setTextColor(statusColor(Ds.State.SUCCESS))
            background = bg(panel, Ds.RADIUS_SM, line)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        heading.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(Ds.SPACE_SM) })
        val headingText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        headingText.addView(label("Hasil", 14f, true))
        headingText.addView(subLabel("${currentPage} • selesai", 12f))
        heading.addView(headingText, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(heading)

        // Output panjang/berbaris banyak (kode, log, JSON) memakai monospace agar rapi.
        val looksLikeCode = safe.contains('\n') || safe.startsWith("{") || safe.startsWith("[")
        val result = TextView(this).apply {
            this.text = safe
            textSize = if (looksLikeCode) 13f else 14f
            if (looksLikeCode) typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(textMain)
            setPadding(dp(Ds.SPACE_MD), dp(Ds.SPACE_MD), dp(Ds.SPACE_MD), dp(Ds.SPACE_MD))
            background = bg(panel, Ds.RADIUS_MD, line)
            setTextIsSelectable(true)
            gravity = Gravity.TOP or Gravity.START
        }
        card.addView(result, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(Ds.SPACE_SM); bottomMargin = dp(Ds.SPACE_SM) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun resultAction(textValue: String, iconName: String, primary: Boolean, onClick: () -> Unit): TextView = TextView(this).apply {
            this.text = textValue
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            minHeight = dp(Ds.TOUCH_MIN)
            isClickable = true
            isFocusable = true
            contentDescription = textValue
            if (primary) styleAsPrimary(this) else styleAsSecondary(this)
            setOnClickListener { onClick() }
        }
        actions.addView(resultAction("Salin", "content-copy", true) { copyText(safe) },
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(Ds.SPACE_XS) })
        actions.addView(resultAction("Bagikan", "share-variant", false) { shareText(safe) },
            LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(Ds.SPACE_XS); rightMargin = dp(Ds.SPACE_XS) })
        actions.addView(resultAction("Bersihkan", "delete-outline", false) { content.removeView(card) },
            LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(Ds.SPACE_XS) })
        card.addView(actions)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(Ds.SPACE_SM); bottomMargin = dp(Ds.SPACE_SM) })
    }
internal fun MainActivity.copyText(value:String) {
        val cm=getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("MyTools",value)); toast("Hasil disalin")
    }
internal fun MainActivity.shareText(value:String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,value) },"Bagikan hasil"))
    }
internal fun MainActivity.saveHistory(tool:String, result:String) {
        val arr=runCatching { JSONArray(prefs.getString("history","[]") ?: "[]") }.getOrElse { JSONArray() }
        val item=JSONObject().apply { put("time",System.currentTimeMillis()); put("tool",tool); put("result",result.take(2000)) }
        val next=JSONArray(); next.put(item)
        for(i in 0 until minOf(arr.length(),49)) next.put(arr.getJSONObject(i))
        prefs.edit().putString("history",next.toString()).apply()
    }
internal fun MainActivity.historyTool() {
        clearPage("History Center")
        content.addView(label("History Center",22f,true))
        content.addView(subLabel("Riwayat tool, hasil, dan aktivitas lokal • maksimal 50 hasil",12f))
        val arr=runCatching { JSONArray(prefs.getString("history","[]") ?: "[]") }.getOrElse { JSONArray() }
        if(arr.length()==0) { content.addView(subLabel("Belum ada riwayat.",13f)); return }
        for(i in 0 until arr.length()) {
            val o=arr.getJSONObject(i); val whenText=SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(Date(o.optLong("time")))
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));background=bg(panel2,14,line);setOnClickListener{copyText(o.optString("result"))}}
            card.addView(label("${o.optString("tool")} • $whenText",12f,true)); card.addView(subLabel(o.optString("result"),13f))
            content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
        }
    }
internal fun MainActivity.profilePrefs() = getSharedPreferences("gitls_profile", Context.MODE_PRIVATE)

internal fun MainActivity.showLocalRegistration() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), 0)
        }
        val name = EditText(this).apply {
            hint = "Nama"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val username = EditText(this).apply {
            hint = "Username (opsional)"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        box.addView(name, LinearLayout.LayoutParams(-1, dp(52)))
        box.addView(username, LinearLayout.LayoutParams(-1, dp(52)))
        AlertDialog.Builder(this)
            .setTitle("Buat profil lokal")
            .setMessage("Profil ini disimpan di perangkat. Tidak membuat akun online.")
            .setView(box)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val n = name.text.toString().trim()
                val u = username.text.toString().trim()
                if (n.isEmpty()) {
                    toast("Nama belum diisi")
                    return@setPositiveButton
                }
                profilePrefs().edit()
                    .putString("name", n)
                    .putString("username", u)
                    .putBoolean("registered", true)
                    .apply()
                toast("Profil lokal dibuat")
                enterApp()
            }.show()
    }
internal fun MainActivity.showProfile() {
        closeDrawer()
        clearPage("profile", true)
        content.setPadding(dp(16), dp(8), dp(16), dp(28))
        val prefs = profilePrefs()
        val name = prefs.getString("name", "Pengguna GITLS") ?: "Pengguna GITLS"
        val username = prefs.getString("username", "") ?: ""
        val registered = prefs.getBoolean("registered", false)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(12), 0, dp(18))
        }
        header.addView(TextView(this).apply {
            text = if (registered) name.take(1).uppercase(Locale.getDefault()) else "G"
            gravity = Gravity.CENTER
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(25, 25, 28))
            }
        }, LinearLayout.LayoutParams(dp(76), dp(76)))
        header.addView(label(name, 21f, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0) })
        header.addView(subLabel(if (registered && username.isNotEmpty()) "@$username" else "Guest Account", 12f).apply { gravity = Gravity.CENTER })
        content.addView(header)

        val edit = button(if (registered) "Edit Profil" else "Buat Profil Lokal") {
            showLocalRegistration()
        }
        content.addView(edit)

        sectionTitle("Akun")
        content.addView(settingRow("Status", if (registered) "Profil lokal aktif" else "Guest", "Data profil tersimpan hanya di perangkat."))
        content.addView(settingRow("Username", if (username.isEmpty()) "Belum diatur" else "@$username", "Nama pengguna lokal."))

        sectionTitle("Aktivitas")
        content.addView(settingRow("Tools", "Tersedia", "Semua tool aplikasi tetap dapat digunakan."))
        content.addView(settingRow("Favorit", "Tersimpan lokal", "Daftar favorit mengikuti penyimpanan aplikasi."))
        content.addView(settingRow("Riwayat", "Tersedia", "Riwayat mengikuti tool yang menyediakannya."))

        sectionTitle("Preferensi")
        content.addView(settingRowClickable("Pengaturan", "Tema, animasi, dan opsi aplikasi", "", "cog-outline") { showSettings() })
        content.addView(settingRowClickable("Backup & Restore", "Kelola data aplikasi yang mendukung backup", "", "backup-restore") { showSettings() })

        sectionTitle("Data Profil")
        content.addView(button("Hapus Profil Lokal") {
            AlertDialog.Builder(this).setTitle("Hapus profil lokal?")
                .setMessage("Profil lokal akan dihapus dari perangkat. Tool dan aplikasi tidak ikut dihapus.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus") { _, _ ->
                    profilePrefs().edit().clear().apply()
                    toast("Profil lokal dihapus")
                    showProfile()
                }.show()
        })
    }
internal fun MainActivity.toggleDrawer() {
        if (drawerOpen) closeDrawer() else openDrawer()
    }
internal fun MainActivity.closeDrawer() {
        drawerOverlay?.let { rootFrame.removeView(it) }
        drawerOverlay = null
        drawerOpen = false
    }
internal fun MainActivity.openDrawer() {
        if (drawerOpen || mainContainer.visibility != View.VISIBLE) return
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x66000000)
            isClickable = true
            setOnClickListener { closeDrawer() }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28) + sysTopInset, dp(14), dp(18) + sysBottomInset)
            background = GradientDrawable().apply {
                setColor(if (isDarkTheme) Color.rgb(20, 21, 24) else Color.WHITE)
                cornerRadii = floatArrayOf(0f,0f,dp(22).toFloat(),dp(22).toFloat(),dp(22).toFloat(),dp(22).toFloat(),0f,0f)
            }
            elevation = dp(12).toFloat()
            isClickable = true
        }
        overlay.addView(panel, FrameLayout.LayoutParams(dp(310), -1).apply { gravity = Gravity.START })
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0,0,0,dp(20)) }
        head.addView(TextView(this).apply { text = "G"; gravity = Gravity.CENTER; textSize = 18f; setTextColor(Color.WHITE); background = GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(Color.rgb(30,30,34)) } }, LinearLayout.LayoutParams(dp(48),dp(48)))
        head.addView(LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,0,0); addView(label("GITLS",18f,true)); addView(subLabel("Semua alat dalam satu aplikasi",11f)) }, LinearLayout.LayoutParams(0,-2,1f))
        head.addView(TextView(this).apply { text="×"; textSize=28f; gravity=Gravity.CENTER; setTextColor(textMain); setOnClickListener{closeDrawer()} }, LinearLayout.LayoutParams(dp(42),dp(42)))
        panel.addView(head)

        val items = listOf(
            Triple("Beranda", "home", "home-outline"),
            Triple("Semua Tools", "all", "view-grid"),
            Triple("Favorit", "favorites", "star"),
            Triple("Riwayat", "history", "history"),
            Triple("Profil", "profile", "account-outline"),
            Triple("Pengaturan", "settings", "cog-outline")
        )
        items.forEachIndexed { index, (name, page, icon) ->
            val row = drawerRow(name, icon) {
                closeDrawer()
                when(page) {
                    "home" -> navigateRoot("home") { showHome() }
                    "all" -> navigateRoot("all") { showAllTools() }
                    "favorites" -> navigateRoot("favorites") { showFavorites() }
                    "history" -> showHistoryPage()
                    "profile" -> showProfile()
                    "settings" -> navigateRoot("settings") { showSettings() }
                }
            }
            // Beri ruang antar-item agar drawer tidak terasa padat.
            panel.addView(row, LinearLayout.LayoutParams(-1, dp(58)).apply {
                bottomMargin = if (index == items.lastIndex) dp(14) else dp(10)
            })
        }

        // Spacer fleksibel menjaga kelompok bantuan tetap berada di bagian bawah drawer.
        panel.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))

        // Kelompok sekunder dipisahkan dari navigasi utama.
        panel.addView(drawerRow("Tentang GITLS", "information-outline") { closeDrawer(); showAbout() },
            LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(10) })
        panel.addView(drawerRow("Bantuan", "help-circle-outline") { closeDrawer(); showHelpPage() },
            LinearLayout.LayoutParams(-1, dp(58)))
        rootFrame.addView(overlay, FrameLayout.LayoutParams(-1,-1))
        drawerOverlay = overlay
        drawerOpen = true
    }
internal fun MainActivity.drawerRow(textValue: String, icon: String, action: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            minimumHeight = dp(58)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            background = rippleBg(if (isDarkTheme) panel2 else Color.WHITE, 16, line)
        }
        row.addView(
            MdiIconView(this).apply {
                setIconName(icon)
                setIconSize(22f)
                setTextColor(textMain)
            },
            LinearLayout.LayoutParams(dp(40), dp(58)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        )
        row.addView(
            label(textValue, 15f, false),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = dp(6)
            }
        )
        return row
    }
internal fun MainActivity.showHistoryPage() {
        clearPage("Riwayat", true)
        content.addView(label("Riwayat", 22f, true))
        content.addView(subLabel("Aktivitas terbaru dari tool yang menyimpan riwayat.", 12f))
        content.addView(settingRow("Status", "Lokal", "Riwayat hanya ditampilkan untuk fitur yang memang menyimpannya."))
        content.addView(button("Kembali ke Beranda") { showHome() })
    }
internal fun MainActivity.showHelpPage() {
        clearPage("Bantuan", true)
        content.addView(label("Bantuan", 22f, true))
        content.addView(subLabel("Panduan singkat penggunaan GITLS.", 12f))
        sectionTitle("Navigasi")
        content.addView(settingRow("☰ Menu", "Navigasi utama", "Gunakan menu kiri atas untuk membuka Profil, Pengaturan, Favorit, dan halaman lainnya."))
        content.addView(settingRow("👤 Profil", "Akun lokal", "Kelola profil lokal perangkat tanpa membuat akun online."))
        content.addView(settingRow("🔎 Search", "Cari tools", "Gunakan pencarian dan geser sedikit untuk Snap Search."))
        content.addView(settingRow("+", "Aksi cepat", "Buka daftar tools yang dapat digunakan."))
        sectionTitle("Masalah umum")
        content.addView(settingRow("Icon kosong", "Perbarui aplikasi", "Versi ini memperbaiki beberapa nama glyph yang tidak tersedia."))
        content.addView(settingRow("Keyboard", "Otomatis", "Bottom navigation disembunyikan ketika keyboard terbuka."))
    }
internal fun MainActivity.showAbout() {
        val msg = buildString {
            append("GITLS — suite utilitas native Android.\n\n")
            append("Lebih dari 100 tool offline-first: File, Network, Security, Text & Dev, Calculator, Finance, QR, dan lainnya.\n\n")
            append("• Data keuangan, catatan, dan preferensi disimpan lokal di perangkat.\n")
            append("• Token GitHub dienkripsi dengan Android Keystore.\n")
            append("• Tidak ada akun wajib dan tidak ada pelacakan iklan.\n")
            append("• Beberapa tool keamanan/jaringan bersifat edukatif/sederhana — gunakan dengan bijak.\n\n")
            append("Versi ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
        }
        AlertDialog.Builder(this)
            .setTitle("Tentang GITLS")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .setNeutralButton("Privasi") { _, _ -> showPrivacyInfo() }
            .show()
    }
internal fun MainActivity.showPrivacyInfo() {
        val msg = buildString {
            append("Ringkasan Privasi GITLS\n\n")
            append("• Hampir semua tool berjalan sepenuhnya offline.\n")
            append("• Data Finance, Secure Notes, riwayat, dan pengaturan hanya disimpan di perangkat Anda.\n")
            append("• Token GitHub (jika dipakai) dienkripsi dengan kunci di Android Keystore dan tidak dikirim ke server GITLS.\n")
            append("• Izin lokasi & Wi-Fi hanya digunakan untuk fitur Network / Wi-Fi Info yang Anda buka sendiri.\n")
            append("• Notifikasi & alarm hanya untuk pengingat yang Anda buat.\n")
            append("• Tidak ada analytics pihak ketiga atau iklan di dalam aplikasi.\n\n")
            append("Anda dapat mencabut izin kapan saja lewat Pengaturan sistem perangkat.")
        }
        AlertDialog.Builder(this)
            .setTitle("Privasi & Data")
            .setMessage(msg)
            .setPositiveButton("Mengerti", null)
            .setNeutralButton("Kelola Izin") { _, _ ->
                runCatching {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
            }
            .show()
    }
internal fun MainActivity.sectionTitle(textValue: String): View {
        val v = TextView(this).apply {
            text = textValue
            textSize = 13f
            setTextColor(textMain)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(14), dp(4), dp(8))
        }
        content.addView(v)
        return v
    }
internal fun MainActivity.sectionTitle(textValue: String, actionText: String, onAction: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(14), dp(4), dp(8))
        }
        row.addView(TextView(this).apply {
            text = textValue
            textSize = 13f
            setTextColor(textMain)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = actionText
            textSize = 12f
            setTextColor(textMain)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = bg(panel2, 10, line)
            setOnClickListener { onAction() }
        })
        content.addView(row)
        return row
    }
internal fun MainActivity.openToolWithPress(id: String, view: View) {
        // Debounce rapid repeated taps so a single card cannot queue the same tool
        // multiple times while the previous transition is still starting.
        addPressFeedback(view)
        val now = android.os.SystemClock.uptimeMillis()
        if (id == lastToolOpenId && now - lastToolOpenAt < 350L) return
        lastToolOpenId = id
        lastToolOpenAt = now
        view.postDelayed({
            if (id == lastToolOpenId) openTool(id)
        }, 70L)
    }
internal fun MainActivity.openTool(id: String) {
        BitRuntimeContext.onToolOpened(id, homeToolMap[id] ?: id)
        recordRecentTool(id)
        when (id) {
            "filemanager" -> fileManager(filesDir)
            "recentfiles" -> recentFilesTool()
            "backuprestore" -> backupRestoreTool()
            "workspace" -> workspaceCenterTool()
            "plugincenter" -> pluginCenterTool()
            "customtools" -> toolCustomizationTool()
            "studiocenter" -> studioCenterTool()
            "editor" -> editor(null)
            "reminder" -> reminderTool()
            "zip" -> zipTool()
            "githubzip" -> githubZipTool()
            "wifi" -> wifiInfo()
            "devicecenter" -> deviceSystemCenterTool()
            "json" -> editor(null, "json")
            "hash" -> hashTool()
            "base64" -> simpleTransform("Base64", "Encode", "Decode")
            "url" -> urlTool()
            "regex" -> regexTool()
            "uuid" -> simpleResultTool("UUID Generator") { UUID.randomUUID().toString() }
            "color" -> colorTool()
            "number" -> calculatorHub()
            "history" -> historyTool()
            // UI Color adalah pipet layar global, bukan mode kalkulator.
            "uicolorcalc" -> uiColorPickerTool()
            // Semua kalkulator tetap berada di satu layar. Mode hanya mengganti isi workspace.
            "basiccalc", "scicalc", "percentcalc", "fractioncalc", "ratiocalc",
            "unitcalc", "areacalc", "volumecalc", "speedcalc", "timecalc", "datecalc",
            "loancalc", "fuelcalc", "pivotcalc", "dividercalc", "dcacalc", "pwmcalc",
            "spritecalc", "installcalc", "powercalc", "aspectcalc", "pphcalc" -> calculatorHub(id)
            "financereader" -> financeReaderTool()
            "financedashboard" -> financeDashboardTool()
            "securitycenter" -> securityCenterTool()
            "helpbot" -> helpBotTool()
            "riskcalc" -> riskRewardCalculator()
            "compoundcalc" -> compoundCalculator()
            "margincalc" -> marginTaxCalculator()
            "discountcalc" -> tieredDiscountCalculator()
            "datacalc" -> dataUnitCalculator()
            "pressurecalc" -> pressureCalculator()
            "worktimecalc" -> workTimeCalculator()
            "basecalc" -> baseCalculator()
            "equationcalc" -> equationCalculator()
            "textstat" -> textStatTool()
            "case" -> caseTool()
            "dedupe" -> dedupeTool()
            "compare" -> compareTool()
            "slug" -> slugTool()
            "lorem" -> loremTool()
            "password" -> passwordTool()
            "token" -> tokenTool()
            "jwt" -> jwtTool()
            "hmac" -> hmacTool()
            "totp" -> totpTool()
            "aes" -> aesTool()
            "random" -> randomTool()
            "checksum" -> checksumTool()
            "hex" -> hexTool()
            "base32" -> base32Tool()
            "dns" -> dnsTool()
            "rdns" -> reverseDnsTool()
            "port" -> portTool()
            "publicip" -> publicIpTool()
            "ping" -> pingTool()
            "ipinfo" -> ipInfoTool()
            "ssl" -> sslTool()
            "apk" -> apkInspector()
            "qr" -> qrTool()
            "fileconvert" -> fileConvertTool()
            "system" -> systemInfo()
            "http" -> httpServer()
            "webhostwifi" -> wifiHtmlHostingTool()
            "filehashcompare" -> fileHashCompareTool()
            "markdown" -> markdownViewerTool()
            "sql" -> sqlToolsTool()
            "yaml" -> yamlFormatterTool()
            "toml" -> tomlInspectorTool()
            "cron" -> cronHelperTool()
            "passwordstrength" -> passwordStrengthTool()
            "fileencryption" -> fileEncryptionTool()
            "steganography" -> steganographyTool()
            "passwordanalyzer" -> passwordStrengthAnalyzerTool()
            "breachchecker" -> dataBreachCheckerTool()
            "securenotes" -> secureNotesTool()
            "totpvault" -> totpVaultTool()
            "pgp" -> pgpTool()
            "sshkeygen" -> sshKeyGeneratorTool()
            "certviewer" -> certificateViewerTool()
            "virusscanner" -> virusScannerTool()
            "urlsafety" -> urlSafetyTool()
            "stopwatch" -> stopwatchTool()
            "timer" -> timerTool()
            "imagestudio" -> imageStudioTool()
            "timestamp" -> timestampTool()
            "unicode" -> unicodeTool()
            "urlparser" -> urlParserTool()
            "mime" -> mimeTool()
            "jsonformat" -> editor(null, "json")
            "xmlformat" -> xmlFormatTool()
            "uuidbatch" -> uuidBatchTool()
            "base64file" -> base64FileTool()
            "httpheaders" -> httpHeadersTool()
            "textreplace" -> textReplaceTool()
            "wordfreq" -> wordFrequencyTool()
            "deviceinfo" -> deviceInfoTool()
            "storage" -> storageAnalyzerTool()
            "apps" -> appManagerTool()
            "network" -> networkInfoTool()
            "battery" -> batteryInfoTool()
            "filesearch" -> fileSearchTool()
            "clipboard" -> clipboardManagerTool()
            "esp" -> espTools()
            "espdiscover" -> espAutoDiscovery()
            "ledstudio" -> espLedStudio()
            "espdevice" -> espDeviceManager()
            "espgpio" -> espGpioController()
            "espsensor" -> espSensorDashboard()
            "espwifi" -> espWifiManager()
            "espota" -> espOtaFirmware()
            "esphttp" -> espHttpApiTester()
            "espmqtt" -> espMqttClient()
            "espusb" -> espUsbInfo()
            "espserial" -> espTcpSerialMonitor()
            "iotdashboard","espstudio","visualwiring" -> modularIotDashboard()
            "ocr" -> ocrTool()
            "unitconverter" -> unitConverterProTool()
            "apkanalyzer" -> apkAnalyzerTool()
            "netscanner" -> networkScannerTool()
            "webproject" -> webProjectBuilder()
            "webeditor" -> editor(null)
            "networkstudio" -> networkStudioTool()
            "developerstudio" -> developerStudioTool()
            "filestudio" -> fileStudioTool()
            "imagestudio" -> imageStudioTool()
            "colorstudio" -> colorTool()
            "systemstudio" -> systemStudioTool()
            "financestudio" -> financeStudioTool()
            "utilitystudio" -> utilityStudioTool()
            "whois" -> whoisTool()
            "traceroute" -> tracerouteTool()
            "subnetcalc" -> subnetCalculatorTool()
            "restclient" -> restApiClientTool()
            "websocket" -> webSocketClientTool()
            "networkcenter" -> networkCenterTool()
            "systemcenter" -> systemCenterTool()
            "apkcompare" -> apkCompareTool()
            "duplicatefinder" -> duplicateFinderTool()
            "largefilefinder" -> largeFileFinderTool()
        }
        // Global UI normalization: the toolbar is the single page title.
        // Older tools sometimes still add the same title (and subtitle) into the
        // scroll content, which makes the page look duplicated/overlapped.
        content.post { normalizeToolContentHeader() }
        if (currentPage != "Editor" && !currentPage.startsWith("Editor - ")) {
            content.post { Motion.openWorkspace(content, Motion.characterFor(id)) }
        }
    }
