package com.colewinfield.liftingtracker.data

import java.util.concurrent.TimeUnit

enum class WeightUnit { LB, KG }

// Tri-state so users can pin Light/Dark *or* explicitly follow the system. The Profile JSX shows
// a single Switch, but persisting the override separately leaves room for an Auto/Light/Dark
// segmented control later without a migration.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    // Anchor timestamp for the cyclic program — `cycleStartedAt` is the wall-clock millis at which
    // "week 1 of cycle 1" began. Live week + cycle are derived from elapsed time via
    // [weekAndCycle], so they advance automatically without a UI tap. A sentinel of 0L means "not
    // yet initialized"; the Application subclass persists a sensible default on first launch (now
    // anchored back so the active week matches `SampleData.defaultCurrentWeek`).
    val cycleStartedAt: Long,
    val unit: WeightUnit,
    val useDynamicColor: Boolean,
    val themeMode: ThemeMode,
    // Profile fields. Stored alongside settings — they're effectively single-user prefs and
    // the DataStore file already rides Auto Backup, so no separate Room table is needed.
    // Bodyweight is stored as the user typed it (in `unit` at save time); we don't auto-convert
    // when units change, so a unit toggle doesn't silently rewrite the number.
    val displayName: String,
    val bodyweight: Double,
    val heightInches: Int,
    val age: Int,
    // Backup (SAF). The user picks a folder once via ACTION_OPEN_DOCUMENT_TREE; we persist the
    // tree URI, the human-readable folder name (resolved from DocumentFile at pick time so the
    // UI can show "Backing up to {Backups}" without an IPC on every recompose), and the
    // timestamp/hash of the most recent successful write. `lastBackupSha` is a SHA-256 of the
    // snapshot JSON so we can skip re-uploads when nothing changed.
    // `hasCheckedForBackupRestore` is a one-shot flag that gates the first-launch "restore from
    // backup?" prompt — it flips to true the first time we either offer-and-decline or restore.
    val backupFolderUri: String?,
    val backupFolderName: String?,
    val lastBackupAt: Long,
    val lastBackupSha: String,
    val hasCheckedForBackupRestore: Boolean,
    // Reminders. `reminderEnabledLeads` holds the set of [ReminderLead] names the user has
    // enabled — empty means no notifications fire. `reminderTier` selects the message tone
    // ([ReminderTier]). Both feed [ReminderScheduler] and the worker reads them at fire time so
    // tier changes take effect on the next firing without rescheduling.
    val reminderEnabledLeads: Set<String>,
    val reminderTier: ReminderTier,
) {
    companion object {
        // currentDayId isn't a setting — it's derived at runtime from Weekday.today() against
        // program.days. The current week is also derived (from cycleStartedAt + elapsed time
        // against the program's cycleLength); see [weekAndCycle].
        val Defaults = AppSettings(
            cycleStartedAt = 0L,
            unit = WeightUnit.LB,
            useDynamicColor = true,
            themeMode = ThemeMode.SYSTEM,
            displayName = "You",
            bodyweight = 0.0,
            heightInches = 0,
            age = 0,
            backupFolderUri = null,
            backupFolderName = null,
            lastBackupAt = 0L,
            lastBackupSha = "",
            hasCheckedForBackupRestore = false,
            // Matches the JSX `NotificationsScreen` initial state — first 3 nudges on, MISSED off.
            reminderEnabledLeads = setOf(
                ReminderLead.THREE_HOURS.name,
                ReminderLead.NINETY_MIN.name,
                ReminderLead.THIRTY_MIN.name,
            ),
            // JSX defaults `tier = 2` → Firm.
            reminderTier = ReminderTier.FIRM,
        )
    }
}

private val MILLIS_PER_WEEK = TimeUnit.DAYS.toMillis(7)

/**
 * Derive the live (week, cycle) pair from a cycle anchor timestamp.
 *
 * - [week] is 1-indexed in 1..[cycleLength]
 * - [cycle] is 1-indexed (cycle 1 starts the moment [cycleStartedAt] anchors)
 *
 * If [cycleStartedAt] is the 0L sentinel, falls back to an implicit anchor `now - (defaultWeek-1)
 * × 7d` so a fresh launch (before the Application subclass writes a real anchor) still lands on
 * the seed's week. If [cycleLength] is non-positive, returns `(1, 1)` defensively.
 */
data class WeekCycle(val week: Int, val cycle: Int)

fun weekAndCycle(
    cycleStartedAt: Long,
    cycleLength: Int,
    now: Long = System.currentTimeMillis(),
): WeekCycle {
    if (cycleLength <= 0) return WeekCycle(week = 1, cycle = 1)
    val anchor = if (cycleStartedAt > 0L) cycleStartedAt
        else now - (SampleData.defaultCurrentWeek - 1).toLong() * MILLIS_PER_WEEK
    val weeksElapsed = ((now - anchor).coerceAtLeast(0L) / MILLIS_PER_WEEK).toInt()
    val cycle = (weeksElapsed / cycleLength) + 1
    val week = (weeksElapsed % cycleLength) + 1
    return WeekCycle(week = week, cycle = cycle)
}

/**
 * Convenience: compute the [cycleStartedAt] millis that would anchor "today" to a given week of
 * cycle 1. Used for the first-launch initialization and for any future "set my week to N" UI.
 */
fun cycleAnchorFor(week: Int, now: Long = System.currentTimeMillis()): Long =
    now - (week - 1).coerceAtLeast(0).toLong() * MILLIS_PER_WEEK
