package com.example.myapplication

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class TouchBlockerModule : IXposedHookLoadPackage {
    
    companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val ANDROID_PACKAGE = "android"
    }
    
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook系统UI和Android框架
        when (lpparam.packageName) {
            SYSTEM_UI_PACKAGE, ANDROID_PACKAGE -> {
                hookTouchEvents(lpparam)
            }
        }
    }
    
    private fun hookTouchEvents(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook View的dispatchTouchEvent方法
            XposedHelpers.findAndHookMethod(
                View::class.java,
                "dispatchTouchEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isTouchBlockingEnabled(param.thisObject as View)) {
                            param.result = true // 拦截触摸事件
                        }
                    }
                }
            )
            
            // Hook ViewGroup的onInterceptTouchEvent方法
            XposedHelpers.findAndHookMethod(
                ViewGroup::class.java,
                "onInterceptTouchEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isTouchBlockingEnabled(param.thisObject as ViewGroup)) {
                            param.result = true // 拦截触摸事件
                        }
                    }
                }
            )
            
            // Hook WindowManager的addView方法，确保新窗口也被拦截
            XposedHelpers.findAndHookMethod(
                "android.view.WindowManagerImpl",
                lpparam.classLoader,
                "addView",
                View::class.java,
                ViewGroup.LayoutParams::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.args[0] as? View
                        view?.let { hookViewRecursively(it) }
                    }
                }
            )
            
            XposedBridge.log("TouchBlocker: Successfully hooked touch events in ${lpparam.packageName}")
            
        } catch (e: Exception) {
            XposedBridge.log("TouchBlocker: Error hooking ${lpparam.packageName}: ${e.message}")
        }
    }
    
    private fun hookViewRecursively(view: View) {
        try {
            // 为新添加的View设置触摸拦截
            view.setOnTouchListener { v, event ->
                if (isTouchBlockingEnabled(v)) {
                    true // 拦截触摸事件
                } else {
                    false // 不拦截，让原有逻辑处理
                }
            }
            
            // 递归处理子View
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    hookViewRecursively(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("TouchBlocker: Error hooking view recursively: ${e.message}")
        }
    }
    
    private fun isTouchBlockingEnabled(view: View): Boolean {
        return try {
            val context = view.context
            val sharedPrefs = context.getSharedPreferences(
                "com.example.myapplication_preferences", 
                Context.MODE_WORLD_READABLE
            )
            sharedPrefs.getBoolean(TouchBlockerApplication.PREF_TOUCH_BLOCKING_ENABLED, false)
        } catch (e: Exception) {
            XposedBridge.log("TouchBlocker: Error reading preferences: ${e.message}")
            false
        }
    }
}