package com.colewinfield.liftingtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ----- Sessions -----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE dayId = :dayId AND weekNumber = :week ORDER BY date DESC LIMIT 1")
    suspend fun findSession(dayId: String, week: Int): SessionEntity?

    @Query("SELECT * FROM sessions WHERE dayId = :dayId AND weekNumber = :week ORDER BY date DESC LIMIT 1")
    fun observeSession(dayId: String, week: Int): Flow<SessionEntity?>

    @Query("UPDATE sessions SET finishedAt = :finishedAt WHERE id = :sessionId")
    suspend fun markSessionFinished(sessionId: String, finishedAt: Long)

    // ----- Performed sets -----

    @Insert
    suspend fun insertPerformedSet(set: PerformedSetEntity): Long

    @Update
    suspend fun updatePerformedSet(set: PerformedSetEntity)

    @Query("SELECT * FROM performed_sets WHERE id = :id")
    suspend fun getPerformedSet(id: Long): PerformedSetEntity?

    @Query("DELETE FROM performed_sets WHERE id = :id")
    suspend fun deletePerformedSet(id: Long)

    @Query("SELECT * FROM performed_sets WHERE sessionId = :sessionId ORDER BY liftId, setIndex")
    fun observeSetsForSession(sessionId: String): Flow<List<PerformedSetEntity>>

    @Query("SELECT COUNT(*) FROM performed_sets WHERE sessionId = :sessionId AND liftId = :liftId")
    suspend fun countSetsForLift(sessionId: String, liftId: String): Int

    /**
     * Most-recent session containing any logged set for the given lift, excluding the active
     * session. Returns null if there's no prior history. Used to populate the "last week" strip.
     */
    @Query("""
        SELECT s.* FROM sessions s
        INNER JOIN performed_sets ps ON ps.sessionId = s.id
        WHERE ps.liftId = :liftId AND s.id != :excludeSessionId
        GROUP BY s.id
        ORDER BY s.date DESC
        LIMIT 1
    """)
    suspend fun lastSessionWithLift(liftId: String, excludeSessionId: String): SessionEntity?

    @Query("""
        SELECT s.* FROM sessions s
        INNER JOIN performed_sets ps ON ps.sessionId = s.id
        WHERE ps.liftId = :liftId
        GROUP BY s.id
        ORDER BY s.date DESC
        LIMIT 1
    """)
    suspend fun lastSessionWithLiftAny(liftId: String): SessionEntity?

    @Query("SELECT * FROM performed_sets WHERE sessionId = :sessionId AND liftId = :liftId ORDER BY setIndex")
    suspend fun setsForLiftInSession(sessionId: String, liftId: String): List<PerformedSetEntity>
}
