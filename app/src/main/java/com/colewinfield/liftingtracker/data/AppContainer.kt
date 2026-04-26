package com.colewinfield.liftingtracker.data

import android.content.Context
import com.colewinfield.liftingtracker.data.db.LiftingDatabase

// Hand-rolled service locator. No DI framework — this is a personal app and a couple of repos
// are enough. Add new singletons here when needed.
object AppContainer {

    @Volatile private var repo: LiftingRepository? = null
    @Volatile private var settingsRepo: SettingsRepository? = null
    @Volatile private var backup: BackupService? = null
    @Volatile private var backupScheduler: BackupScheduler? = null
    @Volatile private var reminderScheduler: ReminderScheduler? = null

    fun repository(context: Context): LiftingRepository {
        repo?.let { return it }
        return synchronized(this) {
            repo ?: build(context).also { repo = it }
        }
    }

    fun settings(context: Context): SettingsRepository {
        settingsRepo?.let { return it }
        return synchronized(this) {
            settingsRepo ?: SettingsRepository.create(context).also { settingsRepo = it }
        }
    }

    fun backup(context: Context): BackupService {
        backup?.let { return it }
        return synchronized(this) {
            backup ?: BackupService(
                applicationContext = context.applicationContext,
                repository = repository(context),
                settingsRepo = settings(context),
            ).also { backup = it }
        }
    }

    fun backupScheduler(context: Context): BackupScheduler {
        backupScheduler?.let { return it }
        return synchronized(this) {
            backupScheduler ?: BackupScheduler(context.applicationContext).also {
                backupScheduler = it
            }
        }
    }

    fun reminderScheduler(context: Context): ReminderScheduler {
        reminderScheduler?.let { return it }
        return synchronized(this) {
            reminderScheduler ?: ReminderScheduler(context.applicationContext).also {
                reminderScheduler = it
            }
        }
    }

    private fun build(context: Context): LiftingRepository {
        val db = LiftingDatabase.get(context)
        return LiftingRepository(
            database = db,
            programDao = db.programDao(),
            sessionDao = db.sessionDao(),
            noteDao = db.noteDao(),
            catalogDao = db.catalogDao(),
            assetReader = { path -> context.applicationContext.assets.open(path) },
        )
    }
}
