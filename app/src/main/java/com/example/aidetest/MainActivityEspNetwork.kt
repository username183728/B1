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


internal fun MainActivity.pivotPointCalculator() {
        clearPage("Pivot Point")
        content.addView(label("Pivot Point • Standard",22f,true))
        content.addView(subLabel("Level Support S1-S3 dan Resistance R1-R3 dari High, Low, Close.",12f))
        val h=edit("High"); val l=edit("Low"); val c=edit("Close")
        content.addView(h); content.addView(l); content.addView(c)
        content.addView(button("Hitung Pivot") {
            val high=h.num(); val low=l.num(); val close=c.num()
            if(high==null||low==null||close==null||high<low) output("High/Low tidak valid.") else {
                val p=(high+low+close)/3.0
                val r1=2*p-low; val s1=2*p-high
                val r2=p+(high-low); val s2=p-(high-low)
                val r3=high+2*(p-low); val s3=low-2*(high-p)
                output("Pivot P = ${fmt(p)}\nS1 = ${fmt(s1)}\nS2 = ${fmt(s2)}\nS3 = ${fmt(s3)}\nR1 = ${fmt(r1)}\nR2 = ${fmt(r2)}\nR3 = ${fmt(r3)}")
            }
        })
    }
internal fun MainActivity.voltageDividerCalculator() {
        clearPage("Voltage Divider")
        content.addView(label("Pembagi Tegangan",22f,true))
        content.addView(subLabel("Vout = Vin × R2 / (R1 + R2)",12f))
        val vin=edit("Vin (V)"); val r1=edit("R1 (ohm)"); val r2=edit("R2 (ohm)"); val target=edit("Target Vout (V)")
        listOf(vin,r1,r2,target).forEach{content.addView(it)}
        content.addView(button("Hitung Vout") {
            val v=vin.num(); val a=r1.num(); val b=r2.num()
            if(v==null||a==null||b==null||v<0||a<=0||b<=0) output("Input tidak valid.")
            else output("Vout = ${fmt(v*b/(a+b))} V\nArus divider = ${fmt(v/(a+b)*1000)} mA")
        })
        content.addView(button("Cari R2 untuk Target Vout") {
            val v=vin.num(); val a=r1.num(); val t=target.num()
            if(v==null||a==null||t==null||a<=0||t<=0||t>=v) output("Vin, R1 dan target Vout tidak valid.")
            else output("R2 ≈ ${fmt(t*a/(v-t))} ohm")
        })
        content.addView(button("Cari R1 untuk Target Vout") {
            val v=vin.num(); val b=r2.num(); val t=target.num()
            if(v==null||b==null||t==null||b<=0||t<=0||t>=v) output("Vin, R2 dan target Vout tidak valid.")
            else output("R1 ≈ ${fmt(b*(v/t-1))} ohm")
        })
    }
internal fun MainActivity.dcaCalculator() {
        clearPage("Averaging Down & DCA")
        content.addView(label("Averaging Down & DCA",22f,true))
        content.addView(subLabel("Hitung harga rata-rata dan tambahan modal untuk target rata-rata.",12f))
        val oldPrice=edit("Harga posisi lama"); val oldQty=edit("Jumlah/unit lama"); val newPrice=edit("Harga pembelian baru"); val newQty=edit("Jumlah/unit baru")
        listOf(oldPrice,oldQty,newPrice,newQty).forEach{content.addView(it)}
        content.addView(button("Hitung rata-rata baru") {
            val p1=oldPrice.num(); val q1=oldQty.num(); val p2=newPrice.num(); val q2=newQty.num()
            if(p1==null||q1==null||p2==null||q2==null||q1<=0||q2<0) output("Input tidak valid.")
            else { val avg=(p1*q1+p2*q2)/(q1+q2); output("Total unit = ${fmt(q1+q2)}\nModal total = ${fmt(p1*q1+p2*q2)}\nHarga rata-rata = ${fmt(avg)}") }
        })
        val target=edit("Target harga rata-rata"); content.addView(target)
        content.addView(button("Cari tambahan unit & modal") {
            val p1=oldPrice.num(); val q1=oldQty.num(); val p2=newPrice.num(); val t=target.num()
            if(p1==null||q1==null||p2==null||t==null||q1<=0||p2<=0) output("Input tidak valid.")
            else {
                val denom=t-p2
                if(kotlin.math.abs(denom)<1e-12) output("Target sama dengan harga pembelian baru; jumlah unit teoritis tidak terbatas.")
                else { val q2=(p1-t)*q1/denom; if(q2<0) output("Target tidak dapat dicapai dengan harga pembelian baru ini.") else output("Tambahan unit ≈ ${fmt(q2)}\nTambahan modal ≈ ${fmt(q2*p2)}\nRata-rata target = ${fmt(t)}") }
            }
        })
    }
internal fun MainActivity.pwmCalculator() {
        clearPage("PWM & Duty Cycle")
        content.addView(label("PWM & Duty Cycle",22f,true))
        val supply=edit("Tegangan supply (V)"); val duty=edit("Duty cycle (%)"); val freq=edit("Frekuensi (Hz)")
        listOf(supply,duty,freq).forEach{content.addView(it)}
        content.addView(button("Hitung PWM") {
            val v=supply.num(); val d=duty.num(); val f=freq.num()
            if(v==null||d==null||f==null||d<0||d>100||f<=0) output("Input tidak valid.")
            else { val avg=v*d/100; val periodUs=1_000_000.0/f; output("Tegangan rata-rata ≈ ${fmt(avg)} V\nFrekuensi = ${fmt(f)} Hz\nPeriode ≈ ${fmt(periodUs)} µs\nHIGH time ≈ ${fmt(periodUs*d/100)} µs") }
        })
        val target=edit("Target tegangan rata-rata (V)"); content.addView(target)
        content.addView(button("Hitung Duty dari Target") { val v=supply.num(); val t=target.num(); if(v==null||t==null||v<=0||t<0||t>v) output("Target harus 0 sampai Vin.") else output("Duty cycle ≈ ${fmt(t/v*100)}%") })
    }
