package com.vigil.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Animated EEG-like waveform inside a dark rounded panel.
 *
 * Idle: flat baseline.
 * Active: scrolling waveform with sharp QRS-like spikes.
 */
class EegWaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(8, 8, 10)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 30, 30)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    var active: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) animator.start() else {
                animator.cancel()
                phase = 0f
                invalidate()
            }
        }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h * 0.06f
        canvas.drawRoundRect(0f, 0f, w, h, r, r, bgPaint)
        canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, r, r, borderPaint)

        val midY = h / 2f
        val path = Path()
        if (!active) {
            path.moveTo(8f, midY)
            path.lineTo(w - 8f, midY)
        } else {
            val step = 2f
            val amp = h * 0.34f
            var x = 8f
            path.moveTo(x, midY)
            // Sum of low-frequency wave + sharp spikes scrolling across.
            val spikeCenters = floatArrayOf(0.2f, 0.55f, 0.85f)
            while (x < w - 8f) {
                val t = (x / w) + phase
                val base = sin(t * 2.0 * PI * 1.0).toFloat() * amp * 0.10f +
                           sin(t * 2.0 * PI * 3.7).toFloat() * amp * 0.05f
                var spike = 0f
                for (c in spikeCenters) {
                    val rel = ((x / w) - ((c + phase) % 1f))
                    val dx = (rel - rel.toInt().toFloat())
                    // Sharp QRS-like: positive small bump + tall negative spike + bump
                    val d = dx * 30f
                    spike -= amp * exp((-d * d).toDouble()).toFloat() * 1.6f
                    spike += amp * exp((-(d - 1.2f) * (d - 1.2f)).toDouble()).toFloat() * 0.6f
                    spike += amp * exp((-(d + 1.0f) * (d + 1.0f)).toDouble()).toFloat() * 0.4f
                }
                val y = midY + base + spike
                path.lineTo(x, y.coerceIn(h * 0.08f, h * 0.92f))
                x += step
            }
        }
        canvas.drawPath(path, linePaint)
    }
}
