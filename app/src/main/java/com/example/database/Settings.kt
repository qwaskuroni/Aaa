package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val id: Long = 1,
    val antiDetectionEnabled: Boolean = false,
    val randomOffsetMaxPx: Int = 5,
    val randomDelayMaxMs: Long = 100L,
    val vibrationFeedbackEnabled: Boolean = true,
    val soundFeedbackEnabled: Boolean = false,
    val floatingMenuAlpha: Float = 0.9f
)