internal fun MainActivity.spriteSheetCalculator() {
        clearPage("Sprite Sheet Grid")
        content.addView(label("Sprite Sheet / Grid",22f,true))
        content.addView(subLabel("Hitung ukuran frame dan jumlah baris/kolom secara tepat.",12f))
        val sheetW=edit("Lebar sprite sheet (px)"); val sheetH=edit("Tinggi sprite sheet (px)"); val cols=edit("Jumlah kolom"); val rows=edit("Jumlah baris")
        listOf(sheetW,sheetH,cols,rows).forEach{content.addView(it)}
        content.addView(button("Hitung frame") { val w=sheetW.num();val h=sheetH.num();val c=cols.num();val r=rows.num(); if(w==null||h==null||c==null||r==null||w<=0||h<=0||c<=0||r<=0) output("Input tidak valid.") else output("Frame = ${fmt(w/c)} × ${fmt(h/r)} px\nTotal frame = ${fmt(c*r)}\nGrid = ${fmt(c)} kolom × ${fmt(r)} baris") })
        val frameW=edit("Lebar frame (px)"); val frameH=edit("Tinggi frame (px)"); content.addView(frameW);content.addView(frameH)
        content.addView(button("Hitung grid dari frame") { val w=sheetW.num();val h=sheetH.num();val fw=frameW.num();val fh=frameH.num(); if(w==null||h==null||fw==null||fh==null||fw<=0||fh<=0) output("Input tidak valid.") else output("Kolom = ${fmt(w/fw)}\nBaris = ${fmt(h/fh)}\nTotal frame = ${fmt(w/fw*h/fh)}") })
    }
internal fun MainActivity.installmentComparisonCalculator() {
        clearPage("Flat vs Efektif / Anuitas")
        content.addView(label("Bunga Flat vs Efektif/Anuitas",22f,true))
        val principal=edit("Pokok pinjaman"); val rate=edit("Bunga tahunan (%)"); val months=edit("Tenor (bulan)")
        listOf(principal,rate,months).forEach{content.addView(it)}
        content.addView(button("Bandingkan") { val p=principal.num();val annual=rate.num();val n=months.num(); if(p==null||annual==null||n==null||p<=0||n<=0||annual<0) output("Input tidak valid.") else { val flatInterest=p*(annual/100)/12; val flatPay=p/n+flatInterest; val flatTotal=flatPay*n; val m=annual/100/12; val annPay=if(m==0.0)p/n else p*m*Math.pow(1+m,n)/(Math.pow(1+m,n)-1); val annTotal=annPay*n; output("FLAT\nCicilan/bulan ≈ ${fmt(flatPay)}\nTotal bayar ≈ ${fmt(flatTotal)}\nTotal bunga ≈ ${fmt(flatTotal-p)}\n\nEFEKTIF/ANUITAS\nCicilan bulanan ≈ ${fmt(annPay)}\nTotal bayar ≈ ${fmt(annTotal)}\nTotal bunga ≈ ${fmt(annTotal-p)}") } })
    }
internal fun MainActivity.uiColorConverterCalculator() {
        uiColorPickerTool()
    }
internal fun MainActivity.uiColorPickerTool() {
        clearPage("UI Color")
        content.addView(label("Pipet Warna Layar", 24f, true))
        content.addView(subLabel("Konversi warna HEX/RGB. Pipet tangkapan layar dinonaktifkan demi privasi dan kebijakan platform.", 12f))

        val preview = FrameLayout(this).apply {
            background = bg(Color.rgb(235, 238, 242), 24, Color.rgb(220, 225, 230))
        }
        val swatch = View(this).apply { background = bg(Color.rgb(90, 120, 220), 22) }
        val marker = TextView(this).apply {
            text = "•"
            gravity = Gravity.CENTER
            textSize = 28f
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(90, 120, 220), 30, Color.WHITE)
        }
        preview.addView(swatch, FrameLayout.LayoutParams(dp(92), dp(92), Gravity.CENTER))
        preview.addView(marker, FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP or Gravity.END).apply { topMargin = dp(16); rightMargin = dp(16) })
        content.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { bottomMargin = dp(12) })

        val status = label("Mode aman: tanpa tangkapan layar", 15f, true)
        status.gravity = Gravity.CENTER
        content.addView(status, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })

        val activate = button("INFO: PIPET LAYAR NONAKTIF") { activateColorPicker() }
        activate.background = bg(Color.rgb(25, 25, 27), 16)
        activate.setTextColor(Color.WHITE)
        content.addView(activate, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(10) })

        val hex = label("HEX  —  Belum ada warna", 16f, true)
        val rgb = subLabel("RGB  —  -", 13f)
        val hsl = subLabel("HSL  —  -", 13f)
        val copy = button("SALIN HEX") {
            val value = hex.text.toString().substringAfter("HEX  —  ").trim()
            if (value.startsWith("#")) copyText(value) else toast("Belum ada warna")
        }
        copy.background = bg(panel2, 14, line)
        content.addView(hex)
        content.addView(rgb)
        content.addView(hsl)
        content.addView(copy, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })

        val note = subLabel("Cara kerja: aktifkan pipet → izinkan tangkapan layar → buka aplikasi apa pun → geser lingkaran pipet ke warna yang diinginkan → tekan tombol pipet.", 11f)
        note.setPadding(0, dp(14), 0, 0)
        content.addView(note)

        colorPickerUiUpdater = { color ->
            keepShapeFill(swatch, color)
            marker.setTextColor(Color.WHITE)
            marker.background = bg(color, 30, Color.WHITE)
            val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
            val hsv = FloatArray(3); Color.colorToHSV(color, hsv)
            val hslValue = rgbToHsl(r, g, b)
            val hx = "#%02X%02X%02X".format(Locale.US, r, g, b)
            hex.text = "HEX  —  $hx"
            rgb.text = "RGB  —  $r, $g, $b"
            hsl.text = "HSL  —  ${fmt(hslValue[0])}°, ${fmt(hslValue[1])}%, ${fmt(hslValue[2])}%"
            status.text = "Pipet aktif  •  $hx"
        }
    }
