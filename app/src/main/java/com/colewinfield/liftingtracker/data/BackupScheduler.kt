package com.colewinfield.liftingtracker.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper over [WorkManager] so a ViewModel can request a backup without taking a
 * direct dependency on Android's WorkManager API (or holding a Context). One scheduler
 * instance lives in [AppContainer] with the application context baked in.
 *
 * Why `KEEP` policy: rapid-fire callers (e.g. several `finishSession` taps in a debounce
 * window, or a background trigger overlapping a user "Back up now") all coalesce onto a
 * single in-flight job. Combined with [BackupService]'s SHA short-circuit, this means a
 * burst of triggers degenerates to "one I/O, then no-ops" without any extra plumbing.
 *
 * Why `NetworkType.CONNECTED`: the expected backup destination is a Drive folder via the
 * Drive Android SAF provider, which needs network. A locally-stored folder will also have
 * network on most devices most of the time; if not, the worker just defers until it does.
 */
class BackupScheduler(private val context: Context) {

    fun scheduleBackupNow() {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(networkConstraint())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Idempotent enqueue of the periodic background backup. Designed to be called from the
     * [com.colewinfield.liftingtracker.LiftingTrackerApplication] `Application.onCreate` on every cold
     * start — `KEEP` policy means we don't reset the existing schedule if one already exists.
     *
     * Why 6 hours:
     * - The high-value capture (post-Finish) is already covered by [scheduleBackupNow].
     * - Periodic ticks catch out-of-session edits — notes added from Detail, program edits,
     *   profile changes. Those don't happen continuously, so 6h is plenty.
     * - `BackupService`'s SHA short-circuit makes idle ticks very cheap (read DB → encode → hash
     *   → compare → no I/O), and the 6-hour cadence keeps the wakeup count low (≤4/day).
     */
    fun schedulePeriodicBackup() {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkConstraint())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    private fun networkConstraint(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        private const val WORK_NAME = "lt-backup-now"
        private const val PERIODIC_WORK_NAME = "lt-backup-periodic"
    }
}
