package com.example.automation

import com.example.model.ClickTarget
import com.example.model.TargetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GestureRecorder {

    private val _recordedTargets = MutableStateFlow<List<ClickTarget>>(emptyList())
    val recordedTargets: StateFlow<List<ClickTarget>> = _recordedTargets.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun startRecording() {
        _recordedTargets.value = emptyList()
        _isRecording.value = true
    }

    fun addRecordedTap(x: Float, y: Float, delayMs: Long = 500L, durationMs: Long = 100L) {
        if (!_isRecording.value) return
        val currentList = _recordedTargets.value.toMutableList()
        val nextOrder = currentList.size + 1
        val newTarget = ClickTarget(
            order = nextOrder,
            type = TargetType.SINGLE_TAP,
            xPx = x,
            yPx = y,
            delayMs = delayMs,
            durationMs = durationMs,
            label = "Tap #$nextOrder"
        )
        currentList.add(newTarget)
        _recordedTargets.value = currentList
    }

    fun addRecordedSwipe(startX: Float, startY: Float, endX: Float, endY: Float, delayMs: Long = 500L, durationMs: Long = 400L) {
        if (!_isRecording.value) return
        val currentList = _recordedTargets.value.toMutableList()
        val nextOrder = currentList.size + 1
        val newTarget = ClickTarget(
            order = nextOrder,
            type = TargetType.SWIPE,
            xPx = startX,
            yPx = startY,
            swipeEndXPx = endX,
            swipeEndYPx = endY,
            delayMs = delayMs,
            durationMs = durationMs,
            label = "Swipe #$nextOrder"
        )
        currentList.add(newTarget)
        _recordedTargets.value = currentList
    }

    fun stopRecording(): List<ClickTarget> {
        _isRecording.value = false
        return _recordedTargets.value
    }

    fun clear() {
        _recordedTargets.value = emptyList()
        _isRecording.value = false
    }
}