internal fun MainActivity.activateColorPicker() {
        // Screen capture / MediaProjection eyedropper removed for policy & privacy compliance.
        // Users can still convert colors via HEX/RGB input or photo palette tools.
        toast("Pipet layar dinonaktifkan demi privasi. Gunakan input HEX/RGB atau ambil warna dari foto.")
    }
internal fun MainActivity.startColorPickerService() {
        // No-op: ColorPickerService and SYSTEM_ALERT_WINDOW / MediaProjection removed.
        toast("Layanan pipet layar tidak tersedia di versi ini.")
    }
internal fun MainActivity.colorFromHex(raw:String):String { var h=raw.trim().removePrefix("#"); if(h.length==3) h=h.map{"$it$it"}.joinToString(""); if(h.length!=6&&h.length!=8) error("HEX"); val a=if(h.length==8) h.substring(0,2).toInt(16) else 255; val off=if(h.length==8)2 else 0; val r=h.substring(off,off+2).toInt(16); val g=h.substring(off+2,off+4).toInt(16); val b=h.substring(off+4,off+6).toInt(16); val hsv=FloatArray(3); Color.colorToHSV(Color.rgb(r,g,b),hsv); val hsl=rgbToHsl(r,g,b); return "HEX = #${h.uppercase(Locale.US)}\nARGB = $a,$r,$g,$b\nRGB = $r,$g,$b\nHSL = ${fmt(hsl[0])}°, ${fmt(hsl[1])}%, ${fmt(hsl[2])}%\nHSV = ${fmt(hsv[0].toDouble())}°, ${fmt((hsv[1]*100).toDouble())}%, ${fmt((hsv[2]*100).toDouble())}%" }
internal fun MainActivity.colorFromRgb(raw:String):String { val p=raw.split(",").map{it.trim().toInt()}; if(p.size!=3||p.any{it !in 0..255}) error("RGB"); return colorFromHex("#%02X%02X%02X".format(Locale.US,p[0],p[1],p[2])) }
internal fun MainActivity.colorFromArgb(raw:String):String { val p=raw.split(",").map{it.trim().toInt()}; if(p.size!=4||p.any{it !in 0..255}) error("ARGB"); return colorFromHex("#%02X%02X%02X%02X".format(Locale.US,p[0],p[1],p[2],p[3])) }
internal fun MainActivity.rgbToHsl(r:Int,g:Int,b:Int):DoubleArray { val rr=r/255.0; val gg=g/255.0; val bb=b/255.0; val max=maxOf(rr,gg,bb); val min=minOf(rr,gg,bb); var h=0.0; val l=(max+min)/2; var sat=0.0; val d=max-min; if(d!=0.0){sat=if(l>0.5)d/(2-max-min) else d/(max+min); h=when(max){rr->(gg-bb)/d+(if(gg<bb)6 else 0);gg->(bb-rr)/d+2;else->(rr-gg)/d+4};h/=6}; return doubleArrayOf(h*360,sat*100,l*100) }
internal fun MainActivity.powerConsumptionCalculator() {
        clearPage("Konsumsi Listrik")
        content.addView(label("Konsumsi Listrik & Biaya",22f,true))
        val watts=edit("Daya perangkat (W) total"); val hours=edit("Jam pemakaian per hari"); val days=edit("Hari per bulan"); val tariff=edit("Tarif listrik per kWh")
        listOf(watts,hours,days,tariff).forEach{content.addView(it)}
        content.addView(button("Hitung") { val w=watts.num();val h=hours.num();val d=days.num();val t=tariff.num(); if(w==null||h==null||d==null||t==null||w<0||h<0||d<0||t<0) output("Input tidak valid.") else { val kwhDay=w*h/1000; val kwhMonth=kwhDay*d; output("Energi/hari = ${fmt(kwhDay)} kWh\nEnergi/bulan = ${fmt(kwhMonth)} kWh\nBiaya/hari ≈ ${fmt(kwhDay*t)}\nBiaya/bulan ≈ ${fmt(kwhMonth*t)}") } })
    }
internal fun MainActivity.aspectRatioCalculator() {
        clearPage("Aspect Ratio")
        content.addView(label("Aspect Ratio & Skala Resolusi",22f,true))
        val w=edit("Lebar (px)"); val h=edit("Tinggi (px)"); content.addView(w);content.addView(h)
        content.addView(button("Hitung rasio") { val a=w.num();val b=h.num(); if(a==null||b==null||a<=0||b<=0) output("Input tidak valid.") else { val ai=kotlin.math.round(a).toLong();val bi=kotlin.math.round(b).toLong();val g=gcd(kotlin.math.abs(ai),kotlin.math.abs(bi)); output("Aspect ratio ≈ ${fmt(a/b)}\nRasio sederhana = ${ai/g}:${bi/g}") } })
        val ratioW=edit("Rasio lebar, contoh 16"); val ratioH=edit("Rasio tinggi, contoh 9"); val known=edit("Ukuran yang diketahui (px)"); content.addView(ratioW);content.addView(ratioH);content.addView(known)
        val mode=Spinner(this); mode.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Diketahui lebar → cari tinggi","Diketahui tinggi → cari lebar")); content.addView(mode,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(7)})
        content.addView(button("Hitung ukuran proporsional") { val rw=ratioW.num();val rh=ratioH.num();val k=known.num(); if(rw==null||rh==null||k==null||rw<=0||rh<=0||k<=0) output("Input tidak valid.") else if(mode.selectedItemPosition==0) output("Resolusi = ${fmt(k)} × ${fmt(k*rh/rw)} px") else output("Resolusi = ${fmt(k*rw/rh)} × ${fmt(k)} px") })
    }
