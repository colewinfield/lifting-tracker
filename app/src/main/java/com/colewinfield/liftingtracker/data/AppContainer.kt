package com.colewinfield.liftingtracker.data

import android.content.Context
import com.colewinfield.liftingtracker.data.db.LiftingDatabase

// Hand-rolled service locator. No DI framework — this is a personal app and one repo is enough.
// Future: if we add a settings repository / sync engine, register them here too.
object AppContainer {

    @Volatile
    private var repo: LiftingRepository? = null

    fun repository(context: Context): LiftingRepository {
        repo?.let { return it }
        return synchronized(this) {
            repo ?: build(context).also { repo = it }
        }
    }

    private fun build(context: Context): LiftingRepository {
        val db = LiftingDatabase.get(context)
        return LiftingRepository(db.programDao(), db.sessionDao())
    }
}
