package com.example.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_buttons",
    foreignKeys = [
        ForeignKey(
            entity = AutomationProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class ActionButton(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val actionType: String = "TAP",
    val label: String = "Button",
    val x: Float = 0f,
    val y: Float = 0f,
    val delayBefore: Long = 0L,
    val delayAfter: Long = 0L,
    val duration: Long = 100L,
    val textContent: String = "",
    val enabled: Boolean = true,
    val repeatCount: Int = 1
)
