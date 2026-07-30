package com.example.repository

import com.example.model.ScriptModel
import kotlinx.coroutines.flow.Flow

interface ScriptRepository {
    fun getAllScripts(): Flow<List<ScriptModel>>
    suspend fun getScriptById(id: Long): ScriptModel?
    suspend fun saveScript(script: ScriptModel): Long
    suspend fun deleteScript(id: Long)
    suspend fun toggleFavorite(id: Long)
}
