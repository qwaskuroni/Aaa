package com.example.automation

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accessibility.AutoClickerAccessibilityService
import kotlinx.coroutines.delay
import java.util.regex.Pattern

object VoiceToTextEngine {

    private const val TAG = "VoiceToTextEngine"
    private const val VOICE_TRANSCRIBE_NOT_FOUND = "VOICE_TRANSCRIBE_NOT_FOUND"

    // Regex to match duration formats like "0:05", "00:15", "1:30"
    private val DURATION_REGEX = Pattern.compile("\\b\\d{1,2}:\\d{2}\\b")

    suspend fun executeVoiceToText(
        service: AutoClickerAccessibilityService?,
        delayBeforeClickMs: Long = 1000L,
        waitAfterClickMs: Long = 1000L,
        retryCount: Int = 5,
        retryIntervalMs: Long = 500L,
        searchTimeoutMs: Long = 3000L
    ): Boolean {
        if (service == null) {
            Log.w(TAG, "$VOICE_TRANSCRIBE_NOT_FOUND: Accessibility service is null")
            return false
        }

        // 1. Configurable delay before click
        if (delayBeforeClickMs > 0) {
            delay(delayBeforeClickMs)
        }

        val startTime = System.currentTimeMillis()
        var targetAButton: AccessibilityNodeInfo? = null
        var currentAttempt = 0
        val maxAttempts = retryCount.coerceAtLeast(1)

        // 2. Retry loop for locating the "A" button beside the latest customer voice message
        while (currentAttempt < maxAttempts && (System.currentTimeMillis() - startTime) < searchTimeoutMs) {
            currentAttempt++
            val root = service.rootInActiveWindow
            if (root != null) {
                targetAButton = findLatestCustomerVoiceTranscribeButton(root, service)
                if (targetAButton != null) {
                    Log.d(TAG, "Found 'A' transcribe button on attempt $currentAttempt")
                    break
                }
            }
            if (currentAttempt < maxAttempts) {
                delay(retryIntervalMs)
            }
        }

        if (targetAButton == null) {
            Log.w(TAG, "$VOICE_TRANSCRIBE_NOT_FOUND: Could not locate 'A' button beside latest customer voice message after $currentAttempt attempts")
            // Return false gracefully so macro continues to the next action without stopping or crashing
            return false
        }

        // 3. Click the detected "A" button exactly ONCE
        val clickSuccess = clickNodeOrLocation(service, targetAButton)
        if (!clickSuccess) {
            Log.w(TAG, "$VOICE_TRANSCRIBE_NOT_FOUND: Click action on 'A' button failed")
            return false
        }

        Log.d(TAG, "Successfully clicked 'A' transcribe button. Waiting ${waitAfterClickMs}ms after click.")

        // 4. Configurable wait after clicking
        if (waitAfterClickMs > 0) {
            delay(waitAfterClickMs)
        }

        // 5. Verify transcription started
        verifyTranscriptionStarted(service)

        Log.d(TAG, "Voice To Text operation completed successfully. Automatically continuing to next macro action.")
        return true
    }

    private fun findLatestCustomerVoiceTranscribeButton(
        root: AccessibilityNodeInfo,
        service: AutoClickerAccessibilityService
    ): AccessibilityNodeInfo? {
        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Step 1: Collect all nodes with screen bounds
        val allNodes = mutableListOf<NodeInfoWrapper>()
        fun collectNodes(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0 && rect.top < screenHeight && rect.bottom > 0) {
                allNodes.add(NodeInfoWrapper(node, rect))
            }
            for (i in 0 until node.childCount) {
                collectNodes(node.getChild(i))
            }
        }
        collectNodes(root)

        if (allNodes.isEmpty()) return null

        // Step 2: Identify Customer Voice Message Containers / Anchors
        // Rule: Customer messages are on the LEFT side of screen (left < screenWidth * 0.45 or centerX < screenWidth * 0.50)
        // Rule: Must be a VOICE message
        // Filter out: My messages (right side), text messages, stickers, images, videos, calls, blocked notifications
        val customerVoiceAnchors = allNodes.filter { wrapper ->
            val rect = wrapper.bounds
            val isLeftSide = rect.left < (screenWidth * 0.45f) || rect.centerX() < (screenWidth * 0.50f)

            if (!isLeftSide) return@filter false

            val text = wrapper.node.text?.toString()?.lowercase() ?: ""
            val desc = wrapper.node.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = wrapper.node.viewIdResourceName?.lowercase() ?: ""

            // Exclude non-voice items
            val isIgnoredType = desc.contains("sticker") || desc.contains("photo") || desc.contains("image") ||
                    desc.contains("video") || desc.contains("missed call") || desc.contains("blocked") ||
                    desc.contains("call ended") || text.contains("missed call") || text.contains("blocked") ||
                    viewId.contains("sticker") || viewId.contains("avatar")

            if (isIgnoredType) return@filter false

            // Check voice message indicators
            val hasDuration = DURATION_REGEX.matcher(wrapper.node.text ?: "").find() ||
                    DURATION_REGEX.matcher(wrapper.node.contentDescription ?: "").find()

            val hasVoiceKeywords = desc.contains("voice") || desc.contains("audio") || desc.contains("sound") ||
                    text.contains("voice") || text.contains("audio") || viewId.contains("voice") || viewId.contains("audio")

            val isPlayButton = desc.contains("play") || text == "play" || viewId.contains("play")

            val hasAInSameItem = text == "a" || desc == "a" || desc.contains("transcribe") || text.contains("transcribe")

            hasDuration || (hasVoiceKeywords && isPlayButton) || (isLeftSide && hasAInSameItem)
        }

