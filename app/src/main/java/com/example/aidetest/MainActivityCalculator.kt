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


internal fun MainActivity.calcDisplay(hint: String = "0"): EditText = EditText(this).apply {
        setTextColor(textMain)
        setHintTextColor(textMuted)
        textSize = 28f
        gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        setSingleLine(true)
        setPadding(dp(14), dp(6), dp(14), dp(6))
        background = bg(panel2, 16, line)
        this.hint = hint
        inputType = InputType.TYPE_CLASS_TEXT
        layoutParams = LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(10) }
    }

internal fun MainActivity.calcButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(textMain)
        background = bg(panel2, 12, line)
        setOnClickListener { onClick() }
        setStateListAnimator(null)
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(58)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
    }

internal fun MainActivity.calculatorTool(scientific: Boolean) {
        // Saat ditampilkan di dalam hub, toolbar + selector sudah menjadi konteks
        // kalkulator. Jangan ulangi judul/deskripsi di area isi.
        if (!embeddedCalculatorRender) {
            clearPage(if (scientific) "Kalkulator Ilmiah" else "Kalkulator Dasar")
            val calcTitle = if (scientific) "Kalkulator Ilmiah" else "Kalkulator Dasar"
            content.setPadding(dp(8), dp(4), dp(8), dp(10))
            addToolHeader(calcTitle, if (scientific) "Perhitungan ilmiah dengan keypad responsif." else "Perhitungan cepat dengan keypad yang nyaman di layar sentuh.", "calculator")
            content.setPadding(0, 0, 0, dp(10))
        } else {
            content.removeAllViews()
            content.setPadding(0, dp(2), 0, dp(14))
        }

        val display = calcDisplay()
        display.layoutParams = LinearLayout.LayoutParams(-1, dp(118)).apply {
            setMargins(dp(10), dp(4), dp(10), dp(8))
        }
        display.textSize = if (scientific) 30f else 36f
        display.setPadding(dp(18), dp(10), dp(18), dp(10))
        content.addView(display)

        val grid = GridLayout(this).apply {
            columnCount = if (scientific) 5 else 4
            useDefaultMargins = false
            setPadding(dp(8), 0, dp(8), 0)
        }

        // Basic mode follows the reference calculator layout:
        // AC, +/-, %, ÷
        // 7, 8, 9, ×
        // 4, 5, 6, −
        // 1, 2, 3, =
        // 0, ., DEL, C
        //
        // Most importantly, "=" is a real button and is wired to evaluateExpression().
        val keys = if (scientific) {
            listOf(
                "sin","cos","tan","log","ln",
                "√","x²","xʸ","(",")",
                "7","8","9","÷","DEL",
                "4","5","6","×","C",
                "1","2","3","−","=",
                "0",".","%","+","π"
            )
        } else {
            listOf(
                "AC","±","%","÷",
                "7","8","9","×",
                "4","5","6","−",
                "1","2","3","=",
                "0",".","DEL","C"
            )
        }

        keys.forEach { key ->
            val keyButton = calcButton(key) {
                when (key) {
                    "C", "AC" -> display.setText("")

                    "DEL" -> {
                        if (display.text.isNotEmpty()) {
                            display.setText(display.text.dropLast(1))
                            display.setSelection(display.text.length)
                        }
                    }

                    "±" -> {
                        val current = display.text.toString()
                        if (current.isBlank()) {
                            display.setText("-")
                        } else if (current.startsWith("-")) {
                            display.setText(current.substring(1))
                        } else {
                            display.setText("-$current")
                        }
                        display.setSelection(display.text.length)
                    }

                    "=" -> {
                        val result = runCatching {
                            evaluateExpression(display.text.toString(), scientific)
                        }.getOrElse {
                            "Error: ${it.message ?: "input"}"
                        }
                        display.setText(result)
                        display.setSelection(display.text.length)
                    }

                    "sin","cos","tan","log","ln","x²" -> display.append(key + "(")
                    "√" -> display.append("sqrt(")
                    "xʸ" -> display.append("^")
                    "×" -> display.append("*")
                    "÷" -> display.append("/")
                    "−" -> display.append("-")
                    "π" -> display.append("pi")
                    else -> display.append(key)
                }
            }

            // Operator keys harus selalu terbaca. Sebelumnya teks operator
            // dibuat terang tetapi background tetap terang sehingga tombol
            // terlihat kosong pada perangkat tertentu.
            if (!scientific && (key == "÷" || key == "×" || key == "−" || key == "%")) {
                keyButton.setTextColor(Color.WHITE)
                keyButton.background = bg(Color.rgb(65, 65, 70), 14)
            } else if (!scientific && key == "=") {
                keyButton.setTextColor(Color.BLACK)
                keyButton.background = bg(Color.rgb(245, 245, 247), 14)
            }

            grid.addView(keyButton)
        }

        content.addView(grid, LinearLayout.LayoutParams(-1, if (embeddedCalculatorRender) -2 else 0).apply {
            if (!embeddedCalculatorRender) weight = 1f
        })

        if (scientific) {
            content.addView(
                subLabel(
                    "Mendukung + − × ÷ %, kurung, pangkat, √, sin, cos, tan, log, ln, π.",
                    11f
                ).apply {
                    setPadding(dp(14), dp(6), dp(14), 0)
                }
            )
        }
    }
