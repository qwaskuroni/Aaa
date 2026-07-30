package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.ActionButton
import com.example.database.ActionSequence
import com.example.database.AppDatabase
import com.example.database.AutomationProfile
import com.example.database.Settings
import com.example.repository.AutomationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = AutomationRepository(
        profileDao = db.automationProfileDao(),
        buttonDao = db.actionButtonDao(),
        sequenceDao = db.actionSequenceDao(),
        settingsDao = db.settingsDao()
    )

    val profiles: StateFlow<List<AutomationProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<Settings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings()
        )

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    val actionButtons: StateFlow<List<ActionButton>> = _selectedProfileId.flatMapLatest { profileId ->
        if (profileId == null) {
            flowOf(emptyList())
        } else {
            repository.getButtonsForProfile(profileId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val actionSequences: StateFlow<List<ActionSequence>> = _selectedProfileId.flatMapLatest { profileId ->
        if (profileId == null) {
            flowOf(emptyList())
        } else {
            repository.getSequencesForProfile(profileId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectProfile(id: Long?) {
        _selectedProfileId.value = id
    }

    fun saveProfile(profile: AutomationProfile, buttons: List<ActionButton> = emptyList()) {
        viewModelScope.launch {
            val profileId = repository.saveProfileWithButtons(profile, buttons)
            _selectedProfileId.value = profileId
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfile(id)
            if (_selectedProfileId.value == id) {
                _selectedProfileId.value = null
            }
        }
    }

    fun toggleFavoriteProfile(profile: AutomationProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile.copy(isFavorite = !profile.isFavorite))
        }
    }

    fun addActionButton(button: ActionButton) {
        viewModelScope.launch {
            repository.insertButton(button)
        }
    }

    fun updateActionButton(button: ActionButton) {
        viewModelScope.launch {
            repository.updateButton(button)
        }
    }

    fun deleteActionButton(button: ActionButton) {
        viewModelScope.launch {
            repository.deleteButton(button)
        }
    }

    fun addActionSequence(sequence: ActionSequence) {
        viewModelScope.launch {
            repository.insertSequence(sequence)
        }
    }

    fun updateActionSequence(sequence: ActionSequence) {
        viewModelScope.launch {
            repository.updateSequence(sequence)
        }
    }

    fun deleteActionSequence(sequence: ActionSequence) {
        viewModelScope.launch {
            repository.deleteSequence(sequence)
        }
    }

    fun updateSettings(newSettings: Settings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }
}
