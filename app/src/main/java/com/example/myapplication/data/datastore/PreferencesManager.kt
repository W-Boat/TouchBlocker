package com.example.myapplication.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for app preferences using DataStore
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "touch_blocker_preferences"
        )
        
        private val TOUCH_BLOCKING_ENABLED = booleanPreferencesKey("touch_blocking_enabled")
        private val FIRST_RUN = booleanPreferencesKey("first_run")
        private val MODULE_ACTIVE = booleanPreferencesKey("module_active")
    }
    
    /**
     * Get touch blocking enabled state
     */
    val isTouchBlockingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TOUCH_BLOCKING_ENABLED] ?: false
    }
    
    /**
     * Get first run state
     */
    val isFirstRun: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_RUN] ?: true
    }
    
    /**
     * Get module active state
     */
    val isModuleActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MODULE_ACTIVE] ?: false
    }
    
    /**
     * Set touch blocking enabled state
     */
    suspend fun setTouchBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TOUCH_BLOCKING_ENABLED] = enabled
        }
    }
    
    /**
     * Set first run state
     */
    suspend fun setFirstRun(isFirstRun: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_RUN] = isFirstRun
        }
    }
    
    /**
     * Set module active state
     */
    suspend fun setModuleActive(isActive: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MODULE_ACTIVE] = isActive
        }
    }
    
    /**
     * Clear all preferences
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}