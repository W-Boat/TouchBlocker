package com.example.myapplication.data.service

import android.content.Context
import android.content.Intent
import com.example.myapplication.PermissionManager.AuthorizationMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of touch blocking service with improved error handling and coroutines
 */
@Singleton
class TouchBlockingServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionService: PermissionService
) {
    
    companion object {
        private const val TAG = "TouchBlockingServiceImpl"
        private const val ROOT_COMMAND_TIMEOUT_MS = 15_000L
        private const val BROADCAST_ACTION = "com.example.myapplication.TOUCH_BLOCKING_CHANGED"
    }
    
    /**
     * Set touch blocking state using available authorization method
     */
    suspend fun setTouchBlocking(enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authMethod = permissionService.getAuthorizationMethod()
            
            val success = when (authMethod) {
                AuthorizationMethod.ROOT -> setTouchBlockingViaRoot(enabled)
                AuthorizationMethod.LSPOSED -> setTouchBlockingViaLSPosed(enabled)
                AuthorizationMethod.NONE -> {
                    Timber.w("No authorization method available for touch blocking")
                    false
                }
            }
            
            if (success) {
                sendStateChangeBroadcast(enabled)
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to ${if (enabled) "enable" else "disable"} touch blocking"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting touch blocking state")
            Result.failure(e)
        }
    }
    
    /**
     * Set touch blocking via Root permission with improved error handling
     */
    private suspend fun setTouchBlockingViaRoot(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(ROOT_COMMAND_TIMEOUT_MS) {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                
                try {
                    if (enabled) {
                        executeEnableCommands(os)
                    } else {
                        executeDisableCommands(os)
                    }
                    
                    os.writeBytes("exit\n")
                    os.flush()
                    
                    val exitValue = process.waitFor()
                    exitValue == 0
                } finally {
                    os.close()
                }
            } ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error executing root commands for touch blocking")
            false
        }
    }
    
    private fun executeEnableCommands(os: DataOutputStream) {
        // Method 1: Disable touchscreen devices
        os.writeBytes("find /sys/class/input -name 'event*' -exec sh -c 'if grep -q touchscreen \\\$1/device/name 2>/dev/null; then echo 0 > \\\$1/device/enable 2>/dev/null || true; fi' _ {} \\;\n")
        
        // Method 2: Kill existing getevent processes and start blocking
        os.writeBytes("pkill -f getevent\n")
        os.writeBytes("getevent -t /dev/input/event* | while read line; do :; done &\n")
        
        // Method 3: Change permissions on input devices
        os.writeBytes("find /dev/input -name 'event*' -exec chmod 000 {} \\; 2>/dev/null || true\n")
        
        Timber.d("Executed enable touch blocking commands")
    }
    
    private fun executeDisableCommands(os: DataOutputStream) {
        // Restore touchscreen devices
        os.writeBytes("find /sys/class/input -name 'event*' -exec sh -c 'if grep -q touchscreen \\\$1/device/name 2>/dev/null; then echo 1 > \\\$1/device/enable 2>/dev/null || true; fi' _ {} \\;\n")
        
        // Restore input device permissions
        os.writeBytes("find /dev/input -name 'event*' -exec chmod 664 {} \\; 2>/dev/null || true\n")
        
        // Kill getevent processes
        os.writeBytes("pkill -f getevent\n")
        
        Timber.d("Executed disable touch blocking commands")
    }
    
    /**
     * Set touch blocking via LSPosed (placeholder implementation)
     */
    private suspend fun setTouchBlockingViaLSPosed(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement LSPosed-based touch blocking
            // This would require Xposed module implementation
            Timber.d("LSPosed touch blocking not yet implemented")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error setting touch blocking via LSPosed")
            false
        }
    }
    
    /**
     * Send broadcast notification for state change
     */
    private fun sendStateChangeBroadcast(enabled: Boolean) {
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