package com.colewinfield.liftingtracker.data

import java.util.Calendar

enum class Effort { HIGH, MED, LOW }

// Stays minSdk-24 friendly (java.time would need core library desugaring). The Calendar lookup
// below maps Calendar.SUNDAY..SATURDAY constants onto our MON-first ordering.
enum class Weekday(val label: String, val short: String) {
    MON("Monday", "Mon"),
    TUE("Tuesday", "Tue"),
    WED("Wednesday", "Wed"),
    THU("Thursday", "Thu"),
    FRI("Friday", "Fri"),
    SAT("Saturday", "Sat"),
    SUN("Sunday", "Sun");

    companion object {
        fun today(): Weekday {
            val cal = Calendar.getInstance()
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> MON
                Calendar.TUESDAY -> TUE
                Calendar.WEDNESDAY -> WED
                Calendar.THURSDAY -> THU
                Calendar.FRIDAY -> FRI
                Calendar.SATURDAY -> SAT
                Calendar.SUNDAY -> SUN
                else -> MON
            }
        }
    }
}

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
    val dayOfWeek: Weekday,
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
