package com.colewinfield.liftingtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE liftId = :liftId ORDER BY date DESC")
    fun observeNotesForLift(liftId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE liftId IN (:liftIds) ORDER BY date DESC")
    fun observeNotesForLifts(liftIds: List<String>): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE sessionId = :sessionId AND liftId = :liftId ORDER BY date ASC")
    suspend fun notesForSessionLift(sessionId: String, liftId: String): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}
