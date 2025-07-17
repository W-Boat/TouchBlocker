package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.service.LSPosedService
import com.example.myapplication.service.PermissionService
import com.example.myapplication.service.RootService
import com.example.myapplication.utils.AccessibilityHelper
import com.example.myapplication.utils.ErrorHandler
import com.example.myapplication.utils.TimeoutHelper

object ServiceLocator {

    private lateinit var context: Context
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    private val timeoutHelper by lazy { TimeoutHelper() }
    private val errorHandler by lazy { ErrorHandler(context) }
    private val accessibilityHelper by lazy { AccessibilityHelper(context) }
    private val rootService by lazy { RootService(timeoutHelper) }
    private val lsposedService by lazy { LSPosedService() }

    val permissionService by lazy {
        PermissionService(context, rootService, lsposedService, accessibilityHelper, errorHandler)
    }
}