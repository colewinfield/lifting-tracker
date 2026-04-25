package com.colewinfield.liftingtracker.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.data.Weekday

// IntRange isn't natively persistable, so set/rep ranges live as paired Min/Max columns.
// Order columns (orderIndex) preserve user-visible ordering in days/lifts.

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cycleLength: Int,
    val deloadWeek: Int,
    val notes: List<String>,
)

@Entity(
    tableName = "days",
    foreignKeys = [ForeignKey(
        entity = ProgramEntity::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("programId")],
)
data class DayEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val name: String,
    val dayOfWeek: Weekday,
    val focus: String,
    val isRest: Boolean,
    val orderIndex: Int,
)

@Entity(
    tableName = "lifts",
    foreignKeys = [ForeignKey(
        entity = DayEntity::class,
        parentColumns = ["id"],
        childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId")],
)
data class LiftEntity(
    @PrimaryKey val id: String,
    val dayId: String,
    val name: String,
    val setsMin: Int,
    val setsMax: Int,
    val repsMin: Int,
    val repsMax: Int,
    val effort: Effort,
    val muscle: String,
    val equipment: String,
    val orderIndex: Int,
)

@Entity(
    tableName = "alternatives",
    foreignKeys = [ForeignKey(
        entity = LiftEntity::class,
        parentColumns = ["id"],
        childColumns = ["liftId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("liftId")],
)
data class AlternativeEntity(
    @PrimaryKey val id: String,
    val liftId: String,
    val name: String,
    val muscle: String,
    val equipment: String,
    val overlapPercent: Int,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = DayEntity::class,
        parentColumns = ["id"],
        childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId"), Index("date"), Index("weekNumber")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val dayId: String,
    val weekNumber: Int,
    val date: Long,
    val finishedAt: Long?,
)

@Entity(
    tableName = "performed_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["liftId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("liftId")],
)
data class PerformedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val liftId: String,
    val setIndex: Int,
    val weight: Double,
    val reps: Int,
    val whoopsy: Boolean,
    val rpe: Int?,
    val done: Boolean,
    val completedAt: Long?,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = LiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["liftId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("liftId"), Index("sessionId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val liftId: String,
    val sessionId: String?,
    val date: Long,
    val text: String,
    val whoopsy: Boolean,
)

// ----- Catalog -----

// Read-only library of exercises (free-exercise-db, Unlicense). Seeded once from
// assets/exercises.json on first launch. Kept in its own table so program lifts and
// library lifts don't collide — a program lift carries user-customised set/rep ranges and
// belongs to a Day; a catalog row is just an exercise definition.
@Entity(
    tableName = "catalog_lifts",
    indices = [
        Index("primaryMuscle"),
        Index("equipment"),
        Index("name"),
    ],
)
data class CatalogLiftEntity(
    @PrimaryKey val id: String,            // free-exercise-db slug (e.g. "Romanian_Deadlift")
    val name: String,
    val primaryMuscle: String,             // normalized lowercase (e.g. "hamstrings")
    val secondaryMuscles: List<String>,    // normalized lowercase
    val equipment: String,                 // normalized lowercase ("" when unknown)
    val category: String,                  // strength / stretching / plyometrics / ...
    val force: String?,                    // push / pull / static / null
    val mechanic: String?,                 // compound / isolation / null
    val level: String,                     // beginner / intermediate / expert
)

// ----- Relations -----

data class DayWithLifts(
    @Embedded val day: DayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId")
    val lifts: List<LiftEntity>,
)

data class ProgramWithStructure(
    @Embedded val program: ProgramEntity,
    @Relation(
        entity = DayEntity::class,
        parentColumn = "id",
        entityColumn = "programId",
    )
    val days: List<DayWithLifts>,
)