internal fun MainActivity.ppnPphCalculator() {
        clearPage("PPN & PPh Final")
        content.addView(label("PPN & PPh Final",22f,true))
        content.addView(subLabel("Masukkan tarif pajak sendiri agar sesuai aturan/kontrak yang berlaku.",12f))
        val gross=edit("Nilai bruto / DPP"); val ppn=edit("PPN (%)"); val pph=edit("PPh Final (%)")
        listOf(gross,ppn,pph).forEach{content.addView(it)}
        content.addView(button("Hitung invoice") { val g=gross.num();val pv=ppn.num();val ph=pph.num(); if(g==null||pv==null||ph==null||g<0||pv<0||ph<0) output("Input tidak valid.") else { val ppnVal=g*pv/100; val pphVal=g*ph/100; val invoice=g+ppnVal; val nett=g+ppnVal-pphVal; output("DPP = ${fmt(g)}\nPPN = ${fmt(ppnVal)}\nTotal invoice = ${fmt(invoice)}\nPPh Final = ${fmt(pphVal)}\nNett setelah PPh = ${fmt(nett)}") } })
    }
internal fun MainActivity.espBaseUrlField(defaultUrl: String = "http://192.168.4.1"): EditText {
        val e = edit("ESP base URL, contoh http://192.168.4.1")
        e.setText(prefs.getString("esp_base_url", defaultUrl) ?: defaultUrl)
        return e
    }
internal fun MainActivity.normalizeEspUrl(raw: String): String {
        var v = raw.trim()
        if (v.isBlank()) v = "http://192.168.4.1"
        if (!v.startsWith("http://") && !v.startsWith("https://")) v = "http://$v"
        return v.trimEnd('/')
    }
