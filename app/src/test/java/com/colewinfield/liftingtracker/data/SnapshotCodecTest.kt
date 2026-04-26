package com.colewinfield.liftingtracker.data

import com.colewinfield.liftingtracker.data.db.AlternativeEntity
import com.colewinfield.liftingtracker.data.db.DayEntity
import com.colewinfield.liftingtracker.data.db.LiftEntity
import com.colewinfield.liftingtracker.data.db.NoteEntity
import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.ProgramEntity
import com.colewinfield.liftingtracker.data.db.SessionEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Round-trip tests for [SnapshotCodec]. The codec is the load-bearing piece for "does my backup
 * actually survive encode/decode?" — a silent regression here would corrupt every subsequent
 * backup. These tests cover:
 * - Full-fidelity round-trip with every field populated (catches missed fields).
 * - Empty / minimal snapshots.
 * - Nullable handling (`finishedAt`, `rpe`, `completedAt`, `sessionId` on notes).
 * - Enum decode resilience (unknown values fall back to a default rather than throwing).
 * - Backup-local settings are NOT encoded (security/correctness — restore must not clobber
 *   the device's own folder URI / lastBackup metadata).
 * - The serialised JSON is structurally valid (parses back as a JSONObject with expected keys).
 * - Schema version is preserved verbatim.
 */
class SnapshotCodecTest {

    @Test
    fun `round-trip preserves a fully populated snapshot`() {
        val original = fullSnapshot()
        val json = SnapshotCodec.encode(original)
        val decoded = SnapshotCodec.decode(json)

        // Top-level scalars
        assertEquals(original.schemaVersion, decoded.schemaVersion)
        assertEquals(original.exportedAt, decoded.exportedAt)

        // Settings — fields the codec actually serialises
        assertEquals(original.settings.cycleStartedAt, decoded.settings.cycleStartedAt)
        assertEquals(original.settings.unit, decoded.settings.unit)
        assertEquals(original.settings.useDynamicColor, decoded.settings.useDynamicColor)
        assertEquals(original.settings.themeMode, decoded.settings.themeMode)
        assertEquals(original.settings.displayName, decoded.settings.displayName)
        assertEquals(original.settings.bodyweight, decoded.settings.bodyweight, 0.0001)
        assertEquals(original.settings.heightInches, decoded.settings.heightInches)
        assertEquals(original.settings.age, decoded.settings.age)

        // Entity collections — equals on data classes catches every field
        assertEquals(original.programs, decoded.programs)
        assertEquals(original.days, decoded.days)
        assertEquals(original.lifts, decoded.lifts)
        assertEquals(original.alternatives, decoded.alternatives)
        assertEquals(original.sessions, decoded.sessions)
        assertEquals(original.performedSets, decoded.performedSets)
        assertEquals(original.notes, decoded.notes)
    }

    @Test
    fun `round-trip survives an empty snapshot`() {
        val original = Snapshot(
            schemaVersion = Snapshot.CURRENT_SCHEMA_VERSION,
            exportedAt = 0L,
            settings = AppSettings.Defaults,
            programs = emptyList(),
            days = emptyList(),
            lifts = emptyList(),
            alternatives = emptyList(),
            sessions = emptyList(),
            performedSets = emptyList(),
            notes = emptyList(),
        )
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(original))
        assertTrue(decoded.programs.isEmpty())
        assertTrue(decoded.days.isEmpty())
        assertTrue(decoded.lifts.isEmpty())
        assertTrue(decoded.alternatives.isEmpty())
        assertTrue(decoded.sessions.isEmpty())
        assertTrue(decoded.performedSets.isEmpty())
        assertTrue(decoded.notes.isEmpty())
    }

    @Test
    fun `nullable fields round-trip as null when null`() {
        val session = SessionEntity(
            id = "s1",
            dayId = "d1",
            weekNumber = 1,
            date = 1_000L,
            finishedAt = null,
        )
        val performedSet = PerformedSetEntity(
            id = 42L,
            sessionId = "s1",
            liftId = "l1",
            setIndex = 1,
            weight = 100.0,
            reps = 5,
            whoopsy = false,
            rpe = null,
            done = true,
            completedAt = null,
        )
        val note = NoteEntity(
            id = 7L,
            liftId = "l1",
            sessionId = null,
            date = 1_000L,
            text = "felt heavy",
            whoopsy = false,
        )
        val snapshot = baselineSnapshot().copy(
            sessions = listOf(session),
            performedSets = listOf(performedSet),
            notes = listOf(note),
        )
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(snapshot))
        assertNull(decoded.sessions.single().finishedAt)
        assertNull(decoded.performedSets.single().rpe)
        assertNull(decoded.performedSets.single().completedAt)
        assertNull(decoded.notes.single().sessionId)
    }

    @Test
    fun `nullable fields round-trip as values when non-null`() {
        val session = SessionEntity(
            id = "s1",
            dayId = "d1",
            weekNumber = 2,
            date = 1_000L,
            finishedAt = 2_000L,
        )
        val performedSet = PerformedSetEntity(
            id = 42L,
            sessionId = "s1",
            liftId = "l1",
            setIndex = 1,
            weight = 100.0,
            reps = 5,
            whoopsy = true,
            rpe = 8,
            done = true,
            completedAt = 1_500L,
        )
        val note = NoteEntity(
            id = 7L,
            liftId = "l1",
            sessionId = "s1",
            date = 1_000L,
            text = "PR!",
            whoopsy = true,
        )
        val snapshot = baselineSnapshot().copy(
            sessions = listOf(session),
            performedSets = listOf(performedSet),
            notes = listOf(note),
        )
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(snapshot))
        assertEquals(2_000L, decoded.sessions.single().finishedAt)
        assertEquals(8, decoded.performedSets.single().rpe)
        assertEquals(1_500L, decoded.performedSets.single().completedAt)
        assertEquals("s1", decoded.notes.single().sessionId)
    }

    @Test
    fun `unknown enum values decode to a default instead of throwing`() {
        // Hand-craft a JSON with garbage enum strings to simulate a future-version snapshot the
        // current app shouldn't crash on (e.g. someone added Effort.MAX or ThemeMode.OLED). The
        // codec's enumOrDefault helper should swallow the parse and substitute a sane default.
        val rawJson = SnapshotCodec.encode(baselineSnapshot().copy(
            lifts = listOf(sampleLift()),
            days = listOf(sampleDay()),
        ))
        val mutated = JSONObject(rawJson)
        mutated.getJSONArray("lifts").getJSONObject(0).put("effort", "GIGA_HARD")
        mutated.getJSONArray("days").getJSONObject(0).put("dayOfWeek", "FUNDAY")
        mutated.getJSONObject("settings").put("unit", "STONE")
        mutated.getJSONObject("settings").put("themeMode", "AMOLED")

        val decoded = SnapshotCodec.decode(mutated.toString())
        // Defaults defined in the codec
        assertEquals(Effort.MED, decoded.lifts.single().effort)
        assertEquals(Weekday.MON, decoded.days.single().dayOfWeek)
        assertEquals(AppSettings.Defaults.unit, decoded.settings.unit)
        assertEquals(AppSettings.Defaults.themeMode, decoded.settings.themeMode)
    }

    @Test
    fun `backup-local settings are never serialised`() {
        // The local device's folder URI / lastBackupAt must NOT travel inside snapshots — a
        // restore would otherwise overwrite the destination device's pointer with the source
        // device's. The codec excludes them; verify the raw JSON contains nothing that looks
        // like a backup-pointer key, and that decode reuses the local defaults.
        val populatedSettings = AppSettings.Defaults.copy(
            backupFolderUri = "content://example/tree/123",
            backupFolderName = "Sensitive Folder",
            lastBackupAt = 99_999L,
            lastBackupSha = "abc123",
            hasCheckedForBackupRestore = true,
        )
        val snapshot = baselineSnapshot().copy(settings = populatedSettings)
        val rawJson = SnapshotCodec.encode(snapshot)

        assertFalse("URI must not appear in snapshot JSON", rawJson.contains("content://example"))
        assertFalse("backupFolderUri key must not appear", rawJson.contains("backupFolderUri"))
        assertFalse("backupFolderName key must not appear", rawJson.contains("backupFolderName"))
        assertFalse("lastBackupAt key must not appear", rawJson.contains("lastBackupAt"))
        assertFalse("lastBackupSha key must not appear", rawJson.contains("lastBackupSha"))
        assertFalse(
            "hasCheckedForBackupRestore key must not appear",
            rawJson.contains("hasCheckedForBackupRestore"),
        )

        val decoded = SnapshotCodec.decode(rawJson)
        // Codec returns the local-device defaults for these fields, regardless of input.
        assertNull(decoded.settings.backupFolderUri)
        assertNull(decoded.settings.backupFolderName)
        assertEquals(0L, decoded.settings.lastBackupAt)
        assertEquals("", decoded.settings.lastBackupSha)
        assertFalse(decoded.settings.hasCheckedForBackupRestore)
    }

    @Test
    fun `encoded JSON is structurally valid and contains expected top-level keys`() {
        val rawJson = SnapshotCodec.encode(fullSnapshot())
        val parsed = JSONObject(rawJson) // throws on malformed JSON

        listOf(
            "schemaVersion", "exportedAt", "settings",
            "programs", "days", "lifts", "alternatives",
            "sessions", "performedSets", "notes",
        ).forEach { key ->
            assertTrue("missing top-level key: $key", parsed.has(key))
        }
    }

    @Test
    fun `schema version is preserved verbatim`() {
        // Forward and back-compat: a snapshot written with schemaVersion = 99 should round-trip
        // with the same number. (BackupService gates the actual restore on a match — this test
        // only verifies the field travels intact.)
        val snapshot = baselineSnapshot().copy(schemaVersion = 99)
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(snapshot))
        assertEquals(99, decoded.schemaVersion)
    }

    @Test
    fun `program notes list survives round-trip`() {
        val program = ProgramEntity(
            id = "p1",
            name = "Test",
            cycleLength = 9,
            deloadWeek = 9,
            notes = listOf("first", "second", "third with a comma, here"),
        )
        val snapshot = baselineSnapshot().copy(programs = listOf(program))
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(snapshot))
        assertEquals(listOf("first", "second", "third with a comma, here"), decoded.programs.single().notes)
    }

    @Test
    fun `legacy snapshot with currentWeek decodes to a derived cycleStartedAt anchor`() {
        // Backward compat: pre-cycleStartedAt snapshots stored a stored `currentWeek` int. Decoder
        // reconstructs the anchor as `exportedAt - (currentWeek-1) * 7d` so the user's apparent
        // week is preserved when restoring an older file.
        val rawJson = SnapshotCodec.encode(baselineSnapshot())
        val mutated = JSONObject(rawJson)
        val settings = mutated.getJSONObject("settings")
        settings.remove("cycleStartedAt")
        settings.put("currentWeek", 4)
        val exportedAt = mutated.getLong("exportedAt")

        val decoded = SnapshotCodec.decode(mutated.toString())

        val expected = exportedAt - 3L * TimeUnit.DAYS.toMillis(7)
        assertEquals(expected, decoded.settings.cycleStartedAt)
    }

    @Test
    fun `reminder settings round-trip both leads and tier`() {
        val populatedSettings = AppSettings.Defaults.copy(
            reminderEnabledLeads = setOf(
                ReminderLead.NINETY_MIN.name,
                ReminderLead.MISSED.name,
            ),
            reminderTier = ReminderTier.PLAYFUL_SHAME,
        )
        val snapshot = baselineSnapshot().copy(settings = populatedSettings)
        val decoded = SnapshotCodec.decode(SnapshotCodec.encode(snapshot))

        assertEquals(
            setOf(ReminderLead.NINETY_MIN.name, ReminderLead.MISSED.name),
            decoded.settings.reminderEnabledLeads,
        )
        assertEquals(ReminderTier.PLAYFUL_SHAME, decoded.settings.reminderTier)
    }

    @Test
    fun `legacy snapshot without reminder fields decodes to defaults`() {
        // Pre-Bundle-C snapshots don't carry reminder keys. Decoder should fall back to the
        // current Defaults rather than throwing or producing a half-initialised settings object.
        val rawJson = SnapshotCodec.encode(baselineSnapshot())
        val mutated = JSONObject(rawJson)
        val settings = mutated.getJSONObject("settings")
        settings.remove("reminderEnabledLeads")
        settings.remove("reminderTier")

        val decoded = SnapshotCodec.decode(mutated.toString())

        assertEquals(
            AppSettings.Defaults.reminderEnabledLeads,
            decoded.settings.reminderEnabledLeads,
        )
        assertEquals(AppSettings.Defaults.reminderTier, decoded.settings.reminderTier)
    }

    @Test
    fun `decode tolerates absent optional fields`() {
        // Hand-craft a minimal JSON body the codec should still consume without throwing. This
        // simulates an older-version snapshot that didn't include optional booleans / strings.
        val minimalLift = """
            {
              "id":"l1","dayId":"d1","name":"Squat",
              "setsMin":3,"setsMax":4,"repsMin":5,"repsMax":8,
              "effort":"HIGH",
              "orderIndex":0
            }
        """.trimIndent()
        val full = SnapshotCodec.encode(baselineSnapshot()).let { JSONObject(it) }
        full.put("lifts", org.json.JSONArray().put(JSONObject(minimalLift)))

        val decoded = SnapshotCodec.decode(full.toString())
        val lift = decoded.lifts.single()
        // Defaulted because absent in the JSON — proves optInt/optString fallbacks work.
        assertEquals("", lift.muscle)
        assertEquals("", lift.equipment)
    }

    // ----- Sample data builders -----

    private fun baselineSnapshot() = Snapshot(
        schemaVersion = Snapshot.CURRENT_SCHEMA_VERSION,
        exportedAt = 1_700_000_000_000L,
        settings = AppSettings.Defaults,
        programs = emptyList(),
        days = emptyList(),
        lifts = emptyList(),
        alternatives = emptyList(),
        sessions = emptyList(),
        performedSets = emptyList(),
        notes = emptyList(),
    )

    private fun sampleDay() = DayEntity(
        id = "d1",
        programId = "p1",
        name = "Lower 1",
        dayOfWeek = Weekday.WED,
        focus = "Quads + glutes",
        isRest = false,
        orderIndex = 0,
    )

    private fun sampleLift() = LiftEntity(
        id = "l1",
        dayId = "d1",
        name = "Back Squat",
        setsMin = 3,
        setsMax = 4,
        repsMin = 5,
        repsMax = 8,
        effort = Effort.HIGH,
        muscle = "Quads",
        equipment = "Barbell",
        orderIndex = 0,
    )

    private fun fullSnapshot(): Snapshot {
        val program = ProgramEntity(
            id = "p1",
            name = "Upper/Lower Hybrid",
            cycleLength = 9,
            deloadWeek = 9,
            notes = listOf("Note 1", "Note 2 with special chars: éñ\u001Fhmm"),
        )
        val day = sampleDay()
        val lift = sampleLift()
        val alt = AlternativeEntity(
            id = "alt1",
            liftId = "l1",
            name = "Front Squat",
            muscle = "Quads",
            equipment = "Barbell",
            overlapPercent = 90,
        )
        val session = SessionEntity(
            id = "s1",
            dayId = "d1",
            weekNumber = 1,
            date = 1_700_000_000_000L,
            finishedAt = 1_700_003_600_000L,
        )
        val set = PerformedSetEntity(
            id = 1L,
            sessionId = "s1",
            liftId = "l1",
            setIndex = 1,
            weight = 225.0,
            reps = 5,
            whoopsy = false,
            rpe = 8,
            done = true,
            completedAt = 1_700_001_000_000L,
        )
        val note = NoteEntity(
            id = 1L,
            liftId = "l1",
            sessionId = "s1",
            date = 1_700_001_000_000L,
            text = "felt strong",
            whoopsy = false,
        )
        val settings = AppSettings.Defaults.copy(
            cycleStartedAt = 1_699_000_000_000L,
            unit = WeightUnit.KG,
            useDynamicColor = false,
            themeMode = ThemeMode.DARK,
            displayName = "Cole",
            bodyweight = 185.5,
            heightInches = 71,
            age = 28,
        )
        return Snapshot(
            schemaVersion = Snapshot.CURRENT_SCHEMA_VERSION,
            exportedAt = 1_700_000_000_000L,
            settings = settings,
            programs = listOf(program),
            days = listOf(day),
            lifts = listOf(lift),
            alternatives = listOf(alt),
            sessions = listOf(session),
            performedSets = listOf(set),
            notes = listOf(note),
        )
    }
}
