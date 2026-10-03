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


internal fun MainActivity.normalizeToolContentHeader() {
        // The toolbar is the single source of truth for the current page title.
        // A number of older/newer screens still add their own title at the top of
        // the scroll content, producing the exact duplicated-heading effect seen
        // on pages such as History Center. Remove only a leading duplicate so
        // real content/section headings further down are never touched.
        if (content.childCount == 0) return

        val toolbarTitle = title.text?.toString()?.trim().orEmpty()
        if (toolbarTitle.isEmpty()) return

        val first = content.getChildAt(0) as? TextView
        if (first != null) {
            val firstText = first.text?.toString()?.trim().orEmpty()
            if (firstText.equals(toolbarTitle, ignoreCase = true)) {
                content.removeViewAt(0)
                val next = content.getChildAt(0) as? TextView
                if (next != null && next.text?.isNotBlank() == true && next.currentTextColor == textMuted) {
                    content.removeViewAt(0)
                }
            }
        }
    }
internal fun MainActivity.webSocketClientTool() {
        clearPage("WebSocket Client")
        toolWorkspace("WebSocket Client", "Hubungkan endpoint WebSocket, kirim pesan, terima frame, dan pantau log.", "connection")
        toolWorkspaceSection("CONNECTION", "Gunakan ws:// atau wss:// lalu kontrol koneksi dari bawah.")
        val url = edit("ws://echo.websocket.events")
        val message = edit("Pesan")
        val log = edit("Log")
        log.isEnabled = false; log.minLines = 8; log.setTextIsSelectable(true)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val connect = button("Connect") {}; val send = button("Send") {}; val receive = button("Receive") {}; val close = button("Close") {}
        row.addView(connect, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin=dp(3) })
        row.addView(send, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin=dp(2); rightMargin=dp(2) })
        row.addView(receive, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin=dp(2); rightMargin=dp(2) })
        row.addView(close, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin=dp(2) })
        content.addView(url); content.addView(message); content.addView(row); content.addView(log)
        fun appendLog(t:String) { log.append((if(log.text.isNotEmpty()) "\n" else "") + t) }
        connect.setOnClickListener {
            val raw=url.text.toString().trim(); if(!raw.startsWith("ws://") && !raw.startsWith("wss://")){toast("Gunakan ws:// atau wss://");return@setOnClickListener}
            connect.isEnabled=false
            toolThread {
                val r=runCatching{ openWebSocket(raw) }.getOrElse{"ERROR: ${it.message}"}
                runOnUiThread{appendLog(r); connect.isEnabled=true}
            }
        }
        send.setOnClickListener {
            val msg=message.text.toString(); if(msg.isBlank()){toast("Pesan kosong");return@setOnClickListener}
            toolThread {val r=runCatching{writeWsText(msg); "TX: $msg"}.getOrElse{"ERROR: ${it.message}"};runOnUiThread{appendLog(r)}}
        }
        receive.setOnClickListener { toolThread { val r=runCatching{readWsText()}.getOrElse{"ERROR: ${it.message}"}; runOnUiThread{appendLog(if(r.startsWith("ERROR")) r else "RX: $r")} } }
        close.setOnClickListener { toolThread { runCatching{closeWebSocket()}; runOnUiThread{appendLog("Closed")} } }
    }
internal fun MainActivity.openWebSocket(raw:String):String {
        closeWebSocket()
        val u=URI(raw); val secure=u.scheme.equals("wss",true); val port=if(u.port>0)u.port else if(secure)443 else 80
        val s:Socket = if(secure) javax.net.ssl.SSLSocketFactory.getDefault().createSocket() else Socket()
        s.connect(InetSocketAddress(u.host,port),8000); s.soTimeout=12000
        val out=s.getOutputStream(); val input=s.getInputStream()
        val key=Base64.getEncoder().encodeToString(ByteArray(16).also{SecureRandom().nextBytes(it)})
        val path=(if(u.rawPath.isNullOrBlank()) "/" else u.rawPath)+(u.rawQuery?.let{"?$it"} ?: "")
        out.write(("GET $path HTTP/1.1\r\nHost: ${u.host}:$port\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: $key\r\nSec-WebSocket-Version: 13\r\n\r\n").toByteArray(StandardCharsets.US_ASCII)); out.flush()
        val header=readHttpHeader(input); if(!header.startsWith("HTTP/1.1 101") && !header.startsWith("HTTP/1.0 101")) throw IOException("Handshake gagal: ${header.lines().firstOrNull()}")
        wsSocket=s; wsInput=input; wsOutput=out
        return "Connected: $raw"
    }
