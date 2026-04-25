package com.colewinfield.liftingtracker.data

import android.content.Context
import com.colewinfield.liftingtracker.data.db.LiftingDatabase

// Hand-rolled service locator. No DI framework — this is a personal app and a couple of repos
// are enough. Add new singletons here when needed.
object AppContainer {

    @Volatile private var repo: LiftingRepository? = null
    @Volatile private var settingsRepo: SettingsRepository? = null

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

    private fun build(context: Context): LiftingRepository {
        val db = LiftingDatabase.get(context)
        return LiftingRepository(
            programDao = db.programDao(),
            sessionDao = db.sessionDao(),
            noteDao = db.noteDao(),
            catalogDao = db.catalogDao(),
            assetReader = { path -> context.applicationContext.assets.open(path) },
        )
    }
}
