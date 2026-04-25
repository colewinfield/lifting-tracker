package com.colewinfield.liftingtracker.data

enum class WeightUnit { LB, KG }

// Tri-state so users can pin Light/Dark *or* explicitly follow the system. The Profile JSX shows
// a single Switch, but persisting the override separately leaves room for an Auto/Light/Dark
// segmented control later without a migration.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val currentWeek: Int,
    val unit: WeightUnit,
    val useDynamicColor: Boolean,
    val themeMode: ThemeMode,
    // Profile fields. Stored alongside settings — they're effectively single-user prefs and
    // the DataStore file already rides Auto Backup, so no separate Room table is needed.
    // Bodyweight is stored as the user typed it (in `unit` at save time); we don't auto-convert
    // when units change, so a unit toggle doesn't silently rewrite the number.
    val displayName: String,
    val bodyweight: Double,
    val heightInches: Int,
    val age: Int,
) {
    companion object {
        // currentDayId isn't a setting — it's derived at runtime from Weekday.today() against
        // program.days. The week, however, only advances on user action (or future auto-advance).
        val Defaults = AppSettings(
            currentWeek = SampleData.defaultCurrentWeek,
            unit = WeightUnit.LB,
            useDynamicColor = true,
            themeMode = ThemeMode.SYSTEM,
            displayName = "You",
            bodyweight = 0.0,
            heightInches = 0,
            age = 0,
        )
    }
}