internal fun MainActivity.evaluateExpression(expr: String, scientific: Boolean): String {
        if (expr.isBlank()) return "0"
        val v = ExprParser(expr, scientific).parse()
        if (!v.isFinite()) error("Hasil tidak valid")
        return if (kotlin.math.abs(v - v.toLong()) < 1e-10) v.toLong().toString() else String.format(Locale.US, "%.10f", v).trimEnd('0').trimEnd('.')
    }
internal fun MainActivity.twoFields(titleText: String, aHint: String, bHint: String, actionText: String, calc: (Double,Double)->String) {
        clearPage(titleText)
        content.addView(label(titleText,22f,true))
        val a=edit(aHint); val b=edit(bHint); content.addView(a); content.addView(b)
        content.addView(button(actionText) {
            val x=runCatching{a.text.toString().replace(",",".").toDouble()}.getOrNull()
            val y=runCatching{b.text.toString().replace(",",".").toDouble()}.getOrNull()
            output(if(x==null||y==null) "Masukkan angka yang valid." else calc(x,y))
        })
    }
internal fun MainActivity.percentCalculator() {
        clearPage("Persentase")
        content.addView(label("Kalkulator Persentase",22f,true))
        val a=edit("Nilai"); val p=edit("Persen (%)"); content.addView(a); content.addView(p)
        content.addView(button("Hitung X% dari nilai") { val x=a.num(); val y=p.num(); output(if(x==null||y==null) "Input tidak valid" else "${fmt(x*y/100)}") })
        content.addView(button("Berapa % X dari Y") { val x=a.num(); val y=p.num(); output(if(x==null||y==null||y==0.0) "Input tidak valid" else "${fmt(x/y*100)}%") })
        content.addView(button("Tambah X% ke nilai") { val x=a.num(); val y=p.num(); output(if(x==null||y==null) "Input tidak valid" else "${fmt(x*(1+y/100))}") })
        content.addView(button("Kurangi X% dari nilai") { val x=a.num(); val y=p.num(); output(if(x==null||y==null) "Input tidak valid" else "${fmt(x*(1-y/100))}") })
    }
internal fun MainActivity.fractionCalculator() {
        clearPage("Pecahan")
        content.addView(label("Operasi Pecahan",22f,true))
        val a=edit("Pecahan A, contoh 3/4"); val b=edit("Pecahan B, contoh 1/2"); content.addView(a); content.addView(b)
        listOf("+","−","×","÷").forEach { op -> content.addView(button("A $op B") { output(fractionOp(a.text.toString(), b.text.toString(), op)) }) }
    }
