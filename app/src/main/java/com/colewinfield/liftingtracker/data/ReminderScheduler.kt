package com.colewinfield.liftingtracker.data

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Reminder scheduling lives on top of [WorkManager] for the same reasons as [BackupScheduler] —
 * we don't need surgical wake-up timing for personal-use lifting nudges, the OS handles persistence
 * across reboots, and it's already a dep.
 *
 * Each `(lead, weekday)` pair is enqueued as its own unique [OneTimeWorkRequest]. The worker
 * posts a notification on fire and re-enqueues itself for the same slot one week later via
 * [enqueueSingle]. This keeps us off `PeriodicWorkRequest` (which would coalesce all leads into
 * one cadence and drift week-over-week against the real session time).
 *
 * Why `REPLACE` policy: when the user toggles reminders or the program's day-of-week shape
 * changes, [applyReminders] re-issues every active slot. REPLACE ensures the new request takes
 * over without leaving the previous one to also fire.
 */
class ReminderScheduler(private val context: Context) {

    /**
     * Reconcile WorkManager state against the current settings + program. Iterates every possible
     * `(lead, weekday)` pair so disabled slots are explicitly cancelled by unique name — this
     * avoids stale work requests lingering after a setting flip.
     */
    fun applyReminders(program: Program?, settings: AppSettings) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val workoutWeekdays = program?.days?.filterNot { it.isRest }?.map { it.dayOfWeek }?.toSet()
            ?: emptySet()
        val enabledLeads = settings.reminderEnabledLeads
            .mapNotNull { runCatching { ReminderLead.valueOf(it) }.getOrNull() }
            .toSet()

        for (lead in ReminderLead.values()) {
            for (weekday in Weekday.values()) {
                val name = uniqueName(lead, weekday)
                val active = lead in enabledLeads && weekday in workoutWeekdays
                if (active) {
                    workManager.enqueueUniqueWork(
                        name,
                        ExistingWorkPolicy.REPLACE,
                        buildRequest(lead, weekday),
                    )
                } else {
                    workManager.cancelUniqueWork(name)
                }
            }
        }
    }

    /**
     * Enqueue a single `(lead, weekday)` slot for its next firing. Called by [ReminderWorker]
     * after it fires so the same slot is re-armed for one week later. Uses REPLACE so a parallel
     * [applyReminders] tick can't end up double-scheduling the same name.
     */
    fun enqueueSingle(lead: ReminderLead, weekday: Weekday) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueName(lead, weekday),
            ExistingWorkPolicy.REPLACE,
            buildRequest(lead, weekday),
        )
    }

    /** Cancel every reminder slot. Used if the user disconnects reminders entirely (future hook). */
    fun cancelAll() {
        val workManager = WorkManager.getInstance(context.applicationContext)
        for (lead in ReminderLead.values()) {
            for (weekday in Weekday.values()) {
                workManager.cancelUniqueWork(uniqueName(lead, weekday))
            }
        }
    }

    private fun buildRequest(lead: ReminderLead, weekday: Weekday): OneTimeWorkRequest {
        val nextMillis = nextFiringMillis(lead, weekday)
        val delay = (nextMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_LEAD, lead.name)
            .putString(ReminderWorker.KEY_WEEKDAY, weekday.name)
            .build()
        return OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
    }

    /**
     * Wall-clock millis for the next time `(lead, weekday)` should fire. For pre-session leads,
     * fire-time = session start − leadMinutes on that weekday; for [ReminderLead.MISSED], fire-time
     * = session start + [ReminderDefaults.MISSED_DELAY_MINUTES]. If today is the target weekday
     * but the slot has already passed, advances to next week.
     */
    private fun nextFiringMillis(
        lead: ReminderLead,
        weekday: Weekday,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val baseMin = ReminderDefaults.SESSION_START_HOUR * 60 + ReminderDefaults.SESSION_START_MINUTE
        val totalMin = if (lead == ReminderLead.MISSED) {
            baseMin + ReminderDefaults.MISSED_DELAY_MINUTES
        } else {
            baseMin - lead.minutesBefore
        }
        // Defensive — if the user ever changes session start very early in the day, a 3hr lead
        // could fall on the prior day. Wrap into [0, 1440) so the Calendar fields are valid.
        val dayMinutes = ((totalMin % (24 * 60)) + 24 * 60) % (24 * 60)
        val hour = dayMinutes / 60
        val minute = dayMinutes % 60

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetDow = weekdayToCalendarConst(weekday)
        val currentDow = cal.get(Calendar.DAY_OF_WEEK)
        val daysUntil = ((targetDow - currentDow + 7) % 7)
        cal.add(Calendar.DAY_OF_YEAR, daysUntil)
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 7)
        return cal.timeInMillis
    }

    private fun uniqueName(lead: ReminderLead, weekday: Weekday): String =
        "$NAME_PREFIX${lead.name}-${weekday.name}"

    private fun weekdayToCalendarConst(weekday: Weekday): Int = when (weekday) {
        Weekday.MON -> Calendar.MONDAY
        Weekday.TUE -> Calendar.TUESDAY
        Weekday.WED -> Calendar.WEDNESDAY
        Weekday.THU -> Calendar.THURSDAY
        Weekday.FRI -> Calendar.FRIDAY
        Weekday.SAT -> Calendar.SATURDAY
        Weekday.SUN -> Calendar.SUNDAY
    }

    companion object {
        private const val TAG = "lt-reminders"
        private const val NAME_PREFIX = "lt-reminder-"
    }
}
