package com.example.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "script_targets",
    foreignKeys = [
        ForeignKey(
            entity = ScriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scriptId"])]
)
data class ScriptTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val targetOrder: Int,
    val type: String,
    val xPx: Float,
    val yPx: Float,
    val swipeEndXPx: Float,
    val swipeEndYPx: Float,
    val delayMs: Long,
    val durationMs: Long,
    val label: String,
    val sizePx: Float = 96f,
    val isLocked: Boolean = false,
    val textContent: String = "",
    val mediaUri: String = "",
    val repeatCount: Int = 1,
    val minUnreadCount: Int = 1,
    val processOrder: String = "TOP_TO_BOTTOM",
    val maxChatsToOpen: Int = 0,
    val skipPinnedChats: Boolean = false,
    val skipMutedChats: Boolean = false,
    val autoScroll: Boolean = true,
    val stopAtEnd: Boolean = true,
    val excelFilePath: String = "",
    val excelRulesContent: String = "",
    val matchThreshold: Float = 0.3f,
    val fallbackReply: String = ""
)
