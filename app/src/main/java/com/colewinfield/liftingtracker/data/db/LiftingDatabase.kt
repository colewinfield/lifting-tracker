package com.colewinfield.liftingtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProgramEntity::class,
        DayEntity::class,
        LiftEntity::class,
        AlternativeEntity::class,
        SessionEntity::class,
        PerformedSetEntity::class,
        NoteEntity::class,
        CatalogLiftEntity::class,
    ],
    // Bump on any schema-relevant change while we're still using fallbackToDestructiveMigration.
    // v1: free-form day-of-week string. v2: Weekday enum + paired converter. v3: catalog_lifts
    // table for the free-exercise-db library. Replace with proper Migration objects before this
    // app sees real-world data.
    version = 3,
    // exportSchema = true requires `ksp { arg("room.schemaLocation", ...) }` in build.gradle.kts.
    // Flip on when we start writing real migrations.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LiftingDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao
    abstract fun noteDao(): NoteDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        const val DB_NAME = "lifting.db"

        @Volatile
        private var INSTANCE: LiftingDatabase? = null

        fun get(context: Context): LiftingDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context).also { INSTANCE = it }
        }

        private fun build(context: Context): LiftingDatabase = Room.databaseBuilder(
            context.applicationContext,
            LiftingDatabase::class.java,
            DB_NAME,
        )
            // Schema is still in flux; once we ship to a real device with persisted data we
            // need to write proper Migration objects instead of dropping tables.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
