package com.colewinfield.liftingtracker.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colewinfield.liftingtracker.MainActivity
import com.colewinfield.liftingtracker.R
import kotlinx.coroutines.flow.first

/**
 * Fires a single reminder notification for a `(lead, weekday)` slot, then re-arms the same slot
 * for one week later via [ReminderScheduler.enqueueSingle].
 *
 * Design notes:
 * - Reads the current tier + enabled-leads from [SettingsRepository] at fire time, not at
 *   schedule time, so a tone change takes effect on the very next firing without rescheduling.
 * - For [ReminderLead.MISSED], skips the notification when [LiftingRepository.isSessionFinished]
 *   reports the user already finished today's session — the nudge would be embarrassing.
 * - On API 33+ where POST_NOTIFICATIONS hasn't been granted, the worker silently no-ops the
 *   notification. The Reminders screen handles requesting that permission when the user enables
 *   the first lead, so this guard is the belt to the screen's suspenders.
 * - Always re-arms the slot at the end (even if we suppressed the notification), so a one-off
 *   skip doesn't permanently silence the slot.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lead = inputData.getString(KEY_LEAD)
            ?.let { runCatching { ReminderLead.valueOf(it) }.getOrNull() }
            ?: return Result.success()
        val weekday = inputData.getString(KEY_WEEKDAY)
            ?.let { runCatching { Weekday.valueOf(it) }.getOrNull() }
            ?: return Result.success()

        val settingsRepo = AppContainer.settings(applicationContext)
        val settings = settingsRepo.settings.first()

        // Lead disabled since scheduling — drop the firing without re-arming so the slot stays
        // dormant until applyReminders re-enqueues it.
        if (lead.name !in settings.reminderEnabledLeads) {
            return Result.success()
        }

        val shouldFire = if (lead == ReminderLead.MISSED) {
            !isAlreadyFinishedToday(weekday, settings)
        } else true

        if (shouldFire) postNotification(lead, settings.reminderTier)

        AppContainer.reminderScheduler(applicationContext).enqueueSingle(lead, weekday)
        return Result.success()
    }

    private suspend fun isAlreadyFinishedToday(weekday: Weekday, settings: AppSettings): Boolean {
        val repo = AppContainer.repository(applicationContext)
        val program = repo.observeCurrentProgram().first() ?: return false
        val day = program.days.firstOrNull { it.dayOfWeek == weekday } ?: return false
        val (week, _) = weekAndCycle(settings.cycleStartedAt, program.cycleLength)
        return repo.isSessionFinished(day.id, week)
    }

    private fun postNotification(lead: ReminderLead, tier: ReminderTier) {
        if (!hasNotificationPermission(applicationContext)) return
        ensureChannel(applicationContext)

        val copy = reminderCopyFor(tier, lead)
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tap = PendingIntent.getActivity(applicationContext, 0, tapIntent, pendingFlags)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()

        // Notification id partitioned by lead so a fresh THREE_HOURS doesn't overwrite an
        // already-shown NINETY_MIN. We don't partition by weekday because at most one weekday's
        // worker fires for a given lead at any moment.
        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID_BASE + lead.ordinal, notification)
    }

    companion object {
        const val KEY_LEAD = "lead"
        const val KEY_WEEKDAY = "weekday"
        const val CHANNEL_ID = "lt_reminders"
        private const val CHANNEL_NAME = "Lifting reminders"
        private const val CHANNEL_DESCRIPTION = "Pre-session nudges and missed-session alerts"
        private const val NOTIFICATION_ID_BASE = 1000

        /**
         * Idempotent channel creation. Required on API 26+; below that the call is skipped and
         * the channel id is ignored at notify time. Safe to call from
         * [com.colewinfield.liftingtracker.LiftingTrackerApplication.onCreate] on every cold start.
         */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        fun hasNotificationPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
