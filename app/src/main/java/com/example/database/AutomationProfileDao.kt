package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationProfileDao {

    @Query("SELECT * FROM automation_profiles ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllProfiles(): Flow<List<AutomationProfile>>

    @Query("SELECT * FROM automation_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): AutomationProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AutomationProfile): Long

    @Update
    suspend fun updateProfile(profile: AutomationProfile)

    @Delete
    suspend fun deleteProfile(profile: AutomationProfile)

    @Query("DELETE FROM automation_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)
}
