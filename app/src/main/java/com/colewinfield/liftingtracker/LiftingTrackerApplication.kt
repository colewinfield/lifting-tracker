package com.colewinfield.liftingtracker

import android.app.Application
import com.colewinfield.liftingtracker.data.AppContainer
import com.colewinfield.liftingtracker.data.ReminderWorker
import com.colewinfield.liftingtracker.data.SampleData
import com.colewinfield.liftingtracker.data.cycleAnchorFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application subclass — runs once on cold start (and on every process restart). Two
 * responsibilities:
 *
 * 1. **Initialise `cycleStartedAt` on first launch.** AppSettings persists `cycleStartedAt: Long`
 *    (the wall-clock anchor for "week 1 of cycle 1"). On a fresh install, no value is stored, so
 *    [AppSettings.Defaults] returns the 0L sentinel. We translate that into a real anchor —
 *    `now - (defaultCurrentWeek - 1) × 7d` — so the live week derived by [weekAndCycle] matches
 *    the seeded program.
 *
 * 2. **Enqueue the periodic backup.** [BackupScheduler.schedulePeriodicBackup] uses
 *    `ExistingPeriodicWorkPolicy.KEEP`, so calling it on every launch is safe — we don't reset
 *    the schedule. The post-finish one-shot in [TodayViewModel] still covers the high-value
 *    capture path; the periodic worker catches out-of-session edits (notes, program edits,
 *    profile changes).
 *
 * Manifest registration: `android:name=".LiftingTrackerApplication"` on `<application>`.
 */
class LiftingTrackerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppContainer.backupScheduler(this).schedulePeriodicBackup()
        // Idempotent — channel creation is safe to run on every cold start. Required before any
        // notify() call on API 26+; ignored below.
        ReminderWorker.ensureChannel(this)
        appScope.launch {
            val settingsRepo = AppContainer.settings(this@LiftingTrackerApplication)
            val settings = settingsRepo.settings.first()
            if (settings.cycleStartedAt == 0L) {
                settingsRepo.setCycleStartedAt(cycleAnchorFor(SampleData.defaultCurrentWeek))
            }
            // Reconcile reminder WorkManager state against the current settings + program. Cold
            // starts often follow reboots / app updates that wipe pending work; this re-arms
            // every enabled (lead, weekday) slot. The scheduler uses REPLACE internally so this
            // is safe even when reminders are already scheduled. cycleStartedAt isn't consumed
            // here (only at fire time inside the worker), so reading settings once is enough.
            val program = AppContainer.repository(this@LiftingTrackerApplication)
                .observeCurrentProgram().first()
            AppContainer.reminderScheduler(this@LiftingTrackerApplication)
                .applyReminders(program, settings)
        }
    }
}
