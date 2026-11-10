package com.pantri.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val darkMode: Boolean = false,
    val haptics: Boolean = true,
    val defaultBudgetCentavos: Long = 600_000,
)

class SettingsStore(private val context: Context) {
    private val dark = booleanPreferencesKey("dark_mode")
    private val haptics = booleanPreferencesKey("haptics")
    private val budget = longPreferencesKey("default_budget")

    val settings: Flow<Settings> = context.dataStore.data.map {
        Settings(it[dark] ?: false, it[haptics] ?: true, it[budget] ?: 600_000)
    }

    suspend fun setDarkMode(on: Boolean) { context.dataStore.edit { it[dark] = on } }
    suspend fun setHaptics(on: Boolean) { context.dataStore.edit { it[haptics] = on } }
    suspend fun setDefaultBudget(centavos: Long) { context.dataStore.edit { it[budget] = centavos } }
}
