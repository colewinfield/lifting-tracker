package com.colewinfield.liftingtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Transaction
    @Query("SELECT * FROM programs LIMIT 1")
    fun observeFirstProgram(): Flow<ProgramWithStructure?>

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<DayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifts(lifts: List<LiftEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlternatives(alts: List<AlternativeEntity>)

    @Query("SELECT * FROM alternatives WHERE liftId = :liftId ORDER BY overlapPercent DESC")
    suspend fun alternativesFor(liftId: String): List<AlternativeEntity>
}
