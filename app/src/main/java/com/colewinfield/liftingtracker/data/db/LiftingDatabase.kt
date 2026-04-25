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
    ],
    // Bump on any schema-relevant change while we're still using fallbackToDestructiveMigration.
    // (v1 stored Day.dayOfWeek as a free-form String like "Monday"; v2 stores the Weekday enum
    // name like "MON", which the v1 rows can't deserialize.) Replace with proper Migration
    // objects before this app sees real-world data.
    version = 2,
    // exportSchema = true requires `ksp { arg("room.schemaLocation", ...) }` in build.gradle.kts.
    // Flip on when we start writing real migrations.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LiftingDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun sessionDao(): SessionDao

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
