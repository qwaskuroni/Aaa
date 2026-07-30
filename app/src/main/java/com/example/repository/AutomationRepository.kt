package com.example.repository

import com.example.database.ActionButton
import com.example.database.ActionButtonDao
import com.example.database.ActionSequence
import com.example.database.ActionSequenceDao
import com.example.database.AutomationProfile
import com.example.database.AutomationProfileDao
import com.example.database.Settings
import com.example.database.SettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutomationRepository(
    private val profileDao: AutomationProfileDao,
    private val buttonDao: ActionButtonDao,
    private val sequenceDao: ActionSequenceDao,
    private val settingsDao: SettingsDao
) {

    val allProfiles: Flow<List<AutomationProfile>> = profileDao.getAllProfiles()

    val settingsFlow: Flow<Settings> = settingsDao.getSettings().map { it ?: Settings() }

    fun getButtonsForProfile(profileId: Long): Flow<List<ActionButton>> {
        return buttonDao.getButtonsForProfile(profileId)
    }

    suspend fun getButtonsListForProfile(profileId: Long): List<ActionButton> {
        return buttonDao.getButtonsListForProfile(profileId)
    }

    fun getSequencesForProfile(profileId: Long): Flow<List<ActionSequence>> {
        return sequenceDao.getSequencesForProfile(profileId)
    }

    suspend fun getProfileById(id: Long): AutomationProfile? {
        return profileDao.getProfileById(id)
    }

    suspend fun insertProfile(profile: AutomationProfile): Long {
        return profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: AutomationProfile) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profileId: Long) {
        profileDao.deleteProfileById(profileId)
    }

    suspend fun saveProfileWithButtons(
        profile: AutomationProfile,
        buttons: List<ActionButton>
    ): Long {
        val profileId = profileDao.insertProfile(profile)
        buttonDao.deleteButtonsForProfile(profileId)
        val updatedButtons = buttons.map { it.copy(profileId = profileId) }
        buttonDao.insertButtons(updatedButtons)
        return profileId
    }

    suspend fun insertButton(button: ActionButton): Long {
        return buttonDao.insertButton(button)
    }

    suspend fun updateButton(button: ActionButton) {
        buttonDao.updateButton(button)
    }

    suspend fun deleteButton(button: ActionButton) {
        buttonDao.deleteButton(button)
    }

    suspend fun insertSequence(sequence: ActionSequence): Long {
        return sequenceDao.insertSequence(sequence)
    }

    suspend fun updateSequence(sequence: ActionSequence) {
        sequenceDao.updateSequence(sequence)
    }

    suspend fun deleteSequence(sequence: ActionSequence) {
        sequenceDao.deleteSequence(sequence)
    }

    suspend fun getSettings(): Settings {
        return settingsDao.getSettingsDirect() ?: Settings()
    }

    suspend fun updateSettings(settings: Settings) {
        settingsDao.insertOrUpdateSettings(settings)
    }
}
