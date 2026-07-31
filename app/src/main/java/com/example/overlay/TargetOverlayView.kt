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
import com.example.model.ClickTarget
import com.example.model.TargetType

class TargetOverlayView(
    context: Context,
    val windowManager: WindowManager,
    var clickTarget: ClickTarget,
    var isOverlayLocked: Boolean = false,
    val onPositionChanged: (ClickTarget) -> Unit,
    val onClickTarget: (ClickTarget) -> Unit
) : View(context) {

    val windowParams = WindowManager.LayoutParams(
        clickTarget.sizePx.toInt().coerceAtLeast(48),
        clickTarget.sizePx.toInt().coerceAtLeast(48),
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
        x = clickTarget.xPx.toInt() - (clickTarget.sizePx / 2f).toInt()
        y = clickTarget.yPx.toInt() - (clickTarget.sizePx / 2f).toInt()
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = getColorForTargetType(clickTarget.type)
        alpha = 220
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val lockBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.FILL
    }

    private val activeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#00FF66")
        strokeWidth = 10f
    }

    private var activeStepOrder: Int = -1

    var isTouchThrough: Boolean = false
        private set

    fun setActiveStep(stepOrder: Int) {
        if (this.activeStepOrder != stepOrder) {
            this.activeStepOrder = stepOrder
            invalidate()
        }
    }

    fun setTouchThrough(enableTouchThrough: Boolean) {
        this.isTouchThrough = enableTouchThrough
        if (enableTouchThrough) {
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

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialPinchDist = 0f
    private var initialSizePx = 96f

    private fun getColorForTargetType(type: TargetType): Int {
        return when (type) {
            TargetType.SINGLE_TAP -> Color.parseColor("#00E5FF")
            TargetType.DOUBLE_TAP -> Color.parseColor("#38BDF8")
            TargetType.LONG_PRESS -> Color.parseColor("#818CF8")
            TargetType.SWIPE -> Color.parseColor("#F59E0B")
            TargetType.WAIT -> Color.parseColor("#64748B")
            TargetType.TEXT_INPUT -> Color.parseColor("#A855F7")
            TargetType.CLIPBOARD_PASTE -> Color.parseColor("#10B981")
            TargetType.VIDEO_PLAY -> Color.parseColor("#FF2D55")
            TargetType.OPEN_UNREAD_CHATS -> Color.parseColor("#10B981")
            TargetType.SMART_MESSAGE_SCANNER -> Color.parseColor("#3B82F6")
            TargetType.XLS_SMART_REPLY -> Color.parseColor("#8B5CF6")
            TargetType.SYSTEM_BACK,
            TargetType.SYSTEM_HOME,
            TargetType.SYSTEM_RECENTS -> Color.parseColor("#EC4899")
        }
    }

    fun updateTargetData(newTarget: ClickTarget) {
        this.clickTarget = newTarget
        val size = newTarget.sizePx.toInt().coerceAtLeast(48)
        windowParams.width = size
        windowParams.height = size
        circlePaint.color = getColorForTargetType(clickTarget.type)
        try {
            windowManager.updateViewLayout(this, windowParams)
        } catch (e: Exception) {
            // Ignore
        }
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = clickTarget.sizePx.toInt().coerceAtLeast(48)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (Math.min(width, height) / 2f) - 6f

        canvas.drawCircle(cx, cy, radius, circlePaint)
        canvas.drawCircle(cx, cy, radius, strokePaint)

        if (clickTarget.order == activeStepOrder) {
            canvas.drawCircle(cx, cy, radius - 2f, activeStrokePaint)
        }

        textPaint.textSize = radius * 0.9f
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(clickTarget.order.toString(), cx, textY, textPaint)

        if (isOverlayLocked || clickTarget.isLocked) {
            canvas.drawCircle(cx + radius * 0.6f, cy - radius * 0.6f, 10f, lockBadgePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTouchThrough || (windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) {
            return false
        }

        if (event.pointerCount == 2) {
            // Two-finger pinch to resize
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    initialPinchDist = getSpacing(event)
                    initialSizePx = clickTarget.sizePx
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newDist = getSpacing(event)
                    if (initialPinchDist > 10f) {
                        val scale = newDist / initialPinchDist
                        val newSize = (initialSizePx * scale).coerceIn(48f, 240f)
                        val updatedTarget = clickTarget.copy(sizePx = newSize)
                        updateTargetData(updatedTarget)
                        onPositionChanged(updatedTarget)
                    }
                    return true
                }
            }
        }

        val effectiveLocked = isOverlayLocked || clickTarget.isLocked

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = windowParams.x
                initialY = windowParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!effectiveLocked) {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    windowParams.x = initialX + dx
                    windowParams.y = initialY + dy
                    try {
                        windowManager.updateViewLayout(this, windowParams)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = Math.abs(event.rawX - initialTouchX)
                val dy = Math.abs(event.rawY - initialTouchY)
                if (dx < 10 && dy < 10) {
                    onClickTarget(clickTarget)
                } else if (!effectiveLocked) {
                    val halfSize = clickTarget.sizePx / 2f
                    val updatedTarget = clickTarget.copy(
                        xPx = windowParams.x + halfSize,
                        yPx = windowParams.y + halfSize
                    )
                    clickTarget = updatedTarget
                    onPositionChanged(updatedTarget)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }
}