internal fun MainActivity.httpRequest(method: String, url: String, body: String? = null, contentType: String = "application/json", timeout: Int = 7000): Pair<Int, String> {
        val parsed = URL(url)
        require(parsed.protocol.equals("http", true) || parsed.protocol.equals("https", true)) { "URL harus menggunakan http:// atau https://" }
        require(parsed.host.isNotBlank()) { "Host URL kosong" }
        require(parsed.userInfo == null) { "URL dengan userinfo tidak didukung" }
        require(timeout in 1000..30000) { "Timeout di luar batas aman" }
        val conn = (parsed.openConnection() as HttpURLConnection).apply {
            requestMethod = method.uppercase(Locale.US)
            connectTimeout = timeout
            readTimeout = timeout
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json, text/plain, */*")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
        if (body != null) conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }?.take(12000).orEmpty()
        conn.disconnect()
        return code to text
    }
internal fun MainActivity.espAutoDiscovery() {
        clearPage("ESP Auto Discovery")
        addToolHeader("ESP Auto Discovery", "Temukan perangkat ESP yang mengiklankan layanan mDNS di jaringan lokal.", "⌁")
        content.addView(toolSection("DISCOVERY", "Hasil perangkat akan muncul di bawah."))
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(result)
        val nsd = getSystemService(Context.NSD_SERVICE) as NsdManager
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) { runOnUiThread { result.addView(subLabel("Memindai $serviceType …", 12f)) } }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        runOnUiThread {
                            val host = info.host?.hostAddress ?: "?"
                            result.addView(label("${info.serviceName} • $host:${info.port}", 13f, true))
                        }
                    }
                })
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { runOnUiThread { result.addView(subLabel("Discovery gagal: $errorCode", 12f)) } }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        nsdDiscoveryManager = nsd
        nsdDiscoveryListener = listener
        content.addView(button("Mulai scan HTTP") { runCatching { nsd.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, listener) }.onFailure { toast("NSD tidak tersedia: ${it.message}") } })
        content.addView(subLabel("ESP yang menjalankan mDNS/Bonjour HTTP dapat muncul otomatis. Perangkat tanpa mDNS tetap dapat dimasukkan melalui Device Manager.", 11f))
    }
internal fun MainActivity.stopEspDiscovery() {
        val manager = nsdDiscoveryManager
        val listener = nsdDiscoveryListener
        if (manager != null && listener != null) runCatching { manager.stopServiceDiscovery(listener) }
        nsdDiscoveryManager = null
        nsdDiscoveryListener = null
    }
internal fun MainActivity.espDeviceManager() {
        clearPage("ESP Device Manager")
        addToolHeader("ESP Device Manager", "Simpan endpoint ESP dan cek status perangkat dari satu panel.", "ESP")
        content.addView(toolSection("DEVICE"))
        val base = espBaseUrlField(); content.addView(base)
        val info = label("Belum dicek", 13f); info.setPadding(dp(10), dp(12), dp(10), dp(12)); info.background = bg(panel2, 14, line); content.addView(info)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(button("🔎 CHECK STATUS") {
            val url = normalizeEspUrl(base.text.toString()) + "/status"
            prefs.edit().putString("esp_base_url", normalizeEspUrl(base.text.toString())).apply()
            info.text = "Menghubungkan…"
            toolThread {
                val r = runCatching { httpRequest("GET", url) }.getOrElse { -1 to (it.message ?: "error") }
                runOnUiThread {
                    info.text = if (r.first in 200..299) formatEspJson(r.second) else "HTTP ${r.first}\n${r.second.ifBlank { "Tidak ada response" }}"
                }
            }
        }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(4) })
        row.addView(button("COPY URL") { copyText(normalizeEspUrl(base.text.toString())) }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(4) })
        content.addView(row)
        content.addView(button("GET /health") { espSimpleGet(base.text.toString(), "/health") })
        content.addView(button("GET /info") { espSimpleGet(base.text.toString(), "/info") })
        content.addView(subLabel("ESP dapat mengembalikan JSON seperti {\"chip\":\"ESP32\",\"ip\":\"192.168.4.1\",\"rssi\":-48,\"uptime\":1234,\"free_heap\":200000,\"firmware\":\"1.0.0\"}.", 11f))
    }
internal fun MainActivity.formatEspJson(raw: String): String = runCatching {
        JSONObject(raw).toString(2)
    }.getOrElse { raw.ifBlank { "ESP terhubung, response kosong." } }

internal fun MainActivity.espSimpleGet(baseRaw: String, path: String) {
        val url = normalizeEspUrl(baseRaw) + path
        toolThread {
            val r = runCatching { httpRequest("GET", url) }.getOrElse { -1 to (it.message ?: "error") }
            runOnUiThread { output("$url\nHTTP ${r.first}\n${formatEspJson(r.second)}") }
        }
    }
internal fun MainActivity.espGpioController() {
    val activity = this
        clearPage("ESP GPIO Controller")
        addToolHeader("ESP GPIO Controller", "Kontrol pin ESP dengan panel HIGH/LOW yang lebih jelas.", "GPIO")
        content.addView(toolSection("CONNECTION"))
        val base = espBaseUrlField(); content.addView(base)
        val pinEdit = edit("GPIO, contoh 2"); pinEdit.setText("2"); content.addView(pinEdit)
        content.addView(toolSection("MODE & PWM"))
        val mode = Spinner(this).apply { adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, arrayOf("OUTPUT", "INPUT", "PWM")) }
        content.addView(mode, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
        val pwm = edit("PWM duty 0-255 (untuk PWM)"); pwm.setText("128"); content.addView(pwm)
        val state = toolStatus("Siap"); content.addView(state)
        content.addView(toolSection("ACTIONS"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(button("HIGH / ON") { sendGpio(base.text.toString(), pinEdit.text.toString(), "HIGH", mode.selectedItem.toString(), pwm.text.toString(), state) }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(4) })
        row.addView(button("LOW / OFF") { sendGpio(base.text.toString(), pinEdit.text.toString(), "LOW", mode.selectedItem.toString(), pwm.text.toString(), state) }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(4) })
        content.addView(row)
        content.addView(button("READ GPIO STATUS") { espSimpleGet(base.text.toString(), "/gpio") })
        content.addView(subLabel("Request JSON: {gpio:2, mode:\"OUTPUT\", state:\"HIGH\", pwm:128}", 11f))
    }
internal fun MainActivity.sendGpio(baseRaw: String, pinRaw: String, stateRaw: String, mode: String, pwmRaw: String, stateView: TextView) {
        val pin = pinRaw.toIntOrNull()
        if (pin == null || pin !in 0..39) { toast("GPIO tidak valid"); return }
        val json = JSONObject().apply { put("gpio", pin); put("mode", mode); put("state", stateRaw); put("pwm", pwmRaw.toIntOrNull()?.coerceIn(0,255) ?: 0) }
        stateView.text = "Mengirim GPIO $pin…"
        toolThread {
            val r = runCatching { httpRequest("POST", normalizeEspUrl(baseRaw) + "/gpio", json.toString()) }.getOrElse { -1 to (it.message ?: "error") }
            runOnUiThread { stateView.text = "HTTP ${r.first}\n${r.second.ifBlank { "OK" }}" }
        }
    }
internal fun MainActivity.espSensorDashboard() {
        clearPage("ESP Sensor Dashboard")
        addToolHeader("ESP Sensor Dashboard", "Pantau data sensor ESP secara berkala tanpa mengubah tema aplikasi.", "◌")
        content.addView(toolSection("DEVICE"))
        val base = espBaseUrlField(); content.addView(base)
        val box = label("Belum ada data", 14f); box.setPadding(dp(14), dp(16), dp(14), dp(16)); box.background = bg(panel2, 16, line); content.addView(box)
        content.addView(toolSection("POLLING"))
        val interval = edit("Interval polling (ms)"); interval.setText("1000"); content.addView(interval)
        content.addView(button("▶ START / REFRESH") {
            val delay = (interval.text.toString().toLongOrNull() ?: 1000L).coerceIn(250L, 60000L)
            startEspSensorPolling(base, box, delay)
        })
        content.addView(button("■ STOP") { stopEspSensorPolling() })
        content.addView(subLabel("Gunakan minimal 250 ms agar ESP tidak dibanjiri request.", 11f))
    }
internal fun MainActivity.startEspSensorPolling(base: EditText, box: TextView, delay: Long) {
        stopEspSensorPolling()
        espSensorPolling = true
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (!espSensorPolling || currentPage != "ESP Sensor Dashboard") return
                val url = normalizeEspUrl(base.text.toString()) + "/sensors"
                toolThread {
                    val res = runCatching { httpRequest("GET", url, timeout = 5000) }.getOrElse { -1 to (it.message ?: "error") }
                    runOnUiThread {
                        if (espSensorPolling) box.text = if (res.first in 200..299) formatEspJson(res.second) else "HTTP ${res.first}\n${res.second}"
                    }
                }
                handler.postDelayed(this, delay)
            }
        }
        espSensorHandler = handler
        espSensorRunnable = runnable
        handler.post(runnable)
    }
internal fun MainActivity.stopEspSensorPolling() {
        espSensorPolling = false
        val handler = espSensorHandler
        val runnable = espSensorRunnable
        if (handler != null && runnable != null) handler.removeCallbacks(runnable)
        espSensorHandler = null
        espSensorRunnable = null
    }
internal fun MainActivity.espWifiManager() {
        clearPage("ESP Wi-Fi Manager")
        addToolHeader("ESP Wi-Fi Manager", "Kelola konfigurasi Wi-Fi ESP dengan status koneksi yang jelas.", "WiFi")
        content.addView(toolSection("CONNECTION"))
        val base = espBaseUrlField(); content.addView(base)
        val ssid = edit("SSID Wi-Fi"); content.addView(ssid)
        val pass = edit("Password Wi-Fi"); pass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; content.addView(pass)
        val result = label("Siap", 13f); content.addView(result)
        content.addView(button("📶 SEND WI-FI CONFIG") {
            if (ssid.text.toString().isBlank()) { toast("SSID kosong"); return@button }
            val json = JSONObject().apply { put("ssid", ssid.text.toString()); put("password", pass.text.toString()) }
            result.text = "Mengirim…"
            toolThread {
                val r = runCatching { httpRequest("POST", normalizeEspUrl(base.text.toString()) + "/wifi/config", json.toString()) }.getOrElse { -1 to (it.message ?: "error") }
                runOnUiThread { result.text = "HTTP ${r.first}\n${r.second.ifBlank { "OK" }}" }
            }
        })
        content.addView(button("GET CURRENT STATUS") { espSimpleGet(base.text.toString(), "/wifi/status") })
    }
internal fun MainActivity.espOtaFirmware() {
        clearPage("ESP OTA Firmware")
        content.addView(label("ESP OTA Firmware", 22f, true))
        content.addView(subLabel("Pilih firmware .bin lalu upload sebagai raw application/octet-stream ke endpoint OTA. Default /update.", 12f))
        val base = espBaseUrlField(); content.addView(base)
        val endpoint = edit("OTA endpoint path, contoh /update"); endpoint.setText("/update"); content.addView(endpoint)
        val status = label("Belum ada firmware", 13f); content.addView(status)
        content.addView(button("📦 PILIH .BIN & UPLOAD") {
            pendingOtaEndpoint = normalizeEspUrl(base.text.toString()) + (endpoint.text.toString().trim().let { if (it.startsWith("/")) it else "/$it" })
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "application/octet-stream"; addCategory(Intent.CATEGORY_OPENABLE) }, 1030)
            status.text = "Menunggu file…"
        })
        content.addView(button("GET /version") { espSimpleGet(base.text.toString(), "/version") })
        content.addView(subLabel("Implementasi ini memakai POST raw. Endpoint ESP harus menerima body binary dan melakukan validasi firmware sebelum reboot.", 11f))
    }
internal fun MainActivity.uploadOtaUri(uri: Uri, endpoint: String) {
        toolThread {
            val result = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 10000; readTimeout = 30000; doOutput = true
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setRequestProperty("X-Firmware-Name", queryName(uri) ?: "firmware.bin")
                }
                contentResolver.openInputStream(uri)?.use { input -> conn.outputStream.use { out -> input.copyTo(out, 8192) } } ?: error("File tidak bisa dibaca")
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }?.take(2000).orEmpty()
                conn.disconnect()
                if (code !in 200..299) error("HTTP $code ${body.ifBlank { "OTA ditolak" }}")
                "OTA berhasil • HTTP $code\n$body"
            }.getOrElse { "OTA gagal: ${it.message}" }
            runOnUiThread { output(result) }
        }
    }
internal fun MainActivity.espHttpApiTester() {
    val activity = this
        clearPage("ESP HTTP/API Tester")
        addToolHeader("ESP HTTP / API Tester", "Uji endpoint ESP dengan method, body, dan response dalam satu workspace.", "API")
        content.addView(toolSection("REQUEST"))
        val method = Spinner(this).apply { adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, arrayOf("GET", "POST", "PUT", "DELETE", "PATCH")) }
        content.addView(method, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
        val url = edit("http://192.168.4.1/status"); content.addView(url)
        val body = edit("JSON body (opsional)", true); content.addView(body)
        content.addView(button("SEND REQUEST") {
            val u = url.text.toString().trim(); if (u.isBlank()) { toast("URL kosong"); return@button }
            toolThread {
                val start = System.currentTimeMillis()
                val r = runCatching { httpRequest(method.selectedItem.toString(), u, body.text.toString().takeIf { it.isNotBlank() }) }.getOrElse { -1 to (it.message ?: "error") }
                val ms = System.currentTimeMillis() - start
                runOnUiThread { output("${method.selectedItem} $u\nHTTP ${r.first}\nResponse time: ${ms} ms\n\n${r.second}") }
            }
        })
    }
internal fun MainActivity.espMqttClient() {
        clearPage("ESP MQTT Client")
        addToolHeader("ESP MQTT Client", "Publish atau subscribe pesan MQTT dengan panel koneksi yang ringkas.", "MQ")
        content.addView(toolSection("BROKER"))
        val host = edit("Broker host, contoh 192.168.1.10"); content.addView(host)
        val port = edit("Port"); port.setText("1883"); content.addView(port)
        val clientId = edit("Client ID"); clientId.setText("MyTools-" + (System.currentTimeMillis() % 100000)); content.addView(clientId)
        val topic = edit("Topic, contoh esp32/led"); content.addView(topic)
        val message = edit("Message"); content.addView(message)
        val result = label("Disconnected", 13f); content.addView(result)
        content.addView(button("PUBLISH") {
            val h = host.text.toString().trim(); val p = port.text.toString().toIntOrNull() ?: 1883
            toolThread {
                val r = runCatching { mqttPublish(h, p, clientId.text.toString(), topic.text.toString(), message.text.toString()) }.getOrElse { it.message ?: "MQTT error" }
                runOnUiThread { result.text = r }
            }
        })
        content.addView(button("SUBSCRIBE (5 detik)") {
            val h = host.text.toString().trim(); val p = port.text.toString().toIntOrNull() ?: 1883
            toolThread {
                val r = runCatching { mqttSubscribe(h, p, clientId.text.toString(), topic.text.toString()) }.getOrElse { it.message ?: "MQTT error" }
                runOnUiThread { output(r) }
            }
        })
        content.addView(subLabel("Broker tanpa TLS/auth didukung pada mode dasar ini. Jangan mengirim kredensial sensitif melalui jaringan terbuka.", 11f))
    }
internal fun MainActivity.mqttEncodeRemainingLength(length: Int): ByteArray {
        var x = length
        val out = ByteArrayOutputStream()
        do { var digit = x % 128; x /= 128; if (x > 0) digit = digit or 128; out.write(digit) } while (x > 0)
        return out.toByteArray()
    }
internal fun MainActivity.mqttUtf8(s: String): ByteArray {
        val b = s.toByteArray(StandardCharsets.UTF_8); val out = ByteArrayOutputStream(); out.write((b.size shr 8) and 255); out.write(b.size and 255); out.write(b); return out.toByteArray()
    }
internal fun MainActivity.mqttPacket(typeFlags: Int, payload: ByteArray): ByteArray = ByteArrayOutputStream().apply { write(typeFlags); write(mqttEncodeRemainingLength(payload.size)); write(payload) }.toByteArray()

internal fun MainActivity.mqttConnect(clientId: String): ByteArray {
        val payload = ByteArrayOutputStream(); payload.write(mqttUtf8("MQTT")); payload.write(4); payload.write(2); payload.write(0); payload.write(30); payload.write(mqttUtf8(clientId)); return mqttPacket(0x10, payload.toByteArray())
    }
internal fun MainActivity.mqttPublish(topic: String, message: String): ByteArray = mqttPacket(0x30, mqttUtf8(topic) + message.toByteArray(StandardCharsets.UTF_8))

internal fun MainActivity.mqttSubscribe(topic: String, packetId: Int = 1): ByteArray {
        val p = ByteArrayOutputStream(); p.write((packetId shr 8) and 255); p.write(packetId and 255); p.write(mqttUtf8(topic)); p.write(0); return mqttPacket(0x82, p.toByteArray())
    }
internal fun MainActivity.readFully(input: InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val n = input.read(buffer, offset, buffer.size - offset)
            if (n < 0) return false
            offset += n
        }
        return true
    }
internal fun MainActivity.mqttPublish(host: String, port: Int, clientId: String, topic: String, message: String): String {
        require(host.isNotBlank() && topic.isNotBlank()) { "Host dan topic wajib diisi" }
        require(port in 1..65535) { "Port MQTT tidak valid" }
        Socket(host, port).use { socket ->
            socket.soTimeout = 5000
            val out = socket.getOutputStream(); val input = socket.getInputStream()
            out.write(mqttConnect(clientId)); out.flush()
            val connAck = ByteArray(4); require(readFully(input, connAck)) { "Broker menutup koneksi sebelum CONNACK" }
            require((connAck[0].toInt() and 0xFF) == 0x20 && (connAck[3].toInt() and 0xFF) == 0) { "CONNACK ditolak" }
            out.write(mqttPublish(topic, message)); out.flush()
            return "PUBLISH berhasil • $topic"
        }
    }
internal fun MainActivity.mqttReadPacket(input: InputStream): ByteArray? {
        val first = input.read()
        if (first < 0) return null
        var multiplier = 1
        var remaining = 0
        var count = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            remaining += (b and 127) * multiplier
            count++
            if ((b and 128) == 0) break
            require(count < 4) { "MQTT remaining length tidak valid" }
            multiplier *= 128
        }
        val body = ByteArray(remaining)
        require(readFully(input, body)) { "Paket MQTT terpotong" }
        return byteArrayOf(first.toByte()) + body
    }
internal fun MainActivity.mqttPublishInfo(packet: ByteArray): String? {
        if (packet.isEmpty() || ((packet[0].toInt() ushr 4) != 3)) return null
        if (packet.size < 3) return null
        val topicLen = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
        if (topicLen < 0 || packet.size < 3 + topicLen) return null
        val topic = String(packet, 3, topicLen, StandardCharsets.UTF_8)
        var pos = 3 + topicLen
        val qos = (packet[0].toInt() ushr 1) and 3
        if (qos > 0) {
            if (packet.size < pos + 2) return null
            pos += 2
        }
        val payload = if (pos < packet.size) String(packet, pos, packet.size - pos, StandardCharsets.UTF_8) else ""
        return "PUBLISH\nTopic: $topic\nPayload: $payload"
    }
internal fun MainActivity.mqttSubscribe(host: String, port: Int, clientId: String, topic: String): String {
        require(host.isNotBlank() && topic.isNotBlank()) { "Host dan topic wajib diisi" }
        require(port in 1..65535) { "Port MQTT tidak valid" }
        Socket(host, port).use { socket ->
            socket.soTimeout = 1000
            val out = socket.getOutputStream(); val input = socket.getInputStream()
            out.write(mqttConnect(clientId)); out.flush()
            val connAck = mqttReadPacket(input) ?: error("Broker menutup koneksi sebelum CONNACK")
            require(connAck.size >= 4 && (connAck[0].toInt() and 0xF0) == 0x20 && (connAck.last().toInt() and 0xFF) == 0) { "CONNACK ditolak" }
            val packetId = 1
            out.write(mqttSubscribe(topic, packetId)); out.flush()
            val started = System.currentTimeMillis()
            var subAckOk = false
            val messages = StringBuilder("Menunggu SUBACK untuk: $topic\n")
            while (System.currentTimeMillis() - started < 5000) {
                try {
                    val packet = mqttReadPacket(input) ?: break
                    if (packet.isEmpty()) continue
                    when (packet[0].toInt() and 0xF0) {
                        0x90 -> {
                            if (packet.size >= 5) {
                                val id = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
                                val rc = packet[3].toInt() and 0xFF
                                require(id == packetId) { "SUBACK packet ID tidak cocok" }
                                require(rc == 0) { "SUBSCRIBE ditolak, return code=$rc" }
                                subAckOk = true
                                messages.append("SUBACK OK\n")
                            }
                        }
                        0x30 -> mqttPublishInfo(packet)?.let { messages.append(it).append("\n") }
                    }
                } catch (_: SocketTimeoutException) { }
            }
            require(subAckOk) { "SUBACK tidak diterima dalam 5 detik" }
            return messages.toString()
        }
    }
internal fun MainActivity.espUsbInfo() {
        clearPage("ESP USB / OTG Info")
        content.addView(label("ESP USB / OTG Info", 22f, true))
        content.addView(subLabel("Membaca perangkat USB yang terdeteksi Android. Untuk serial USB, Android harus mengizinkan akses perangkat terlebih dahulu.", 12f))
        val usb = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) content.addView(subLabel("Tidak ada perangkat USB yang terdeteksi. Sambungkan ESP melalui OTG.", 13f))
        devices.forEach { d ->
            val info = "${d.deviceName}\nVID: ${d.vendorId} • PID: ${d.productId}\nInterfaces: ${d.interfaceCount}\nClass: ${d.deviceClass}"
            content.addView(label(info, 13f).apply { setPadding(dp(12), dp(12), dp(12), dp(12)); background = bg(panel2, 14, line) })
        }
        content.addView(button("REFRESH") { espUsbInfo() })
    }
internal fun MainActivity.espTcpSerialMonitor() {
        clearPage("ESP TCP Serial Monitor")
        content.addView(label("ESP TCP Serial Monitor", 22f, true))
        content.addView(subLabel("Monitor serial yang diekspos ESP sebagai TCP socket, misalnya firmware bridge di port 23/3333. Ini bukan driver USB serial.", 12f))
        val host = edit("ESP IP / host"); content.addView(host)
        val port = edit("TCP port"); port.setText("3333"); content.addView(port)
        val command = edit("Kirim command (opsional)"); content.addView(command)
        val log = edit("Log", true); log.isEnabled = false; content.addView(log)
        content.addView(button("CONNECT / READ 5 DETIK") {
            val h = host.text.toString().trim(); val p = port.text.toString().toIntOrNull() ?: 3333
            toolThread {
                val r = runCatching {
                    Socket(h, p).use { socket ->
                        socket.soTimeout = 1000
                        if (command.text.toString().isNotBlank()) socket.getOutputStream().apply { write((command.text.toString() + "\n").toByteArray(StandardCharsets.UTF_8)); flush() }
                        val start = System.currentTimeMillis(); val bytes = ByteArrayOutputStream(); val buf = ByteArray(1024)
                        while (System.currentTimeMillis() - start < 5000) { try { val n = socket.getInputStream().read(buf); if (n > 0) bytes.write(buf, 0, n) } catch (_: SocketTimeoutException) { /* read timeout window */ } }
                        bytes.toString("UTF-8")
                    }
                }.getOrElse { "Serial TCP error: ${it.message}" }
                runOnUiThread { log.isEnabled = true; log.setText(r.ifBlank { "Tidak ada data selama 5 detik." }); log.isEnabled = false }
            }
        })
    }
