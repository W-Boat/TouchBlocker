package com.example.myapplication.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.example.myapplication.KeyListenerService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityHelper @Inject constructor(
    private val context: Context
) {

    private val accessibilityManager: AccessibilityManager by lazy {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    /**
     * 检查无障碍服务是否已启用
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            if (enabledServices.isNullOrEmpty()) {
                Timber.d("No accessibility services enabled")
                return false
            }
            
            val serviceName = ComponentName(context, KeyListenerService::class.java).flattenToString()
            val isEnabled = enabledServices.contains(serviceName)
            
            Timber.d("Accessibility service enabled: $isEnabled")
            Timber.d("Service name: $serviceName")
            Timber.d("Enabled services: $enabledServices")
            
            isEnabled
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
            val runningServices = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            
            val serviceName = ComponentName(context, KeyListenerService::class.java)
            val isRunning = runningServices.any { serviceInfo ->
                serviceInfo.resolveInfo.serviceInfo.let { info ->
                    info.packageName == serviceName.packageName && 
                    info.name == serviceName.className
                }
            }
            
            Timber.d("Accessibility service running: $isRunning")
            isRunning
        } catch (e: Exception) {
            Timber.e(e, "Error checking accessibility service running status")
            false
        }
    }

    /**
     * 获取无障碍服务的详细状态
     */
    fun getAccessibilityServiceStatus(): AccessibilityServiceStatus {
        val isEnabled = isAccessibilityServiceEnabled()
        val isRunning = isAccessibilityServiceRunning()
        
        return AccessibilityServiceStatus(
            isEnabled = isEnabled,
            isRunning = isRunning,
            isAvailable = isEnabled && isRunning
        )
    }

    /**
     * 打开无障碍设置页面
     */
    fun openAccessibilitySettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Timber.d("Opened accessibility settings")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to open accessibility settings")
            false
        }
    }

    /**
     * 尝试直接打开应用的无障碍服务设置页面
     */
    fun openAppAccessibilitySettings(): Boolean {
        return try {
            val serviceName = ComponentName(context, KeyListenerService::class.java)
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // 尝试直接定位到我们的服务
                putExtra(":settings:fragment_args_key", serviceName.flattenToString())
            }
            context.startActivity(intent)
            Timber.d("Opened app accessibility settings")
            true
        } catch (e: Exception) {
            Timber.w(e, "Failed to open app accessibility settings, falling back to general settings")
            openAccessibilitySettings()
        }
    }

    /**
     * 获取无障碍服务的配置信息
     */
    fun getServiceConfiguration(): AccessibilityServiceConfiguration? {
        return try {
            val serviceName = ComponentName(context, KeyListenerService::class.java)
            val packageManager = context.packageManager
            val serviceInfo = packageManager.getServiceInfo(serviceName, 0)
            
            AccessibilityServiceConfiguration(
                serviceName = serviceName.flattenToString(),
                packageName = serviceInfo.packageName,
                className = serviceInfo.name,
                isInstalled = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get service configuration")
            null
        }
    }

    /**
     * 监听无障碍服务状态变化
     */
    fun addAccessibilityStateChangeListener(listener: AccessibilityManager.AccessibilityStateChangeListener) {
        accessibilityManager.addAccessibilityStateChangeListener(listener)
    }

    /**
     * 移除无障碍服务状态变化监听器
     */
    fun removeAccessibilityStateChangeListener(listener: AccessibilityManager.AccessibilityStateChangeListener) {
        accessibilityManager.removeAccessibilityStateChangeListener(listener)
    }
}

/**
 * 无障碍服务状态数据类
 */
data class AccessibilityServiceStatus(
    val isEnabled: Boolean,
    val isRunning: Boolean,
    val isAvailable: Boolean
)

/**
 * 无障碍服务配置信息数据类
 */
data class AccessibilityServiceConfiguration(
    val serviceName: String,
    val packageName: String,
    val className: String,
    val isInstalled: Boolean
)