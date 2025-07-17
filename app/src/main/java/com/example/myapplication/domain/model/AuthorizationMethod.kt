package com.example.myapplication.domain.model

/**
 * Enum representing different authorization methods for touch blocking
 */
enum class AuthorizationMethod {
    /**
     * Root access authorization
     */
    ROOT,
    
    /**
     * LSPosed framework authorization
     */
    LSPOSED,
    
    /**
     * No authorization available
     */
    NONE;
    
    /**
     * Get human-readable name for the authorization method
     */
    fun getDisplayName(): String {
        return when (this) {
            ROOT -> "Root权限"
            LSPOSED -> "LSPosed框架"
            NONE -> "无可用授权"
        }
    }
    
    /**
     * Check if this authorization method is available
     */
    fun isAvailable(): Boolean {
        return this != NONE
    }
}