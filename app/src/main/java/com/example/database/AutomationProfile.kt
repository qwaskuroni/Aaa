package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_profiles")
data class AutomationProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val repeatCount: Int = -1,
    val repeatIntervalMs: Long = 500L
)
