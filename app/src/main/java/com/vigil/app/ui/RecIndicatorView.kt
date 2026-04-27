package com.vigil.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Red bracketed REC indicator with a blinking dot.
 *
 * Renders: `[● REC]` with red color. The dot blinks while [recording] is true.
 */
class RecIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val red = Color.rgb(230, 40, 40)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = red
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = red
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = red
        textSize = 30f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }

    private var blinkOn = true
    private val blinker = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val newOn = (it.animatedValue as Float) < 0.5f
            if (newOn != blinkOn) {
                blinkOn = newOn
                invalidate()
            }
        }
    }

    var recording: Boolean = true
        set(value) {
            field = value
            if (value) blinker.start() else { blinker.cancel(); blinkOn = true; invalidate() }
        }

    init {
        blinker.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blinker.cancel()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec)
        textPaint.textSize = h * 0.55f
        val w = (h * 2.4f).toInt()
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = h * 0.10f
        val cornerLen = h * 0.30f
        val sw = (h * 0.10f).coerceAtLeast(2f)
        paint.strokeWidth = sw

        // Camera-style brackets at the four corners.
        // Top-left
        canvas.drawLine(pad, pad, pad + cornerLen, pad, paint)
        canvas.drawLine(pad, pad, pad, pad + cornerLen, paint)
        // Top-right
        canvas.drawLine(w - pad, pad, w - pad - cornerLen, pad, paint)
        canvas.drawLine(w - pad, pad, w - pad, pad + cornerLen, paint)
        // Bottom-left
        canvas.drawLine(pad, h - pad, pad + cornerLen, h - pad, paint)
        canvas.drawLine(pad, h - pad, pad, h - pad - cornerLen, paint)
        // Bottom-right
        canvas.drawLine(w - pad, h - pad, w - pad - cornerLen, h - pad, paint)
        canvas.drawLine(w - pad, h - pad, w - pad, h - pad - cornerLen, paint)

        // Blinking dot
        if (blinkOn || !recording) {
            val cy = h / 2f
            val dotR = h * 0.13f
            val dotCx = pad + cornerLen + dotR + 2f
            canvas.drawCircle(dotCx, cy, dotR, fillPaint)

            val txt = "REC"
            val tw = textPaint.measureText(txt)
            val fm = textPaint.fontMetrics
            val ty = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(txt, w - pad - cornerLen - tw - 4f, ty, textPaint)
        } else {
            // Keep the REC text always visible; only the dot blinks.
            val cy = h / 2f
            val txt = "REC"
            val tw = textPaint.measureText(txt)
            val fm = textPaint.fontMetrics
            val ty = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(txt, w - pad - cornerLen - tw - 4f, ty, textPaint)
        }
    }
}
