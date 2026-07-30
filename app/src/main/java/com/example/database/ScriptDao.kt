package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.ScriptWithTargets
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Transaction
    @Query("SELECT * FROM scripts ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllScriptsWithTargets(): Flow<List<ScriptWithTargets>>

    @Transaction
    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptWithTargetsById(id: Long): ScriptWithTargets?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<ScriptTargetEntity>)

    @Query("DELETE FROM script_targets WHERE scriptId = :scriptId")
    suspend fun deleteTargetsForScript(scriptId: Long)

    @Transaction
    suspend fun saveScriptWithTargets(script: ScriptEntity, targets: List<ScriptTargetEntity>): Long {
        val scriptId = insertScript(script)
        deleteTargetsForScript(scriptId)
        val updatedTargets = targets.map { it.copy(scriptId = scriptId) }
        insertTargets(updatedTargets)
        return scriptId
    }

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Long)

    @Query("UPDATE scripts SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)
}