internal fun MainActivity.wifiInfo() {
        clearPage("Wi-Fi Info")
        val wifiLocationGranted=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val wifiNearbyGranted=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.NEARBY_WIFI_DEVICES)==PackageManager.PERMISSION_GRANTED
        if(!wifiLocationGranted || !wifiNearbyGranted){
            modernInfoCard("Izin diperlukan","Izinkan Lokasi dan Nearby Wi-Fi agar SSID, sinyal, dan scan jaringan dapat dibaca.")
            modernActionRow("Izinkan akses Wi-Fi","map-marker"){requestWifiPermissions()}
            return
        }
        modernUiRunnable?.let{modernUiHandler.removeCallbacks(it)}
        wifiHistory.clear()
        val hero=modernWifiHero()
        val chart=MiniLineChart(this).apply{lineColor=accentTeal();fillColor=Color.argb(42,0,137,123);minValue=-90f;maxValue=-50f}
        modernChartCard("Grafik Kekuatan Sinyal",chart)
        val details=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(8));background=bg(Color.WHITE,16,line)}
        details.addView(label("Informasi Jaringan",13f,true));details.addView(subLabel("Alamat lokal dan jalur koneksi perangkat",10f))
        val rows=listOf("IP Address","Gateway","Subnet","DNS","IPv6").map{infoLine(it,"-")};rows.forEach{details.addView(it)}
        content.addView(details,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
        val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        actions.addView(modernActionButton("Test Koneksi","wifi-check"){testWifiConnection()},weightLp(1f,0))
        actions.addView(modernActionButton("Scan Wi-Fi","access-point"){scanNearbyWifi()},weightLp(1f,6))
        content.addView(actions)
        fun update(){
            if(currentPage!="Wi-Fi Info")return
            val wm=applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION") val i=wm.connectionInfo
            val cm=getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val n=cm.activeNetwork
            val caps=if(n!=null)cm.getNetworkCapabilities(n)else null
            val wifiConnected=caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)==true
            val lp=if(wifiConnected && n!=null) cm.getLinkProperties(n) else null
            val rawSsid=i.ssid?.trim().orEmpty()
            val ssid=when {
                rawSsid.isBlank() || rawSsid=="<unknown ssid>" -> if(wifiConnected) "Wi-Fi terhubung" else "<unknown ssid>"
                else -> rawSsid.trim('\"')
            }
            val rssi=i.rssi
            val freq=i.frequency
            val band=when{freq in 2400..2500->"2.4 GHz";freq in 4900..5900->"5 GHz";freq>=5925->"6 GHz";else->"-"}
            val ch=wifiChannel(freq)
            val ip=lp?.linkAddresses?.firstOrNull{it.address is Inet4Address}?.address?.hostAddress ?: "-"
            val gateway=lp?.routes?.firstOrNull{it.isDefaultRoute && it.gateway?.isAnyLocalAddress == false}
                ?.gateway?.hostAddress ?: "-"
            val subnetPrefix=lp?.linkAddresses?.firstOrNull{it.address is Inet4Address}?.prefixLength ?: -1
            val subnet=ipv4MaskFromPrefix(subnetPrefix)
            val dns=lp?.dnsServers?.joinToString(", "){it.hostAddress ?: "-"}.orEmpty().ifBlank{"-"}
            val ipv6=lp?.linkAddresses?.firstOrNull{it.address is Inet6Address && !it.address.isLinkLocalAddress}
                ?.address?.hostAddress ?: "-"
            val vals=listOf(ip,gateway,subnet,dns,ipv6)
            rows.forEachIndexed{k,row->(row.getChildAt(1) as? TextView)?.text=vals[k]}
            hero.ssid.text=ssid
            hero.status.text=if(wifiConnected)"●  Terhubung" else "●  Tidak terhubung"
            hero.rssi.text=if(rssi in -100..-1)"$rssi dBm" else "- dBm"
            hero.status.setTextColor(if(wifiConnected)accentTeal() else Color.rgb(211,47,47))
            hero.rssi.setTextColor(if(rssi>=-60)accentTeal() else if(rssi>=-75)Color.rgb(245,166,35) else Color.rgb(211,47,47))
            val linkSpeed=if(i.linkSpeed>=0)"${i.linkSpeed} Mbps" else "-"
            setWifiMetric(hero.linkBox,"Link speed",linkSpeed,"")
            setWifiMetric(hero.freqBox,"Frekuensi",if(freq>0)"$freq MHz" else "-",band)
            setWifiMetric(hero.channelBox,"Channel",if(ch>0)ch.toString() else "-",band)
            if(wifiConnected && rssi in -100..-1){
                wifiHistory.add(rssi.coerceIn(-90,-50))
                if(wifiHistory.size>40)wifiHistory.removeAt(0)
            }
            chart.values=wifiHistory.map{it.toFloat()}
            chart.invalidate()
        }
        val r=object:Runnable{override fun run(){update();modernUiHandler.postDelayed(this,1500)}}
        modernUiRunnable=r
        r.run()
    }
