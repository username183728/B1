package com.example.aidetest

import android.animation.Animator
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * Bit animation state machine.
 *
 * Each reaction is a separate Lottie asset. This prevents a reaction from
 * accidentally playing the animation immediately before it (for example TAP
 * before BUMP). The normal face is bit_idle.json; special reactions swap to
 * their own asset and then return to idle.
 */
internal class BitAnimationController(private val activity: MainActivity) : SensorEventListener {
    private val handler = Handler(Looper.getMainLooper())
    private val faces = linkedSetOf<LottieAnimationView>()
    private var token = 0
    private var taps = 0
    private var lastTapAt = 0L
    private var resetTapRunnable: Runnable? = null
    private var sensorManager: SensorManager? = null
    private var lastShakeAt = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var sensorReady = false

    fun attach(face: LottieAnimationView) {
        faces.add(face)
        configure(face)
        loadIdle(face)
    }

    fun detach(face: LottieAnimationView?) {
        if (face != null) {
            faces.remove(face)
            face.removeAllAnimatorListeners()
            face.cancelAnimation()
        }
    }

    fun onTap(face: LottieAnimationView? = null, openChat: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        taps = if (now - lastTapAt <= 1000L) taps + 1 else 1
        lastTapAt = now
        resetTapRunnable?.let(handler::removeCallbacks)
        val reset = Runnable { taps = 0 }
        resetTapRunnable = reset
        handler.postDelayed(reset, 1400L)

        // Three taps go DIRECTLY to BUMP. The individual TAP animation is not
        // played on the third tap, so the user never sees TAP -> BUMP.
        if (taps >= 3) {
            taps = 0
            playBump()
        } else {
            playTap(face)
        }

        if (openChat) {
            activity.navBot.postDelayed({
                if (!activity.isFinishing) activity.showBitChat()
            }, 180L)
        }
    }

    fun playThinking(face: LottieAnimationView? = null) {
        val target = face ?: activity.bitFace
        val myToken = ++token
        playRange(target, "bit_idle.json", 108, 286, myToken) {
            if (myToken == token) playIdle(target)
        }
    }

    fun playTap(face: LottieAnimationView? = null) {
        val target = face ?: activity.bitFace
        val myToken = ++token
        playAsset(target, "bit_tap.json", 450, 480, myToken) {
            if (myToken == token) playIdle(target)
        }
    }

    private fun playBump() {
        val myToken = ++token
        faces.forEach { face ->
            playAsset(face, "bit_bump.json", 480, 560, myToken) {
                if (myToken == token) playIdle(face)
            }
        }
    }

    fun playDizzy() {
        val myToken = ++token
        faces.forEach { face ->
            playAsset(face, "bit_dizzy.json", 560, 660, myToken) {
                if (myToken == token) playIdle(face)
            }
        }
    }

    fun playError() {
        val myToken = ++token
        faces.forEach { face ->
            playAsset(face, "bit_error.json", 720, 770, myToken) {
                if (myToken == token) holdFrame(face, 768, 720, 770)
            }
        }
    }

    fun playRecovered() {
        val myToken = ++token
        faces.forEach { face ->
            playAsset(face, "bit_wink.json", 900, 940, myToken) {
                if (myToken == token) playIdle(face)
            }
        }
    }

    fun playIdleAll() {
        ++token
        faces.forEach(::playIdle)
    }

    fun startShakeDetection() {
        if (sensorReady) return
        sensorManager = activity.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        sensorReady = true
    }

    fun stopShakeDetection() {
        if (!sensorReady) return
        sensorManager?.unregisterListener(this)
        sensorReady = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val delta = kotlin.math.abs(x - lastX) + kotlin.math.abs(y - lastY) + kotlin.math.abs(z - lastZ)
        lastX = x; lastY = y; lastZ = z
        val now = android.os.SystemClock.elapsedRealtime()
        if (delta > 18f && now - lastShakeAt > 900L && !activity.isFinishing) {
            lastShakeAt = now
            activity.runOnUiThread { playDizzy() }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun configure(face: LottieAnimationView) {
        face.repeatMode = LottieDrawable.RESTART
        face.repeatCount = 0
        face.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        face.isClickable = false
        face.isFocusable = false
    }

    private fun playIdle(face: LottieAnimationView?) {
        if (face == null || !faces.contains(face)) return
        val myToken = token
        playAsset(face, "bit_idle.json", 0, 450, myToken) {
            if (myToken == token) playIdle(face)
        }
    }

    private fun loadIdle(face: LottieAnimationView) {
        face.setAnimation("bit/bit_idle.json")
        activity.applyBitFaceTheme(face)
        face.setMinAndMaxFrame(0, 450)
    }

    private fun playRange(face: LottieAnimationView?, asset: String, start: Int, end: Int, myToken: Int, onEnd: (() -> Unit)? = null) {
        playAsset(face, asset, start, end, myToken, onEnd)
    }

    private fun playAsset(face: LottieAnimationView?, asset: String, start: Int, end: Int, myToken: Int, onEnd: (() -> Unit)? = null) {
        if (face == null || !faces.contains(face) || myToken != token) return
        face.removeAllAnimatorListeners()
        face.cancelAnimation()
        face.setAnimation("bit/$asset")
        activity.applyBitFaceTheme(face)
        face.repeatCount = 0
        face.setMinAndMaxFrame(start, end)
        face.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) = Unit
            override fun onAnimationCancel(animation: Animator) = Unit
            override fun onAnimationRepeat(animation: Animator) = Unit
            override fun onAnimationEnd(animation: Animator) {
                if (myToken != token || !faces.contains(face)) return
                onEnd?.invoke()
            }
        })
        face.playAnimation()
    }

    private fun holdFrame(face: LottieAnimationView, frame: Int, start: Int, end: Int) {
        face.removeAllAnimatorListeners()
        face.cancelAnimation()
        face.setMinAndMaxFrame(start, end)
        face.progress = ((frame - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    }
}
