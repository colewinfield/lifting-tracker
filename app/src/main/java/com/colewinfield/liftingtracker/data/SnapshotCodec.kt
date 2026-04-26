package com.colewinfield.liftingtracker.data

import com.colewinfield.liftingtracker.data.db.AlternativeEntity
import com.colewinfield.liftingtracker.data.db.DayEntity
import com.colewinfield.liftingtracker.data.db.LiftEntity
import com.colewinfield.liftingtracker.data.db.NoteEntity
import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.ProgramEntity
import com.colewinfield.liftingtracker.data.db.SessionEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure (Snapshot ↔ String) codec. No Android dependencies — easy to unit-test, and lets the
 * BackupService stay focused on SAF I/O. Uses `org.json` for the same reason CatalogSeeder does:
 * one fewer dependency to drag in for a single use case, and the snapshot is small (~hundreds of
 * KB at most) so streaming would be premature.
 *
 * Key choices:
 * - `notes: List<String>` on ProgramEntity is encoded as a JSON array, NOT the U+001F-joined
 *   string the Room TypeConverter writes. We're outside the DB here; the converter wraps and
 *   unwraps automatically when we hand entities to DAOs.
 * - Enums (Effort, Weekday, WeightUnit, ThemeMode) are encoded as their `name`. Unknown values
 *   on import fall back to defaults rather than throwing — survives a downgrade where the writer
 *   added a new enum case.
 * - Nullables are encoded as `JSONObject.NULL` so the field is always present (helps when
 *   eyeballing the file, and avoids ambiguity with "missing key").
 */
object SnapshotCodec {

