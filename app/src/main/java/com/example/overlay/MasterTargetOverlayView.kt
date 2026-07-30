package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

data class MasterTargetSpec(
    val id: String,
    val label: String,
    val badgeNumber: Int,
    val colorHex: String,
    var xPx: Int,
    var yPx: Int
)

class MasterTargetOverlayView(
    context: Context,
    val windowManager: WindowManager,
    var spec: MasterTargetSpec,
    val onPositionSaved: (MasterTargetSpec) -> Unit,
    val onTargetTapped: (MasterTargetSpec) -> Unit
) : View(context) {

    private val targetRadius = 56f

    val windowParams = WindowManager.LayoutParams(
        (targetRadius * 2).toInt(),
        (targetRadius * 2).toInt(),
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = spec.xPx - targetRadius.toInt()
        y = spec.yPx - targetRadius.toInt()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = try { Color.parseColor(spec.colorHex) } catch (e: Exception) { Color.CYAN }
        alpha = 230
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 5f
    }

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A")
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 22f
        isFakeBoldText = true
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isClick = false

    fun setTouchThrough(enable: Boolean) {
        if (enable) {
            windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            windowParams.flags = windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        try {
            windowManager.updateViewLayout(this, windowParams)
        } catch (e: Exception) {
            // Ignore if layout update fails
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val r = cx - 4f

        // Circle fill
        canvas.drawCircle(cx, cy, r, bgPaint)
        // Circle border
        canvas.drawCircle(cx, cy, r, borderPaint)

        // Draw badge circle top-left
        canvas.drawCircle(cx - 20f, cy - 20f, 16f, badgePaint)
        canvas.drawText("${spec.badgeNumber}", cx - 20f, cy - 10f, badgeTextPaint)

        // Draw label text in center
        canvas.drawText(spec.label, cx, cy + 18f, labelTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = windowParams.x
                initialY = windowParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isClick = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                    isClick = false
                }

                windowParams.x = initialX + dx
                windowParams.y = initialY + dy
                try {
                    windowManager.updateViewLayout(this, windowParams)
                } catch (e: Exception) {
                    // Ignore layout update errors
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isClick) {
                    onTargetTapped(spec)
                } else {
                    val centerX = windowParams.x + targetRadius.toInt()
                    val centerY = windowParams.y + targetRadius.toInt()
                    spec = spec.copy(xPx = centerX, yPx = centerY)
                    onPositionSaved(spec)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
