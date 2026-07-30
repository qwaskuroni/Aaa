package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.databinding.OverlayFloatingControlBinding

class OverlayFloatingBar(
    private val context: Context,
    private val windowManager: WindowManager,
    private val listener: Listener
) {

    interface Listener {
        fun onPlayPauseClicked()
        fun onStopClicked()
        fun onAddTargetClicked()
        fun onAddSwipeClicked()
        fun onLockToggleClicked()
        fun onRemoveTargetClicked()
        fun onCloseClicked()
    }

    private val binding: OverlayFloatingControlBinding = OverlayFloatingControlBinding.inflate(
        LayoutInflater.from(context)
    )

    val windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 50
        y = 300
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isAttached = false

    init {
        setupListeners()
        setupDragHandle()
    }

    private fun setupListeners() {
        binding.btnPlayPause.setOnClickListener { listener.onPlayPauseClicked() }
        binding.btnStop.setOnClickListener { listener.onStopClicked() }
        binding.btnAddTarget.setOnClickListener { listener.onAddTargetClicked() }
        binding.btnAddSwipe.setOnClickListener { listener.onAddSwipeClicked() }
        binding.btnLockUnlock.setOnClickListener { listener.onLockToggleClicked() }
        binding.btnRemoveTarget.setOnClickListener { listener.onRemoveTargetClicked() }
        binding.btnMinimize.setOnClickListener { listener.onCloseClicked() }
    }

    private fun setupDragHandle() {
        binding.btnDragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    windowParams.x = initialX + dx
                    windowParams.y = initialY + dy
                    if (isAttached) {
                        try {
                            windowManager.updateViewLayout(binding.root, windowParams)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun setPlayState(isPlaying: Boolean, isPaused: Boolean = false) {
        if (isPlaying || isPaused) {
            binding.btnStop.visibility = View.VISIBLE
            if (isPaused) {
                binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                binding.btnPlayPause.setColorFilter(0xFF10B981.toInt())
            } else {
                binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                binding.btnPlayPause.setColorFilter(0xFFFFB300.toInt())
            }
        } else {
            binding.btnStop.visibility = View.GONE
            binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            binding.btnPlayPause.setColorFilter(0xFF00E5FF.toInt())
        }
    }

    fun setLockState(isLocked: Boolean) {
        if (isLocked) {
            binding.btnLockUnlock.setImageResource(android.R.drawable.ic_lock_lock)
            binding.btnLockUnlock.setColorFilter(0xFFEF4444.toInt())
        } else {
            binding.btnLockUnlock.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            binding.btnLockUnlock.setColorFilter(0xFFA855F7.toInt())
        }
    }

    fun show() {
        if (!isAttached) {
            try {
                windowManager.addView(binding.root, windowParams)
                isAttached = true
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun hide() {
        if (isAttached) {
            try {
                windowManager.removeView(binding.root)
                isAttached = false
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
