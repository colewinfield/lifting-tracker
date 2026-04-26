package com.colewinfield.liftingtracker.data

import com.colewinfield.liftingtracker.data.db.AlternativeEntity
import com.colewinfield.liftingtracker.data.db.DayEntity
import com.colewinfield.liftingtracker.data.db.LiftEntity
import com.colewinfield.liftingtracker.data.db.NoteEntity
import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.ProgramEntity
import com.colewinfield.liftingtracker.data.db.SessionEntity

/**
 * In-memory representation of a complete user snapshot. Carries every Room row that contains
 * user-authored or user-derived data plus the full settings blob. Catalog rows are deliberately
 * excluded — they're a static library re-seeded from `assets/exercises.json` on every fresh
 * install, so re-shipping them in every snapshot would just inflate file size with no benefit.
 *
 * The schema version is the Room database version number. A restore is only attempted if the
 * snapshot's [schemaVersion] matches the current DB schema; mismatches are surfaced to the user
 * as "this backup was made on an older/newer app version" rather than silently corrupted.
 */
data class Snapshot(
    val schemaVersion: Int,
    val exportedAt: Long,
    val settings: AppSettings,
    val programs: List<ProgramEntity>,
    val days: List<DayEntity>,
    val lifts: List<LiftEntity>,
    val alternatives: List<AlternativeEntity>,
    val sessions: List<SessionEntity>,
    val performedSets: List<PerformedSetEntity>,
    val notes: List<NoteEntity>,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        const val FILENAME = "lifting-tracker-snapshot.json"
        // Write-in-progress filename. BackupService writes to this first, verifies the bytes,
        // then promotes via rename to FILENAME. If a crash happens mid-write, the partial stays
        // on disk and the previous FILENAME (if any) is untouched — no torn-write window. On the
        // next backup we delete any stale `.partial` before starting a new write; on restore we
        // fall back to the partial if FILENAME is missing.
        const val PARTIAL_FILENAME = "lifting-tracker-snapshot.json.partial"
        const val MIME_TYPE = "application/json"
    }
}
