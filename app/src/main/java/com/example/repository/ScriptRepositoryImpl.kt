package com.example.repository

import com.example.data.DataMapper
import com.example.database.ScriptDao
import com.example.model.ScriptModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScriptRepositoryImpl(private val scriptDao: ScriptDao) : ScriptRepository {

    override fun getAllScripts(): Flow<List<ScriptModel>> {
        return scriptDao.getAllScriptsWithTargets().map { list ->
            list.map { DataMapper.mapToDomain(it) }
        }
    }

    override suspend fun getScriptById(id: Long): ScriptModel? {
        val relation = scriptDao.getScriptWithTargetsById(id) ?: return null
        return DataMapper.mapToDomain(relation)
    }

    override suspend fun saveScript(script: ScriptModel): Long {
        val scriptEntity = DataMapper.mapToEntity(script)
        val targetEntities = script.targets.map { DataMapper.mapTargetToEntity(it, script.id) }
        return scriptDao.saveScriptWithTargets(scriptEntity, targetEntities)
    }

    override suspend fun deleteScript(id: Long) {
        scriptDao.deleteScriptById(id)
    }

    override suspend fun toggleFavorite(id: Long) {
        scriptDao.toggleFavorite(id)
    }
}
