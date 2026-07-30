package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val repeatCount: Int,
    val repeatIntervalMs: Long,
    val randomOffsetPx: Int,
    val createdAt: Long,
    val isFavorite: Boolean
)
