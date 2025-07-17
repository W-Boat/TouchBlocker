package com.example.myapplication.di

import android.content.Context

import com.example.myapplication.data.repository.TouchBlockingRepository
import com.example.myapplication.data.repository.TouchBlockingRepositoryImpl
import com.example.myapplication.service.LSPosedService
import com.example.myapplication.service.PermissionService
import com.example.myapplication.service.RootService
import com.example.myapplication.utils.AccessibilityHelper
import com.example.myapplication.utils.ErrorHandler
import com.example.myapplication.utils.TimeoutHelper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Hilt module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    /**
     * Bind TouchBlockingRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindTouchBlockingRepository(
        touchBlockingRepositoryImpl: TouchBlockingRepositoryImpl
    ): TouchBlockingRepository
    
    companion object {
        
        /**
         * Provide Timber logger
         */
        @Provides
        @Singleton
        fun provideTimber(): Timber.Tree {
            // For now, always use debug tree
            // TODO: Add proper production logging when BuildConfig is available
            return Timber.DebugTree()
        }

        @Provides
    @Singleton
    fun provideTimeoutHelper(): TimeoutHelper {
        return TimeoutHelper()
    }
    
    @Provides
    @Singleton
    fun provideRootService(timeoutHelper: TimeoutHelper): RootService {
        return RootService(timeoutHelper)
    }

        @Provides
        @Singleton
        fun provideLSPosedService(): LSPosedService {
            return LSPosedService()
        }

        @Provides
        @Singleton
        fun provideErrorHandler(@ApplicationContext context: Context): ErrorHandler {
            return ErrorHandler(context)
        }
        
        @Provides
        @Singleton
        fun provideAccessibilityHelper(@ApplicationContext context: Context): AccessibilityHelper {
            return AccessibilityHelper(context)
        }
        
        @Provides
        @Singleton
        fun providePermissionService(
            @ApplicationContext context: Context,
            rootService: RootService,
            lsposedService: LSPosedService,
            accessibilityHelper: AccessibilityHelper,
            errorHandler: ErrorHandler
        ): PermissionService {
            return PermissionService(context, rootService, lsposedService, accessibilityHelper, errorHandler)
        }
    }
}