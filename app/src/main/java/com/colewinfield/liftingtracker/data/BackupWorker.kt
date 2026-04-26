package com.colewinfield.liftingtracker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Calls [BackupService.backupNow] in the background. Triggered after `finishSession` (and any
 * other moment we want a snapshot to land), enqueued via [BackupScheduler].
 *
 * Result mapping:
 * - `Success` / `Skipped` → [Result.success]. Skipped is a "nothing to do" no-op, not a failure.
 * - `NoFolder` / `NoAccess` → [Result.success]. The user hasn't set up backup or has revoked
 *   permission; retrying won't fix that, so we don't burn battery on backoff.
 * - `Error` → [Result.retry]. Most write errors are transient (Drive offline, momentary I/O
 *   failure). WorkManager applies exponential backoff up to its default retry cap.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val service = AppContainer.backup(applicationContext)
        return when (val result = service.backupNow()) {
            is BackupResult.Success, is BackupResult.Skipped -> Result.success()
            BackupResult.NoFolder, BackupResult.NoAccess -> Result.success()
            is BackupResult.Error -> Result.retry()
        }
    }
}
