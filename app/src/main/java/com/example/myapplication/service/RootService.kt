package com.example.myapplication.service

import android.os.Build
import com.example.myapplication.domain.model.AuthorizationMethod
import com.example.myapplication.utils.TimeoutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootService @Inject constructor(
    private val timeoutHelper: TimeoutHelper
) {

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    suspend fun requestRootPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo 'root_test_success'"))
            val exitCode = process.waitFor()
            val input = process.inputStream.bufferedReader().use { it.readText() }
            exitCode == 0 && input.contains("root_test_success")
        } catch (e: Exception) {
            false
        }
    }

    // Root检查方法1：检查常见的su二进制文件和Root应用
    private fun checkRootMethod1(): Boolean {
        return try {
            val suPaths = arrayOf(
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
            
            val result = suPaths.any { File(it).exists() }
            Timber.d("Root method 1 (binary files) check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error in root method 1 check")
            false
        }
    }

    // Root检查方法2：检查Build.TAGS
    private fun checkRootMethod2(): Boolean {
        return try {
            val buildTags = Build.TAGS
            val result = buildTags != null && buildTags.contains("test-keys")
            Timber.d("Root method 2 (build tags) check: $result, tags: $buildTags")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error in root method 2 check")
            false
        }
    }

    // Root检查方法3：检查常见的Root管理应用
    private fun checkRootMethod3(): Boolean {
        return try {
            val packages = arrayOf(
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su",
                "com.topjohnwu.magisk",
                "io.github.huskydg.magisk",
                "com.kingroot.kinguser",
                "com.kingo.root",
                "com.smedialink.oneclickroot",
                "com.zhiqupk.root.global",
                "com.alephzain.framaroot"
            )
            
            val result = packages.any { packageExists(it) }
            Timber.d("Root method 3 (root apps) check: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error in root method 3 check")
            false
        }
    }

    private fun packageExists(packageName: String): Boolean {
        return try {
            // 通过检查系统属性或文件来判断包是否存在
            // 这是一个简化的实现，实际应用中可能需要更复杂的检查
            val process = Runtime.getRuntime().exec(arrayOf("pm", "list", "packages", packageName))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            
            val result = output.contains(packageName)
            Timber.d("Package $packageName exists: $result")
            result
        } catch (e: Exception) {
            Timber.w(e, "Error checking package existence for $packageName")
            false
        }
    }

    fun getAuthorizationMethod(): AuthorizationMethod {
        // This is a placeholder. The actual logic will be in a higher-level service.
        return AuthorizationMethod.NONE
    }
}