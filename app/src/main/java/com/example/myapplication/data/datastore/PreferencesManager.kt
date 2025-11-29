package com.example.myapplication.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.model.BlockRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        private val BLOCK_REGIONS = stringPreferencesKey("block_regions")
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
     * Get block regions
     */
    val blockRegions: Flow<List<BlockRegion>> = context.dataStore.data.map { preferences ->
        val json = preferences[BLOCK_REGIONS] ?: "[]"
        try {
            Json.decodeFromString<List<Map<String, Float>>>(json).map {
                BlockRegion(it["left"]!!, it["top"]!!, it["right"]!!, it["bottom"]!!)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Set block regions
     */
    suspend fun setBlockRegions(regions: List<BlockRegion>) {
        context.dataStore.edit { preferences ->
            val json = Json.encodeToString(regions.map {
                mapOf("left" to it.left, "top" to it.top, "right" to it.right, "bottom" to it.bottom)
            })
            preferences[BLOCK_REGIONS] = json
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