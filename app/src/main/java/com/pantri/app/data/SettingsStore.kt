package com.pantri.app.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

data class Settings(
    val darkMode: Boolean = false,
    val haptics: Boolean = true,
    val defaultBudgetCentavos: Long = 600_000,
    /** Folder (a content:// tree address) the backups go to; null until chosen. */
    val backupFolder: String? = null,
    /** Back up every this many days; 0 is off. Ignored when [backupWeekday] is set. */
    val backupDays: Int = 0,
    /** 1 (Monday) to 7 (Sunday) for a weekly backup on that day; 0 for none. */
    val backupWeekday: Int = 0,
    val lastBackupAt: Long = 0,
    val backupError: String? = null,
)

class SettingsStore(private val context: Context) {
    private val dark = booleanPreferencesKey("dark_mode")
    private val haptics = booleanPreferencesKey("haptics")
    private val budget = longPreferencesKey("default_budget")
    private val folder = stringPreferencesKey("backup_folder")
    private val days = intPreferencesKey("backup_days")
    private val weekday = intPreferencesKey("backup_weekday")
    private val last = longPreferencesKey("backup_last")
    private val error = stringPreferencesKey("backup_error")

    val settings: Flow<Settings> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        Settings(it[dark] ?: false, it[haptics] ?: true, it[budget] ?: 600_000, it[folder], it[days] ?: 0, it[weekday] ?: 0, it[last] ?: 0, it[error])
    }

    suspend fun setDarkMode(on: Boolean) { context.dataStore.edit { it[dark] = on } }
    suspend fun setHaptics(on: Boolean) { context.dataStore.edit { it[haptics] = on } }
    suspend fun setDefaultBudget(centavos: Long) { context.dataStore.edit { it[budget] = centavos } }

    suspend fun setBackupFolder(uri: String) { context.dataStore.edit { it[folder] = uri; it.remove(error) } }
    suspend fun setBackupSchedule(everyDays: Int, onWeekday: Int) { context.dataStore.edit { it[days] = everyDays; it[weekday] = onWeekday } }

    /** Records how the last backup went: [error] null means it worked at [at]. */
    suspend fun setBackupResult(at: Long, error: String?) {
        context.dataStore.edit {
            if (error == null) { it[last] = at; it.remove(this.error) } else it[this.error] = error
        }
    }
}
