package com.colewinfield.liftingtracker.data

enum class Effort { HIGH, MED, LOW }

data class Lift(
    val id: String,
    val name: String,
    val sets: IntRange,
    val reps: IntRange,
    val effort: Effort,
    val muscle: String,
    val equipment: String,
)

data class Day(
    val id: String,
    val name: String,
    val dayOfWeek: String,
    val focus: String = "",
    val isRest: Boolean = false,
    val lifts: List<Lift> = emptyList(),
)

data class Program(
    val id: String,
    val name: String,
    val cycleLength: Int,
    val deloadWeek: Int,
    val days: List<Day>,
    val notes: List<String> = emptyList(),
)

data class Alternative(
    val liftId: String,
    val id: String,
    val name: String,
    val muscle: String,
    val equipment: String,
    val overlapPercent: Int,
)

data class HistorySet(val weight: Double, val reps: Int)

data class HistoryEntry(
    val week: Int,
    val date: String,
    val sets: List<HistorySet>,
    val notes: String = "",
)

data class PerformedSet(
    // 0L = transient (not yet persisted). Repository assigns the real id after insert.
    val id: Long = 0L,
    val weight: Double,
    val reps: Int,
    val done: Boolean = false,
    val whoopsy: Boolean = false,
)

data class CurrentState(
    val week: Int,
    val dayId: String,
    val dayIndex: Int,
)
