package com.colewinfield.liftingtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// File lives in /data/data/<pkg>/files/datastore/, which is covered by the default Auto Backup
// rules — settings will restore alongside the Room DB.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lt_settings")

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            currentWeek = prefs[Keys.CurrentWeek] ?: AppSettings.Defaults.currentWeek,
            unit = prefs[Keys.Unit]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
                ?: AppSettings.Defaults.unit,
            useDynamicColor = prefs[Keys.UseDynamicColor] ?: AppSettings.Defaults.useDynamicColor,
            themeMode = prefs[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: AppSettings.Defaults.themeMode,
            displayName = prefs[Keys.DisplayName] ?: AppSettings.Defaults.displayName,
            bodyweight = prefs[Keys.Bodyweight] ?: AppSettings.Defaults.bodyweight,
            heightInches = prefs[Keys.HeightInches] ?: AppSettings.Defaults.heightInches,
            age = prefs[Keys.Age] ?: AppSettings.Defaults.age,
        )
    }

    suspend fun setCurrentWeek(week: Int) = store.edit { it[Keys.CurrentWeek] = week }
    suspend fun setUnit(unit: WeightUnit) = store.edit { it[Keys.Unit] = unit.name }
    suspend fun setUseDynamicColor(enabled: Boolean) = store.edit { it[Keys.UseDynamicColor] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.ThemeMode] = mode.name }

    /**
     * Atomic write of every profile field — used by the Edit profile sheet so a partial edit
     * can't observe a half-updated profile snapshot.
     */
    suspend fun setProfile(
        displayName: String,
        bodyweight: Double,
        heightInches: Int,
        age: Int,
    ) = store.edit { prefs ->
        prefs[Keys.DisplayName] = displayName
        prefs[Keys.Bodyweight] = bodyweight
        prefs[Keys.HeightInches] = heightInches
        prefs[Keys.Age] = age
    }

    private object Keys {
        val CurrentWeek = intPreferencesKey("current_week")
        val Unit = stringPreferencesKey("unit")
        val UseDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val DisplayName = stringPreferencesKey("display_name")
        val Bodyweight = doublePreferencesKey("bodyweight")
        val HeightInches = intPreferencesKey("height_inches")
        val Age = intPreferencesKey("age")
    }

    companion object {
        fun create(context: Context): SettingsRepository =
            SettingsRepository(context.applicationContext.dataStore)
    }
}