internal fun MainActivity.readHttpHeader(input:InputStream):String { val b=ByteArrayOutputStream(); var state=0; while(true){val c=input.read();if(c<0)break;b.write(c);state=if(state==0&&c==13)1 else if(state==1&&c==10)2 else if(state==2&&c==13)3 else if(state==3&&c==10)4 else 0;if(state==4)break;if(b.size()>16000)throw IOException("Header terlalu besar")};return b.toString("ISO-8859-1") }
internal fun MainActivity.writeWsText(text:String){ val out=wsOutput ?: throw IOException("Belum terhubung"); val data=text.toByteArray(StandardCharsets.UTF_8); val mask=ByteArray(4).also{SecureRandom().nextBytes(it)}; val first=0x81; out.write(first); when { data.size<126 -> out.write(0x80 or data.size); data.size<=65535 -> {out.write(0x80 or 126);out.write(data.size shr 8);out.write(data.size and 255)} else -> throw IOException("Pesan terlalu besar") }; out.write(mask); for(i in data.indices) out.write(data[i].toInt() xor mask[i%4].toInt()); out.flush() }
internal fun MainActivity.readWsText():String{
        val input=wsInput ?: throw IOException("Belum terhubung")
        val h1=input.read(); val h2=input.read(); if(h1<0||h2<0)throw IOException("Koneksi ditutup")
        val opcode=h1 and 0x0f; var len=(h2 and 0x7f).toLong(); val masked=(h2 and 0x80)!=0
        if(len==126L){len=((input.read() shl 8) or input.read()).toLong()} else if(len==127L){len=0;repeat(8){len=(len shl 8) or input.read().toLong()}}
        if(len>1024*1024)throw IOException("Frame terlalu besar")
        val mask=if(masked)ByteArray(4).also{readFullyWs(input,it)} else null
        val data=ByteArray(len.toInt());readFullyWs(input,data);if(mask!=null)for(i in data.indices)data[i]=(data[i].toInt() xor mask[i%4].toInt()).toByte()
        return when(opcode){1->String(data,StandardCharsets.UTF_8);8->"[CLOSE]";9->"[PING]";10->"[PONG]";else->"[opcode=$opcode, ${data.size} bytes]"}
    }
