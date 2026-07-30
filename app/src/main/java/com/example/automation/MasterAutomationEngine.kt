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

        if (isProcessing) {
            Log.d(TAG, "Engine busy processing another notification")
            return
        }

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
                logAndSetState(MasterEngineState.OpeningChat, "Triggering Notification Target...")
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

                delay(1200L) // UI render delay

                // 2. EVENT: Scan Active Chat Node via Accessibility or Scanner Target
                logAndSetState(MasterEngineState.ScanningChat, "Scanning active chat screen...")
                accessibility?.performTap(settings.scannerX.toFloat(), settings.scannerY.toFloat())
                delay(300L)

                val chatContent = scanChatWindow(accessibility)
                val detectedText = chatContent.text
                val isVoice = chatContent.isVoiceMessage
                val isNonText = chatContent.isNonTextContent

                if (isNonText) {
                    logAndSetState(MasterEngineState.NavigatingBack, "Ignored non-text message (Call/Photo/Sticker/Emoji). Navigating Back.")
                    accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                    accessibility?.performBack()
                    delay(500L)
                    resetToIdle()
                    return@launch
                }

                var messageForAi = detectedText.ifBlank { text }

                // Check for Voice / Audio message
                if (isVoice || messageForAi.lowercase().contains("voice message") || messageForAi.lowercase().contains("audio")) {
                    logAndSetState(MasterEngineState.ConvertingAudioToText, "Voice message detected. Triggering Audio-to-Text ('A' icon)...")
                    accessibility?.performTap(settings.audioToTextX.toFloat(), settings.audioToTextY.toFloat())
                    delay(1500L) // Wait for text conversion

                    // Re-scan after audio-to-text
                    val reScanned = scanChatWindow(accessibility)
                    if (reScanned.text.isNotBlank()) {
                        messageForAi = reScanned.text
                    }
                }

                if (messageForAi.isBlank()) {
                    logAndSetState(MasterEngineState.NavigatingBack, "No text content found. Navigating back.")
                    accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                    accessibility?.performBack()
                    delay(500L)
                    resetToIdle()
                    return@launch
                }

                // 3. Call OpenAI API
                logAndSetState(MasterEngineState.CallingAi(messageForAi), "Querying AI model with: '$messageForAi'")
                val aiResult = openAiService.generateReply(
                    apiKey = settings.openAiApiKey,
                    systemPrompt = settings.systemPrompt,
                    userMessage = messageForAi
                )

                if (aiResult.isSuccess) {
                    val aiReply = aiResult.getOrDefault("Hello! How can I help you?")

                    // 4. Inject AI Reply into Text Input Target
                    logAndSetState(MasterEngineState.InjectingReply(aiReply), "Injecting AI reply: '$aiReply'")
                    accessibility?.performTap(settings.textInputX.toFloat(), settings.textInputY.toFloat())
                    delay(300L)

                    val textInjected = accessibility?.inputText(aiReply) ?: false
                    if (!textInjected) {
                        Log.w(TAG, "Direct input failed, fallback tap text input target")
                    }
                    delay(400L)

                    // 5. Send Target
                    logAndSetState(MasterEngineState.SendingMessage, "Triggering Send Target...")
                    accessibility?.performTap(settings.sendX.toFloat(), settings.sendY.toFloat())
                    feedbackUtils.playClickSound()
                    delay(600L)

                    // 6. Back Navigation Target
                    logAndSetState(MasterEngineState.NavigatingBack, "Triggering Back Navigation Target...")
                    accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                    accessibility?.performBack()
                    delay(500L)

                    logAndSetState(MasterEngineState.Idle, "Master Mode pipeline completed successfully.")
                } else {
                    val err = aiResult.exceptionOrNull()?.message ?: "AI request failed"
                    logAndSetState(MasterEngineState.Error(err), "AI Error: $err")
                    delay(1000L)
                    accessibility?.performTap(settings.backX.toFloat(), settings.backY.toFloat())
                    accessibility?.performBack()
                }
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
        val isVoiceMessage: Boolean = false,
        val isNonTextContent: Boolean = false
    )

    private fun scanChatWindow(service: AutoClickerAccessibilityService?): ScannedChatResult {
        val root = service?.rootInActiveWindow ?: return ScannedChatResult()

        val textNodes = mutableListOf<String>()
        var isVoice = false
        var isNonText = false

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val nodeText = node.text?.toString()?.trim() ?: ""
            val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
            val combined = "$nodeText $contentDesc".trim()

            if (combined.isNotBlank()) {
                val lower = combined.lowercase()
                if (lower.contains("missed call") ||
                    lower.contains("photo") ||
                    lower.contains("sticker") ||
                    lower.contains("video call") ||
                    lower.contains("incoming call")
                ) {
                    isNonText = true
                }
                if (lower.contains("voice message") || lower.contains("audio") || lower == "a") {
                    isVoice = true
                }
                textNodes.add(combined)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)

        val joinedText = textNodes.takeLast(3).joinToString(" ")
        return ScannedChatResult(
            text = joinedText,
            isVoiceMessage = isVoice,
            isNonTextContent = isNonText
        )
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
