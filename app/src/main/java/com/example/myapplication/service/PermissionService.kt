package com.example.myapplication.service

import android.content.Context
import android.provider.Settings
import com.example.myapplication.domain.model.AuthorizationMethod
import com.example.myapplication.utils.AccessibilityHelper
import com.example.myapplication.utils.Constants
import com.example.myapplication.utils.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionService @Inject constructor(
    private val context: Context,
    private val rootService: RootService,
    private val lsposedService: LSPosedService,
    private val accessibilityHelper: AccessibilityHelper,
    private val errorHandler: ErrorHandler
) {

    suspend fun canEnableTouchBlocking(): Boolean = withContext(Dispatchers.IO) {
        try {
            val hasRoot = hasRootPermission()
            val hasLSPosed = isLSPosedActive()
            val result = hasRoot || hasLSPosed
            
            Timber.d("Touch blocking availability check: hasRoot=$hasRoot, hasLSPosed=$hasLSPosed, result=$result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking touch blocking availability")
            false
        }
    }

    suspend fun getAuthorizationMethod(): AuthorizationMethod = withContext(Dispatchers.IO) {
        try {
            when {
                hasRootPermission() -> {
                    Timber.d("Authorization method: ROOT")
                    AuthorizationMethod.ROOT
                }
                isLSPosedActive() -> {
                    Timber.d("Authorization method: LSPOSED")
                    AuthorizationMethod.LSPOSED
                }
                else -> {
                    Timber.d("Authorization method: NONE")
                    AuthorizationMethod.NONE
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error determining authorization method")
            AuthorizationMethod.NONE
        }
    }

    suspend fun requestRootPermission(): Boolean {
        return try {
            Timber.d("Requesting root permission")
            val result = rootService.requestRootPermission()
            Timber.d("Root permission request result: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to request root permission")
            throw Exception(errorHandler.handleRootError(e))
        }
    }

    suspend fun hasRootPermission(): Boolean {
        return try {
            val result = rootService.requestRootPermission()
            Timber.d("Root permission check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking root permission")
            false
        }
    }

    suspend fun isRootAvailable(): Boolean {
        return try {
            val result = rootService.isRooted()
            Timber.d("Root availability check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking root availability")
            false
        }
    }

    fun isLSPosedActive(): Boolean {
        return try {
            val result = lsposedService.isLSPosedActive()
            Timber.d("LSPosed active check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking LSPosed status")
            false
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val result = accessibilityHelper.isAccessibilityServiceEnabled()
            Timber.d("Accessibility service enabled check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking accessibility service status")
            false
        }
    }
    
    /**
     * 检查无障碍服务是否正在运行
     */
    fun isAccessibilityServiceRunning(): Boolean {
        return try {
            val result = accessibilityHelper.isAccessibilityServiceRunning()
            Timber.d("Accessibility service running check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking accessibility service running status")
            false
        }
    }
    
    /**
     * 获取无障碍服务状态
     */
    fun getAccessibilityServiceStatus() = accessibilityHelper.getAccessibilityServiceStatus()
    
    /**
     * 打开无障碍设置页面
     */
    fun openAccessibilitySettings(): Boolean {
        return try {
            accessibilityHelper.openAppAccessibilitySettings()
        } catch (e: Exception) {
            Timber.e(e, "Failed to open accessibility settings")
            false
        }
    }
}