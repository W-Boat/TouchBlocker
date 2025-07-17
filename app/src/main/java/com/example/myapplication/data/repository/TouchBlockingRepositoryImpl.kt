package com.example.myapplication.data.repository

import android.content.Context
import android.content.Intent
import com.example.myapplication.data.datastore.PreferencesManager
import com.example.myapplication.data.model.TouchBlockingState
import com.example.myapplication.data.service.PermissionService
import com.example.myapplication.data.service.TouchBlockingServiceImpl
import com.example.myapplication.PermissionManager.AuthorizationMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TouchBlockingRepository
 */
@Singleton
class TouchBlockingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val permissionService: PermissionService,
    private val touchBlockingService: TouchBlockingServiceImpl
) : TouchBlockingRepository {
    
    companion object {
        private const val BROADCAST_ACTION = "com.example.myapplication.TOUCH_BLOCKING_CHANGED"
    }
    
    override fun getTouchBlockingState(): Flow<TouchBlockingState> {
        return combine(
            preferencesManager.isTouchBlockingEnabled,
            preferencesManager.isModuleActive
        ) { isEnabled, isModuleActive ->
            try {
                val isAccessibilityEnabled = permissionService.isAccessibilityServiceEnabled()
                val authMethod = permissionService.getAuthorizationMethod()
                val canEnable = permissionService.canEnableTouchBlocking()
                
                TouchBlockingState(
                    isEnabled = isEnabled,
                    isAccessibilityServiceEnabled = isAccessibilityEnabled,
                    authorizationMethod = authMethod,
                    canEnable = canEnable,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Timber.e(e, "Error getting touch blocking state")
                TouchBlockingState(
                    isEnabled = isEnabled,
                    isAccessibilityServiceEnabled = false,
                    authorizationMethod = AuthorizationMethod.NONE,
                    canEnable = false,
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    override suspend fun toggleTouchBlocking(): Boolean {
        return try {
            val isCurrentlyEnabled = preferencesManager.isTouchBlockingEnabled.first()
            setTouchBlocking(!isCurrentlyEnabled)
        } catch (e: Exception) {
            Timber.e(e, "Error toggling touch blocking")
            false
        }
    }
    
    override suspend fun setTouchBlocking(enabled: Boolean): Boolean {
        return try {
            val result = touchBlockingService.setTouchBlocking(enabled)
            
            if (result.isSuccess) {
                preferencesManager.setTouchBlockingEnabled(enabled)
                sendStateChangeBroadcast(enabled)
                true
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to set touch blocking")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting touch blocking")
            false
        }
    }
    
    override suspend fun canEnableTouchBlocking(): Boolean {
        return try {
            permissionService.canEnableTouchBlocking()
        } catch (e: Exception) {
            Timber.e(e, "Error checking if touch blocking can be enabled")
            false
        }
    }
    
    override suspend fun getAuthorizationMethod(): AuthorizationMethod {
        return try {
            permissionService.getAuthorizationMethod()
        } catch (e: Exception) {
            Timber.e(e, "Error getting authorization method")
            AuthorizationMethod.NONE
        }
    }
    
    override suspend fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            permissionService.isAccessibilityServiceEnabled()
        } catch (e: Exception) {
            Timber.e(e, "Error checking accessibility service")
            false
        }
    }
    
    override suspend fun sendStateChangeBroadcast(enabled: Boolean) {
        try {
            val intent = Intent(BROADCAST_ACTION).apply {
                putExtra("enabled", enabled)
            }
            context.sendBroadcast(intent)
            Timber.d("Sent touch blocking state change broadcast: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Error sending state change broadcast")
        }
    }
}