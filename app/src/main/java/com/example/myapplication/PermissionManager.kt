package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class PermissionManager {
    
    companion object {
        private const val TAG = "PermissionManager"
        private const val ROOT_TIMEOUT_SECONDS = 10L
        
        /**
         * 检查设备是否已Root（不请求权限）
         */
        fun isDeviceRooted(): Boolean {
            return checkRootMethod1() || checkRootMethod2() || checkRootMethod3() || checkRootMethod4()
        }
        
        /**
         * 检查是否有Root权限（会弹出权限请求）
         */
        fun hasRootPermission(): Boolean {
            return requestRootPermissionInternal(testOnly = true)
        }
        
        /**
         * 请求Root权限 - 使用多种方法确保兼容性
         */
        fun requestRootPermission(): Boolean {
            return requestRootPermissionInternal(testOnly = false)
        }
        
        /**
         * 内部root权限请求方法
         */
        private fun requestRootPermissionInternal(testOnly: Boolean): Boolean {
            // 方法1: 使用交互式shell (推荐方法)
            if (tryInteractiveShell(testOnly)) return true
            
            // 方法2: 使用su -c命令
            if (trySuCommand(testOnly)) return true
            
            // 方法3: 尝试不同路径的su
            if (tryDifferentSuPaths(testOnly)) return true
            
            return false
        }
        
        /**
         * 方法1: 使用交互式shell (最兼容的方法)
         */
        private fun tryInteractiveShell(testOnly: Boolean): Boolean {
            return try {
                val process = Runtime.getRuntime().exec("su")
                val outputStream = DataOutputStream(process.outputStream)
                val inputStream = BufferedReader(InputStreamReader(process.inputStream))
                
                // 发送测试命令
                outputStream.writeBytes("echo 'root_test_success'\n")
                outputStream.writeBytes("exit\n")
                outputStream.flush()
                outputStream.close()
                
                // 等待结果，设置超时
                val finished = try {
                    process.waitFor()
                    true
                } catch (e: InterruptedException) {
                    process.destroy()
                    false
                }
                if (!finished) {
                    return false
                }
                
                val result = inputStream.readLine()
                val exitValue = process.exitValue()
                
                inputStream.close()
                
                exitValue == 0 && result?.contains("root_test_success") == true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * 方法2: 使用su -c命令
         */
        private fun trySuCommand(testOnly: Boolean): Boolean {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo 'root_test_success'"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                
                val finished = try {
                    process.waitFor()
                    true
                } catch (e: InterruptedException) {
                    process.destroy()
                    false
                }
                if (!finished) {
                    return false
                }
                
                val result = reader.readLine()
                val exitValue = process.exitValue()
                
                reader.close()
                
                exitValue == 0 && result?.contains("root_test_success") == true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * 方法3: 尝试不同路径的su命令
         */
        private fun tryDifferentSuPaths(testOnly: Boolean): Boolean {
            val suPaths = arrayOf(
                "su",
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            
            for (suPath in suPaths) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf(suPath, "-c", "echo 'root_test_success'"))
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    
                    val finished = try {
                        process.waitFor()
                        true
                    } catch (e: InterruptedException) {
                        process.destroy()
                        false
                    }
                    if (!finished) {
                        continue
                    }
                    
                    val result = reader.readLine()
                    val exitValue = process.exitValue()
                    
                    reader.close()
                    
                    if (exitValue == 0 && result?.contains("root_test_success") == true) {
                        return true
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            return false
        }
        
        /**
         * 检查LSPosed是否激活
         */
        fun isLSPosedActive(): Boolean {
            // 检查LSPosed模块状态
            val moduleActive = TouchBlockerApplication.instance.getSharedPreferences(
                "com.example.myapplication_preferences",
                Context.MODE_PRIVATE
            ).getBoolean(
                TouchBlockerApplication.PREF_MODULE_ACTIVE,
                false
            )
            
            // 检查Xposed框架是否存在
            val xposedActive = try {
                Class.forName("de.robv.android.xposed.XposedBridge")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            
            return moduleActive && xposedActive
        }
        
        /**
         * 检查是否可以启用触摸屏蔽
         */
        fun canEnableTouchBlocking(): Boolean {
            return hasRootPermission() || isLSPosedActive()
        }
        
        /**
         * 获取当前授权方式
         */
        fun getAuthorizationMethod(): AuthorizationMethod {
            return when {
                hasRootPermission() -> AuthorizationMethod.ROOT
                isLSPosedActive() -> AuthorizationMethod.LSPOSED
                else -> AuthorizationMethod.NONE
            }
        }
        
        // Root检查方法1：检查常见的su二进制文件和Root应用
        private fun checkRootMethod1(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su",
                "/system/etc/init.d/99SuperSUDaemon",
                "/dev/com.koushikdutta.superuser.daemon/",
                "/system/app/SuperSU.apk"
            )
            
            for (path in paths) {
                if (java.io.File(path).exists()) return true
            }
            return false
        }
        
        // Root检查方法2：检查Build标签
        private fun checkRootMethod2(): Boolean {
            val buildTags = Build.TAGS
            return buildTags != null && buildTags.contains("test-keys")
        }
        
        // Root检查方法3：检查常见Root应用（包含Magisk）
        private fun checkRootMethod3(): Boolean {
            val packages = arrayOf(
                // SuperSU相关
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su",
                // Magisk相关
                "com.topjohnwu.magisk",
                "io.github.huskydg.magisk",
                // KingRoot相关
                "com.kingroot.kinguser",
                "com.kingo.root",
                "com.smedialink.oneclickroot",
                "com.zhiqupk.root.global",
                "com.alephzain.framaroot",
                // 其他Root工具
                "com.koushikdutta.rommanager",
                "com.koushikdutta.rommanager.license",
                "com.dimonvideo.luckypatcher",
                "com.chelpus.lackypatch",
                "com.ramdroid.appquarantine",
                "com.ramdroid.appquarantinepro",
                "com.devadvance.rootcloak",
                "com.devadvance.rootcloakplus",
                "de.robv.android.xposed.installer",
                "com.saurik.substrate",
                "com.zachspong.temprootremovejb",
                "com.amphoras.hidemyroot",
                "com.amphoras.hidemyrootadfree",
                "com.formyhm.hiderootPremium",
                "com.formyhm.hideroot"
            )
            
            return packages.any { packageExists(it) }
        }
        
        // Root检查方法4：检查系统属性
        private fun checkRootMethod4(): Boolean {
            return try {
                val buildTags = Build.TAGS
                val buildType = Build.TYPE
                val buildUser = Build.USER
                val buildHost = Build.HOST
                
                // 检查是否为测试版本
                if (buildTags?.contains("test-keys") == true) return true
                if (buildType?.equals("eng", ignoreCase = true) == true) return true
                if (buildUser?.equals("android-build", ignoreCase = true) == true) return true
                
                // 检查是否有调试属性
                val debuggable = try {
                    val systemProperties = Class.forName("android.os.SystemProperties")
                    val get = systemProperties.getMethod("get", String::class.java)
                    val debugProp = get.invoke(null, "ro.debuggable") as String
                    debugProp == "1"
                } catch (e: Exception) {
                    false
                }
                
                debuggable
            } catch (e: Exception) {
                false
            }
        }
        
        private fun packageExists(packageName: String): Boolean {
            return try {
                val context = TouchBlockerApplication.instance
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
    
    enum class AuthorizationMethod {
        ROOT,
        LSPOSED,
        NONE;
        
        fun getDisplayName(): String {
            return when (this) {
                ROOT -> "Root权限"
                LSPOSED -> "LSPosed模块"
                NONE -> "无授权"
            }
        }
    }
}