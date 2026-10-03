package com.example.aidetest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable

/**
 * Animated Bit button used for the top-right action button.
 * When its text is "⋮", the dots are replaced by the real Bit Lottie animation.
 * The animation is transparent and follows the same theme/background as the page.
 */
class BotMenuButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var botMode = false
    private var botDrawable: LottieDrawable? = null
    private var botLoaded = false
    private var taps = 0
    private var lastTapAt = 0L
    private var reactionToken = 0
    private var resetRunnable: Runnable? = null

    override fun setText(text: CharSequence?, type: BufferType?) {
        val isBot = text?.toString()?.trim() == "⋮"
        botMode = isBot
        super.setText(if (isBot) "" else text, type)
        if (isBot) {
            ensureBotDrawable()
            post { playIdle() }
        } else {
            botDrawable?.cancelAnimation()
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        resetRunnable?.let { removeCallbacks(it) }
        botDrawable?.cancelAnimation()
        botDrawable?.callback = null
        super.onDetachedFromWindow()
    }

    override fun performClick(): Boolean {
        if (botMode) playTapReaction()
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        if (!botMode) {
            super.onDraw(canvas)
            return
        }
        val d = botDrawable
        if (d == null || !botLoaded) {
            // Keep a clean fallback while the Lottie asset loads.
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(width, height) * 0.31f
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.color = currentTextColor
            canvas.drawCircle(cx, cy, r, paint)
            paint.color = if (currentTextColor == Color.WHITE) Color.rgb(20,20,26) else Color.WHITE
            canvas.drawRoundRect(cx-r*0.5f, cy-r*0.25f, cx-r*0.15f, cy+r*0.25f, r*0.12f, r*0.12f, paint)
            canvas.drawRoundRect(cx+r*0.15f, cy-r*0.25f, cx+r*0.5f, cy+r*0.25f, r*0.12f, r*0.12f, paint)
            return
        }
        val size = minOf(width, height)
        val left = (width - size) / 2
        val top = (height - size) / 2
        d.bounds = android.graphics.Rect(left, top, left + size, top + size)
        d.draw(canvas)
    }

    private fun ensureBotDrawable() {
        if (botDrawable != null) return
        val drawable = LottieDrawable().apply {
            repeatMode = LottieDrawable.RESTART
            repeatCount = LottieDrawable.INFINITE
            callback = this@BotMenuButton
        }
        botDrawable = drawable
        LottieCompositionFactory.fromAsset(context, "bit/bit_idle.json")
            .addListener { composition ->
                drawable.setComposition(composition)
                drawable.setMinAndMaxFrame(0, 450)
                botLoaded = true
                drawable.playAnimation()
                invalidate()
            }
            .addFailureListener {
                botLoaded = false
                invalidate()
            }
    }

    private fun playIdle() {
        if (!botMode) return
        ensureBotDrawable()
        val d = botDrawable ?: return
        if (!botLoaded) return
        reactionToken++
        d.removeAllAnimatorListeners()
        d.setMinAndMaxFrame(0, 450)
        d.repeatCount = LottieDrawable.INFINITE
        d.playAnimation()
        invalidate()
    }

    private fun playTapReaction() {
        val now = android.os.SystemClock.elapsedRealtime()
        taps = if (now - lastTapAt <= 1000L) taps + 1 else 1
        lastTapAt = now
        resetRunnable?.let { removeCallbacks(it) }
        val r = Runnable { taps = 0 }
        resetRunnable = r
        postDelayed(r, 1400L)

        if (taps >= 3) {
            taps = 0
            playSequence("bit_bump.json", 480, 560, null, 0, 0)
        } else {
            playSequence("bit_tap.json", 450, 480, null, 0, 0)
        }
    }

    private fun playSequence(first: String, firstStart: Int, firstEnd: Int, second: String?, secondStart: Int, secondEnd: Int) {
        val d = botDrawable ?: return
        if (!botLoaded) return
        val my = ++reactionToken
        d.cancelAnimation()
        playAsset(d, first, firstStart, firstEnd, my) {
            if (second != null && my == reactionToken && botMode) {
                playAsset(d, second, secondStart, secondEnd, my) { if (my == reactionToken) playIdle() }
            } else if (my == reactionToken) playIdle()
        }
    }

    private fun playAsset(d: LottieDrawable, asset: String, start: Int, end: Int, token: Int, onEnd: () -> Unit) {
        LottieCompositionFactory.fromAsset(context, "bit/$asset").addListener { composition ->
            if (token != reactionToken || !botMode) return@addListener
            d.setComposition(composition)
            d.setMinAndMaxFrame(start, end)
            d.repeatCount = 0
            d.removeAllAnimatorListeners()
            d.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) = Unit
                override fun onAnimationCancel(animation: android.animation.Animator) = Unit
                override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
                override fun onAnimationEnd(animation: android.animation.Animator) { if (token == reactionToken) onEnd() }
            })
            d.playAnimation()
            invalidate()
        }
    }
}
