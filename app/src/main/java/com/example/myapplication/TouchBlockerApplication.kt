package com.example.myapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TouchBlockerApplication : Application() {

    companion object {
        lateinit var instance: TouchBlockerApplication
            private set
            
        // SharedPreferences keys for Xposed module communication
        const val PREF_TOUCH_BLOCKING_ENABLED = "touch_blocking_enabled"
        const val PREF_MODULE_ACTIVE = "module_active"
    }
    
    @Inject
    lateinit var timberTree: Timber.Tree

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Timber
        Timber.plant(timberTree)
        
        Timber.d("TouchBlockerApplication initialized")
    }
}