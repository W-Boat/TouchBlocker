package com.example.myapplication.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myapplication.R
import timber.log.Timber

/**
 * 对话框工具类
 * 统一管理应用中的对话框逻辑
 */
object DialogUtils {
    
    /**
     * 显示无障碍服务启用引导对话框
     * @param activity 当前Activity实例
     */
    fun showAccessibilityDialog(activity: Activity) {
        try {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity.getString(R.string.accessibility_dialog_title))
            builder.setMessage(activity.getString(R.string.accessibility_dialog_message))
            
            builder.setPositiveButton(activity.getString(R.string.go_to_settings)) { dialog, _ ->
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        // 添加标志确保用户可以正确返回到应用
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    activity.startActivity(intent)
                    
                    // 提示用户如何返回
                    showReturnHintDialog(activity)
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e, "无法打开无障碍设置界面")
                    showErrorDialog(
                        activity,
                        activity.getString(R.string.accessibility_settings_not_found)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "启动无障碍设置时发生未知错误")
                    showErrorDialog(
                        activity,
                        activity.getString(R.string.settings_not_available)
                    )
                }
                dialog.dismiss()
            }
            
            builder.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            
            builder.show()
        } catch (e: Exception) {
            Timber.e(e, "显示无障碍对话框时发生错误")
        }
    }
    
    /**
     * 显示返回提示对话框
     * @param activity 当前Activity实例
     */
    private fun showReturnHintDialog(activity: Activity) {
        try {
            // 延迟显示提示，给设置界面时间加载
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle("返回提示")
                    builder.setMessage("启用无障碍服务后，请使用以下方式返回应用：\n\n1. 按手机的返回键\n2. 使用任务切换器（最近使用的应用）\n3. 点击通知栏中的应用图标（如果有）")
                    builder.setPositiveButton("知道了") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.setCancelable(true)
                    builder.show()
                }
            }, 1000) // 延迟1秒显示
        } catch (e: Exception) {
            Timber.e(e, "显示返回提示对话框时发生错误")
        }
    }
    
    /**
     * 显示错误对话框
     * @param activity 当前Activity实例
     * @param message 错误消息
     */
    private fun showErrorDialog(activity: Activity, message: String) {
        try {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("错误")
            builder.setMessage(message)
            builder.setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        } catch (e: Exception) {
            Timber.e(e, "显示错误对话框时发生错误")
        }
    }
}