internal fun MainActivity.fractionOp(a:String,b:String,op:String):String { return runCatching { val x=frac(a); val y=frac(b); val n=when(op){"+"->x.first*y.second+y.first*x.second;"−"->x.first*y.second-y.first*x.second;"×"->x.first*y.first;else->x.first*y.second}; val d=when(op){"+","−"->x.second*y.second;"×"->x.second*y.second;else->{if(y.first==0L) error("Pembagi 0");x.second*y.first}}; val g=gcd(kotlin.math.abs(n),kotlin.math.abs(d)); "${n/g}/${d/g} = ${fmt(n.toDouble()/d)}" }.getOrElse{"Format harus seperti 3/4"} }
internal fun MainActivity.frac(s:String):Pair<Long,Long>{ val p=s.trim().split("/"); if(p.size!=2) error("pecahan"); val n=p[0].trim().toLong(); val d=p[1].trim().toLong(); if(d==0L) error("0"); return if(d<0) -n to -d else n to d }
internal fun MainActivity.gcd(a0:Long,b0:Long):Long { var a=a0; var b=b0; while(b!=0L){val t=a%b;a=b;b=t};return if(a==0L)1L else a }
internal fun MainActivity.ratioCalculator() { twoFields("Rasio & Proporsi","A","B","Sederhanakan rasio") { a,b -> val scale=1000000.0; val ai=kotlin.math.round(a*scale).toLong(); val bi=kotlin.math.round(b*scale).toLong(); val g=gcd(kotlin.math.abs(ai),kotlin.math.abs(bi)); "${ai/g} : ${bi/g}" } }
internal fun MainActivity.unitCalculator() {
        clearPage("Konverter Satuan")
        content.addView(label("Konverter Satuan",22f,true))
        val input=edit("Nilai"); content.addView(input)
        val from=Spinner(this); val to=Spinner(this)
        val units=arrayOf("meter","kilometer","centimeter","milimeter","inch","feet","yard","mile","gram","kilogram","pound","celsius","fahrenheit","kelvin","reamur","mps","kmh","mph","pascal","kpa","bar","psi")
        listOf(from,to).forEach { it.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units); content.addView(it, LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(7)}) }
        content.addView(button("Konversi") { val v=input.num(); output(if(v==null)"Input tidak valid" else "${fmt(convertUnit(v,from.selectedItem.toString(),to.selectedItem.toString()))} ${to.selectedItem}") })
    }
internal fun MainActivity.convertUnit(v:Double,from:String,to:String):Double {
        val temps=setOf("celsius","fahrenheit","kelvin","reamur")
        if(from in temps || to in temps){
            val c=when(from){"celsius"->v;"fahrenheit"->(v-32)*5/9;"kelvin"->v-273.15;"reamur"->v*5/4;else->v}
            return when(to){"celsius"->c;"fahrenheit"->c*9/5+32;"kelvin"->c+273.15;"reamur"->c*4/5;else->error("Temperatur") }
        }
        val speedBase=mapOf("mps" to 1.0,"kmh" to 1.0/3.6,"mph" to 0.44704)
        if(from in speedBase || to in speedBase){ return v*speedBase.getValue(from)/speedBase.getValue(to) }
        val pressureBase=mapOf("pascal" to 1.0,"kpa" to 1000.0,"bar" to 100000.0,"psi" to 6894.757293)
        if(from in pressureBase || to in pressureBase){ return v*pressureBase.getValue(from)/pressureBase.getValue(to) }
        val factors=mapOf("meter" to 1.0,"kilometer" to 1000.0,"centimeter" to .01,"milimeter" to .001,"inch" to .0254,"feet" to .3048,"yard" to .9144,"mile" to 1609.344,"gram" to .001,"kilogram" to 1.0,"pound" to .45359237)
        if(from !in factors || to !in factors) error("Satuan tidak sejenis")
        return v*factors.getValue(from)/factors.getValue(to)
    }
