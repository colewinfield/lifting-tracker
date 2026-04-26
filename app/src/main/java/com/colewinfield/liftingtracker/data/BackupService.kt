package com.colewinfield.liftingtracker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

/**
 * SAF-backed backup. The user picks a folder once via `ACTION_OPEN_DOCUMENT_TREE`; we persist
 * the tree URI permission and read/write a single `lifting-tracker-snapshot.json` inside it. The
 * picked folder can be on Google Drive (the Drive Android app exposes Drive as a SAF provider —
 * authority `com.google.android.apps.docs.storage`), OneDrive, Dropbox, internal storage, an SD
 * card, anywhere. Same code path for every provider.
 *
 * Why not Drive AppData / Sign-In: AppData would be invisible-and-automatic but requires Cloud
 * Console OAuth setup and a `google-services.json`. SAF achieves the same online-persistence
 * outcome with no external setup, at the cost of one folder pick at install time. See HANDOFF.md
 * "Backup (SAF)" for the trade-off.
 *
 * The service is stateless beyond DataStore — every call resolves the folder URI fresh, so a
 * permission revoked in system settings surfaces as `BackupResult.NoAccess` on the next attempt
 * rather than a stale-handle crash.
 */
class BackupService(
    private val applicationContext: Context,
    private val repository: LiftingRepository,
    private val settingsRepo: SettingsRepository,
) {

    // Serialises backup + restore so a WorkManager auto-trigger and a user "Back up now" tap
    // can't race each other (or worse, race a restore). Held across the entire I/O; the SHA
    // short-circuit means a queued duplicate after a real write becomes a fast no-op.
    private val ioMutex = Mutex()

    /**
     * Persist the URI permission returned by the SAF picker so it survives reboots. The picker's
     * Intent already includes the grant flags, but `takePersistableUriPermission` is what makes
     * Android *remember* the grant — without this, the URI works for the current process only.
     */
    suspend fun setBackupFolder(uri: Uri): Result<Unit> = runCatching {
        applicationContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        // Resolve the human-readable folder name once at pick time so the Profile UI can show
        // "Backing up to {Backups}" without an IPC on every recompose. Falls back to null if the
        // provider doesn't supply a name (rare — both Drive and the AOSP picker do).
        val displayName = runCatching {
            DocumentFile.fromTreeUri(applicationContext, uri)?.name
        }.getOrNull()
        settingsRepo.setBackupFolder(uri = uri.toString(), name = displayName)
    }

    /**
     * Drop the persisted URI permission AND clear the stored URI from settings. Both happen so
     * the UI doesn't end up showing "backup folder set" when we no longer have access. Best-effort:
     * a permission release that throws (e.g. URI already invalid) still clears the local pointer.
     */
    suspend fun clearBackupFolder() {
        val uriStr = settingsRepo.settings.first().backupFolderUri
        if (uriStr != null) {
            runCatching {
                applicationContext.contentResolver.releasePersistableUriPermission(
                    Uri.parse(uriStr),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        settingsRepo.setBackupFolder(uri = null, name = null)
        // Re-arm the "found a backup, restore?" prompt for the next folder pick. Without this,
        // a deliberate disconnect-and-reconnect would silently overwrite whatever snapshot was in
        // the newly picked folder.
        settingsRepo.setHasCheckedForBackupRestore(false)
    }

    /**
     * True if we still hold a persisted read+write grant for the configured folder. Lets the UI
     * disambiguate "no folder set yet" from "folder was set but permission was revoked from
     * system settings", and the first-launch restore prompt from skipping silently when access
     * is gone.
     */
    suspend fun isBackupFolderAccessible(): Boolean {
        val uriStr = settingsRepo.settings.first().backupFolderUri ?: return false
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return false
        return applicationContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    /**
     * Write the current snapshot to `lifting-tracker-snapshot.json` inside the configured folder.
     * Skipped (no I/O performed) when the snapshot's SHA-256 matches the last successful write —
     * the WorkManager auto-trigger will hammer this every period and we don't want to churn
     * Drive every time even though nothing changed.
     *
     * The write is atomic-ish: we write to `<filename>.partial`, verify the bytes by SHA, then
     * delete the previous final and rename the partial. A crash mid-write leaves `.partial` on
     * disk with junk content but the previous final is untouched. A crash mid-rename (after the
     * delete, before the rename) leaves only `.partial` — `restoreNow` falls back to it. The
     * next backup cleans up any stale partial before starting, so a partial never ages past one
     * cycle.
     */
    suspend fun backupNow(): BackupResult = ioMutex.withLock {
        val current = settingsRepo.settings.first()
        val uriStr = current.backupFolderUri ?: return@withLock BackupResult.NoFolder
        val folderUri = runCatching { Uri.parse(uriStr) }.getOrNull()
            ?: return@withLock BackupResult.NoFolder
        if (!isBackupFolderAccessible()) return@withLock BackupResult.NoAccess

        val snapshot = repository.exportSnapshot(current)
        val json = SnapshotCodec.encode(snapshot)
        val sha = sha256(json)

        if (sha == current.lastBackupSha && current.lastBackupAt > 0L) {
            return@withLock BackupResult.Skipped(at = current.lastBackupAt)
        }

        val tree = DocumentFile.fromTreeUri(applicationContext, folderUri)
            ?: return@withLock BackupResult.Error("Couldn't open backup folder.")

        // 1. Clean up any stale partial from a prior crashed run. createFile with the same name
        //    would otherwise create a sibling (Android doesn't overwrite by name in SAF).
        tree.findFile(Snapshot.PARTIAL_FILENAME)?.delete()

        // 2. Write JSON to .partial, then verify by reading back. Verification catches storage
        //    corruption (rare but real on some Drive providers under low-memory pressure) before
        //    we promote — better to fail here than silently store a torn file.
        val partial = tree.createFile(Snapshot.MIME_TYPE, Snapshot.PARTIAL_FILENAME)
            ?: return@withLock BackupResult.Error("Couldn't create temp backup file.")
        try {
            applicationContext.contentResolver.openOutputStream(partial.uri, "w")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: run {
                runCatching { partial.delete() }
                return@withLock BackupResult.Error("Couldn't open backup file for writing.")
            }
        } catch (e: Exception) {
            runCatching { partial.delete() }
            return@withLock BackupResult.Error(e.message ?: "Write failed.")
        }
        val verifySha = try {
            applicationContext.contentResolver.openInputStream(partial.uri)?.use { input ->
                sha256(input.readBytes().toString(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            null
        }
        if (verifySha != sha) {
            runCatching { partial.delete() }
            return@withLock BackupResult.Error("Wrote backup but verification failed; aborted to keep previous backup intact.")
        }

        // 3. Promote: delete the existing final (if any), rename partial → final. The window
        //    between (delete) and (rename) is the only place where a crash leaves only the
        //    partial; restoreNow falls back to it explicitly.
        tree.findFile(Snapshot.FILENAME)?.delete()
        if (!partial.renameTo(Snapshot.FILENAME)) {
            return@withLock BackupResult.Error(
                "Wrote backup as '${Snapshot.PARTIAL_FILENAME}' but couldn't promote it. " +
                    "Restore will pick it up automatically; on the next backup we'll retry the rename."
            )
        }

        val now = System.currentTimeMillis()
        settingsRepo.setLastBackup(at = now, sha = sha)
        BackupResult.Success(at = now)
    }

    /**
     * Whether a snapshot file exists in the configured folder. Falls back to the `.partial` if
     * the final is missing — that means the most recent backup crashed during the rename step,
     * and the partial is the latest valid snapshot. Used by the first-launch restore prompt to
     * decide whether to offer a restore at all.
     */
    suspend fun hasExistingSnapshot(): Boolean {
        val uriStr = settingsRepo.settings.first().backupFolderUri ?: return false
        val folderUri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return false
        if (!isBackupFolderAccessible()) return false
        val tree = DocumentFile.fromTreeUri(applicationContext, folderUri) ?: return false
        return findSnapshotFile(tree) != null
    }

    /** Find the snapshot file, falling back to `.partial` if the final is missing. */
    private fun findSnapshotFile(tree: DocumentFile): DocumentFile? =
        tree.findFile(Snapshot.FILENAME)
            ?: tree.findFile(Snapshot.PARTIAL_FILENAME)?.takeIf { it.exists() }

    /**
     * Read + apply the snapshot. Wipes local user data (programs / days / lifts / sessions /
     * sets / notes) and replaces with snapshot contents in a single transaction; settings are
     * restored field-by-field via SettingsRepository (DataStore lives outside Room).
     *
     * Schema-version mismatches abort cleanly without touching the local DB — we only restore
     * snapshots produced by an exact-matching schema. A "soft" upgrade path can come later when
     * we actually have schema migrations, not while we're still on `fallbackToDestructiveMigration`.
     */
    suspend fun restoreNow(): RestoreResult = ioMutex.withLock {
        val current = settingsRepo.settings.first()
        val uriStr = current.backupFolderUri ?: return@withLock RestoreResult.NoFolder
        val folderUri = runCatching { Uri.parse(uriStr) }.getOrNull()
            ?: return@withLock RestoreResult.NoFolder
        if (!isBackupFolderAccessible()) return@withLock RestoreResult.NoAccess

        val tree = DocumentFile.fromTreeUri(applicationContext, folderUri)
            ?: return@withLock RestoreResult.Error("Couldn't open backup folder.")
        val file = findSnapshotFile(tree)
            ?: return@withLock RestoreResult.NotFound

        val json = try {
            applicationContext.contentResolver.openInputStream(file.uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return@withLock RestoreResult.Error("Couldn't open backup file for reading.")
        } catch (e: Exception) {
            return@withLock RestoreResult.Error(e.message ?: "Read failed.")
        }

        val snapshot = try {
            SnapshotCodec.decode(json)
        } catch (e: Exception) {
            return@withLock RestoreResult.Error("Backup file is corrupt: ${e.message}")
        }

        if (snapshot.schemaVersion != Snapshot.CURRENT_SCHEMA_VERSION) {
            return@withLock RestoreResult.SchemaMismatch(
                snapshotVersion = snapshot.schemaVersion,
                currentVersion = Snapshot.CURRENT_SCHEMA_VERSION,
            )
        }

        repository.importSnapshot(snapshot)
        // Settings restore — write each field via the existing setters. Backup-local fields
        // (folder URI, lastBackupAt/Sha, hasCheckedForBackupRestore) intentionally stay as they
        // were on this device; the codec already excludes them from the snapshot.
        val s = snapshot.settings
        settingsRepo.setCycleStartedAt(s.cycleStartedAt)
        settingsRepo.setUnit(s.unit)
        settingsRepo.setUseDynamicColor(s.useDynamicColor)
        settingsRepo.setThemeMode(s.themeMode)
        settingsRepo.setProfile(
            displayName = s.displayName,
            bodyweight = s.bodyweight,
            heightInches = s.heightInches,
            age = s.age,
        )
        // The local DB now exactly matches the file we just read, so future backups should
        // short-circuit until something changes. Recording the snapshot's own hash + its
        // exportedAt makes "Last backup" read like "5 mins ago" right after a fresh backup.
        settingsRepo.setLastBackup(at = snapshot.exportedAt, sha = sha256(json))
        settingsRepo.setHasCheckedForBackupRestore(true)

        RestoreResult.Success(snapshotExportedAt = snapshot.exportedAt)
    }

    /** Mark the first-launch restore prompt as resolved without restoring. */
    suspend fun dismissRestorePrompt() {
        settingsRepo.setHasCheckedForBackupRestore(true)
    }

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

sealed class BackupResult {
    /** Wrote a fresh file. [at] is the new lastBackupAt. */
    data class Success(val at: Long) : BackupResult()

    /** Snapshot SHA matched the previous successful write — no I/O performed. [at] is the
     *  prior lastBackupAt, so the UI can still display "Last backup: ...". */
    data class Skipped(val at: Long) : BackupResult()

    /** No folder configured yet (first-time setup needed). */
    data object NoFolder : BackupResult()

    /** Folder URI is stored, but the persisted permission is gone (user revoked it). */
    data object NoAccess : BackupResult()

    /** Anything else — message is human-readable, may surface to a Snackbar. */
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    /** Local DB now reflects the snapshot. [snapshotExportedAt] is the exportedAt timestamp
     *  from the snapshot file (i.e., when the backup was originally taken). */
    data class Success(val snapshotExportedAt: Long) : RestoreResult()

    data object NoFolder : RestoreResult()
    data object NoAccess : RestoreResult()

    /** Folder is set + accessible, but no snapshot file inside it. */
    data object NotFound : RestoreResult()

    /** Snapshot is from a different schema version — refused to avoid data corruption. */
    data class SchemaMismatch(val snapshotVersion: Int, val currentVersion: Int) : RestoreResult()

    data class Error(val message: String) : RestoreResult()
}
