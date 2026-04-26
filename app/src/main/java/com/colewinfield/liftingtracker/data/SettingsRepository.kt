package com.colewinfield.liftingtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// File lives in /data/data/<pkg>/files/datastore/, which is covered by the default Auto Backup
// rules — settings will restore alongside the Room DB.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lt_settings")

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            cycleStartedAt = prefs[Keys.CycleStartedAt] ?: AppSettings.Defaults.cycleStartedAt,
            unit = prefs[Keys.Unit]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
                ?: AppSettings.Defaults.unit,
            useDynamicColor = prefs[Keys.UseDynamicColor] ?: AppSettings.Defaults.useDynamicColor,
            themeMode = prefs[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: AppSettings.Defaults.themeMode,
            displayName = prefs[Keys.DisplayName] ?: AppSettings.Defaults.displayName,
            bodyweight = prefs[Keys.Bodyweight] ?: AppSettings.Defaults.bodyweight,
            heightInches = prefs[Keys.HeightInches] ?: AppSettings.Defaults.heightInches,
            age = prefs[Keys.Age] ?: AppSettings.Defaults.age,
            backupFolderUri = prefs[Keys.BackupFolderUri],
            backupFolderName = prefs[Keys.BackupFolderName],
            lastBackupAt = prefs[Keys.LastBackupAt] ?: AppSettings.Defaults.lastBackupAt,
            lastBackupSha = prefs[Keys.LastBackupSha] ?: AppSettings.Defaults.lastBackupSha,
            hasCheckedForBackupRestore = prefs[Keys.HasCheckedForBackupRestore]
                ?: AppSettings.Defaults.hasCheckedForBackupRestore,
            reminderEnabledLeads = prefs[Keys.ReminderEnabledLeads]
                ?: AppSettings.Defaults.reminderEnabledLeads,
            reminderTier = prefs[Keys.ReminderTier]
                ?.let { runCatching { ReminderTier.valueOf(it) }.getOrNull() }
                ?: AppSettings.Defaults.reminderTier,
        )
    }

    suspend fun setCycleStartedAt(millis: Long) = store.edit { it[Keys.CycleStartedAt] = millis }
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

    // ----- Backup -----

    /**
     * Atomic write of the SAF tree URI plus its human-readable folder name. Pairing them avoids
     * a transient state where the URI is set but the display name is stale (e.g. UI showing the
     * old folder's name while the next backup writes to the new folder).
     */
    suspend fun setBackupFolder(uri: String?, name: String?) = store.edit { prefs ->
        if (uri == null) prefs.remove(Keys.BackupFolderUri) else prefs[Keys.BackupFolderUri] = uri
        if (name == null) prefs.remove(Keys.BackupFolderName) else prefs[Keys.BackupFolderName] = name
    }

    /**
     * Atomic write of the post-backup metadata. Both fields move together so a future read can't
     * observe a fresh timestamp paired with the previous-backup hash.
     */
    suspend fun setLastBackup(at: Long, sha: String) = store.edit { prefs ->
        prefs[Keys.LastBackupAt] = at
        prefs[Keys.LastBackupSha] = sha
    }

    suspend fun setHasCheckedForBackupRestore(checked: Boolean) = store.edit { prefs ->
        prefs[Keys.HasCheckedForBackupRestore] = checked
    }

    // ----- Reminders -----

    suspend fun setReminderEnabledLeads(leads: Set<String>) = store.edit { prefs ->
        prefs[Keys.ReminderEnabledLeads] = leads
    }

    suspend fun setReminderTier(tier: ReminderTier) = store.edit { prefs ->
        prefs[Keys.ReminderTier] = tier.name
    }

    private object Keys {
        val CycleStartedAt = longPreferencesKey("cycle_started_at")
        val Unit = stringPreferencesKey("unit")
        val UseDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val DisplayName = stringPreferencesKey("display_name")
        val Bodyweight = doublePreferencesKey("bodyweight")
        val HeightInches = intPreferencesKey("height_inches")
        val Age = intPreferencesKey("age")
        val BackupFolderUri = stringPreferencesKey("backup_folder_uri")
        val BackupFolderName = stringPreferencesKey("backup_folder_name")
        val LastBackupAt = longPreferencesKey("last_backup_at")
        val LastBackupSha = stringPreferencesKey("last_backup_sha")
        val HasCheckedForBackupRestore = booleanPreferencesKey("has_checked_for_backup_restore")
        val ReminderEnabledLeads = stringSetPreferencesKey("reminder_enabled_leads")
        val ReminderTier = stringPreferencesKey("reminder_tier")
    }

    companion object {
        fun create(context: Context): SettingsRepository =
            SettingsRepository(context.applicationContext.dataStore)
    }
}