internal fun MainActivity.riskRewardCalculator() {
        clearPage("Risk-Reward & Position Sizing")
        content.addView(label("Risk-Reward & Position Sizing",22f,true))
        val capital=edit("Total modal"); val risk=edit("Risiko (%) contoh 1-2"); val entry=edit("Harga entry"); val stop=edit("Stop Loss"); val target=edit("Target harga (opsional)"); val lot=edit("Ukuran 1 lot (opsional, default 1)")
        listOf(capital,risk,entry,stop,target,lot).forEach{content.addView(it)}
        content.addView(button("Hitung posisi") {
            val c=capital.num();val r=risk.num();val e=entry.num();val sl=stop.num();val t=target.num();val ls=lot.num()?:1.0
            if(c==null||r==null||e==null||sl==null||r<=0||e==sl||ls<=0) output("Input tidak valid.") else {
                val riskMoney=c*r/100; val riskUnit=kotlin.math.abs(e-sl); val qty=riskMoney/riskUnit; val lots=qty/ls
                val rr=if(t==null) null else kotlin.math.abs(t-e)/riskUnit
                output("Modal risiko: ${fmt(riskMoney)}\nRisiko/unit: ${fmt(riskUnit)}\nUkuran posisi: ${fmt(qty)} unit\nLot: ${fmt(lots)}${if(rr!=null) "\nRisk-Reward: 1 : ${fmt(rr)}" else ""}")
            }
        })
    }
internal fun MainActivity.compoundCalculator() {
        clearPage("Compound & Target Tabungan")
        content.addView(label("Compound Interest & Target Tabungan",22f,true))
        val initial=edit("Modal awal");val contribution=edit("Setoran berkala");val rate=edit("Bunga/return tahunan (%)");val periods=edit("Jumlah periode (bulan)")
        listOf(initial,contribution,rate,periods).forEach{content.addView(it)}
        val freq=Spinner(this);freq.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Bulanan","Mingguan"));content.addView(freq)
        content.addView(button("Proyeksikan") {
            val p=initial.num();val add=contribution.num();val annual=rate.num();val months=periods.num()
            if(p==null||add==null||annual==null||months==null||months<0) output("Input tidak valid.") else {
                val n=if(freq.selectedItemPosition==0) months.toInt() else kotlin.math.round(months*52.0/12.0).toInt(); val ratePer=if(freq.selectedItemPosition==0) annual/100/12 else annual/100/52
                val fv=if(ratePer==0.0) p+add*n else p*Math.pow(1.0+ratePer,n.toDouble())+add*((Math.pow(1.0+ratePer,n.toDouble())-1.0)/ratePer)
                output("Periode: $n\nProyeksi akhir: ${fmt(fv)}\nTotal setoran: ${fmt(p+add*n)}\nPertumbuhan: ${fmt(fv-(p+add*n))}")
            }
        })
        val target=edit("Target nominal (opsional)"); content.addView(target)
        content.addView(button("Hitung setoran bulanan ke target") {
            val tar=target.num();val p=initial.num();val annual=rate.num();val m=periods.num()
            if(tar==null||p==null||annual==null||m==null||m<=0) output("Isi target, modal awal, return tahunan, dan periode.") else {
                val rr=annual/100/12; val n=m.toInt(); val need=if(rr==0.0)(tar-p)/n else (tar-p*Math.pow(1.0+rr,n.toDouble()))*rr/(Math.pow(1.0+rr,n.toDouble())-1.0); output("Setoran bulanan yang diperlukan: ${fmt(kotlin.math.max(0.0,need))}")
            }
        })
    }
internal fun MainActivity.marginTaxCalculator() {
        clearPage("Margin & PPN/Pajak")
        content.addView(label("Harga Jual • Margin • Pajak",22f,true))
        val cogs=edit("COGS / modal barang");val margin=edit("Target margin (%)");val tax=edit("PPN / pajak (%)")
        listOf(cogs,margin,tax).forEach{content.addView(it)}
        content.addView(button("Hitung harga jual") {
            val c=cogs.num();val m=margin.num();val t=tax.num()?:0.0
            if(c==null||m==null||m<0||m>=100||t<0) output("Input tidak valid. Margin harus 0-99.99%.") else {
                val before=c/(1-m/100); val taxMoney=before*t/100; output("Harga sebelum pajak: ${fmt(before)}\nPajak: ${fmt(taxMoney)}\nHarga akhir: ${fmt(before+taxMoney)}\nLaba kotor: ${fmt(before-c)}")
            }
        })
    }
