package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.TouchBlockingState
import com.example.myapplication.PermissionManager.AuthorizationMethod
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for touch blocking functionality
 */
interface TouchBlockingRepository {
    
    /**
     * Get current touch blocking state as Flow
     */
    fun getTouchBlockingState(): Flow<TouchBlockingState>
    
    /**
     * Toggle touch blocking state
     * @return true if operation was successful
     */
    suspend fun toggleTouchBlocking(): Boolean
    
    /**
     * Set touch blocking state
     * @param enabled whether to enable touch blocking
     * @return true if operation was successful
     */
    suspend fun setTouchBlocking(enabled: Boolean): Boolean
    
    /**
     * Check if touch blocking can be enabled
     */
    suspend fun canEnableTouchBlocking(): Boolean
    
    /**
     * Get current authorization method
     */
    suspend fun getAuthorizationMethod(): AuthorizationMethod
    
    /**
     * Check if accessibility service is enabled
     */
    suspend fun isAccessibilityServiceEnabled(): Boolean
    
    /**
     * Send broadcast notification for state change
     */
    suspend fun sendStateChangeBroadcast(enabled: Boolean)
}