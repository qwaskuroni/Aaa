package com.example.gesture

import android.graphics.Path
import com.example.accessibility.AutoClickerAccessibilityService
import com.example.model.ClickTarget
import com.example.model.TargetType
import kotlin.random.Random

object GestureExecutor {

    suspend fun executeTarget(
        target: ClickTarget,
        randomOffsetPx: Int = 0
    ): Boolean {
        val service = AutoClickerAccessibilityService.instance ?: return false

        // Anti-detection random offset jitter calculation
        val offsetX = if (randomOffsetPx > 0) Random.nextInt(-randomOffsetPx, randomOffsetPx + 1) else 0
        val offsetY = if (randomOffsetPx > 0) Random.nextInt(-randomOffsetPx, randomOffsetPx + 1) else 0

        val startX = (target.xPx + offsetX).coerceAtLeast(0f)
        val startY = (target.yPx + offsetY).coerceAtLeast(0f)

        return when (target.type) {
            TargetType.SINGLE_TAP -> {
                service.performTap(startX, startY, target.durationMs.coerceAtLeast(50L))
            }
            TargetType.LONG_PRESS -> {
                val duration = if (target.durationMs < 500L) 1000L else target.durationMs
                service.performLongPress(startX, startY, duration)
            }
            TargetType.DOUBLE_TAP -> {
                service.performDoubleTap(startX, startY)
            }
            TargetType.SWIPE -> {
                val endX = (target.swipeEndXPx + offsetX).coerceAtLeast(0f)
                val endY = (target.swipeEndYPx + offsetY).coerceAtLeast(0f)
                service.performSwipe(startX, startY, endX, endY, target.durationMs.coerceAtLeast(300L))
            }
            TargetType.WAIT -> {
                if (target.delayMs > 0) {
                    kotlinx.coroutines.delay(target.delayMs)
                }
                true
            }
            TargetType.TEXT_INPUT -> {
                service.performTap(startX, startY, 50L)
                kotlinx.coroutines.delay(200L)
                service.inputText(target.textContent)
            }
            TargetType.CLIPBOARD_PASTE -> {
                service.performTap(startX, startY, 50L)
                kotlinx.coroutines.delay(200L)
                service.pasteClipboard()
            }
            TargetType.SYSTEM_BACK -> {
                service.performBack()
            }
            TargetType.SYSTEM_HOME -> {
                service.performHome()
            }
            TargetType.SYSTEM_RECENTS -> {
                service.performRecents()
            }
        }
    }
}
