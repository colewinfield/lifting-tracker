package com.colewinfield.liftingtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    // ----- Edit mutations -----

    @Update
    suspend fun updateProgram(program: ProgramEntity)

    @Update
    suspend fun updateDay(day: DayEntity)

    @Update
    suspend fun updateLift(lift: LiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: DayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLift(lift: LiftEntity)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteProgramById(id: String)

    @Query("DELETE FROM days WHERE id = :id")
    suspend fun deleteDayById(id: String)

    @Query("DELETE FROM lifts WHERE id = :id")
    suspend fun deleteLiftById(id: String)

    @Query("SELECT * FROM programs WHERE id = :id LIMIT 1")
    suspend fun getProgram(id: String): ProgramEntity?

    @Query("SELECT * FROM days WHERE id = :id LIMIT 1")
    suspend fun getDay(id: String): DayEntity?

    @Query("SELECT * FROM lifts WHERE id = :id LIMIT 1")
    suspend fun getLift(id: String): LiftEntity?

    @Query("SELECT MAX(orderIndex) FROM days WHERE programId = :programId")
    suspend fun maxDayOrder(programId: String): Int?

    @Query("SELECT MAX(orderIndex) FROM lifts WHERE dayId = :dayId")
    suspend fun maxLiftOrder(dayId: String): Int?
}
