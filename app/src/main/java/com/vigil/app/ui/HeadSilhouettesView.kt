package com.vigil.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Two head silhouettes top: left = red X = no coherence, right = green ✓ = coherence.
 *
 * Both are drawn dimly until a result is shown. After a check, the active result
 * (positive or negative) is highlighted brightly and the other is dimmed further.
 */
class HeadSilhouettesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Result { NONE, POSITIVE, NEGATIVE }

    var result: Result = Result.NONE
        set(value) {
            field = value
            invalidate()
        }

    /** Selected head in validation mode (NONE = not in validation). */
    var validationSelection: Result = Result.NONE
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Listener for taps on left (NEGATIVE / red X) or right (POSITIVE / green ✓) head. */
    var onHeadTap: ((Result) -> Unit)? = null

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            val r = if (event.x < width / 2f) Result.NEGATIVE else Result.POSITIVE
            onHeadTap?.invoke(r)
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val halfW = w / 2f

        // Brightness logic
        val leftActive = result == Result.NEGATIVE || validationSelection == Result.NEGATIVE
        val rightActive = result == Result.POSITIVE || validationSelection == Result.POSITIVE
        val anyActive = leftActive || rightActive

        val redBright = if (anyActive) leftActive else true
        val greenBright = if (anyActive) rightActive else true

        // Left head (red, faces right) with red X box
        drawHead(
            canvas, 0f, 0f, halfW, h,
            faceRight = true,
            color = if (redBright) Color.rgb(255, 60, 60) else Color.rgb(90, 25, 25),
            symbol = Symbol.X
        )
        // Right head (green, faces left) with green check box
        drawHead(
            canvas, halfW, 0f, halfW, h,
            faceRight = false,
            color = if (greenBright) Color.rgb(60, 230, 90) else Color.rgb(25, 70, 30),
            symbol = Symbol.CHECK
        )
    }

    private enum class Symbol { X, CHECK }

    private fun drawHead(
        canvas: Canvas,
        left: Float, top: Float, w: Float, h: Float,
        faceRight: Boolean, color: Int, symbol: Symbol
    ) {
        paint.color = color
        paint.strokeWidth = (h * 0.022f).coerceAtLeast(3f)

        // Layout inside the cell
        val cellPadX = w * 0.10f
        val cellPadY = h * 0.10f
        val headBoxLeft = left + cellPadX
        val headBoxTop = top + cellPadY
        val headBoxRight = left + w - cellPadX
        val headBoxBottom = top + h - cellPadY

        val headW = headBoxRight - headBoxLeft
        val headH = headBoxBottom - headBoxTop

        // Draw the symbol box (X or check) in the upper-outer corner relative to face direction.
        val boxSize = minOf(headW, headH) * 0.45f
        val boxLeft: Float
        val boxTop = headBoxTop
        if (faceRight) {
            // Face points right -> symbol over the back of the head (left side of the cell)
            boxLeft = headBoxLeft + headW * 0.05f
        } else {
            boxLeft = headBoxRight - boxSize - headW * 0.05f
        }
        val boxRect = RectF(boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize)
        canvas.drawRect(boxRect, paint)

        when (symbol) {
            Symbol.X -> {
                canvas.drawLine(boxRect.left + boxSize * 0.18f, boxRect.top + boxSize * 0.18f,
                    boxRect.right - boxSize * 0.18f, boxRect.bottom - boxSize * 0.18f, paint)
                canvas.drawLine(boxRect.right - boxSize * 0.18f, boxRect.top + boxSize * 0.18f,
                    boxRect.left + boxSize * 0.18f, boxRect.bottom - boxSize * 0.18f, paint)
            }
            Symbol.CHECK -> {
                val p = Path()
                p.moveTo(boxRect.left + boxSize * 0.18f, boxRect.top + boxSize * 0.55f)
                p.lineTo(boxRect.left + boxSize * 0.42f, boxRect.bottom - boxSize * 0.18f)
                p.lineTo(boxRect.right - boxSize * 0.12f, boxRect.top + boxSize * 0.18f)
                canvas.drawPath(p, paint)
            }
        }

        // Head silhouette (profile facing right or left)
        val profile = Path()
        // Build a generic right-facing profile in normalized coords [0..1] x [0..1]
        // then mirror if facing left.
        val pts = listOf(
            // back of head top
            0.10f to 0.18f,
            0.05f to 0.40f,
            0.07f to 0.60f,
            0.12f to 0.78f,
            // neck (back)
            0.18f to 0.95f,
            // neck/chin transition
            0.42f to 0.98f,
            // chin
            0.50f to 0.88f,
            // mouth/jaw
            0.58f to 0.78f,
            // lips
            0.70f to 0.70f,
            // upper lip / nose base
            0.72f to 0.62f,
            // nose tip
            0.92f to 0.55f,
            // bridge of nose
            0.78f to 0.48f,
            // brow
            0.82f to 0.40f,
            // forehead curve
            0.70f to 0.22f,
            // top of head
            0.45f to 0.10f,
            // back to start
            0.20f to 0.12f,
            0.10f to 0.18f
        )

        // Use the lower 70% of the cell so the silhouette sits below the symbol box.
        val silTop = headBoxTop + headH * 0.18f
        val silH = headH * 0.82f
        val silLeft = headBoxLeft
        val silW = headW

        var first = true
        for ((nx, ny) in pts) {
            val xMapped = if (faceRight) nx else 1f - nx
            val px = silLeft + xMapped * silW
            val py = silTop + ny * silH
            if (first) {
                profile.moveTo(px, py)
                first = false
            } else {
                profile.lineTo(px, py)
            }
        }
        profile.close()
        canvas.drawPath(profile, paint)
    }
}
