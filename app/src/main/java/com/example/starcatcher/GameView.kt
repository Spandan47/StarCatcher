package com.example.starcatcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom view: draws the night scene (sky, moon, hills) with shapes and
 * handles falling stars and touches.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Star(var x: Float, var y: Float, val golden: Boolean) {
        var scale = 1f
        var alpha = 1f
        var caught = false
        var fallAnimator: ValueAnimator? = null
        var catchAnimator: AnimatorSet? = null
    }

    var onStarCaught: ((points: Int) -> Unit)? = null
    var onStarMissed: (() -> Unit)? = null
    var onMoonTapped: (() -> Unit)? = null
    var fallDurationMs = 4000L

    private var running = true
    private val density = resources.displayMetrics.density
    private val stars = mutableListOf<Star>()

    private val skyPaint = Paint()
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(250, 250, 220) }
    private val craterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(225, 225, 190) }
    private val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 40, 30) }
    private val hillPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(12, 28, 20) }
    private val twinklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(170, 255, 255, 255) }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val moonRect = RectF()
    private val starPath = Path()
    private var twinkles = FloatArray(0)

    init {
        // Unit 5-point star (radius 1) that is scaled when drawn
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 1f else 0.42f
            val angle = Math.toRadians(-90.0 + 36.0 * i)
            val px = (r * cos(angle)).toFloat()
            val py = (r * sin(angle)).toFloat()
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(10, 10, 50), Color.rgb(60, 40, 100), Shader.TileMode.CLAMP
        )
        val moonSize = 72f * density
        moonRect.set(w * 0.72f, h * 0.06f, w * 0.72f + moonSize, h * 0.06f + moonSize)

        val rnd = Random(42)
        twinkles = FloatArray(80) { i ->
            if (i % 2 == 0) rnd.nextFloat() * w else rnd.nextFloat() * h * 0.7f
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, skyPaint)

        // Background twinkles
        var i = 0
        while (i < twinkles.size - 1) {
            canvas.drawCircle(twinkles[i], twinkles[i + 1], 1.5f * density, twinklePaint)
            i += 2
        }

        // Moon with craters
        canvas.drawOval(moonRect, moonPaint)
        val mw = moonRect.width()
        canvas.drawCircle(moonRect.left + mw * 0.35f, moonRect.top + mw * 0.35f, mw * 0.1f, craterPaint)
        canvas.drawCircle(moonRect.left + mw * 0.65f, moonRect.top + mw * 0.55f, mw * 0.14f, craterPaint)
        canvas.drawCircle(moonRect.left + mw * 0.4f, moonRect.top + mw * 0.75f, mw * 0.07f, craterPaint)

        // Hills
        canvas.drawCircle(w * 0.2f, h * 1.05f, w * 0.4f, hillPaint)
        canvas.drawCircle(w * 0.85f, h * 1.08f, w * 0.45f, hillPaint2)
        canvas.drawCircle(w * 0.55f, h * 1.15f, w * 0.4f, hillPaint)

        // Falling stars
        for (s in stars) {
            val r = 18f * density * s.scale
            starPaint.color = if (s.golden) Color.rgb(255, 215, 0) else Color.WHITE
            starPaint.alpha = (s.alpha * 255).toInt().coerceIn(0, 255)
            canvas.save()
            canvas.translate(s.x, s.y)
            canvas.scale(r, r)
            canvas.drawPath(starPath, starPaint)
            canvas.restore()
        }
    }

    fun spawnStar(golden: Boolean = false) {
        if (!running || width == 0 || height == 0) return
        val margin = 30f * density
        val star = Star(margin + Random.nextFloat() * (width - 2 * margin), -margin, golden)
        stars.add(star)

        val endY = height * 0.93f
        star.fallAnimator = ValueAnimator.ofFloat(-margin, endY).apply {
            duration = if (golden) (fallDurationMs * 0.8f).toLong() else fallDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                star.y = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (running && !star.caught && stars.remove(star)) {
                        invalidate()
                        onStarMissed?.invoke()
                    }
                }
            })
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        if (!running) return true
        performClick()

        if (moonRect.contains(event.x, event.y)) {
            onMoonTapped?.invoke()
            return true
        }

        val hitRadius = 36f * density
        val hit = stars.firstOrNull {
            !it.caught && hypot(it.x - event.x, it.y - event.y) < hitRadius
        }
        if (hit != null) catchStar(hit)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun catchStar(star: Star) {
        star.caught = true
        star.fallAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }

        val grow = ValueAnimator.ofFloat(1f, 2f).apply {
            addUpdateListener { star.scale = it.animatedValue as Float }
        }
        val fade = ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener {
                star.alpha = it.animatedValue as Float
                invalidate()
            }
        }
        star.catchAnimator = AnimatorSet().apply {
            playTogether(grow, fade)
            duration = 250
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    stars.remove(star)
                    invalidate()
                }
            })
            start()
        }
        onStarCaught?.invoke(if (star.golden) 5 else 1)
    }

    /** Stops and clears all stars WITHOUT counting them as misses. */
    fun pauseGame() {
        running = false
        val copy = stars.toList()
        stars.clear()
        copy.forEach {
            it.fallAnimator?.removeAllListeners()
            it.fallAnimator?.cancel()
            it.catchAnimator?.removeAllListeners()
            it.catchAnimator?.cancel()
        }
        invalidate()
    }

    fun resumeGame() {
        running = true
    }

    override fun onDetachedFromWindow() {
        pauseGame() // prevents leaks and crashes after rotation
        super.onDetachedFromWindow()
    }
}
