package com.vigil.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Three small square indicator boxes that light up one by one during a check sequence.
 *
 * litCount: 0..3 — number of boxes currently lit (left to right).
 */
class IndicatorBoxesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var litCount: Int = 0
        set(value) {
            field = value.coerceIn(0, 3)
            invalidate()
        }

    /** Optional per-box color override. Null means use default (white when lit). */
    var litColors: Array<Int?> = arrayOf(null, null, null)
        set(value) {
            field = value
            invalidate()
        }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.rgb(180, 180, 180)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Width derived from height (3 squares + gaps).
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val box = h.toFloat()
        val gap = box * 0.25f
        val w = (box * 3 + gap * 2).toInt()
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val box = h
        val gap = box * 0.25f
        val r = box * 0.18f
        for (i in 0..2) {
            val left = i * (box + gap)
            val rect = RectF(left + 2f, 2f, left + box - 2f, box - 2f)
            if (i < litCount) {
                fill.color = litColors[i] ?: Color.WHITE
                canvas.drawRoundRect(rect, r, r, fill)
            } else {
                canvas.drawRoundRect(rect, r, r, stroke)
            }
        }
    }
}
