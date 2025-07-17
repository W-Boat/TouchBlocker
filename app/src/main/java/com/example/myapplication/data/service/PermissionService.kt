package com.example.myapplication.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.myapplication.data.datastore.PreferencesManager
import com.example.myapplication.PermissionManager.AuthorizationMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing permissions and authorization methods
 */
@Singleton
class PermissionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    companion object {
        // 为不同操作设置不同的超时时间
        private const val ROOT_TIMEOUT_MS = 8_000L // 主动请求root权限的超时时间
        private const val ROOT_CHECK_TIMEOUT_MS = 3_000L // 检查root权限的超时时间
        private const val ROOT_TEST_TIMEOUT_MS = 1_000L // 测试模式下的超时时间
        private const val TAG = "PermissionService"
    }
    
    /**
     * Check if device is rooted (without requesting permission)
     */
    suspend fun isDeviceRooted(): Boolean = withContext(Dispatchers.IO) {
        try {
            checkRootBinaries() || checkBuildTags() || checkRootApps() || checkSystemProperties()
        } catch (e: Exception) {
            Timber.e(e, "Error checking if device is rooted")
            false
        }
    }
    
    /**
     * Check if root is available (alias for isDeviceRooted)
     */
    suspend fun isRootAvailable(): Boolean = isDeviceRooted()
    
    /**
     * Check if root permission is available (may show permission dialog)
     */
    suspend fun hasRootPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            requestRootPermissionInternal(testOnly = true)
        } catch (e: Exception) {
            Timber.e(e, "Error checking root permission")
            false
        }
    }
    
    /**
     * Request root permission
     */
    suspend fun requestRootPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            requestRootPermissionInternal(testOnly = false)
        } catch (e: Exception) {
            Timber.e(e, "Error requesting root permission")
            false
        }
    }
    
    /**
     * Check if LSPosed is active
     */
    suspend fun isLSPosedActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val moduleActive = preferencesManager.isModuleActive.first()
            val xposedActive = checkXposedFramework()
            
            moduleActive && xposedActive
        } catch (e: Exception) {
            Timber.e(e, "Error checking LSPosed status")
            false
        }
    }
    
    /**
     * Check if accessibility service is enabled
     */
    suspend fun isAccessibilityServiceEnabled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val packageName = context.packageName
            val serviceName = "$packageName/.KeyListenerService"
            val fullServiceName = "$packageName/com.example.myapplication.KeyListenerService"
            
            Timber.d("Package name: $packageName")
            Timber.d("Checking accessibility service (short): $serviceName")
            Timber.d("Checking accessibility service (full): $fullServiceName")
            Timber.d("Enabled services: $enabledServices")
            
            val isEnabled = enabledServices?.let { services ->
                services.contains(serviceName) || services.contains(fullServiceName)
            } ?: false
            
            Timber.d("Accessibility service enabled: $isEnabled")
            isEnabled
        } catch (e: Exception) {
            Timber.e(e, "Error checking accessibility service status")
            false
        }
    }
    
    /**
     * Get current authorization method
     */
    suspend fun getAuthorizationMethod(): AuthorizationMethod {
        return when {
            hasRootPermission() -> AuthorizationMethod.ROOT
            isLSPosedActive() -> AuthorizationMethod.LSPOSED
            else -> AuthorizationMethod.NONE
        }
    }
    
    /**
     * Check if touch blocking can be enabled
     */
    suspend fun canEnableTouchBlocking(): Boolean {
        return hasRootPermission() || isLSPosedActive()
    }
    
    /**
     * Internal method to check or request root permission
     * @param testOnly If true, only checks for root without triggering permission dialogs
     * @return true if root permission is available, false otherwise
     */
    private suspend fun requestRootPermissionInternal(testOnly: Boolean): Boolean {
        // 当testOnly为true时，使用保守的检测方法，避免触发权限请求对话框
        if (testOnly) {
            // 首先检查设备是否已root（通过检查二进制文件、构建标签等）
            val isRooted = checkRootBinaries() || checkBuildTags() || checkRootApps() || checkSystemProperties()
            
            if (!isRooted) {
                return false // 如果设备未root，直接返回false
            }
            
            // 如果设备已root，尝试使用非交互方式检查权限
             // 这种方法在大多数情况下不会触发权限请求对话框
             return try {
                 withTimeoutOrNull(ROOT_TEST_TIMEOUT_MS) { // 使用测试模式的超时时间
                     val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                     val exitValue = process.waitFor()
                     exitValue == 0
                 } ?: false
             } catch (e: Exception) {
                 Timber.d(e, "Non-interactive root check failed")
                 false
             }
        }
        
        // 当testOnly为false时，尝试所有可能的方法获取root权限
        return tryInteractiveShell() || trySuCommand() || tryDifferentSuPaths()
    }
    
    private suspend fun tryInteractiveShell(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 根据操作类型选择适当的超时时间
            val timeoutMs = if (Thread.currentThread().stackTrace.any { it.methodName.contains("testOnly", ignoreCase = true) }) {
                ROOT_CHECK_TIMEOUT_MS // 检查权限时使用较短超时
            } else {
                ROOT_TIMEOUT_MS // 请求权限时使用较长超时
            }
            
            withTimeoutOrNull(timeoutMs) {
                val process = Runtime.getRuntime().exec("su")
                val outputStream = DataOutputStream(process.outputStream)
                val inputStream = BufferedReader(InputStreamReader(process.inputStream))
                
                outputStream.writeBytes("echo 'root_test_success'\n")
                outputStream.writeBytes("exit\n")
                outputStream.flush()
                outputStream.close()
                
                val result = inputStream.readLine()
                val exitValue = process.waitFor()
                
                inputStream.close()
                
                exitValue == 0 && result?.contains("root_test_success") == true
            } ?: false
        } catch (e: Exception) {
            Timber.w(e, "Interactive shell method failed")
            false
        }
    }
    
    private suspend fun trySuCommand(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 根据操作类型选择适当的超时时间
            val timeoutMs = if (Thread.currentThread().stackTrace.any { it.methodName.contains("testOnly", ignoreCase = true) }) {
                ROOT_CHECK_TIMEOUT_MS // 检查权限时使用较短超时
            } else {
                ROOT_TIMEOUT_MS // 请求权限时使用较长超时
            }
            
            withTimeoutOrNull(timeoutMs) {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo 'root_test_success'"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                
                val result = reader.readLine()
                val exitValue = process.waitFor()
                
                reader.close()
                
                exitValue == 0 && result?.contains("root_test_success") == true
            } ?: false
        } catch (e: Exception) {
            Timber.w(e, "Su command method failed")
            false
        }
    }
    
    private suspend fun tryDifferentSuPaths(): Boolean = withContext(Dispatchers.IO) {
        // 根据操作类型选择适当的超时时间
        val timeoutMs = if (Thread.currentThread().stackTrace.any { it.methodName.contains("testOnly", ignoreCase = true) }) {
            ROOT_CHECK_TIMEOUT_MS // 检查权限时使用较短超时
        } else {
            ROOT_TIMEOUT_MS // 请求权限时使用较长超时
        }
        
        val suPaths = arrayOf(
            "su", "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        
        for (suPath in suPaths) {
            try {
                val success = withTimeoutOrNull(timeoutMs) {
                    val process = Runtime.getRuntime().exec(arrayOf(suPath, "-c", "echo 'root_test_success'"))
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    
                    val result = reader.readLine()
                    val exitValue = process.waitFor()
                    
                    reader.close()
                    
                    exitValue == 0 && result?.contains("root_test_success") == true
                } ?: false
                
                if (success) {
                    return@withContext true
                }
            } catch (e: Exception) {
                Timber.w(e, "Su path $suPath failed")
                continue
            }
        }
        false
    }
    
    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su",
            "/system/etc/init.d/99SuperSUDaemon", "/dev/com.koushikdutta.superuser.daemon/",
            "/system/app/SuperSU.apk"
        )
        
        return paths.any { java.io.File(it).exists() }
    }
    
    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    private fun checkRootApps(): Boolean {
        val packages = arrayOf(
            "com.noshufou.android.su", "com.noshufou.android.su.elite", "eu.chainfire.supersu",
            "com.koushikdutta.superuser", "com.thirdparty.superuser", "com.yellowes.su",
            "com.topjohnwu.magisk", "io.github.huskydg.magisk", "com.kingroot.kinguser",
            "com.kingo.root", "com.smedialink.oneclickroot", "com.zhiqupk.root.global",
            "com.alephzain.framaroot", "de.robv.android.xposed.installer"
        )
        
        return packages.any { packageExists(it) }
    }
    
    private fun checkSystemProperties(): Boolean {
        return try {
            val buildTags = Build.TAGS
            val buildType = Build.TYPE
            
            buildTags?.contains("test-keys") == true ||
            buildType?.equals("eng", ignoreCase = true) == true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkXposedFramework(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    private fun packageExists(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}