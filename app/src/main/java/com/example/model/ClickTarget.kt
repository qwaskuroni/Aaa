package com.example.model

data class ClickTarget(
    val id: Long = 0,
    val scriptId: Long = 0,
    val order: Int = 1,
    val type: TargetType = TargetType.SINGLE_TAP,
    val xPx: Float = 500f,
    val yPx: Float = 1000f,
    val swipeEndXPx: Float = 500f,
    val swipeEndYPx: Float = 500f,
    val delayMs: Long = 500L,
    val durationMs: Long = 100L,
    val label: String = "Target 1",
    val sizePx: Float = 96f,
    val isLocked: Boolean = false,
    val textContent: String = "",
    val mediaUri: String = "",
    val repeatCount: Int = 1
)
