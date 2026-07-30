package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionSequenceDao {

    @Query("SELECT * FROM action_sequences WHERE profileId = :profileId ORDER BY sequenceOrder ASC")
    fun getSequencesForProfile(profileId: Long): Flow<List<ActionSequence>>

    @Query("SELECT * FROM action_sequences WHERE id = :id")
    suspend fun getSequenceById(id: Long): ActionSequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSequence(sequence: ActionSequence): Long

    @Update
    suspend fun updateSequence(sequence: ActionSequence)

    @Delete
    suspend fun deleteSequence(sequence: ActionSequence)

    @Query("DELETE FROM action_sequences WHERE profileId = :profileId")
    suspend fun deleteSequencesForProfile(profileId: Long)
}