internal fun MainActivity.tieredDiscountCalculator() {
        clearPage("Diskon Bertingkat")
        content.addView(label("Diskon Bertingkat",22f,true))
        val price=edit("Harga awal");val d1=edit("Diskon 1 (%)");val d2=edit("Diskon 2 (%)");val d3=edit("Diskon 3 (%) opsional");listOf(price,d1,d2,d3).forEach{content.addView(it)}
        content.addView(button("Hitung harga akhir") {
            val p=price.num();val a=d1.num();val b=d2.num();val c=d3.num()?:0.0
            if(p==null||a==null||b==null||a<0||b<0||c<0||a>100||b>100||c>100) output("Input diskon tidak valid.") else { val end=p*(1-a/100)*(1-b/100)*(1-c/100); output("Harga akhir: ${fmt(end)}\nTotal diskon efektif: ${fmt((1-end/p)*100)}%\nHemat: ${fmt(p-end)}") }
        })
    }
internal fun MainActivity.dataUnitCalculator() {
        clearPage("Ukuran Data Digital")
        content.addView(label("Byte • KB • MB • GB • TB",22f,true))
        val input=edit("Nilai");content.addView(input);val from=Spinner(this);val to=Spinner(this);val units=arrayOf("Byte","KB","MB","GB","TB");from.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,units);to.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,units);content.addView(from);content.addView(to)
        content.addView(button("Konversi") { val v=input.num(); output(if(v==null)"Input tidak valid" else "${fmt(v*Math.pow(1024.0,from.selectedItemPosition-to.selectedItemPosition.toDouble()))} ${to.selectedItem}") })
    }
internal fun MainActivity.pressureCalculator() {
        clearPage("Konverter Tekanan")
        content.addView(label("Konverter Tekanan",22f,true))
        val input=edit("Nilai"); content.addView(input)
        val units=arrayOf("Pa","kPa","bar","psi")
        val from=Spinner(this); val to=Spinner(this)
        from.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,units); to.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,units)
        content.addView(from); content.addView(to)
        content.addView(button("Konversi") {
            val v=input.num(); output(if(v==null) "Input tidak valid" else {
                val base=v*when(from.selectedItemPosition){0->1.0;1->1000.0;2->100000.0;else->6894.757293}
                val result=base/when(to.selectedItemPosition){0->1.0;1->1000.0;2->100000.0;else->6894.757293}
                "${fmt(result)} ${to.selectedItem}"
            })
        })
    }
internal fun MainActivity.workTimeCalculator() {
        clearPage("Jam Kerja")
        content.addView(label("Durasi Jam Kerja",22f,true))
        val start=edit("Mulai HH:mm");val end=edit("Selesai HH:mm");val breakMin=edit("Istirahat (menit)",false);listOf(start,end,breakMin).forEach{content.addView(it)}
        content.addView(button("Hitung durasi") {
            output(runCatching { val f=SimpleDateFormat("HH:mm",Locale.US).apply{isLenient=false}; val s=f.parse(start.text.toString())?.time ?: error("mulai"); var e=f.parse(end.text.toString())?.time ?: error("selesai"); if(e<s)e+=86400000; val br=breakMin.num()?:0.0; val mins=((e-s)/60000.0-br).coerceAtLeast(0.0); "Durasi kerja: ${fmt(mins/60)} jam\n${mins.toLong()} menit" }.getOrElse{"Format waktu harus HH:mm"})
        })
    }
