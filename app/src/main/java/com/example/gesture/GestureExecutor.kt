package com.example.gesture

import android.graphics.Path
import com.example.accessibility.AutoClickerAccessibilityService
import com.example.model.ClickTarget
import com.example.model.TargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

object GestureExecutor {

    var videoPlayerHandler: (suspend (ClickTarget) -> Unit)? = null

    suspend fun executeTarget(
        target: ClickTarget,
        randomOffsetPx: Int = 0
    ): Boolean {
        // Trigger video overlay asynchronously if mediaUri is present or type is VIDEO_PLAY
        if (target.type == TargetType.VIDEO_PLAY || target.mediaUri.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    videoPlayerHandler?.invoke(target)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (target.type == TargetType.VIDEO_PLAY) {
            val waitTime = target.durationMs.coerceAtLeast(1000L)
            kotlinx.coroutines.delay(waitTime)
            return true
        }

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
                val resolvedText = com.example.automation.MacroContext.resolveVariables(target.textContent)
                service.inputText(resolvedText)
            }
            TargetType.CLIPBOARD_PASTE -> {
                service.performTap(startX, startY, 50L)
                kotlinx.coroutines.delay(200L)
                service.pasteClipboard()
            }
            TargetType.VIDEO_PLAY -> {
                true
            }
            TargetType.OPEN_UNREAD_CHATS -> {
                val unreadSettings = com.example.model.UnreadChatSettings(
                    minUnreadCount = target.minUnreadCount,
                    processOrder = target.processOrder,
                    maxChatsToOpen = target.maxChatsToOpen,
                    skipPinnedChats = target.skipPinnedChats,
                    skipMutedChats = target.skipMutedChats,
                    autoScroll = target.autoScroll,
                    stopAtEnd = target.stopAtEnd
                )
                com.example.automation.UnreadChatDetector.openNextUnreadChat(service, unreadSettings)
            }
            TargetType.SMART_MESSAGE_SCANNER -> {
                val scanned = com.example.automation.SmartMessageScanner.scanLatestCustomerMessage(service)
                scanned.isNotEmpty()
            }
            TargetType.XLS_SMART_REPLY -> {
                val reply = com.example.automation.XlsSmartReplyEngine.processAutoReply(
                    context = service.applicationContext,
                    excelFilePath = target.excelFilePath,
                    excelRulesContent = target.excelRulesContent,
                    scannedMessage = com.example.automation.MacroContext.scannedMessage,
                    matchThreshold = target.matchThreshold,
                    fallbackReply = target.fallbackReply
                )
                if (reply.isNotBlank()) {
                    if (startX > 0f || startY > 0f) {
                        service.performTap(startX, startY, 50L)
                        kotlinx.coroutines.delay(200L)
                    }
                    val injected = service.inputText(reply)
                    if (!injected) {
                        if (startX > 0f || startY > 0f) {
                            service.performTap(startX, startY, 50L)
                            kotlinx.coroutines.delay(200L)
                        }
                        service.inputText(reply)
                    }
                }
                reply.isNotEmpty()
            }
            TargetType.VOICE_TO_TEXT -> {
                com.example.automation.VoiceToTextEngine.executeVoiceToText(
                    service = service,
                    delayBeforeClickMs = target.voiceToTextDelayBeforeMs,
                    waitAfterClickMs = target.voiceToTextWaitAfterMs
                )
            }
            TargetType.AI_INTENT_SCANNER -> {
                com.example.automation.AiIntentScannerEngine.scanAndClassifyIntent(
                    service = service,
                    customApiKey = target.aiIntentApiKey
                )
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