internal fun MainActivity.readFullyWs(input: InputStream, b: ByteArray) { var p = 0; while (p < b.size) { val n = input.read(b, p, b.size - p); if (n < 0) throw EOFException(); p += n } }
internal fun MainActivity.closeWebSocket(){runCatching{wsSocket?.close()};wsSocket=null;wsInput=null;wsOutput=null}
internal fun MainActivity.systemCenterTool() {
        clearPage("System Center")
        val am=getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem=ActivityManager.MemoryInfo().also{am.getMemoryInfo(it)}
        val stat=StatFs(Environment.getDataDirectory().path)
        val ramPct=((mem.totalMem-mem.availMem).toDouble()/mem.totalMem*100).roundToInt().coerceIn(0,100)
        val storagePct=((stat.totalBytes-stat.availableBytes).toDouble()/stat.totalBytes*100).roundToInt().coerceIn(0,100)
        modernProgressCard("CPU", cpuLoadPercent(), "Penggunaan & suhu prosesor", "${Runtime.getRuntime().availableProcessors()} core")
        modernProgressCard("RAM", ramPct, "Penggunaan memori", "${bytesText(mem.totalMem-mem.availMem)} / ${bytesText(mem.totalMem)}")
        modernProgressCard("Storage", storagePct, "Penyimpanan internal", "${bytesText(stat.totalBytes-stat.availableBytes)} / ${bytesText(stat.totalBytes)}")
        val processes=am.runningAppProcesses?.size ?: 0
        modernProgressCard("Layanan Sistem", processes.coerceAtMost(100), "Layanan & proses aktif", "$processes aktif")
        modernActionRow("Pengaturan Cepat", "flash") { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
internal fun MainActivity.formatDuration(ms:Long):String { var s=ms/1000; val d=s/86400;s%=86400;val h=s/3600;s%=3600;val m=s/60;s%=60;return "${d}d ${h}h ${m}m ${s}s" }
internal fun MainActivity.apkCompareTool(){
        clearPage("APK Compare"); content.addView(label("APK Compare",22f,true)); content.addView(subLabel("Bandingkan metadata dan isi dua APK tanpa menginstalnya.",12f))
        content.addView(button("Pilih APK A") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/vnd.android.package-archive";addCategory(Intent.CATEGORY_OPENABLE)},1301) })
        content.addView(button("Pilih APK B") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/vnd.android.package-archive";addCategory(Intent.CATEGORY_OPENABLE)},1302) })
    }
internal fun MainActivity.compareApks(a:Uri,b:Uri){toolThread {val r=runCatching{
        val fa=uriToCacheFile(a,"apk_a.apk");val fb=uriToCacheFile(b,"apk_b.apk");val pa=packageArchiveInfo(fa);val pb=packageArchiveInfo(fb)
        val sa=fa.length();val sb=fb.length();val ha=sha256(fa);val hb=sha256(fb)
        val ea=zipSummary(fa);val eb=zipSummary(fb)
        "APK A\nPackage: ${pa.first}\nVersion: ${pa.second}\nSize: ${bytesText(sa)}\nSHA-256: $ha\nZIP entries: ${ea.first}\n\nAPK B\nPackage: ${pb.first}\nVersion: ${pb.second}\nSize: ${bytesText(sb)}\nSHA-256: $hb\nZIP entries: ${eb.first}\n\nMetadata package sama: ${pa.first==pb.first}\nVersion sama: ${pa.second==pb.second}\nHash sama: ${ha.equals(hb,true)}\nUkuran beda: ${bytesText(kotlin.math.abs(sa-sb))}\nEntry beda: ${kotlin.math.abs(ea.first-eb.first)}"
    }.getOrElse{"APK Compare gagal: ${it.message}"};runOnUiThread{clearPage("APK Compare");output(r)}}}
internal fun MainActivity.uriToCacheFile(uri:Uri,name:String):File{val f=File(cacheDir,name);contentResolver.openInputStream(uri)?.use{input->FileOutputStream(f).use{input.copyTo(it)}}?:throw IOException("File tidak dapat dibaca");return f}
internal fun MainActivity.packageArchiveInfo(f:File):Pair<String,String>{val flags=if(Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else 0;val p=packageManager.getPackageArchiveInfo(f.absolutePath,flags)?:throw IOException("APK tidak valid");return p.packageName to (if(Build.VERSION.SDK_INT>=28)p.longVersionCode.toString() else p.versionCode.toString())}
internal fun MainActivity.sha256(f:File):String{val md=MessageDigest.getInstance("SHA-256");FileInputStream(f).use{inp->val buf=ByteArray(8192);while(true){val n=inp.read(buf);if(n<0)break;md.update(buf,0,n)}};return md.digest().joinToString(""){String.format("%02x",it)} }
internal fun MainActivity.zipSummary(f:File):Pair<Int,Long>{var c=0;var total=0L;ZipInputStream(BufferedInputStream(FileInputStream(f))).use{z->while(true){val e=z.nextEntry?:break;c++;if(!e.isDirectory)total+=e.size.coerceAtLeast(0)}};return c to total}
internal fun MainActivity.scanRoots():List<File> = listOfNotNull(filesDir, getExternalFilesDir(null), File(Environment.getExternalStorageDirectory().path,"Download")).distinctBy{it.absolutePath}.filter{it.exists()}

internal fun MainActivity.scanFiles(dir:File,out:MutableList<File>,limit:Int){if(out.size>=limit)return;val list=runCatching{dir.listFiles()}.getOrNull()?:return;for(f in list){if(out.size>=limit)return;if(f.isFile)out.add(f) else if(f.isDirectory)scanFiles(f,out,limit)}}
internal fun MainActivity.infoCard(titleText:String,bodyText:String):View{val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));background=bg(panel2,14,line)};c.addView(label(titleText,13f,true));c.addView(subLabel(bodyText,11f));c.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)};return c}
