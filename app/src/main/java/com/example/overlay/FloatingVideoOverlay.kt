package com.example.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView

class FloatingVideoOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: View? = null
    private var videoView: VideoView? = null
    private var onCompleteCallback: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoCloseRunnable: Runnable? = null
    private var isPlayingVideo = false
    private var currentVideoUriStr = ""

    fun playVideo(videoUriStr: String, stepOrder: Int, onComplete: () -> Unit) {
        val trimmedUri = videoUriStr.trim()

        mainHandler.post {
            dismiss()
            this.onCompleteCallback = onComplete

            val dpToPx = { dp: Int ->
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#0F172A"))
                elevation = dpToPx(8).toFloat()
            }

            // Header bar
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(dpToPx(12), dpToPx(8), dpToPx(8), dpToPx(8))
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleText = TextView(context).apply {
                text = "▶ Video Player (#$stepOrder)"
                setTextColor(Color.parseColor("#38BDF8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val closeButton = TextView(context).apply {
                text = " ✕ "
                setTextColor(Color.parseColor("#EF4444"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                setOnClickListener {
                    dismissAndComplete()
                }
            }

            header.addView(titleText)
            header.addView(closeButton)
            container.addView(header)

            // Touch drag header
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            val windowParams = WindowManager.LayoutParams(
                dpToPx(300),
                dpToPx(210),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(100)
            }

            header.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = windowParams.x
                        initialY = windowParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        windowParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager.updateViewLayout(container, windowParams)
                        } catch (e: Exception) {
                            // ignore
                        }
                        true
                    }
                    else -> false
                }
            }

            // Video / Frame content (uses weight 1f to fill remaining vertical height)
            val frameLayout = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }

            val statusText = TextView(context).apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val vView = VideoView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            }

            frameLayout.addView(vView)
            frameLayout.addView(statusText)
            container.addView(frameLayout)

            this.videoView = vView
            this.overlayView = container

            try {
                windowManager.addView(container, windowParams)
            } catch (e: Exception) {
                dismissAndComplete()
                return@post
            }

            if (trimmedUri.isEmpty()) {
                statusText.text = "🎬 Step #$stepOrder Video Action\nNo gallery video selected.\nPlaying preview card for 5s..."
                scheduleAutoClose(5000L)
            } else {
                try {
                    if (trimmedUri.startsWith("/")) {
                        vView.setVideoPath(trimmedUri)
                    } else {
                        vView.setVideoURI(Uri.parse(trimmedUri))
                    }

                    vView.setOnPreparedListener { mp ->
                        isPlayingVideo = true
                        currentVideoUriStr = trimmedUri
                        statusText.visibility = View.GONE
                        mp.isLooping = false
                        mp.start()
                    }
                    vView.setOnCompletionListener {
                        isPlayingVideo = false
                        currentVideoUriStr = ""
                        dismissAndComplete()
                    }
                    vView.setOnErrorListener { _, _, _ ->
                        isPlayingVideo = false
                        currentVideoUriStr = ""
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Unable to play gallery video:\n$trimmedUri\nClosing window in 4s..."
                        scheduleAutoClose(4000L)
                        true
                    }
                } catch (e: Exception) {
                    isPlayingVideo = false
                    currentVideoUriStr = ""
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Error loading video:\n${e.localizedMessage}\nClosing window in 4s..."
                    scheduleAutoClose(4000L)
                }
            }
        }
    }

    private fun scheduleAutoClose(delayMs: Long) {
        cancelAutoClose()
        val runnable = Runnable { dismissAndComplete() }
        autoCloseRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelAutoClose() {
        autoCloseRunnable?.let { mainHandler.removeCallbacks(it) }
        autoCloseRunnable = null
    }

    private fun dismissAndComplete() {
        cancelAutoClose()
        val callback = onCompleteCallback
        onCompleteCallback = null
        dismiss()
        callback?.invoke()
    }

    fun dismiss() {
        mainHandler.post {
            cancelAutoClose()
            isPlayingVideo = false
            currentVideoUriStr = ""
            try {
                videoView?.stopPlayback()
            } catch (e: Exception) {
                // ignore
            }
            videoView = null

            overlayView?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // ignore
                }
            }
            overlayView = null
        }
    }
}
