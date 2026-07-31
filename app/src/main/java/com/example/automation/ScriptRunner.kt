package com.example.automation

import com.example.gesture.GestureExecutor
import com.example.model.ExecutionState
import com.example.model.GlobalSettings
import com.example.model.ScriptModel
import com.example.utils.FeedbackUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class ScriptRunner(private val feedbackUtils: FeedbackUtils) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var runnerJob: Job? = null

    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    private var isPaused = false

    fun startScript(script: ScriptModel, settings: GlobalSettings) {
        stopScript()
        isPaused = false
        com.example.automation.UnreadChatDetector.resetSession()
        com.example.automation.MacroContext.reset()

        runnerJob = scope.launch {
            val targets = script.targets
            if (targets.isEmpty()) {
                _executionState.value = ExecutionState.Idle
                return@launch
            }

            var currentRepeat = 0
            val totalRepeats = script.repeatCount // -1 means infinite

            while (runnerJob?.isActive == true) {
                if (totalRepeats != -1 && currentRepeat >= totalRepeats) {
                    break
                }

                currentRepeat++

                for (index in targets.indices) {
                    while (isPaused && runnerJob?.isActive == true) {
                        _executionState.value = ExecutionState.Paused
                        delay(200L)
                    }

                    if (runnerJob?.isActive != true) break

                    val target = targets[index]

                    // Wait for delayBefore (target.delayMs) before executing action
                    var delayBefore = target.delayMs.coerceAtLeast(10L)
                    if (settings.antiDetectionEnabled && settings.randomDelayMaxMs > 0) {
                        delayBefore += Random.nextLong(0, settings.randomDelayMaxMs)
                    }

                    var elapsed = 0L
                    val stepInterval = 100L
                    while (elapsed < delayBefore && runnerJob?.isActive == true) {
                        while (isPaused && runnerJob?.isActive == true) {
                            _executionState.value = ExecutionState.Paused
                            delay(200L)
                        }
                        if (runnerJob?.isActive != true) break
                        val chunk = Math.min(stepInterval, delayBefore - elapsed)
                        delay(chunk)
                        elapsed += chunk
                    }

                    if (runnerJob?.isActive != true) break

                    _executionState.value = ExecutionState.Running(
                        currentStep = index + 1,
                        totalSteps = targets.size,
                        currentRepeat = currentRepeat,
                        totalRepeats = totalRepeats
                    )

                    val randomOffsetPx = if (settings.antiDetectionEnabled) {
                        script.randomOffsetPx.coerceAtLeast(settings.randomOffsetMaxPx)
                    } else 0

                    if (settings.vibrationFeedbackEnabled) {
                        feedbackUtils.vibrate(20L)
                    }
                    if (settings.soundFeedbackEnabled) {
                        feedbackUtils.playClickSound()
                    }

                    GestureExecutor.executeTarget(target, randomOffsetPx)
                }

                // Delay between loop repetitions
                var loopDelayMs = script.repeatIntervalMs.coerceAtLeast(100L)
                if (settings.antiDetectionEnabled && settings.randomDelayMaxMs > 0) {
                    loopDelayMs += Random.nextLong(0, settings.randomDelayMaxMs)
                }
                delay(loopDelayMs)
            }

            _executionState.value = ExecutionState.Idle
        }
    }

    fun pauseScript() {
        isPaused = true
        _executionState.value = ExecutionState.Paused
    }

    fun resumeScript() {
        isPaused = false
    }

    fun stopScript() {
        runnerJob?.cancel()
        runnerJob = null
        isPaused = false
        _executionState.value = ExecutionState.Idle
    }
}
