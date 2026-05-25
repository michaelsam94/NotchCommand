package com.example.presentation.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View

class NotchRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var batteryPercent: Int = 100
        set(value) {
            field = value
            rateLimitedRedraw()
        }

    var isCharging: Boolean = false
        set(value) {
            field = value
            rateLimitedRedraw()
        }

    var audioLevel: Float = 0f
        set(value) {
            field = value
            rateLimitedRedraw()
        }

    // Configurable coordinates (can be synced from DataStore preferences)
    var customRadius: Float = 36f
    var customThickness: Float = 5f
    var isRgbMode: Boolean = false

    private var lastInvalidate = 0L

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        alpha = 0
    }

    private val chargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#13E280") // Bright green
    }

    private val backgroundRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(45, 255, 255, 255) // Semi-transparent overlay track
    }

    private fun rateLimitedRedraw() {
        val now = SystemClock.uptimeMillis()
        if (now - lastInvalidate > 33L) { // 30 fps cap to avoid battery drain
            lastInvalidate = now
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val r = customRadius
        val thickness = customThickness

        backgroundRingPaint.strokeWidth = thickness + 0.5f
        ringPaint.strokeWidth = thickness

        // Draw background trace ring
        canvas.drawCircle(cx, cy, r, backgroundRingPaint)

        // Select color based on battery level
        val batteryColor = when {
            isRgbMode -> {
                // Dynamic cycle
                val hue = (SystemClock.uptimeMillis() / 20L) % 360f
                Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.95f))
            }
            batteryPercent > 60 -> Color.parseColor("#13E280") // Charging/Nice green
            batteryPercent > 20 -> Color.parseColor("#FFB300") // Neon Amber
            else -> Color.parseColor("#FF3D00") // Bright Red
        }

        ringPaint.color = batteryColor

        // Draw battery sweep arc
        val rectF = RectF(cx - r, cy - r, cx + r, cy + r)
        val sweepAngle = (batteryPercent / 100f) * 360f

        canvas.drawArc(rectF, -90f, sweepAngle, false, ringPaint)

        // Audio visualizer expansion pulse
        if (audioLevel > 0.02f) {
            val audioScale = (audioLevel * 14f).coerceAtMost(24f)
            glowPaint.color = batteryColor
            glowPaint.strokeWidth = thickness * (1f + audioLevel * 2f)
            glowPaint.alpha = (audioLevel * 180 + 20).toInt().coerceIn(0, 255)

            val glowRect = RectF(
                cx - r - audioScale,
                cy - r - audioScale,
                cx + r + audioScale,
                cy + r + audioScale
            )
            canvas.drawArc(glowRect, -90f, sweepAngle, false, glowPaint)
        }

        // Charging small indicator dot at the top/center
        if (isCharging) {
            val dotRadius = (thickness * 1.0f).coerceAtLeast(6f)
            // Pulse the dot slightly
            val pulse = (SystemClock.uptimeMillis() % 1000) / 1000f
            val dynamicDotRadius = dotRadius * (1f + 0.3f * kotlin.math.sin(pulse * 2 * Math.PI).toFloat())
            canvas.drawCircle(cx, cy - r, dynamicDotRadius, chargePaint)
        }
    }
}
