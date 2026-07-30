package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.model.GlobalSettings
import com.example.model.ScriptModel
import com.example.permission.PermissionUtils
import com.example.repository.ScriptRepositoryImpl
import com.example.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val scriptRepository = ScriptRepositoryImpl(db.scriptDao())
    private val settingsRepository = SettingsRepository(application)

    val scripts: StateFlow<List<ScriptModel>> = scriptRepository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<GlobalSettings> = settingsRepository.settings

    private val _hasOverlayPermission = MutableStateFlow(PermissionUtils.hasOverlayPermission(application))
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(PermissionUtils.isAccessibilityServiceEnabled(application))
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _activeScript = MutableStateFlow<ScriptModel?>(null)
    val activeScript: StateFlow<ScriptModel?> = _activeScript.asStateFlow()

    fun checkPermissions() {
        val app = getApplication<Application>()
        _hasOverlayPermission.value = PermissionUtils.hasOverlayPermission(app)
        _isAccessibilityEnabled.value = PermissionUtils.isAccessibilityServiceEnabled(app)
    }

    fun duplicateScript(script: ScriptModel) {
        viewModelScope.launch {
            val duplicate = script.copy(
                id = 0,
                name = "${script.name} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            scriptRepository.saveScript(duplicate)
        }
    }

    fun renameScript(scriptId: Long, newName: String) {
        viewModelScope.launch {
            val script = scriptRepository.getScriptById(scriptId)
            if (script != null) {
                scriptRepository.saveScript(script.copy(name = newName))
            }
        }
    }

    fun importScriptFromJson(jsonString: String): Boolean {
        val imported = com.example.utils.ScriptJsonUtils.importFromJson(jsonString)
        if (imported != null) {
            viewModelScope.launch {
                scriptRepository.saveScript(imported)
            }
            return true
        }
        return false
    }

    fun saveScript(script: ScriptModel) {
        viewModelScope.launch {
            val id = scriptRepository.saveScript(script)
            val saved = scriptRepository.getScriptById(id)
            if (saved != null) {
                _activeScript.value = saved
            }
        }
    }

    fun deleteScript(id: Long) {
        viewModelScope.launch {
            scriptRepository.deleteScript(id)
            if (_activeScript.value?.id == id) {
                _activeScript.value = null
            }
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            scriptRepository.toggleFavorite(id)
        }
    }

    fun selectActiveScript(script: ScriptModel) {
        _activeScript.value = script
    }

    fun updateSettings(newSettings: GlobalSettings) {
        settingsRepository.updateSettings(newSettings)
    }
}
