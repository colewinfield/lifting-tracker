package com.colewinfield.liftingtracker.data.db

import androidx.room.TypeConverter
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.data.Weekday

// Notes are stored as a single TEXT column joined by U+001F (unit separator). Safe because
// no user-typed coaching note will contain an ASCII control character.
private const val LIST_DELIMITER = "\u001F"

class Converters {
    @TypeConverter
    fun effortToString(effort: Effort): String = effort.name

    @TypeConverter
    fun stringToEffort(value: String): Effort = Effort.valueOf(value)

    @TypeConverter
    fun stringListToString(list: List<String>): String = list.joinToString(LIST_DELIMITER)

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LIST_DELIMITER)

    @TypeConverter
    fun weekdayToString(weekday: Weekday): String = weekday.name

    @TypeConverter
    fun stringToWeekday(value: String): Weekday = Weekday.valueOf(value)
}
