package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutoClickerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AutoClickerAccessibilityService Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing if node inspection is required
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoClickerAccessibilityService Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    suspend fun performTap(x: Float, y: Float, durationMs: Long = 50L): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        performGesturePath(path, durationMs.coerceAtLeast(10L))
    }

    suspend fun performDoubleTap(x: Float, y: Float): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path1 = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        val res1 = performGesturePath(path1, 50L)
        kotlinx.coroutines.delay(100L)
        val path2 = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        val res2 = performGesturePath(path2, 50L)
        res1 && res2
    }

    suspend fun performLongPress(x: Float, y: Float, durationMs: Long = 1000L): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        performGesturePath(path, durationMs.coerceAtLeast(500L))
    }

    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        performGesturePath(path, durationMs.coerceAtLeast(100L))
    }

    suspend fun performGesturePath(path: Path, durationMs: Long): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(1L))
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(stroke)
            val gesture = gestureBuilder.build()

            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            val dispatched = dispatchGesture(gesture, callback, null)
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    suspend fun inputText(text: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        if (text.isEmpty()) return@withContext true

        var success = false
        for (attempt in 1..3) {
            val root = rootInActiveWindow
            val focusedNode = findFocusedEditableNode(root)

            if (focusedNode != null) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                if (success) break
            }
            kotlinx.coroutines.delay(150L)
        }

        if (!success) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AutoClickerInput", text)
                clipboard.setPrimaryClip(clip)
                success = pasteClipboard()
            } catch (e: Exception) {
                Log.e(TAG, "Failed text input fallback", e)
            }
        }

        success
    }

    suspend fun pasteClipboard(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val root = rootInActiveWindow
        val focusedNode = findFocusedEditableNode(root)

        focusedNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE) ?: false
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocus != null) return inputFocus

        val accessibilityFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (accessibilityFocus != null) return accessibilityFocus

        return searchFocusedRecursively(root)
    }

    private fun searchFocusedRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused || node.isAccessibilityFocused) return node
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchFocusedRecursively(child)
            if (result != null) return result
        }
        return null
    }

    suspend fun performBack(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    suspend fun performHome(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    suspend fun performRecents(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    companion object {
        @Volatile
        var instance: AutoClickerAccessibilityService? = null
            private set

        private const val TAG = "AutoClickerAccessibility"
    }
}
