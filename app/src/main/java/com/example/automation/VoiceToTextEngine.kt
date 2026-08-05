package com.example.automation

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accessibility.AutoClickerAccessibilityService
import kotlinx.coroutines.delay

object VoiceToTextEngine {

    private const val TAG = "VoiceToTextEngine"

    suspend fun executeVoiceToText(
        service: AutoClickerAccessibilityService?,
        delayBeforeClickMs: Long = 2000L,
        waitAfterClickMs: Long = 2000L
    ): Boolean {
        if (service == null) {
            Log.w(TAG, "ACTION_NOT_AVAILABLE: Accessibility service is null")
            return false
        }

        val root = service.rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "ACTION_NOT_AVAILABLE: Root accessibility node is null")
            return false
        }

        // 1. Detect if an "A" (Transcribe) button for voice message exists
        val transcribeButton = findTranscribeAButton(root)

        if (transcribeButton == null) {
            Log.w(TAG, "ACTION_NOT_AVAILABLE: Voice message 'A' transcribe button not found in current view")
            return false
        }

        Log.d(TAG, "Voice message 'A' transcribe button detected. Waiting ${delayBeforeClickMs}ms before click.")

        // 2. Configurable delay before clicking
        if (delayBeforeClickMs > 0) {
            delay(delayBeforeClickMs)
        }

        // 3. Click the detected "A" button exactly once
        val clicked = clickNodeOrLocation(service, transcribeButton)
        if (!clicked) {
            Log.w(TAG, "ACTION_NOT_AVAILABLE: Failed to click 'A' transcribe button")
            return false
        }

        Log.d(TAG, "Clicked 'A' transcribe button. Waiting ${waitAfterClickMs}ms after click for transcription process.")

        // 4. Configurable wait after clicking
        if (waitAfterClickMs > 0) {
            delay(waitAfterClickMs)
        }

        Log.d(TAG, "Voice To Text operation completed successfully.")
        return true
    }

    private fun findTranscribeAButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<NodeWithBounds>()

        fun traverseTree(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val rect = Rect()
            node.getBoundsInScreen(rect)

            if (rect.width() > 0 && rect.height() > 0) {
                val text = node.text?.toString()?.trim() ?: ""
                val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
                val viewId = node.viewIdResourceName?.lowercase() ?: ""

                if (isTranscribeButton(text, contentDesc, viewId, node)) {
                    candidates.add(NodeWithBounds(node, rect))
                }
            }

            for (i in 0 until node.childCount) {
                traverseTree(node.getChild(i))
            }
        }

        traverseTree(root)

        if (candidates.isEmpty()) return null

        // Bottom-most "A" transcribe button corresponds to the latest voice message in chat
        return candidates.maxByOrNull { it.bounds.bottom }?.node
    }

    private fun isTranscribeButton(
        text: String,
        contentDesc: String,
        viewId: String,
        node: AccessibilityNodeInfo
    ): Boolean {
        val lowerText = text.lowercase()
        val lowerDesc = contentDesc.lowercase()

        // Exact match for "A" transcribe button in messaging apps
        if (text == "A" || text == "a" || lowerDesc == "a") {
            return true
        }

        // Match common voice transcribe keywords or content descriptions
        if (lowerDesc.contains("transcribe") || lowerDesc.contains("voice to text") ||
            lowerDesc.contains("speech to text") || lowerDesc.contains("ভয়েস") ||
            lowerText.contains("transcribe") || lowerText.contains("অনুবাদ")
        ) {
            return true
        }

        // ViewId checks for voice transcribe
        if (viewId.contains("transcribe") || viewId.contains("voice_to_text") ||
            viewId.contains("speech_to_text") || viewId.contains("audio_transcribe")
        ) {
            return true
        }

        return false
    }

    private suspend fun clickNodeOrLocation(
        service: AutoClickerAccessibilityService,
        node: AccessibilityNodeInfo
    ): Boolean {
        // First try accessibility action click on node or parent
        var success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!success) {
            var curr = node.parent
            var depth = 0
            while (curr != null && depth < 3) {
                if (curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    success = true
                    break
                }
                curr = curr.parent
                depth++
            }
        }

        // Fallback to gesture tap on screen coordinates
        if (!success) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val cx = rect.centerX().toFloat()
                val cy = rect.centerY().toFloat()
                success = service.performTap(cx, cy, 50L)
            }
        }

        return success
    }

    private data class NodeWithBounds(
        val node: AccessibilityNodeInfo,
        val bounds: Rect
    )
}
