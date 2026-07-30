package com.example.model

data class ScriptModel(
    val id: Long = 0,
    val name: String = "Default Clicker Script",
    val repeatCount: Int = -1, // -1 means infinite loop
    val repeatIntervalMs: Long = 500L,
    val randomOffsetPx: Int = 0, // Anti-detection random radius
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val targets: List<ClickTarget> = emptyList()
)
