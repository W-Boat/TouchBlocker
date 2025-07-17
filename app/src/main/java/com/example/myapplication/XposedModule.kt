package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.MotionEvent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedModule : IXposedHookLoadPackage {
    
    companion object {
        private const val TAG = "TouchBlockerXposed"
    }
    
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 只在系统进程中Hook
        if (lpparam.packageName != "android") return
        
        try {
            // Hook InputManagerService的触摸事件处理
            val inputManagerServiceClass = XposedHelpers.findClass(
                "com.android.server.input.InputManagerService",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                inputManagerServiceClass,
                "injectInputEvent",
                "android.view.InputEvent",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val inputEvent = param.args[0]
                        
                        // 检查是否是触摸事件
                        if (inputEvent is MotionEvent) {
                            // 检查触摸屏蔽是否启用
                            if (isTouchBlockingEnabled()) {
                                // 屏蔽触摸事件
                                param.result = true
                                return
                            }
                        }
                    }
                }
            )
            
            // 标记模块已激活
            markModuleActive()
            
            XposedBridge.log("$TAG: Touch blocking module loaded successfully")
            
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error loading module: ${e.message}")
        }
    }
    
    private fun isTouchBlockingEnabled(): Boolean {
        return try {
            val context = getSystemContext()
            val prefs = context.getSharedPreferences("com.example.myapplication_preferences", Context.MODE_WORLD_READABLE)
            prefs.getBoolean(TouchBlockerApplication.PREF_TOUCH_BLOCKING_ENABLED, false)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun markModuleActive() {
        try {
            val context = getSystemContext()
            val prefs = context.getSharedPreferences("com.example.myapplication_preferences", Context.MODE_WORLD_READABLE)
            prefs.edit().putBoolean(TouchBlockerApplication.PREF_MODULE_ACTIVE, true).apply()
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error marking module active: ${e.message}")
        }
    }
    
    private fun getSystemContext(): Context {
        return XposedHelpers.callStaticMethod(
            XposedHelpers.findClass("android.app.ActivityThread", null),
            "currentApplication"
        ) as Context
    }
}