internal fun MainActivity.areaCalculator() {
        clearPage("Luas & Keliling")
        content.addView(label("Luas & Keliling",22f,true))
        val shape=Spinner(this); val shapes=arrayOf("Persegi","Persegi panjang","Segitiga","Lingkaran")
        shape.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,shapes); content.addView(shape)
        val a=edit("Sisi / panjang"); val b=edit("Lebar / tinggi (jika perlu)"); content.addView(a); content.addView(b)
        content.addView(button("Hitung") { val x=a.num(); val y=b.num(); if(x==null) output("Input tidak valid") else when(shape.selectedItemPosition){0->output("Luas=${fmt(x*x)} • Keliling=${fmt(4*x)}");1->if(y==null)output("Masukkan lebar")else output("Luas=${fmt(x*y)} • Keliling=${fmt(2*(x+y))}");2->if(y==null)output("Masukkan tinggi")else output("Luas=${fmt(.5*x*y)}");3->output("Luas=${fmt(Math.PI*x*x)} • Keliling=${fmt(2*Math.PI*x)}") } })
    }
internal fun MainActivity.volumeCalculator() {
        clearPage("Volume")
        content.addView(label("Kalkulator Volume",22f,true))
        val shape = Spinner(this)
        shape.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Kubus","Balok","Tabung","Bola"))
        content.addView(shape)
        val a = edit("Ukuran / radius")
        val b = edit("Lebar / tinggi")
        val c = edit("Panjang / tinggi")
        content.addView(a)
        content.addView(b)
        content.addView(c)
        content.addView(button("Hitung Volume") {
            val x = a.num()
            val y = b.num()
            val z = c.num()
            val result = when (shape.selectedItemPosition) {
                0 -> if (x == null) "Input tidak valid" else fmt(x * x * x)
                1 -> if (x == null || y == null || z == null) "Butuh 3 ukuran" else fmt(x * y * z)
                2 -> if (x == null || y == null) "Butuh radius + tinggi" else fmt(Math.PI * x * x * y)
                else -> if (x == null) "Input tidak valid" else fmt(4.0 / 3.0 * Math.PI * x * x * x)
            }
            output(result)
        })
    }
internal fun MainActivity.speedCalculator() {
        clearPage("Kecepatan")
        content.addView(label("Jarak • Waktu • Kecepatan",22f,true))
        val d = edit("Jarak")
        val t = edit("Waktu (jam)")
        content.addView(d)
        content.addView(t)
        content.addView(button("Hitung kecepatan") {
            val x = d.num()
            val y = t.num()
            output(if (x == null || y == null || y == 0.0) "Input tidak valid" else "Kecepatan = ${fmt(x / y)} unit/jam")
        })
        content.addView(button("Hitung jarak dari kecepatan × waktu") {
            val x = d.num()
            val y = t.num()
            output(if (x == null || y == null) "Input tidak valid" else "Jarak = ${fmt(x * y)} unit")
        })
    }
internal fun MainActivity.timeCalculator() {
        clearPage("Waktu & Durasi")
        content.addView(label("Konversi Durasi",22f,true))
        val v = edit("Detik")
        content.addView(v)
        content.addView(button("Konversi") {
            val x = v.num()
            if (x == null || x < 0) {
                output("Input tidak valid")
            } else {
                val sec = x.toLong()
                val h = sec / 3600
                val m = (sec % 3600) / 60
                val ss = sec % 60
                output("$h jam $m menit $ss detik")
            }
        })
    }
internal fun MainActivity.dateCalculator() {
        clearPage("Selisih Tanggal")
        content.addView(label("Selisih dua tanggal",22f,true))
        val a=edit("Tanggal 1: YYYY-MM-DD"); val b=edit("Tanggal 2: YYYY-MM-DD")
        content.addView(a); content.addView(b)
        content.addView(button("Hitung hari") {
            output(runCatching {
                val f=SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient=false }
                val d1=f.parse(a.text.toString().trim()) ?: error("tanggal")
                val d2=f.parse(b.text.toString().trim()) ?: error("tanggal")
                "${kotlin.math.abs((d2.time-d1.time)/86400000L)} hari"
            }.getOrElse{"Format tanggal: YYYY-MM-DD"})
        })
        content.addView(button("Hitung umur dari Tanggal 1") {
            output(runCatching {
                val f=SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient=false }; val birth=f.parse(a.text.toString().trim()) ?: error("tanggal"); val now=Calendar.getInstance(); val dob=Calendar.getInstance().apply{time=birth}; var years=now.get(Calendar.YEAR)-dob.get(Calendar.YEAR); if(now.get(Calendar.DAY_OF_YEAR)<dob.get(Calendar.DAY_OF_YEAR)) years--; val days=((now.timeInMillis-birth.time)/86400000L).coerceAtLeast(0); "Umur sekitar $years tahun\nTotal hari hidup: $days"
            }.getOrElse{"Format tanggal: YYYY-MM-DD"})
        })
    }