    fun encode(snapshot: Snapshot): String = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("exportedAt", snapshot.exportedAt)
        put("settings", encodeSettings(snapshot.settings))
        put("programs", JSONArray().apply { snapshot.programs.forEach { put(encodeProgram(it)) } })
        put("days", JSONArray().apply { snapshot.days.forEach { put(encodeDay(it)) } })
        put("lifts", JSONArray().apply { snapshot.lifts.forEach { put(encodeLift(it)) } })
        put("alternatives", JSONArray().apply { snapshot.alternatives.forEach { put(encodeAlternative(it)) } })
        put("sessions", JSONArray().apply { snapshot.sessions.forEach { put(encodeSession(it)) } })
        put("performedSets", JSONArray().apply { snapshot.performedSets.forEach { put(encodePerformedSet(it)) } })
        put("notes", JSONArray().apply { snapshot.notes.forEach { put(encodeNote(it)) } })
    }.toString(2)

    fun decode(json: String): Snapshot {
        val root = JSONObject(json)
        val exportedAt = root.getLong("exportedAt")
        return Snapshot(
            schemaVersion = root.getInt("schemaVersion"),
            exportedAt = exportedAt,
            settings = decodeSettings(root.getJSONObject("settings"), exportedAt),
            programs = root.getJSONArray("programs").map { decodeProgram(it) },
            days = root.getJSONArray("days").map { decodeDay(it) },
            lifts = root.getJSONArray("lifts").map { decodeLift(it) },
            alternatives = root.getJSONArray("alternatives").map { decodeAlternative(it) },
            sessions = root.getJSONArray("sessions").map { decodeSession(it) },
            performedSets = root.getJSONArray("performedSets").map { decodePerformedSet(it) },
            notes = root.getJSONArray("notes").map { decodeNote(it) },
        )
    }

    // ----- Settings -----

    private fun encodeSettings(s: AppSettings): JSONObject = JSONObject().apply {
        put("cycleStartedAt", s.cycleStartedAt)
        put("unit", s.unit.name)
        put("useDynamicColor", s.useDynamicColor)
        put("themeMode", s.themeMode.name)
        put("displayName", s.displayName)
        put("bodyweight", s.bodyweight)
        put("heightInches", s.heightInches)
        put("age", s.age)
        put("reminderEnabledLeads", JSONArray(s.reminderEnabledLeads.toList()))
        put("reminderTier", s.reminderTier.name)
        // Backup fields are deliberately NOT exported — they're device-local. The folder URI
        // refers to a SAF tree on *this* device, and lastBackupAt/Sha describe the file you're
        // currently reading. Re-importing them would be circular and confusing.
    }

    /**
     * Decode settings out of a snapshot. [exportedAt] is the snapshot's export wall-clock so we
     * can rebuild a sensible `cycleStartedAt` anchor from a *legacy* snapshot that only stored
     * `currentWeek`: anchor = exportedAt − (currentWeek − 1) × 7d preserves the user's apparent
     * week when restoring an older file. After one round-trip the legacy field disappears.
     */
    private fun decodeSettings(o: JSONObject, exportedAt: Long): AppSettings {
        val defaults = AppSettings.Defaults
        val cycleStartedAt = when {
            o.has("cycleStartedAt") -> o.optLong("cycleStartedAt", defaults.cycleStartedAt)
            o.has("currentWeek") -> cycleAnchorFor(
                week = o.optInt("currentWeek", SampleData.defaultCurrentWeek),
                now = exportedAt,
            )
            else -> defaults.cycleStartedAt
        }
        return AppSettings(
            cycleStartedAt = cycleStartedAt,
            unit = enumOrDefault(o.optString("unit"), defaults.unit, WeightUnit::valueOf),
            useDynamicColor = o.optBoolean("useDynamicColor", defaults.useDynamicColor),
            themeMode = enumOrDefault(o.optString("themeMode"), defaults.themeMode, ThemeMode::valueOf),
            displayName = o.optString("displayName", defaults.displayName),
            bodyweight = o.optDouble("bodyweight", defaults.bodyweight),
            heightInches = o.optInt("heightInches", defaults.heightInches),
            age = o.optInt("age", defaults.age),
            // Reuse the local device's backup metadata (we never overwrite it from a snapshot).
            backupFolderUri = defaults.backupFolderUri,
            backupFolderName = defaults.backupFolderName,
            lastBackupAt = defaults.lastBackupAt,
            lastBackupSha = defaults.lastBackupSha,
            hasCheckedForBackupRestore = defaults.hasCheckedForBackupRestore,
            reminderEnabledLeads = if (o.has("reminderEnabledLeads")) {
                o.getJSONArray("reminderEnabledLeads").toStringList().toSet()
            } else defaults.reminderEnabledLeads,
            reminderTier = enumOrDefault(
                o.optString("reminderTier"),
                defaults.reminderTier,
                ReminderTier::valueOf,
            ),
        )
    }

    // ----- Entities -----

    private fun encodeProgram(e: ProgramEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("name", e.name)
        put("cycleLength", e.cycleLength)
        put("deloadWeek", e.deloadWeek)
        put("notes", JSONArray(e.notes))
    }

    private fun decodeProgram(o: JSONObject): ProgramEntity = ProgramEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        cycleLength = o.getInt("cycleLength"),
        deloadWeek = o.getInt("deloadWeek"),
        notes = o.getJSONArray("notes").toStringList(),
    )

    private fun encodeDay(e: DayEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("programId", e.programId)
        put("name", e.name)
        put("dayOfWeek", e.dayOfWeek.name)
        put("focus", e.focus)
        put("isRest", e.isRest)
        put("orderIndex", e.orderIndex)
    }

    private fun decodeDay(o: JSONObject): DayEntity = DayEntity(
        id = o.getString("id"),
        programId = o.getString("programId"),
        name = o.getString("name"),
        dayOfWeek = enumOrDefault(o.optString("dayOfWeek"), Weekday.MON, Weekday::valueOf),
        focus = o.optString("focus", ""),
        isRest = o.optBoolean("isRest", false),
        orderIndex = o.getInt("orderIndex"),
    )

    private fun encodeLift(e: LiftEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("dayId", e.dayId)
        put("name", e.name)
        put("setsMin", e.setsMin)
        put("setsMax", e.setsMax)
        put("repsMin", e.repsMin)
        put("repsMax", e.repsMax)
        put("effort", e.effort.name)
        put("muscle", e.muscle)
        put("equipment", e.equipment)
        put("orderIndex", e.orderIndex)
    }

    private fun decodeLift(o: JSONObject): LiftEntity = LiftEntity(
        id = o.getString("id"),
        dayId = o.getString("dayId"),
        name = o.getString("name"),
        setsMin = o.getInt("setsMin"),
        setsMax = o.getInt("setsMax"),
        repsMin = o.getInt("repsMin"),
        repsMax = o.getInt("repsMax"),
        effort = enumOrDefault(o.optString("effort"), Effort.MED, Effort::valueOf),
        muscle = o.optString("muscle", ""),
        equipment = o.optString("equipment", ""),
        orderIndex = o.getInt("orderIndex"),
    )

    private fun encodeAlternative(e: AlternativeEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("liftId", e.liftId)
        put("name", e.name)
        put("muscle", e.muscle)
        put("equipment", e.equipment)
        put("overlapPercent", e.overlapPercent)
    }

    private fun decodeAlternative(o: JSONObject): AlternativeEntity = AlternativeEntity(
        id = o.getString("id"),
        liftId = o.getString("liftId"),
        name = o.getString("name"),
        muscle = o.optString("muscle", ""),
        equipment = o.optString("equipment", ""),
        overlapPercent = o.optInt("overlapPercent", 0),
    )

    private fun encodeSession(e: SessionEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("dayId", e.dayId)
        put("weekNumber", e.weekNumber)
        put("date", e.date)
        if (e.finishedAt != null) put("finishedAt", e.finishedAt) else put("finishedAt", JSONObject.NULL)
    }

    private fun decodeSession(o: JSONObject): SessionEntity = SessionEntity(
        id = o.getString("id"),
        dayId = o.getString("dayId"),
        weekNumber = o.getInt("weekNumber"),
        date = o.getLong("date"),
        finishedAt = if (o.isNull("finishedAt")) null else o.getLong("finishedAt"),
    )

    private fun encodePerformedSet(e: PerformedSetEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("sessionId", e.sessionId)
        put("liftId", e.liftId)
        put("setIndex", e.setIndex)
        put("weight", e.weight)
        put("reps", e.reps)
        put("whoopsy", e.whoopsy)
        if (e.rpe != null) put("rpe", e.rpe) else put("rpe", JSONObject.NULL)
        put("done", e.done)
        if (e.completedAt != null) put("completedAt", e.completedAt) else put("completedAt", JSONObject.NULL)
    }

    private fun decodePerformedSet(o: JSONObject): PerformedSetEntity = PerformedSetEntity(
        id = o.getLong("id"),
        sessionId = o.getString("sessionId"),
        liftId = o.getString("liftId"),
        setIndex = o.getInt("setIndex"),
        weight = o.getDouble("weight"),
        reps = o.getInt("reps"),
        whoopsy = o.optBoolean("whoopsy", false),
        rpe = if (o.isNull("rpe")) null else o.getInt("rpe"),
        done = o.optBoolean("done", false),
        completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
    )

    private fun encodeNote(e: NoteEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("liftId", e.liftId)
        if (e.sessionId != null) put("sessionId", e.sessionId) else put("sessionId", JSONObject.NULL)
        put("date", e.date)
        put("text", e.text)
        put("whoopsy", e.whoopsy)
    }

    private fun decodeNote(o: JSONObject): NoteEntity = NoteEntity(
        id = o.getLong("id"),
        liftId = o.getString("liftId"),
        sessionId = if (o.isNull("sessionId")) null else o.getString("sessionId"),
        date = o.getLong("date"),
        text = o.optString("text", ""),
        whoopsy = o.optBoolean("whoopsy", false),
    )

    // ----- Helpers -----

    private inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    /** Map a serialized enum name back to the enum value, falling back to [default] on miss. */
    private inline fun <reified E : Enum<E>> enumOrDefault(
        raw: String,
        default: E,
        valueOf: (String) -> E,
    ): E = runCatching { valueOf(raw) }.getOrDefault(default)
}
