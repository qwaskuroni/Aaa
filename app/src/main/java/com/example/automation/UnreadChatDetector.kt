package com.example.automation

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accessibility.AutoClickerAccessibilityService
import com.example.model.UnreadChatSettings

data class DetectedChatRow(
    val node: AccessibilityNodeInfo,
    val bounds: Rect,
    val title: String,
    val subtitle: String,
    val badgeCount: Int,
    val signature: String,
    val isPinned: Boolean,
    val isMuted: Boolean
)

object UnreadChatDetector {

    private const val TAG = "UnreadChatDetector"

    // Set of chat signatures processed in current session
    private val processedSignatures = mutableSetOf<String>()
    private var openedCount = 0

    fun resetSession() {
        processedSignatures.clear()
        openedCount = 0
        Log.d(TAG, "UnreadChatDetector session reset")
    }

    suspend fun openNextUnreadChat(
        service: AutoClickerAccessibilityService?,
        settings: UnreadChatSettings
    ): Boolean {
        if (service == null) return false

        if (settings.maxChatsToOpen > 0 && openedCount >= settings.maxChatsToOpen) {
            Log.d(TAG, "Max chats limit reached ($openedCount / ${settings.maxChatsToOpen}). Stopping.")
            return false
        }

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Boundary limits for chat list (avoid top app bar tabs and bottom navigation bars)
        val listTopBoundary = (screenHeight * 0.12).toInt()
        val listBottomBoundary = (screenHeight * 0.92).toInt()

        // 1. First attempt: scan visible window
        var candidate = scanVisibleUnreadChat(service, settings, listTopBoundary, listBottomBoundary, screenWidth, screenHeight)

        // 2. Auto-scroll fallback if no unread chat found on current screen
        if (candidate == null && settings.autoScroll) {
            Log.d(TAG, "No unread chats visible on current screen. Auto scrolling to scan more...")
            
            val startY = if (settings.processOrder == UnreadChatSettings.ORDER_BOTTOM_TO_TOP) (screenHeight * 0.35f) else (screenHeight * 0.72f)
            val endY = if (settings.processOrder == UnreadChatSettings.ORDER_BOTTOM_TO_TOP) (screenHeight * 0.72f) else (screenHeight * 0.35f)
            val centerX = screenWidth / 2f

            val scrolled = service.performSwipe(centerX, startY, centerX, endY, 400L)
            if (scrolled) {
                kotlinx.coroutines.delay(700L) // Wait for list animation to settle
                candidate = scanVisibleUnreadChat(service, settings, listTopBoundary, listBottomBoundary, screenWidth, screenHeight)
            }
        }

        if (candidate == null) {
            Log.d(TAG, "No more unread chats found.")
            return false
        }

        // 3. Click the target unread chat row
        val centerX = candidate.bounds.centerX().toFloat()
        val centerY = candidate.bounds.centerY().toFloat()

        Log.d(TAG, "Opening unread chat: '${candidate.title}' with badge ${candidate.badgeCount} at ($centerX, $centerY)")

        // Perform click on node tree or parent
        var curr: AccessibilityNodeInfo? = candidate.node
        var actionClicked = false
        var depth = 0
        while (curr != null && depth < 4) {
            if (curr.isClickable) {
                actionClicked = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (actionClicked) break
            }
            curr = curr.parent
            depth++
        }

        // Always tap location to ensure chat row opens reliably
        service.performTap(centerX, centerY)

        // Track signature to prevent duplicate opening
        processedSignatures.add(candidate.signature)
        openedCount++

        return true
    }

    private fun scanVisibleUnreadChat(
        service: AutoClickerAccessibilityService,
        settings: UnreadChatSettings,
        topBoundary: Int,
        bottomBoundary: Int,
        screenWidth: Int,
        screenHeight: Int
    ): DetectedChatRow? {
        val root = service.rootInActiveWindow ?: return null

        val detectedRows = mutableListOf<DetectedChatRow>()

        // Find candidate list row nodes
        fun searchRows(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val rect = Rect()
            node.getBoundsInScreen(rect)

            // Filter nodes within vertical chat list bounds
            if (rect.width() > (screenWidth * 0.5) && rect.top >= topBoundary && rect.bottom <= bottomBoundary) {
                val rowInfo = extractChatRowInfo(node, settings, screenWidth)
                if (rowInfo != null && rowInfo.badgeCount >= settings.minUnreadCount) {
                    if (!processedSignatures.contains(rowInfo.signature)) {
                        val skipPinned = settings.skipPinnedChats && rowInfo.isPinned
                        val skipMuted = settings.skipMutedChats && rowInfo.isMuted
                        if (!skipPinned && !skipMuted) {
                            detectedRows.add(rowInfo)
                        }
                    }
                }
            }

            for (i in 0 until node.childCount) {
                searchRows(node.getChild(i))
            }
        }

        searchRows(root)

        if (detectedRows.isEmpty()) return null

        // Deduplicate rows with same signature or overlapping bounds
        val uniqueRows = detectedRows.distinctBy { it.signature }

        // Sort rows by process order
        return if (settings.processOrder == UnreadChatSettings.ORDER_BOTTOM_TO_TOP) {
            uniqueRows.sortedByDescending { it.bounds.top }.firstOrNull()
        } else {
            uniqueRows.sortedBy { it.bounds.top }.firstOrNull()
        }
    }

