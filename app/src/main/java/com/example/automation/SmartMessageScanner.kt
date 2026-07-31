package com.example.automation

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accessibility.AutoClickerAccessibilityService

object SmartMessageScanner {

    private const val TAG = "SmartMessageScanner"

    // Regex for matching emoji-only strings
    private val EMOJI_REGEX = Regex("^[\\p{So}\\p{Cn}\\p{Cs}\\u200D\\uFE0F\\u1F600-\\u1F64F\\u1F300-\\u1F5FF\\u1F680-\\u1F6FF\\u1F700-\\u1F77F\\u1F780-\\u1F7FF\\u1F800-\\u1F8FF\\u1F900-\\u1F9FF\\u1FA00-\\u1FA6F\\u1FA70-\\u1FAFF\\u2600-\\u26FF\\u2700-\\u27BF\\s]+$")

    suspend fun scanLatestCustomerMessage(service: AutoClickerAccessibilityService?): String {
        if (service == null) return ""

        val root = service.rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "Root accessibility node is null")
            return ""
        }

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Top and bottom boundaries to ignore top action bar and bottom input box
        val topBoundary = (screenHeight * 0.10).toInt()
        val bottomBoundary = (screenHeight * 0.88).toInt()

        val textCandidates = mutableListOf<MessageBubbleInfo>()

        fun traverseTree(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val rect = Rect()
            node.getBoundsInScreen(rect)

            // Ensure node is within main chat conversation vertical view area
            if (rect.top >= topBoundary && rect.bottom <= bottomBoundary && rect.width() > 0 && rect.height() > 0) {
                val text = node.text?.toString()?.trim() ?: ""
                val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
                val viewId = node.viewIdResourceName?.toString()?.lowercase() ?: ""

                val textToEvaluate = if (text.isNotEmpty()) text else contentDesc

                if (isValidCustomerTextMessage(textToEvaluate, viewId, rect, screenWidth)) {
                    // Check if message is incoming (left-aligned or customer side) vs outgoing
                    val isOutgoing = isOutgoingMessage(node, rect, screenWidth)
                    if (!isOutgoing) {
                        textCandidates.add(
                            MessageBubbleInfo(
                                text = textToEvaluate,
                                top = rect.top,
                                bottom = rect.bottom,
                                isOutgoing = false
                            )
                        )
                    }
                }
            }

            for (i in 0 until node.childCount) {
                traverseTree(node.getChild(i))
            }
        }

        traverseTree(root)

        if (textCandidates.isEmpty()) {
            Log.d(TAG, "No valid customer text messages found in current chat view")
            return ""
        }

        // Sort candidates by Y coordinate (bottom-most is the latest message in chat)
        val latestMessage = textCandidates.maxByOrNull { it.bottom }?.text ?: ""

        Log.d(TAG, "Scanned latest customer text message: '$latestMessage'")

        // Save into MacroContext
        MacroContext.scannedMessage = latestMessage
        return latestMessage
    }

    private fun isValidCustomerTextMessage(
        text: String,
        viewId: String,
        rect: Rect,
        screenWidth: Int
    ): Boolean {
        if (text.isBlank()) return false
        if (text.length < 2 && !text.matches(Regex("^[a-zA-Z0-9অ-হ।]\$"))) return false

        val lower = text.lowercase()

        // 1. Ignore Timestamp / Date headers
        if (isTimeOrDateHeader(lower)) return false

        // 2. Ignore Audio / Voice notes
        if (lower.contains("voice message") || lower.contains("audio") || lower.contains("ভয়েস") ||
            lower.matches(Regex("^(0|1|2|3|4|5|6|7|8|9):\\d{2}$"))
        ) return false

        // 3. Ignore Calls
        if (lower.contains("missed call") || lower.contains("missed video call") ||
            lower.contains("call history") || lower.contains("মিশড কল") || lower.contains("কল")
        ) return false

        // 4. Ignore Images / Photos / Stickers / GIFs
        if (lower.contains("photo") || lower.contains("image") || lower.contains("picture") ||
            lower.contains("video") || lower.contains("sticker") || lower.contains("gif") ||
            lower.contains("ছবি") || lower.contains("ভিডিও") || lower.contains("স্টিকার") ||
            viewId.contains("image") || viewId.contains("photo") || viewId.contains("sticker")
        ) return false

        // 5. Ignore Links / URLs
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) return false

        // 6. Ignore Emoji-only messages
        if (EMOJI_REGEX.matches(text)) return false

        // 7. Ignore Documents / Files / Location / Payments / System / Reactions
        if (lower.contains("pdf") || lower.contains("document") || lower.contains("vcard") ||
            lower.contains("location") || lower.contains("map") || lower.contains("payment") ||
            lower.contains("reacted") || lower.contains("joined") || lower.contains("encryption") ||
            lower.contains("ডকুমেন্ট") || lower.contains("ফাইল") || lower.contains("অবস্থান") || lower.contains("পেমেন্ট")
        ) return false

        // 8. Ignore single letters or UI navigation strings like "A", "->A", "Back", "Send"
        if (lower == "a" || lower == "->a" || lower == "back" || lower == "send" || lower == "type a message") return false

        return true
    }

    private fun isOutgoingMessage(node: AccessibilityNodeInfo, rect: Rect, screenWidth: Int): Boolean {
        // Outgoing chat bubbles are typically positioned towards the right side of the screen
        val rightThreshold = screenWidth * 0.55
        if (rect.left > rightThreshold) {
            return true
        }

        // Check if node or parent has outgoing viewId
        var curr: AccessibilityNodeInfo? = node
        var depth = 0
        while (curr != null && depth < 3) {
            val id = curr.viewIdResourceName?.lowercase() ?: ""
            if (id.contains("out") || id.contains("me_msg") || id.contains("sent")) {
                return true
            }
            curr = curr.parent
            depth++
        }

        return false
    }

    private fun isTimeOrDateHeader(str: String): Boolean {
        if (str.matches(Regex("^(0|1)?[0-9]:[0-5][0-9]\\s*(am|pm)\$", RegexOption.IGNORE_CASE))) return true
        if (str.equals("today", ignoreCase = true) || str.equals("yesterday", ignoreCase = true) || str.equals("গতকাল", ignoreCase = true)) return true
        if (str.matches(Regex("^[0-3]?[0-9]/[0-1]?[0-9]/[0-9]{2,4}\$"))) return true
        return false
    }

    private data class MessageBubbleInfo(
        val text: String,
        val top: Int,
        val bottom: Int,
        val isOutgoing: Boolean
    )
}
