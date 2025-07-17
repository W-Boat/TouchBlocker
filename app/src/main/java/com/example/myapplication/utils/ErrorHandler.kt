package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {

    /**
     * 处理Root权限相关错误
     */
    fun handleRootError(exception: Throwable): String {
        Timber.e(exception, "Root permission error")
        
        return when {
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                context.getString(R.string.error_root_timeout)
            exception.message?.contains("permission", ignoreCase = true) == true -> 
                context.getString(R.string.error_root_permission_denied)
            exception.message?.contains("not found", ignoreCase = true) == true -> 
                context.getString(R.string.error_su_not_found)
            exception.message?.contains("denied", ignoreCase = true) == true -> 
                context.getString(R.string.error_root_access_denied)
            else -> context.getString(R.string.error_root_unknown, exception.message ?: "Unknown error")
        }
    }

    /**
     * 处理无障碍服务相关错误
     */
    fun handleAccessibilityError(exception: Throwable): String {
        Timber.e(exception, "Accessibility service error")
        
        return when {
            exception.message?.contains("not enabled", ignoreCase = true) == true -> 
                context.getString(R.string.error_accessibility_not_enabled)
            exception.message?.contains("permission", ignoreCase = true) == true -> 
                context.getString(R.string.error_accessibility_permission)
            else -> context.getString(R.string.error_accessibility_unknown, exception.message ?: "Unknown error")
        }
    }

    /**
     * 处理触摸屏蔽相关错误
     */
    fun handleTouchBlockingError(exception: Throwable): String {
        Timber.e(exception, "Touch blocking error")
        
        return when {
            exception.message?.contains("not available", ignoreCase = true) == true -> 
                context.getString(R.string.error_touch_blocking_not_available)
            exception.message?.contains("permission", ignoreCase = true) == true -> 
                context.getString(R.string.error_touch_blocking_permission)
            else -> context.getString(R.string.error_touch_blocking_unknown, exception.message ?: "Unknown error")
        }
    }

    /**
     * 处理网络相关错误
     */
    fun handleNetworkError(exception: Throwable): String {
        Timber.e(exception, "Network error")
        
        return when {
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                context.getString(R.string.error_network_timeout)
            exception.message?.contains("connection", ignoreCase = true) == true -> 
                context.getString(R.string.error_network_connection)
            else -> context.getString(R.string.error_network_unknown, exception.message ?: "Unknown error")
        }
    }

    /**
     * 处理通用错误
     */
    fun handleGenericError(exception: Throwable): String {
        Timber.e(exception, "Generic error")
        return context.getString(R.string.error_generic, exception.message ?: "Unknown error")
    }
}