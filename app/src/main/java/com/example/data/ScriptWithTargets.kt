package com.example.data

import androidx.room.Embedded
import androidx.room.Relation
import com.example.database.ScriptEntity
import com.example.database.ScriptTargetEntity

data class ScriptWithTargets(
    @Embedded val script: ScriptEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scriptId"
    )
    val targets: List<ScriptTargetEntity>
)
