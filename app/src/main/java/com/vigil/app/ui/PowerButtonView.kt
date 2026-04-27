package com.vigil.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Large circular power button. White (idle) / green (active).
 *
 * Renders a glossy black bezel ring with a glowing inner disc and the standard
 * power glyph (broken circle + vertical bar) in the center.
 */
class PowerButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var active: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = (minOf(w, h) / 2f) - 4f

        // Outer black bezel with a subtle vertical gradient
        bezelPaint.shader = android.graphics.LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf(Color.rgb(40, 40, 40), Color.rgb(8, 8, 8)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, bezelPaint)
        bezelPaint.shader = null

        // Inner ring (glowing color)
        val innerR = r * 0.78f
        val color = if (active) Color.rgb(60, 230, 90) else Color.WHITE
        val glowColor = if (active) Color.argb(140, 60, 230, 90) else Color.argb(110, 255, 255, 255)

        // Glow halo
        glowPaint.shader = RadialGradient(
            cx, cy, r * 1.05f,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 1.05f, glowPaint)
        glowPaint.shader = null

        // Inner disc (slightly recessed look)
        ringPaint.shader = RadialGradient(
            cx, cy, innerR,
            intArrayOf(Color.rgb(20, 20, 20), Color.rgb(2, 2, 2)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, innerR, ringPaint)
        ringPaint.shader = null

        // Power glyph: broken circle + vertical bar.
        glyphPaint.color = color
        glyphPaint.strokeWidth = r * 0.10f
        val glyphR = innerR * 0.55f
        val arcRect = RectF(cx - glyphR, cy - glyphR, cx + glyphR, cy + glyphR)
        // Arc from 130° around to 50° (CW) leaving the top open.
        canvas.drawArc(arcRect, 130f, 280f, false, glyphPaint)
        // Vertical bar
        val barTop = cy - glyphR * 1.18f
        val barBottom = cy - glyphR * 0.05f
        canvas.drawLine(cx, barTop, cx, barBottom, glyphPaint)

        // Subtle light reflection on the bezel top
        val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = android.graphics.LinearGradient(
                cx, cy - r, cx, cy - r * 0.4f,
                intArrayOf(Color.argb(60, 255, 255, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        val path = Path().apply {
            addCircle(cx, cy, r, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawRect(0f, cy - r, w, cy - r * 0.3f, highlight)
        canvas.restore()
    }
}
