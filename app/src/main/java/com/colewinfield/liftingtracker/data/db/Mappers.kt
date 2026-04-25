package com.colewinfield.liftingtracker.data.db

import com.colewinfield.liftingtracker.data.Alternative
import com.colewinfield.liftingtracker.data.Day
import com.colewinfield.liftingtracker.data.Lift
import com.colewinfield.liftingtracker.data.Note
import com.colewinfield.liftingtracker.data.PerformedSet
import com.colewinfield.liftingtracker.data.Program

// ----- Entity → Domain -----

fun ProgramWithStructure.toDomain(): Program = Program(
    id = program.id,
    name = program.name,
    cycleLength = program.cycleLength,
    deloadWeek = program.deloadWeek,
    notes = program.notes,
    days = days.sortedBy { it.day.orderIndex }.map { it.toDomain() },
)

fun DayWithLifts.toDomain(): Day = Day(
    id = day.id,
    name = day.name,
    dayOfWeek = day.dayOfWeek,
    focus = day.focus,
    isRest = day.isRest,
    lifts = lifts.sortedBy { it.orderIndex }.map { it.toDomain() },
)

fun LiftEntity.toDomain(): Lift = Lift(
    id = id,
    name = name,
    sets = setsMin..setsMax,
    reps = repsMin..repsMax,
    effort = effort,
    muscle = muscle,
    equipment = equipment,
)

fun AlternativeEntity.toDomain(): Alternative = Alternative(
    liftId = liftId,
    id = id,
    name = name,
    muscle = muscle,
    equipment = equipment,
    overlapPercent = overlapPercent,
)

fun PerformedSetEntity.toDomain(): PerformedSet = PerformedSet(
    id = id,
    weight = weight,
    reps = reps,
    done = done,
    whoopsy = whoopsy,
)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    liftId = liftId,
    sessionId = sessionId,
    date = date,
    text = text,
    whoopsy = whoopsy,
)

// ----- Domain → Entity (used by the seeder) -----

fun Program.toEntity(): ProgramEntity = ProgramEntity(
    id = id,
    name = name,
    cycleLength = cycleLength,
    deloadWeek = deloadWeek,
    notes = notes,
)

fun Day.toEntity(programId: String, orderIndex: Int): DayEntity = DayEntity(
    id = id,
    programId = programId,
    name = name,
    dayOfWeek = dayOfWeek,
    focus = focus,
    isRest = isRest,
    orderIndex = orderIndex,
)

fun Lift.toEntity(dayId: String, orderIndex: Int): LiftEntity = LiftEntity(
    id = id,
    dayId = dayId,
    name = name,
    setsMin = sets.first,
    setsMax = sets.last,
    repsMin = reps.first,
    repsMax = reps.last,
    effort = effort,
    muscle = muscle,
    equipment = equipment,
    orderIndex = orderIndex,
)

fun Alternative.toEntity(): AlternativeEntity = AlternativeEntity(
    id = id,
    liftId = liftId,
    name = name,
    muscle = muscle,
    equipment = equipment,
    overlapPercent = overlapPercent,
)

// ----- Catalog -----

/**
 * Project a catalog row into the existing [Alternative] domain type so the SwapSheet UI can
 * render curated alternatives and library matches in one list. The catalog row's slug becomes
 * the synthetic Alternative id (no DB row in `alternatives` is created).
 */
fun CatalogLiftEntity.toAlternative(sourceLiftId: String, overlapPercent: Int): Alternative =
    Alternative(
        liftId = sourceLiftId,
        id = id,
        name = name,
        muscle = displayMuscle(primaryMuscle),
        equipment = displayEquipment(equipment),
        overlapPercent = overlapPercent,
    )

/** Title-case a normalised muscle token for display. "lats" -> "Lats", "" -> "—". */
fun displayMuscle(normalized: String): String = when {
    normalized.isBlank() -> "\u2014"
    else -> normalized.split(' ').joinToString(" ") {
        it.replaceFirstChar { ch -> ch.titlecase() }
    }
}

/** Title-case equipment for display. "body only" -> "Body only", "" -> "—". */
fun displayEquipment(normalized: String): String = when {
    normalized.isBlank() -> "\u2014"
    else -> normalized.replaceFirstChar { it.titlecase() }
}
