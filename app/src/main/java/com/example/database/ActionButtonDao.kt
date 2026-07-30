package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionButtonDao {

    @Query("SELECT * FROM action_buttons WHERE profileId = :profileId ORDER BY id ASC")
    fun getButtonsForProfile(profileId: Long): Flow<List<ActionButton>>

    @Query("SELECT * FROM action_buttons WHERE profileId = :profileId ORDER BY id ASC")
    suspend fun getButtonsListForProfile(profileId: Long): List<ActionButton>

    @Query("SELECT * FROM action_buttons WHERE id = :id")
    suspend fun getButtonById(id: Long): ActionButton?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: ActionButton): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtons(buttons: List<ActionButton>)

    @Update
    suspend fun updateButton(button: ActionButton)

    @Delete
    suspend fun deleteButton(button: ActionButton)

    @Query("DELETE FROM action_buttons WHERE profileId = :profileId")
    suspend fun deleteButtonsForProfile(profileId: Long)
}