    private fun extractChatRowInfo(
        rowNode: AccessibilityNodeInfo,
        settings: UnreadChatSettings,
        screenWidth: Int
    ): DetectedChatRow? {
        val rect = Rect()
        rowNode.getBoundsInScreen(rect)

        var title = ""
        var subtitle = ""
        var badgeCount = 0
        var isPinned = false
        var isMuted = false

        fun inspectNode(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val text = node.text?.toString()?.trim() ?: ""
            val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
            val viewId = node.viewIdResourceName?.toString()?.lowercase() ?: ""

            val combined = "$text $contentDesc".lowercase()

            // Check for pin
            if (combined.contains("pin") || combined.contains("pinned") || combined.contains("পিন") || viewId.contains("pin")) {
                isPinned = true
            }

            // Check for mute
            if (combined.contains("mute") || combined.contains("muted") || combined.contains("নিশব্দ") || viewId.contains("mute")) {
                isMuted = true
            }

            // Check for unread badge node
            if (badgeCount == 0) {
                val parsedCount = parseBadgeCount(text, contentDesc, viewId, node, screenWidth)
                if (parsedCount > 0) {
                    badgeCount = parsedCount
                }
            }

            // Collect title / subtitle text for identification
            if (text.isNotEmpty() && !isTimeOrDateString(text)) {
                val countNum = text.toIntOrNull()
                if (countNum == null) {
                    if (title.isEmpty()) {
                        title = text
                    } else if (subtitle.isEmpty() && text != title) {
                        subtitle = text
                    }
                }
            }

            for (i in 0 until node.childCount) {
                inspectNode(node.getChild(i))
            }
        }

        inspectNode(rowNode)

        if (badgeCount == 0) return null

        val cleanTitle = title.ifEmpty { "Chat_${rect.top}" }
        val signature = "$cleanTitle|$subtitle|$badgeCount"

        return DetectedChatRow(
            node = rowNode,
            bounds = rect,
            title = cleanTitle,
            subtitle = subtitle,
            badgeCount = badgeCount,
            signature = signature,
            isPinned = isPinned,
            isMuted = isMuted
        )
    }

    private fun parseBadgeCount(
        text: String,
        contentDesc: String,
        viewId: String,
        node: AccessibilityNodeInfo,
        screenWidth: Int
    ): Int {
        // 1. Check if node text is purely numeric badge (e.g., "1", "2", "20", "7465")
        if (text.matches(Regex("^\\d{1,5}$"))) {
            val num = text.toIntOrNull() ?: 0
            if (num > 0) {
                // Ensure it's not a timestamp like "11" or phone number
                val nodeRect = Rect()
                node.getBoundsInScreen(nodeRect)
                // Badges are usually compact views (width < screenWidth * 0.25)
                if (nodeRect.width() > 0 && nodeRect.width() < (screenWidth * 0.25)) {
                    return num
                }
            }
        }

        // 2. Check if contentDesc or viewId indicates unread count
        val combined = "$contentDesc $viewId".lowercase()
        if (combined.contains("unread") || combined.contains("badge") || combined.contains("count") || combined.contains("msg_count")) {
            val digits = Regex("\\d+").find("$text $contentDesc")?.value
            val num = digits?.toIntOrNull() ?: 0
            if (num > 0) return num
            return 1 // Fallback to 1 unread message if badge indicator present
        }

        return 0
    }

    private fun isTimeOrDateString(str: String): Boolean {
        val lower = str.lowercase()
        if (lower.contains("am") || lower.contains("pm") || lower.contains(":")) return true
        if (lower.contains("yesterday") || lower.contains("today") || lower.contains("গতকাল")) return true
        return false
    }
}
