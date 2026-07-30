package com.example.settings

import android.content.Context
import com.example.model.GlobalSettings
import com.example.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {

    private val repository = SettingsRepository(context)

    val settingsState: StateFlow<GlobalSettings> = repository.settings

    fun saveSettings(settings: GlobalSettings) {
        repository.updateSettings(settings)
    }
}
