package com.colewinfield.liftingtracker.data

import androidx.room.withTransaction
import com.colewinfield.liftingtracker.data.db.CatalogDao
import com.colewinfield.liftingtracker.data.db.CatalogLiftEntity
import com.colewinfield.liftingtracker.data.db.DayEntity
import com.colewinfield.liftingtracker.data.db.LiftEntity
import com.colewinfield.liftingtracker.data.db.LiftingDatabase
import com.colewinfield.liftingtracker.data.db.NoteDao
import com.colewinfield.liftingtracker.data.db.NoteEntity
import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.ProgramDao
import com.colewinfield.liftingtracker.data.db.SessionDao
import com.colewinfield.liftingtracker.data.db.SessionEntity
import com.colewinfield.liftingtracker.data.db.toAlternative
import com.colewinfield.liftingtracker.data.db.toCatalogLift
import com.colewinfield.liftingtracker.data.db.toDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.InputStream
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
    private val database: LiftingDatabase,
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
    private val noteDao: NoteDao,
    private val catalogDao: CatalogDao,
    private val assetReader: (String) -> InputStream,
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
     * True if a session for [dayId] at [week] exists and was already marked finished. Used by
     * [ReminderWorker] to suppress a "you missed today" nudge when the user actually completed
     * the workout. Returns false if there's no session row yet (the user hasn't logged anything).
     */
    suspend fun isSessionFinished(dayId: String, week: Int): Boolean =
        sessionDao.findSession(dayId, week)?.finishedAt != null

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

    /** Observe notes for one lift across all sessions, newest first. */
    fun observeNotesForLift(liftId: String): Flow<List<Note>> =
        noteDao.observeNotesForLift(liftId).map { rows -> rows.map { it.toDomain() } }

    /** Observe notes grouped by lift id for a batch of lifts (Today screen). */
    fun observeNotesByLift(liftIds: List<String>): Flow<Map<String, List<Note>>> {
        if (liftIds.isEmpty()) return flowOf(emptyMap())
        return noteDao.observeNotesForLifts(liftIds).map { rows ->
            rows.map { it.toDomain() }.groupBy { it.liftId }
        }
    }

    /**
     * Append a note for a lift. Lazily creates a session row for (dayId, week) if one doesn't
     * exist yet — matches `appendSet` so notes attach to the active session even before sets
     * are logged.
     */
    suspend fun appendNote(
        dayId: String,
        week: Int,
        liftId: String,
        text: String,
        whoopsy: Boolean,
    ): Long {
        val session = ensureSession(dayId, week)
        return noteDao.insertNote(
            NoteEntity(
                liftId = liftId,
                sessionId = session.id,
                date = System.currentTimeMillis(),
                text = text,
                whoopsy = whoopsy,
            )
        )
    }

    suspend fun deleteNote(noteId: Long) = noteDao.deleteNote(noteId)

    /**
     * Full chronological history for one lift, newest first. Each entry carries the session week,
     * formatted date, the sets performed (weight × reps), and any notes attached to the
     * (session, lift) pair joined by ` · `. Used by Exercise Detail's history and graph tabs.
     */
    suspend fun historyForLift(liftId: String): List<HistoryEntry> {
        val sessions = sessionDao.sessionsWithLift(liftId)
        return sessions.mapNotNull { session ->
            val sets = sessionDao.setsForLiftInSession(session.id, liftId)
            if (sets.isEmpty()) null
            else {
                val notes = noteDao.notesForSessionLift(session.id, liftId)
                    .joinToString(" \u00B7 ") { it.text }
                HistoryEntry(
                    week = session.weekNumber,
                    date = formatHistoryDate(session.date),
                    sets = sets.map { HistorySet(weight = it.weight, reps = it.reps) },
                    notes = notes,
                )
            }
        }
    }

    suspend fun ensureSeeded() {
        if (programDao.count() == 0) Seeder.seed(programDao, sessionDao)
        // Catalog seeding is independent — it's a read-only library, not user data.
        // CatalogSeeder.seed() short-circuits if the table is already populated.
        CatalogSeeder.seed(catalogDao, assetReader)
    }

    // ----- Snapshot export / import -----

    /**
     * Read every user-authored row + the current settings into an in-memory [Snapshot]. Settings
     * are passed in (rather than fetched here) so the snapshot's settings come from the same
     * SettingsRepository instance the rest of the app sees, without coupling this repository to
     * DataStore. The catalog table is excluded — it's re-seeded from the bundled asset on every
     * fresh install, and re-shipping ~900 read-only rows in every snapshot would just bloat the
     * file.
     */
    suspend fun exportSnapshot(settings: AppSettings): Snapshot = Snapshot(
        schemaVersion = Snapshot.CURRENT_SCHEMA_VERSION,
        exportedAt = System.currentTimeMillis(),
        settings = settings,
        programs = programDao.getAllPrograms(),
        days = programDao.getAllDays(),
        lifts = programDao.getAllLifts(),
        alternatives = programDao.getAllAlternatives(),
        sessions = sessionDao.getAllSessionsList(),
        performedSets = sessionDao.getAllPerformedSetsList(),
        notes = noteDao.getAllNotes(),
    )

    /**
     * Replace every user-authored row with the snapshot's contents in a single transaction.
     * Insert order matters because of FK declarations (programs → days → lifts → alternatives;
     * days → sessions → performed_sets; lifts → notes). The opening `deleteAllPrograms()` cascades
     * across every dependent table, so this is a true wipe-and-replace, not a merge.
     *
     * Catalog rows are not touched — they're seeded separately and aren't user data. Settings
     * restoration is the caller's responsibility (see BackupService) since DataStore lives
     * outside Room.
     */
    suspend fun importSnapshot(snapshot: Snapshot) {
        database.withTransaction {
            programDao.deleteAllPrograms()
            programDao.insertPrograms(snapshot.programs)
            programDao.insertDays(snapshot.days)
            programDao.insertLifts(snapshot.lifts)
            programDao.insertAlternatives(snapshot.alternatives)
            sessionDao.insertSessions(snapshot.sessions)
            sessionDao.insertPerformedSets(snapshot.performedSets)
            noteDao.insertAll(snapshot.notes)
        }
    }

    // ----- Catalog (free-exercise-db) -----

    /**
     * Same-muscle catalog matches for a program lift, ranked by relevance:
     * - Catalog rows whose `primaryMuscle` matches the lift's normalised muscle, with same
     *   equipment first (overlap 90%) then any equipment (overlap 80%).
     * - Catalog rows where the lift's muscle appears in `secondaryMuscles` (overlap 60%).
     * The source lift itself (case-insensitive name match) is filtered out.
     */
    suspend fun catalogMatchesFor(liftId: String): List<Alternative> {
        val lift = programDao.getLift(liftId) ?: return emptyList()
        return rankByMuscle(
            sourceMuscle = lift.muscle,
            sourceEquipment = lift.equipment,
            sourceLiftId = liftId,
            sourceName = lift.name,
        )
    }

    /** Same as [catalogMatchesFor] but driven from a free-form (muscle, equipment) pair. */
    suspend fun catalogMatchesByMuscle(
        sourceLiftId: String,
        sourceName: String,
        muscle: String,
        equipment: String,
    ): List<Alternative> = rankByMuscle(
        sourceMuscle = muscle,
        sourceEquipment = equipment,
        sourceLiftId = sourceLiftId,
        sourceName = sourceName,
    )

    private suspend fun rankByMuscle(
        sourceMuscle: String,
        sourceEquipment: String,
        sourceLiftId: String,
        sourceName: String,
    ): List<Alternative> {
        val muscle = CatalogSeeder.normalizeMuscle(sourceMuscle)
        if (muscle.isBlank()) return emptyList()
        val equip = sourceEquipment.lowercase().trim()
        val sameSource: (CatalogLiftEntity) -> Boolean = { row ->
            row.name.equals(sourceName, ignoreCase = true)
        }
        val primary = catalogDao.byPrimaryMuscle(muscle).filterNot(sameSource)
        val secondary = catalogDao
            .bySecondaryMuscle(muscle = muscle, likeQuery = "%$muscle%")
            .filterNot(sameSource)

        val primaryAlts = primary.map { row ->
            val equipMatch = row.equipment.equals(equip, ignoreCase = true) && equip.isNotBlank()
            row.toAlternative(
                sourceLiftId = sourceLiftId,
                overlapPercent = if (equipMatch) 90 else 80,
            )
        }
        val secondaryAlts = secondary.map { row ->
            row.toAlternative(sourceLiftId = sourceLiftId, overlapPercent = 60)
        }
        return (primaryAlts + secondaryAlts).sortedByDescending { it.overlapPercent }
    }

    /** Catalog lifts that share the program lift's equipment (any muscle). Source lift removed. */
    suspend fun catalogByEquipmentFor(liftId: String): List<Alternative> {
        val lift = programDao.getLift(liftId) ?: return emptyList()
        val equip = lift.equipment.lowercase().trim()
        if (equip.isBlank()) return emptyList()
        return catalogDao.byEquipment(equip)
            .filterNot { it.name.equals(lift.name, ignoreCase = true) }
            .map { row ->
                val sameMuscle = row.primaryMuscle == CatalogSeeder.normalizeMuscle(lift.muscle)
                row.toAlternative(
                    sourceLiftId = liftId,
                    overlapPercent = if (sameMuscle) 85 else 50,
                )
            }
            .sortedByDescending { it.overlapPercent }
    }

    /**
     * Free-text catalog search. Pass a non-blank query (matched as `%query%` against name and
     * primary muscle). Returns at most [limit] rows. Results carry a 0% overlap so the Swap UI
     * doesn't render a misleading match badge.
     */
    suspend fun searchCatalog(
        query: String,
        sourceLiftId: String,
        limit: Int = 80,
    ): List<Alternative> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val pattern = "%$q%"
        return catalogDao.search(pattern, limit).map { row ->
            row.toAlternative(sourceLiftId = sourceLiftId, overlapPercent = 0)
        }
    }

    /** First page of the catalog when the user opens "Browse all" with no search query. */
    suspend fun catalogPage(
        sourceLiftId: String,
        offset: Int = 0,
        limit: Int = 80,
    ): List<Alternative> = catalogDao.page(offset, limit).map { row ->
        row.toAlternative(sourceLiftId = sourceLiftId, overlapPercent = 0)
    }

    /**
     * Catalog browser query — drives the standalone Exercise DB picker. Returns [CatalogLift]
     * (no overlap badge) instead of [Alternative].
     *
     * Selection rules:
     * - If [primaryMuscle] is non-null, results are restricted to that primary muscle. A non-blank
     *   [query] then narrows by name match within the muscle group (in-memory, since the muscle
     *   list is short).
     * - Otherwise if [query] is non-blank, run the free-text DAO search across name + primary
     *   muscle.
     * - Otherwise return the first [limit] rows alphabetically.
     */
    suspend fun browseCatalog(
        query: String,
        primaryMuscle: String?,
        limit: Int = 100,
    ): List<CatalogLift> {
        val q = query.trim()
        val rows = when {
            primaryMuscle != null -> {
                val byMuscle = catalogDao.byPrimaryMuscle(primaryMuscle)
                if (q.isBlank()) byMuscle
                else byMuscle.filter { it.name.contains(q, ignoreCase = true) }
            }
            q.isNotBlank() -> catalogDao.search("%$q%", limit)
            else -> catalogDao.page(offset = 0, limit = limit)
        }
        return rows.take(limit).map { it.toCatalogLift() }
    }

    // ----- Program / Day / Lift edits -----

    suspend fun getDay(dayId: String): Day? =
        programDao.getDay(dayId)?.let { entity ->
            Day(
                id = entity.id,
                name = entity.name,
                dayOfWeek = entity.dayOfWeek,
                focus = entity.focus,
                isRest = entity.isRest,
                lifts = emptyList(),
            )
        }

    suspend fun getLift(liftId: String): Lift? =
        programDao.getLift(liftId)?.toDomain()

    suspend fun updateProgramMeta(
        programId: String,
        name: String,
        cycleLength: Int,
        deloadWeek: Int,
    ) {
        val current = programDao.getProgram(programId) ?: return
        programDao.updateProgram(
            current.copy(
                name = name,
                cycleLength = cycleLength,
                deloadWeek = deloadWeek,
            )
        )
    }

    suspend fun deleteProgram(programId: String) =
        programDao.deleteProgramById(programId)

    suspend fun addDay(
        programId: String,
        name: String,
        dayOfWeek: Weekday,
        focus: String,
        isRest: Boolean,
    ): String {
        val newId = UUID.randomUUID().toString()
        val nextOrder = (programDao.maxDayOrder(programId) ?: -1) + 1
        programDao.insertDay(
            DayEntity(
                id = newId,
                programId = programId,
                name = name,
                dayOfWeek = dayOfWeek,
                focus = focus,
                isRest = isRest,
                orderIndex = nextOrder,
            )
        )
        return newId
    }

    suspend fun updateDay(
        dayId: String,
        name: String,
        dayOfWeek: Weekday,
        focus: String,
        isRest: Boolean,
    ) {
        val current = programDao.getDay(dayId) ?: return
        programDao.updateDay(
            current.copy(
                name = name,
                dayOfWeek = dayOfWeek,
                focus = focus,
                isRest = isRest,
            )
        )
    }

    suspend fun deleteDay(dayId: String) = programDao.deleteDayById(dayId)

    suspend fun addLift(
        dayId: String,
        name: String,
        setsMin: Int,
        setsMax: Int,
        repsMin: Int,
        repsMax: Int,
        effort: Effort,
        muscle: String,
        equipment: String,
    ): String {
        val newId = UUID.randomUUID().toString()
        val nextOrder = (programDao.maxLiftOrder(dayId) ?: -1) + 1
        programDao.insertLift(
            LiftEntity(
                id = newId,
                dayId = dayId,
                name = name,
                setsMin = setsMin,
                setsMax = setsMax,
                repsMin = repsMin,
                repsMax = repsMax,
                effort = effort,
                muscle = muscle,
                equipment = equipment,
                orderIndex = nextOrder,
            )
        )
        return newId
    }

    suspend fun updateLift(
        liftId: String,
        name: String,
        setsMin: Int,
        setsMax: Int,
        repsMin: Int,
        repsMax: Int,
        effort: Effort,
        muscle: String,
        equipment: String,
    ) {
        val current = programDao.getLift(liftId) ?: return
        programDao.updateLift(
            current.copy(
                name = name,
                setsMin = setsMin,
                setsMax = setsMax,
                repsMin = repsMin,
                repsMax = repsMax,
                effort = effort,
                muscle = muscle,
                equipment = equipment,
            )
        )
    }

    suspend fun deleteLift(liftId: String) = programDao.deleteLiftById(liftId)

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
