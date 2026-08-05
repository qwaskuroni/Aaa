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

    // Regex to match audio duration formats like "0:05", "00:15", "1:30"
    private val DURATION_REGEX = Pattern.compile("\\b\\d{1,2}:\\d{2}\\b")
    // Regex to match timestamps like "10:40 AM", "10:40", "22:15"
    private val TIMESTAMP_REGEX = Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?\\b")

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

        // 1. User-configured delay before search & click
        if (delayBeforeClickMs > 0) {
            delay(delayBeforeClickMs)
        }

        val startTime = System.currentTimeMillis()
        var targetAButton: AccessibilityNodeInfo? = null
        var currentAttempt = 0
        val maxAttempts = retryCount.coerceAtLeast(1)

        // 2. Retry loop: Search strictly for the "A" button inside the latest customer voice message
        while (currentAttempt < maxAttempts && (System.currentTimeMillis() - startTime) < searchTimeoutMs) {
            currentAttempt++
            val root = service.rootInActiveWindow
            if (root != null) {
                targetAButton = findLatestCustomerVoiceTranscribeButton(root, service)
                if (targetAButton != null) {
                    Log.d(TAG, "Found valid 'A' transcribe button on attempt $currentAttempt")
                    break
                }
            }
            if (currentAttempt < maxAttempts) {
                delay(retryIntervalMs)
            }
        }

        if (targetAButton == null) {
            Log.w(TAG, "$VOICE_TRANSCRIBE_NOT_FOUND: Could not locate verified 'A' button inside latest customer voice message after $currentAttempt attempts")
            // Return false gracefully without clicking any wrong UI element, continuing macro flow safely
            return false
        }

        // 3. Click ONLY the exact center / node of the verified "A" button exactly ONCE
        val clickSuccess = clickNodeExactCenter(service, targetAButton)
        if (!clickSuccess) {
            Log.w(TAG, "$VOICE_TRANSCRIBE_NOT_FOUND: Click action on 'A' button failed")
            return false
        }

        Log.d(TAG, "Successfully clicked 'A' transcribe button once. Waiting ${waitAfterClickMs}ms after click.")

        // 4. Configurable wait after clicking
        if (waitAfterClickMs > 0) {
            delay(waitAfterClickMs)
        }

        Log.d(TAG, "Voice To Text action completed successfully. Continuing to next macro action.")
        return true
    }

    private fun findLatestCustomerVoiceTranscribeButton(
        root: AccessibilityNodeInfo,
        service: AutoClickerAccessibilityService
    ): AccessibilityNodeInfo? {
        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Step 1: Collect all valid nodes with screen bounds
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

        // Step 2: Identify Customer Voice Message Containers
        // Customer messages are left-aligned (left < 45% screenWidth or centerX < 50% screenWidth)
        // Must contain voice indicators (duration regex or play button/audio view ID)
        // Strictly exclude: My messages (right side), text messages, stickers, images, videos, calls, blocked call notifications
        val customerVoiceContainers = allNodes.filter { wrapper ->
            val rect = wrapper.bounds
            val isLeftSide = rect.left < (screenWidth * 0.45f) || rect.centerX() < (screenWidth * 0.50f)

            if (!isLeftSide) return@filter false

            val text = wrapper.node.text?.toString()?.lowercase() ?: ""
            val desc = wrapper.node.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = wrapper.node.viewIdResourceName?.lowercase() ?: ""

            // Strict exclusion of non-voice items
            val isIgnored = desc.contains("sticker") || desc.contains("photo") || desc.contains("image") ||
                    desc.contains("video") || desc.contains("missed call") || desc.contains("blocked") ||
                    desc.contains("call ended") || text.contains("missed call") || text.contains("blocked") ||
                    text.contains("sticker") || viewId.contains("sticker") || viewId.contains("avatar") ||
                    viewId.contains("profile")

            if (isIgnored) return@filter false

            // Voice message detection logic
            val hasAudioDuration = DURATION_REGEX.matcher(wrapper.node.text ?: "").find() ||
                    DURATION_REGEX.matcher(wrapper.node.contentDescription ?: "").find()

            val hasVoiceKeywords = desc.contains("voice") || desc.contains("audio") || desc.contains("sound") ||
                    text.contains("voice") || text.contains("audio") || viewId.contains("voice") || viewId.contains("audio")

            val isPlayButton = desc.contains("play") || text == "play" || viewId.contains("play")

            val containsAButton = isAButtonTextOrDesc(text, desc, viewId)

            hasAudioDuration || (hasVoiceKeywords && isPlayButton) || (isLeftSide && containsAButton)
        }

        // Step 3: Find the LATEST (bottom-most) customer voice message container
        val latestCustomerVoiceContainer = customerVoiceContainers.maxByOrNull { it.bounds.bottom }

        // Step 4: Search ONLY inside / vertically aligned with this latest customer voice message for the "A" button
        if (latestCustomerVoiceContainer != null) {
            val containerRect = latestCustomerVoiceContainer.bounds
            val topMargin = 80
            val bottomMargin = 80

            // Filter candidate nodes inside the row
            val candidateAButtonsInRow = allNodes.filter { wrapper ->
                val r = wrapper.bounds
                // Must be vertically aligned with the latest customer voice message bubble
                val isVerticallyAligned = r.top >= (containerRect.top - topMargin) && r.bottom <= (containerRect.bottom + bottomMargin)
                // Must be inside customer area (left < 75% screen width)
                val isLeftArea = r.left < (screenWidth * 0.75f)

                if (!isVerticallyAligned || !isLeftArea) return@filter false

                // Must NOT be an excluded element (profile photo, sticker, play button, emoji, waveform, message background)
                if (isExcludedElement(wrapper, containerRect, screenWidth)) return@filter false

                val text = wrapper.node.text?.toString()?.trim() ?: ""
                val desc = wrapper.node.contentDescription?.toString()?.trim() ?: ""
                val viewId = wrapper.node.viewIdResourceName?.lowercase() ?: ""

                // Verify "A" transcribe button attributes
                isAButtonTextOrDesc(text, desc, viewId)
            }

            // Pick candidate closest to the right end of the voice bubble row (where transcribe 'A' button resides)
            val bestAInRow = candidateAButtonsInRow.minByOrNull { Math.abs(it.bounds.centerY() - containerRect.centerY()) }
            if (bestAInRow != null) {
                return bestAInRow.node
            }
        }

        // Step 5: Strict Fallback - Search screen for bottom-most verified "A" transcribe button on left side
        val verifiedAButtonsOnLeft = allNodes.filter { wrapper ->
            val r = wrapper.bounds
            val isLeftSide = r.left < (screenWidth * 0.65f)
            if (!isLeftSide) return@filter false

            if (isExcludedElement(wrapper, null, screenWidth)) return@filter false

            val text = wrapper.node.text?.toString()?.trim() ?: ""
            val desc = wrapper.node.contentDescription?.toString()?.trim() ?: ""
            val viewId = wrapper.node.viewIdResourceName?.lowercase() ?: ""

            isAButtonTextOrDesc(text, desc, viewId)
        }

        val latestVerifiedAButton = verifiedAButtonsOnLeft.maxByOrNull { it.bounds.bottom }
        if (latestVerifiedAButton != null) {
            return latestVerifiedAButton.node
        }

        // High confidence constraint: If no node strictly satisfies the "A" transcribe button criteria, return null
        return null
    }

    private fun isAButtonTextOrDesc(text: String, desc: String, viewId: String): Boolean {
        val lowerText = text.lowercase()
        val lowerDesc = desc.lowercase()

        // Match exact "A" or "a" text / content description
        if (text == "A" || text == "a" || lowerDesc == "a" || lowerText == "a") {
            return true
        }

        // Match transcribe / speech-to-text keywords
        if (lowerDesc.contains("transcribe") || lowerText.contains("transcribe") ||
            lowerDesc.contains("voice to text") || lowerText.contains("voice to text") ||
            lowerDesc.contains("speech to text") || lowerText.contains("speech to text") ||
            lowerDesc.contains("ভয়েস") || lowerText.contains("অনুবাদ")
        ) {
            return true
        }

        // Match view IDs
        if (viewId.contains("transcribe") || viewId.contains("v2t") || viewId.contains("speech_to_text")) {
            return true
        }

        return false
    }

    private fun isExcludedElement(
        wrapper: NodeInfoWrapper,
        containerRect: Rect?,
        screenWidth: Int
    ): Boolean {
        val rect = wrapper.bounds
        val text = wrapper.node.text?.toString()?.trim() ?: ""
        val desc = wrapper.node.contentDescription?.toString()?.trim() ?: ""
        val viewId = wrapper.node.viewIdResourceName?.lowercase() ?: ""
        val className = wrapper.node.className?.toString()?.lowercase() ?: ""

        val combined = "$text $desc $viewId $className".lowercase()

        // ❌ Exclude Profile Photo / Avatar (usually far left, e.g. left < 15% screenWidth or avatar view ID)
        if (rect.left < (screenWidth * 0.15f) && (viewId.contains("avatar") || viewId.contains("photo") || viewId.contains("profile") || combined.contains("avatar"))) {
            return true
        }

        // ❌ Exclude Stickers, Photos, Videos, Emoji Reactions
        if (combined.contains("sticker") || combined.contains("photo") || combined.contains("video") ||
            combined.contains("reaction") || combined.contains("emoji") || combined.contains("like")
        ) {
            return true
        }

        // ❌ Exclude Play button & Waveform
        if (combined.contains("play") || combined.contains("pause") || combined.contains("waveform") || combined.contains("seek")) {
            return true
        }

        // ❌ Exclude Timestamp / Duration text
        if (DURATION_REGEX.matcher(text).matches() || TIMESTAMP_REGEX.matcher(text).matches()) {
            return true
        }

        // ❌ Exclude Chat background / full-width container
        if (rect.width() > (screenWidth * 0.85f)) {
            return true
        }

        // ❌ Exclude extremely tiny invisible elements (< 10px)
        if (rect.width() < 10 || rect.height() < 10) {
            return true
        }

        return false
    }

    private suspend fun clickNodeExactCenter(
        service: AutoClickerAccessibilityService,
        node: AccessibilityNodeInfo
    ): Boolean {
        // Try performAction CLICK on node or clickable parent first
        var success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!success) {
            var curr = node.parent
            var depth = 0
            while (curr != null && depth < 3) {
                if (curr.isClickable && curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    success = true
                    break
                }
                curr = curr.parent
                depth++
            }
        }

        // If accessibility click is not handled directly by app, perform exact center tap gesture on bounds
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

    private data class NodeInfoWrapper(
        val node: AccessibilityNodeInfo,
        val bounds: Rect
    )
}