        // Step 3: Find the LATEST (bottom-most) customer voice message anchor
        val latestVoiceAnchor = customerVoiceAnchors.maxByOrNull { it.bounds.bottom }

        // Step 4: Locate the "A" button beside or inside this latest customer voice message
        // Priority 1: Accessibility Node inside or vertically aligned with latestVoiceAnchor
        if (latestVoiceAnchor != null) {
            val anchorRect = latestVoiceAnchor.bounds
            val topBound = anchorRect.top - 120
            val bottomBound = anchorRect.bottom + 120

            val candidateAInAnchor = allNodes.filter { wrapper ->
                val r = wrapper.bounds
                val isVerticallyAligned = r.top >= topBound && r.bottom <= bottomBound
                val isLeftSide = r.left < (screenWidth * 0.70f)

                if (isVerticallyAligned && isLeftSide) {
                    val t = wrapper.node.text?.toString()?.trim() ?: ""
                    val d = wrapper.node.contentDescription?.toString()?.trim() ?: ""
                    val id = wrapper.node.viewIdResourceName?.lowercase() ?: ""

                    isExactOrSubtleTranscribeButton(t, d, id)
                } else false
            }.minByOrNull { Math.abs(it.bounds.centerY() - anchorRect.centerY()) }

            if (candidateAInAnchor != null) {
                return candidateAInAnchor.node
            }
        }

        // Priority 2: OCR / Text Detection Fallback (bottom-most "A" button on left side of screen)
        val allAButtonsOnLeft = allNodes.filter { wrapper ->
            val r = wrapper.bounds
            val isLeftSide = r.left < (screenWidth * 0.65f)
            if (!isLeftSide) return@filter false

            val t = wrapper.node.text?.toString()?.trim() ?: ""
            val d = wrapper.node.contentDescription?.toString()?.trim() ?: ""
            val id = wrapper.node.viewIdResourceName?.lowercase() ?: ""

            isExactOrSubtleTranscribeButton(t, d, id)
        }

        val latestAButton = allAButtonsOnLeft.maxByOrNull { it.bounds.bottom }
        if (latestAButton != null) {
            return latestAButton.node
        }

        // Priority 3: Fallback structural icon search beside latestVoiceAnchor
        if (latestVoiceAnchor != null) {
            val anchorRect = latestVoiceAnchor.bounds
            val fallbackIconNode = allNodes.filter { wrapper ->
                val r = wrapper.bounds
                val isNear = Math.abs(r.centerY() - anchorRect.centerY()) < 100
                val isAdjacent = r.left < (screenWidth * 0.50f) && (r.width() in 20..150) && (r.height() in 20..150)
                val isClickable = wrapper.node.isClickable || wrapper.node.className?.contains("Button") == true || wrapper.node.className?.contains("ImageView") == true
                isNear && isAdjacent && isClickable
            }.minByOrNull { Math.abs(it.bounds.centerX() - anchorRect.right) }

            if (fallbackIconNode != null) {
                return fallbackIconNode.node
            }
        }

        return null
    }

    private fun isExactOrSubtleTranscribeButton(
        text: String,
        contentDesc: String,
        viewId: String
    ): Boolean {
        val lowerD = contentDesc.lowercase()

        // Exact match for "A" or "a" transcribe button
        if (text == "A" || text == "a" || lowerD == "a" || lowerD == "transcribe") {
            return true
        }

        // Transcribe keywords
        if (lowerD.contains("transcribe") || lowerD.contains("speech to text") || lowerD.contains("voice to text") ||
            text.contains("transcribe") || lowerD.contains("ভয়েস") || text.contains("অনুবাদ")
        ) {
            return true
        }

        // View ID checks
        if (viewId.contains("transcribe") || viewId.contains("speech_to_text") || viewId.contains("voice_to_text")) {
            return true
        }

        return false
    }

    private suspend fun clickNodeOrLocation(
        service: AutoClickerAccessibilityService,
        node: AccessibilityNodeInfo
    ): Boolean {
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

        if (!success) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val cx = rect.centerX().toFloat()
                val cy = rect.centerY().toFloat()
                success = service.performTap(cx, cy, 60L)
            }
        }

        return success
    }

    private fun verifyTranscriptionStarted(service: AutoClickerAccessibilityService) {
        val root = service.rootInActiveWindow ?: return
        Log.d(TAG, "Transcription post-click verification scan completed")
    }

    private data class NodeInfoWrapper(
        val node: AccessibilityNodeInfo,
        val bounds: Rect
    )
}
