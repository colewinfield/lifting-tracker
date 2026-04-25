package com.colewinfield.liftingtracker.data

import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.ProgramDao
import com.colewinfield.liftingtracker.data.db.SessionDao
import com.colewinfield.liftingtracker.data.db.SessionEntity
import com.colewinfield.liftingtracker.data.db.toDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ActiveSessionState(
    val sessionId: String?,
    val setsByLift: Map<String, List<PerformedSet>>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LiftingRepository(
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
) {

    fun observeCurrentProgram(): Flow<Program?> =
        programDao.observeFirstProgram().map { it?.toDomain() }

    fun observeAllSessions() = sessionDao.observeAllSessions()
    fun observeAllPerformedSets() = sessionDao.observeAllPerformedSets()

    fun observeActiveSession(dayId: String, week: Int): Flow<ActiveSessionState> =
        sessionDao.observeSession(dayId, week).flatMapLatest { session ->
            if (session == null) {
                flowOf(ActiveSessionState(sessionId = null, setsByLift = emptyMap()))
            } else {
                sessionDao.observeSetsForSession(session.id).map { rows ->
                    ActiveSessionState(
                        sessionId = session.id,
                        setsByLift = rows.groupBy { it.liftId }
                            .mapValues { (_, list) ->
                                list.sortedBy { it.setIndex }.map { it.toDomain() }
                            },
                    )
                }
            }
        }

    suspend fun appendSet(
        dayId: String,
        week: Int,
        liftId: String,
        weight: Double,
        reps: Int,
    ): Long {
        val session = ensureSession(dayId, week)
        val nextIndex = sessionDao.countSetsForLift(session.id, liftId) + 1
        return sessionDao.insertPerformedSet(
            PerformedSetEntity(
                sessionId = session.id,
                liftId = liftId,
                setIndex = nextIndex,
                weight = weight,
                reps = reps,
                whoopsy = false,
                rpe = null,
                done = false,
                completedAt = null,
            )
        )
    }

    suspend fun toggleSetDone(setId: Long) {
        val s = sessionDao.getPerformedSet(setId) ?: return
        val flipped = !s.done
        sessionDao.updatePerformedSet(
            s.copy(done = flipped, completedAt = if (flipped) System.currentTimeMillis() else null)
        )
    }

    suspend fun adjustWeight(setId: Long, delta: Double) {
        val s = sessionDao.getPerformedSet(setId) ?: return
        sessionDao.updatePerformedSet(s.copy(weight = (s.weight + delta).coerceAtLeast(0.0)))
    }

    suspend fun adjustReps(setId: Long, delta: Int) {
        val s = sessionDao.getPerformedSet(setId) ?: return
        sessionDao.updatePerformedSet(s.copy(reps = (s.reps + delta).coerceAtLeast(0)))
    }

    suspend fun removeSet(setId: Long) {
        val s = sessionDao.getPerformedSet(setId) ?: return
        sessionDao.deletePerformedSet(setId)
        // Re-number subsequent sets so setIndex stays contiguous (1..N).
        sessionDao.setsForLiftInSession(s.sessionId, s.liftId)
            .filter { it.setIndex > s.setIndex }
            .forEach { row ->
                sessionDao.updatePerformedSet(row.copy(setIndex = row.setIndex - 1))
            }
    }

    suspend fun finishSession(sessionId: String) {
        sessionDao.markSessionFinished(sessionId, System.currentTimeMillis())
    }

    /**
     * Most-recent prior session containing logged sets for the given lift.
     * Pass [excludeSessionId] to skip the currently-active session.
     */
    suspend fun lastSessionFor(liftId: String, excludeSessionId: String?): HistoryEntry? {
        val session = if (excludeSessionId != null) {
            sessionDao.lastSessionWithLift(liftId, excludeSessionId)
        } else {
            sessionDao.lastSessionWithLiftAny(liftId)
        } ?: return null
        val sets = sessionDao.setsForLiftInSession(session.id, liftId)
        if (sets.isEmpty()) return null
        return HistoryEntry(
            week = session.weekNumber,
            date = formatHistoryDate(session.date),
            sets = sets.map { HistorySet(weight = it.weight, reps = it.reps) },
        )
    }

    suspend fun lastSessionsFor(
        liftIds: List<String>,
        excludeSessionId: String?,
    ): Map<String, HistoryEntry> = liftIds
        .mapNotNull { id -> lastSessionFor(id, excludeSessionId)?.let { id to it } }
        .toMap()

    suspend fun alternativesFor(liftId: String): List<Alternative> =
        programDao.alternativesFor(liftId).map { it.toDomain() }

    /**
     * Full chronological history for one lift, newest first. Each entry carries the session week,
     * formatted date, and the sets performed (weight × reps). Used by Exercise Detail's history
     * and graph tabs.
     */
    suspend fun historyForLift(liftId: String): List<HistoryEntry> {
        val sessions = sessionDao.sessionsWithLift(liftId)
        return sessions.mapNotNull { session ->
            val sets = sessionDao.setsForLiftInSession(session.id, liftId)
            if (sets.isEmpty()) null
            else HistoryEntry(
                week = session.weekNumber,
                date = formatHistoryDate(session.date),
                sets = sets.map { HistorySet(weight = it.weight, reps = it.reps) },
            )
        }
    }

    suspend fun ensureSeeded() {
        if (programDao.count() > 0) return
        Seeder.seed(programDao, sessionDao)
    }

    private suspend fun ensureSession(dayId: String, week: Int): SessionEntity {
        sessionDao.findSession(dayId, week)?.let { return it }
        val created = SessionEntity(
            id = UUID.randomUUID().toString(),
            dayId = dayId,
            weekNumber = week,
            date = System.currentTimeMillis(),
            finishedAt = null,
        )
        sessionDao.insertSession(created)
        return created
    }
}

private val historyDateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)

private fun formatHistoryDate(epochMillis: Long): String =
    historyDateFormat.format(Date(epochMillis))
