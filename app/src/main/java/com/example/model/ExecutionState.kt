package com.example.model

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(
        val currentStep: Int,
        val totalSteps: Int,
        val currentRepeat: Int,
        val totalRepeats: Int
    ) : ExecutionState()
    object Paused : ExecutionState()
    object Recording : ExecutionState()
}
