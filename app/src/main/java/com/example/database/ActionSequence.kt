package com.example.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_sequences",
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
data class ActionSequence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val name: String = "Sequence 1",
    val sequenceOrder: Int = 1,
    val loopCount: Int = 1,
    val delayBetweenActionsMs: Long = 100L,
    val enabled: Boolean = true
)
