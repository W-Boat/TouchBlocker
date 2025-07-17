package com.example.myapplication.data.model

import com.example.myapplication.PermissionManager.AuthorizationMethod

/**
 * Data class representing the current state of touch blocking functionality
 */
data class TouchBlockingState(
    val isEnabled: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false,
    val authorizationMethod: AuthorizationMethod = AuthorizationMethod.NONE,
    val canEnable: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Check if touch blocking functionality is available
     */
    val isAvailable: Boolean
        get() = isAccessibilityServiceEnabled && canEnable && authorizationMethod != AuthorizationMethod.NONE
}