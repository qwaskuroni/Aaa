package com.example.automation

import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accessibility.AutoClickerAccessibilityService
import com.example.model.MasterModeSettings
import com.example.repository.MasterModeRepository
import com.example.utils.FeedbackUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MasterEngineState {
    object Idle : MasterEngineState()
    data class NotificationReceived(val packageName: String, val sender: String, val text: String) : MasterEngineState()
    object OpeningChat : MasterEngineState()
    object ScanningChat : MasterEngineState()
    object ConvertingAudioToText : MasterEngineState()
    data class CallingAi(val query: String) : MasterEngineState()
    data class InjectingReply(val reply: String) : MasterEngineState()
    object SendingMessage : MasterEngineState()
    object NavigatingBack : MasterEngineState()
    data class Error(val message: String) : MasterEngineState()
}

class MasterAutomationEngine private constructor(private val context: Context) {

    private val repository = MasterModeRepository(context)
    private val openAiService = OpenAiService()
    private val feedbackUtils = FeedbackUtils(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _engineState = MutableStateFlow<MasterEngineState>(MasterEngineState.Idle)
    val engineState: StateFlow<MasterEngineState> = _engineState.asStateFlow()

    private val _lastLogMessage = MutableStateFlow("Master Mode Engine Initialized")
    val lastLogMessage: StateFlow<String> = _lastLogMessage.asStateFlow()

    @Volatile
    private var isProcessing = false

    private var lastProcessedTitle = ""
    private var lastProcessedText = ""
    private var lastProcessedTime = 0L

    fun onImoNotificationReceived(
        packageName: String,
        title: String,
        text: String,
        sbn: StatusBarNotification
    ) {
        val settings = repository.settings.value
        if (!settings.isEnabled && !isMasterModeActive) {
            Log.d(TAG, "Master Mode disabled, ignoring notification")
            return
        }

        // Filter out empty or system status notifications
        if (title.isBlank() && text.isBlank()) return
        val lowerText = text.lowercase()
        if (lowerText.contains("running in background") ||
            lowerText.contains("back-ground") ||
            lowerText.contains("tap for settings")
        ) {
            return
        }

        // Debounce duplicate notifications received within 4 seconds
        val now = System.currentTimeMillis()
        if (title == lastProcessedTitle && text == lastProcessedText && (now - lastProcessedTime) < 4000L) {
            Log.d(TAG, "Duplicate notification ignored: $title - $text")
            return
        }

        if (isProcessing) {
            Log.d(TAG, "Engine busy processing another notification")
            return
        }

        lastProcessedTitle = title
        lastProcessedText = text
        lastProcessedTime = now

        val pendingIntent = sbn.notification.contentIntent
        processImoPipeline(packageName, title, text, pendingIntent)
    }

    fun processImoPipeline(
        packageName: String,
        title: String,
        text: String,
        pendingIntent: PendingIntent? = null
    ) {
        if (isProcessing) return
        isProcessing = true

        scope.launch {
            val settings = repository.settings.value
            try {
                logAndSetState(
                    MasterEngineState.NotificationReceived(packageName, title, text),
                    "IMO Notification from $title: $text"
                )

                // 1. EVENT: Trigger Notification Target to open chat window
                logAndSetState(MasterEngineState.OpeningChat, "Opening chat screen...")
                val accessibility = AutoClickerAccessibilityService.instance

                if (pendingIntent != null) {
                    try {
                        pendingIntent.send()
                    } catch (e: Exception) {
                        Log.e(TAG, "PendingIntent send failed, tapping notification coordinates", e)
                        accessibility?.performTap(settings.notificationX.toFloat(), settings.notificationY.toFloat())
                    }
                } else {
                    accessibility?.performTap(settings.notificationX.toFloat(), settings.notificationY.toFloat())
                }

                delay(1200L) // Wait for chat window to render

                // Check for Voice / Audio message before scanning
                val notifLower = text.lowercase()
                val isAudioNotification = notifLower.contains("voice message") ||
                        notifLower.contains("audio") ||
                        text.contains("ভয়েস") ||
                        notifLower.contains("0:") || notifLower.contains("1:")

                val hasVoiceInWindow = checkForVoiceInWindow(accessibility)

                if (isAudioNotification || hasVoiceInWindow) {
                    logAndSetState(MasterEngineState.ConvertingAudioToText, "Voice message detected. Triggering Audio 'A' button...")
                    findAndClickAudioToTextButton(accessibility, settings.audioToTextX, settings.audioToTextY)
                    delay(1200L) // Wait 1 second for audio-to-text conversion to finish
                }

                // 2. EVENT: Scan Active Chat Node via Accessibility or Scanner Target
                logAndSetState(MasterEngineState.ScanningChat, "Scanning active chat screen...")
                accessibility?.performTap(settings.scannerX.toFloat(), settings.scannerY.toFloat())
                delay(300L)

                var chatContent = scanChatWindow(accessibility, settings.scannerX, settings.scannerY)

                // Secondary check: if voice detected during scan and not yet converted
                if (chatContent.isVoiceMessage && !isAudioNotification && !hasVoiceInWindow) {
                    logAndSetState(MasterEngineState.ConvertingAudioToText, "Audio detected during scan. Clicking Audio 'A' button...")
                    findAndClickAudioToTextButton(accessibility, settings.audioToTextX, settings.audioToTextY)
                    delay(1200L)
                    chatContent = scanChatWindow(accessibility, settings.scannerX, settings.scannerY)
                }

                val scannedText = chatContent.text
                val isVoice = chatContent.isVoiceMessage

                // Message for AI: Prefer scanned text from screen, fallback to notification text
                var messageForAi = scannedText.ifBlank { text }.trim()

                // Check if the message is non-text (Photo, Sticker, Call)
                val isNonText = isNonTextMessage(text) || (scannedText.isNotBlank() && isNonTextMessage(scannedText))
                if (isNonText && !isVoice) {
                    logAndSetState(MasterEngineState.NavigatingBack, "Non-text message detected (Call/Photo/Sticker). Navigating Back.")
                    accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                    accessibility?.performBack()
                    delay(500L)
                    resetToIdle()
                    return@launch
                }

                if (messageForAi.isBlank()) {
                    logAndSetState(MasterEngineState.NavigatingBack, "No text content found to reply. Navigating back.")
                    accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                    accessibility?.performBack()
                    delay(500L)
                    resetToIdle()
                    return@launch
                }

                // 3. Call AI API or Fallback Auto-Reply
                logAndSetState(MasterEngineState.CallingAi(messageForAi), "Querying AI for: '$messageForAi'")

                val aiReply: String = if (settings.openAiApiKey.isNotBlank()) {
                    val aiResult = openAiService.generateReply(
                        apiKey = settings.openAiApiKey,
                        systemPrompt = settings.systemPrompt,
                        userMessage = messageForAi
                    )
                    if (aiResult.isSuccess) {
                        aiResult.getOrDefault("ধন্যবাদ! আমি মেসেজ পেয়েছি।")
                    } else {
                        val err = aiResult.exceptionOrNull()?.message ?: "AI request failed"
                        Log.w(TAG, "AI Call failed ($err), using default response")
                        "ধন্যবাদ, আমি এই মুহূর্তে ব্যস্ত আছি। পরে কথা বলছি!"
                    }
                } else {
                    Log.i(TAG, "OpenAI API key missing, using smart default reply")
                    "ধন্যবাদ, আমি এই মুহূর্তে ব্যস্ত আছি। পরে কথা বলছি!"
                }

                // 4. Inject AI Reply into Text Input Target
                logAndSetState(MasterEngineState.InjectingReply(aiReply), "Injecting reply: '$aiReply'")
                accessibility?.performTap(settings.textInputX.toFloat(), settings.textInputY.toFloat())
                delay(400L)

                val textInjected = accessibility?.inputText(aiReply) ?: false
                if (!textInjected) {
                    Log.w(TAG, "Direct input failed, retrying tap & input")
                    accessibility?.performTap(settings.textInputX.toFloat(), settings.textInputY.toFloat())
                    delay(300L)
                    accessibility?.inputText(aiReply)
                }
                delay(500L)

                // 5. Send Target
                logAndSetState(MasterEngineState.SendingMessage, "Triggering Send Target...")
                accessibility?.performTap(settings.sendX.toFloat(), settings.sendY.toFloat())
                feedbackUtils.playClickSound()
                delay(800L) // Wait for message to be sent

                // 6. Back Navigation Target
                logAndSetState(MasterEngineState.NavigatingBack, "Triggering Back Navigation...")
                accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                accessibility?.performBack()
                delay(500L)

                logAndSetState(MasterEngineState.Idle, "Master Mode pipeline completed successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error during Master Automation Pipeline", e)
                logAndSetState(MasterEngineState.Error(e.localizedMessage ?: "Pipeline error"), "Pipeline error: ${e.message}")
            } finally {
                resetToIdle()
            }
        }
    }

    private data class ScannedChatResult(
        val text: String = "",
        val isVoiceMessage: Boolean = false
    )

    private fun isNonTextMessage(str: String): Boolean {
        if (str.isBlank()) return false
        val lower = str.lowercase().trim()
        val exactNonTextKeywords = listOf(
            "sent a photo", "photo", "sticker", "sent a sticker",
            "missed call", "missed video call", "missed audio call",
            "sent a video", "gif"
        )
        return exactNonTextKeywords.any { lower == it || lower.startsWith(it) }
    }

    private data class ChatNodeItem(
        val text: String,
        val top: Int,
        val centerX: Int,
        val isIncoming: Boolean
    )

    private fun scanChatWindow(
        service: AutoClickerAccessibilityService?,
        scannerX: Int = 0,
        scannerY: Int = 0
    ): ScannedChatResult {
        val root = service?.rootInActiveWindow ?: return ScannedChatResult()

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Filter out system UI, headers, profile info, and action buttons
        val headerIgnoreList = listOf(
            "video call", "voice call", "call", "search", "back", "imo",
            "more options", "online", "typing...", "last seen", "details",
            "group info", "tap for settings", "type a message", "type message",
            "message", "sent", "delivered", "read", "seen", "yesterday", "today",
            "from \"phone number\"", "safety tools", "block", "accept",
            "common contacts", "common groups"
        )

        var isVoice = false
        val allChatNodes = mutableListOf<ChatNodeItem>()

        // Chat vertical boundaries (ignore action bar & profile header at top, ignore input box at bottom)
        val topBoundary = (screenHeight * 0.10).toInt()
        val bottomBoundary = (screenHeight * 0.86).toInt()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val nodeText = node.text?.toString()?.trim() ?: ""
            val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
            val rawCombined = "$nodeText $contentDesc".trim()

            if (rawCombined.isNotBlank()) {
                val lower = rawCombined.lowercase()

                if (lower.contains("voice message") || lower.contains("audio") || lower.contains("ভয়েস") || lower == "a") {
                    isVoice = true
                }

                val isHeaderOrSystem = headerIgnoreList.any { lower == it || lower.startsWith(it) }

                if (!isHeaderOrSystem) {
                    val rect = android.graphics.Rect()
                    node.getBoundsInScreen(rect)

                    // Keep nodes strictly inside the middle chat message area
                    if (rect.top >= topBoundary && rect.bottom <= bottomBoundary && rect.height() > 10) {
                        // Clean trailing timestamps from message string (e.g., "আমি কাজকরতে চাই 9:32 AM")
                        val cleanText = rawCombined
                            .replace(Regex("\\s*\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?.*$"), "")
                            .trim()

                        if (cleanText.isNotBlank() && !cleanText.matches(Regex("^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|জানু|ফেব্রু|মার্চ|এপ্রিল|মে|জুন|জুলাই|আগস্ট|সেপ্ট|অক্টো|নভে|ডিসে).*$", RegexOption.IGNORE_CASE))) {
                            val centerX = rect.centerX()
                            // Incoming customer messages are left-aligned (centerX < 52% of screen width)
                            val isIncoming = centerX < (screenWidth * 0.52)

                            allChatNodes.add(
                                ChatNodeItem(
                                    text = cleanText,
                                    top = rect.top,
                                    centerX = centerX,
                                    isIncoming = isIncoming
                                )
                            )
                        }
                    }
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)

        // Sort nodes vertically from top to bottom
        allChatNodes.sortBy { it.top }

        // Find the index of my last outgoing message
        val lastOutgoingIndex = allChatNodes.indexOfLast { !it.isIncoming }

        // Extract customer messages sent AFTER my last response
        val customerNodesToProcess = if (lastOutgoingIndex != -1) {
            allChatNodes.subList(lastOutgoingIndex + 1, allChatNodes.size).filter { it.isIncoming }
        } else {
            allChatNodes.filter { it.isIncoming }
        }

        // Fallback if no new customer messages were found after last outgoing index
        val finalCustomerNodes = if (customerNodesToProcess.isNotEmpty()) {
            customerNodesToProcess
        } else {
            allChatNodes.filter { it.isIncoming }.takeLast(5)
        }

        val finalScannedText = finalCustomerNodes.joinToString(" ") { it.text }.trim()

        return ScannedChatResult(
            text = finalScannedText,
            isVoiceMessage = isVoice
        )
    }

    private fun checkForVoiceInWindow(service: AutoClickerAccessibilityService?): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        var voiceFound = false

        fun search(node: AccessibilityNodeInfo?) {
            if (node == null || voiceFound) return
            val text = node.text?.toString()?.trim() ?: ""
            val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
            val combined = "$text $contentDesc".lowercase()

            if (combined.contains("voice message") ||
                combined.contains("audio") ||
                combined.contains("ভয়েস") ||
                combined == "a" || combined == "->a" || combined.contains("→a") ||
                combined.matches(Regex(".*\\d{1,2}:\\d{2}.*"))
            ) {
                voiceFound = true
                return
            }

            for (i in 0 until node.childCount) {
                search(node.getChild(i))
                if (voiceFound) return
            }
        }

        search(root)
        return voiceFound
    }

    private suspend fun findAndClickAudioToTextButton(
        service: AutoClickerAccessibilityService?,
        fallbackX: Int,
        fallbackY: Int
    ): Boolean {
        val root = service?.rootInActiveWindow
        var targetNode: AccessibilityNodeInfo? = null
        var targetRect = android.graphics.Rect()

        if (root != null) {
            fun searchTree(node: AccessibilityNodeInfo?) {
                if (node == null || targetNode != null) return

                val text = node.text?.toString()?.trim() ?: ""
                val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
                val viewId = node.viewIdResourceName?.toString() ?: ""

                val isAButton = text.equals("A", ignoreCase = true) ||
                        text.equals("->A", ignoreCase = true) ||
                        text.equals("-> A", ignoreCase = true) ||
                        text.contains("→A") ||
                        text.contains("文A") ||
                        contentDesc.equals("A", ignoreCase = true) ||
                        contentDesc.contains("transcribe", ignoreCase = true) ||
                        viewId.contains("transcribe") ||
                        viewId.contains("stt")

                if (isAButton) {
                    val rect = android.graphics.Rect()
                    node.getBoundsInScreen(rect)
                    if (rect.width() > 0 && rect.height() > 0 && rect.top > 100) {
                        targetNode = node
                        targetRect = rect
                        return
                    }
                }

                for (i in 0 until node.childCount) {
                    searchTree(node.getChild(i))
                    if (targetNode != null) return
                }
            }

            searchTree(root)
        }

        if (targetNode != null) {
            val centerX = targetRect.centerX().toFloat()
            val centerY = targetRect.centerY().toFloat()
            Log.d(TAG, "Audio 'A' button node detected at ($centerX, $centerY). Triggering click...")
            targetNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            service?.performTap(centerX, centerY)
            return true
        }

        if (fallbackX > 0 && fallbackY > 0) {
            Log.d(TAG, "Audio 'A' button accessibility node not found, tapping fallback target ($fallbackX, $fallbackY)")
            service?.performTap(fallbackX.toFloat(), fallbackY.toFloat())
            return true
        }

        return false
    }

    private fun logAndSetState(state: MasterEngineState, message: String) {
        _engineState.value = state
        _lastLogMessage.value = message
        Log.d(TAG, message)
    }

    private fun resetToIdle() {
        isProcessing = false
        _engineState.value = MasterEngineState.Idle
    }

    companion object {
        @Volatile
        private var INSTANCE: MasterAutomationEngine? = null

        @Volatile
        var isMasterModeActive: Boolean = false

        fun getInstance(context: Context): MasterAutomationEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = MasterAutomationEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private const val TAG = "MasterAutomationEngine"
    }
}
