package com.example.aidetest

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * GitLis loading screen dengan 4 level animasi (mengikuti Motion.mode).
 *
 * - High : ink reveal atas->bawah, settle dengan sedikit overshoot, partikel halus,
 *          teks "GitLis" naik + fade-in, lalu fade-out.
 * - Mid  : ikon fade + scale-in halus, teks fade-in, lalu fade-out.
 * - Low  : fade-in/out singkat tanpa gerakan.
 * - Off  : ikon + teks langsung tampil, ditahan sebentar, lalu hilang tanpa animasi.
 *
 * Semua efek hanya memakai alpha/scale/translation/clip sehingga ringan di GPU.
 * Tidak ada animasi yang berjalan setelah splash selesai atau view terlepas.
 */
class GitlisSplashView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
    }
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(10, 10, 12)
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(10, 10, 12)
        textSize = 24f * d
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacing = 0.06f
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(10, 10, 12)
        style = Paint.Style.FILL
    }

    private val icon: Bitmap? by lazy {
        runCatching { BitmapFactory.decodeResource(resources, R.drawable.gitlis_loading_icon) }.getOrNull()
    }

    // Partikel deterministik (tanpa Random) agar animasi konsisten setiap peluncuran.
    private val particleCount = 10
    private val particleAngle = FloatArray(particleCount) { i ->
        ((i * 360f / particleCount) + (if (i % 2 == 0) 9f else -7f)) * (PI.toFloat() / 180f)
    }
    private val particleDist = FloatArray(particleCount) { i -> 0.34f + (i % 4) * 0.055f }
    private val particleSize = FloatArray(particleCount) { i -> (1.6f + (i % 3) * 0.9f) * d }

    private val decel = DecelerateInterpolator(1.6f)
    private val overshoot = OvershootInterpolator(1.2f)

    private var mode: Motion.Mode = Motion.Mode.HIGH
    private var totalMs = 0L
    private var exitStartMs = 0L
    private var elapsed = 0f
    private var animator: ValueAnimator? = null
    private var started = false
    private var finished = false
    private var onFinished: (() -> Unit)? = null

    init {
        setBackgroundColor(Color.WHITE)
        isClickable = true
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        elevation = 100f * d
    }

    fun start(onDone: () -> Unit) {
        if (started) return
        started = true
        onFinished = onDone
        mode = Motion.mode
        when (mode) {
            Motion.Mode.HIGH -> { exitStartMs = 1500L; totalMs = 1760L }
            Motion.Mode.MID -> { exitStartMs = 1000L; totalMs = 1200L }
            Motion.Mode.LOW -> { exitStartMs = 600L; totalMs = 760L }
            Motion.Mode.OFF -> { exitStartMs = 300L; totalMs = 300L }
        }
        elapsed = 0f
        animator = ValueAnimator.ofFloat(0f, totalMs.toFloat()).apply {
            duration = totalMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                elapsed = it.animatedValue as Float
                // Fade-out seluruh layar di akhir timeline (Off: langsung hilang tanpa fade).
                alpha = if (elapsed >= exitStartMs && totalMs > exitStartMs) {
                    1f - clamp01((elapsed - exitStartMs) / (totalMs - exitStartMs).toFloat())
                } else 1f
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false
                override fun onAnimationCancel(animation: Animator) { canceled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (!canceled) finish()
                }
            })
            start()
        }
    }

    private fun finish() {
        if (finished) return
        finished = true
        animator = null
        (parent as? ViewGroup)?.removeView(this)
        val cb = onFinished
        onFinished = null
        cb?.invoke()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = elapsed

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f - 16f * d
        val targetW = min(w, h) * 0.42f

        // Nilai animasi per level.
        var iconAlpha = 1f
        var scale = 1f
        var reveal = 1f
        var textAlpha = 1f
        var textRise = 0f
        var particleP = -1f
        when (mode) {
            Motion.Mode.HIGH -> {
                iconAlpha = clamp01(t / 140f)
                scale = 0.93f + 0.07f * overshoot.getInterpolation(clamp01(t / 620f))
                reveal = decel.getInterpolation(clamp01(t / 700f))
                textAlpha = clamp01((t - 620f) / 360f)
                textRise = (1f - decel.getInterpolation(textAlpha)) * 10f * d
                particleP = clamp01((t - 380f) / 870f)
            }
            Motion.Mode.MID -> {
                val p = decel.getInterpolation(clamp01(t / 380f))
                iconAlpha = p
                scale = 0.92f + 0.08f * p
                textAlpha = clamp01((t - 300f) / 300f)
                textRise = (1f - decel.getInterpolation(textAlpha)) * 6f * d
            }
            Motion.Mode.LOW -> {
                iconAlpha = clamp01(t / 180f)
                textAlpha = iconAlpha
            }
            Motion.Mode.OFF -> Unit
        }

        val bmp = icon
        val targetH: Float
        if (bmp != null) {
            targetH = targetW * bmp.height.toFloat() / bmp.width.toFloat()
            val dest = RectF(cx - targetW / 2f, cy - targetH / 2f, cx + targetW / 2f, cy + targetH / 2f)
            canvas.save()
            canvas.scale(scale, scale, cx, cy)
            canvas.clipRect(dest.left, dest.top, dest.right, dest.top + dest.height() * reveal)
            markPaint.alpha = (iconAlpha * 255f).toInt()
            canvas.drawBitmap(bmp, null, dest, markPaint)
            canvas.restore()
        } else {
            // Fallback aman jika asset hilang.
            targetH = targetW * 0.8f
            val r = targetW * 0.4f
            fallbackPaint.alpha = (iconAlpha * 255f).toInt()
            canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), 28f * d, 28f * d, fallbackPaint)
        }

        // Partikel halus (khusus High).
        if (particleP in 0f..1f && particleP > 0f) {
            val e = decel.getInterpolation(particleP)
            val a = sin(PI.toFloat() * particleP) * 0.5f
            particlePaint.alpha = (a * 255f).toInt()
            for (i in 0 until particleCount) {
                val dist = targetW * (0.28f + particleDist[i] * e)
                val px = cx + cos(particleAngle[i]) * dist
                val py = cy + sin(particleAngle[i]) * dist * 0.85f
                canvas.drawCircle(px, py, particleSize[i], particlePaint)
            }
        }

        // Teks "GitLis" di bawah ikon.
        if (textAlpha > 0f) {
            textPaint.alpha = (textAlpha * 255f).toInt()
            canvas.drawText("GitLis", cx, cy + targetH / 2f + 44f * d + textRise, textPaint)
        }
    }
}
