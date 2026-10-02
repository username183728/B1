package com.example.aidetest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Tombol kanan-atas (menu/aksi). Setiap kali teksnya diset ke "⋮" (titik tiga),
 * tombol ini menggambar wajah bot Bit sebagai gantinya. Teks lain (mis. "+")
 * tetap tampil seperti biasa, sehingga semua `action.text = "⋮"` di project
 * otomatis berubah menjadi bot tanpa mengubah logika klik.
 *
 * Warna badan bot mengikuti warna teks tombol (diatur styleTopFabs sesuai tema),
 * warna mata dibalik agar selalu kontras.
 */
class BotMenuButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var botMode = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyeRect = RectF()

    override fun setText(text: CharSequence?, type: android.widget.TextView.BufferType?) {
        val isBot = text?.toString()?.trim() == "⋮"
        botMode = isBot
        super.setText(if (isBot) "" else text, type)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!botMode) return

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.31f
        val body = currentTextColor or (0xFF shl 24)
        val luminance = (0.299f * Color.red(body) + 0.587f * Color.green(body) + 0.114f * Color.blue(body)) / 255f
        val eyes = if (luminance > 0.5f) Color.rgb(20, 20, 26) else Color.WHITE

        paint.style = Paint.Style.FILL
        paint.color = body
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = eyes
        val eyeW = radius * 0.24f
        val eyeH = radius * 0.50f
        val dx = radius * 0.38f
        for (side in intArrayOf(-1, 1)) {
            val ex = cx + side * dx
            eyeRect.set(ex - eyeW / 2f, cy - eyeH / 2f, ex + eyeW / 2f, cy + eyeH / 2f)
            canvas.drawRoundRect(eyeRect, eyeW / 2f, eyeW / 2f, paint)
        }
    }
}