internal fun MainActivity.loanCalculator() { clearPage("Cicilan Pinjaman"); content.addView(label("Kalkulator cicilan",22f,true)); val principal=edit("Pokok pinjaman"); val rate=edit("Bunga tahunan (%)"); val months=edit("Tenor (bulan)"); content.addView(principal);content.addView(rate);content.addView(months); content.addView(button("Hitung cicilan") {val p=principal.num();val r=rate.num();val n=months.num(); if(p==null||r==null||n==null||n<=0)output("Input tidak valid")else{val m=r/100/12; val pay=if(m==0.0)p/n else p*m*Math.pow(1+m,n)/(Math.pow(1+m,n)-1); output("Cicilan ≈ ${fmt(pay)} per bulan\nTotal ≈ ${fmt(pay*n)}")}}) }
internal fun MainActivity.fuelCalculator() {
        clearPage("Konsumsi BBM")
        content.addView(label("Konsumsi BBM", 22f, true))
        val distance = edit("Jarak (km)")
        val fuel = edit("BBM (liter)")
        val price = edit("Harga per liter (opsional)")
        content.addView(distance)
        content.addView(fuel)
        content.addView(price)
        content.addView(button("Hitung") {
            val d = distance.num()
            val f = fuel.num()
            val p = price.num()
            if (d == null || f == null || f <= 0) {
                output("Input tidak valid")
            } else {
                val kmpl = d / f
                val l100 = f / d * 100
                val cost = if (p == null) "" else "\nBiaya ≈ ${fmt(f * p)}"
                output("${fmt(kmpl)} km/l\n${fmt(l100)} L/100 km$cost")
            }
        })
    }
internal fun MainActivity.baseCalculator() {
        clearPage("Basis Angka")
        val e = edit("Masukkan angka, mis. 101101 atau FF")
        content.addView(e)
        val from = Spinner(this)
        from.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("2", "8", "10", "16"))
        content.addView(from)
        content.addView(button("Konversi ke semua basis") {
            try {
                val radix = from.selectedItem.toString().toInt()
                val raw = e.text.toString().trim()
                val n = raw.toLong(radix)
                val result = "BIN  " + n.toString(2) + "\n" +
                        "OCT  " + n.toString(8) + "\n" +
                        "DEC  " + n.toString(10) + "\n" +
                        "HEX  " + n.toString(16).uppercase(Locale.getDefault())
                output(result)
            } catch (ex: Exception) {
                output("Angka tidak valid untuk basis yang dipilih.")
            }
        })
    }
internal fun MainActivity.equationCalculator() { clearPage("Persamaan Linear"); content.addView(label("ax + b = c",22f,true)); val a=edit("a"); val b=edit("b"); val c=edit("c"); content.addView(a);content.addView(b);content.addView(c); content.addView(button("Cari x") {val aa=a.num();val bb=b.num();val cc=c.num();output(if(aa==null||bb==null||cc==null||aa==0.0)"Input tidak valid / a tidak boleh 0" else "x = ${fmt((cc-bb)/aa)}")}) }
internal fun EditText.num(): Double? = text.toString().trim().replace(",",".").toDoubleOrNull()

internal fun MainActivity.fmt(v:Double):String = if(v.isFinite() && kotlin.math.abs(v-v.toLong())<1e-10) v.toLong().toString() else String.format(Locale.US,"%.8f",v).trimEnd('0').trimEnd('.')

