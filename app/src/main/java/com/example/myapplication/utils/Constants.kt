package com.example.myapplication.utils

/**
 * 应用常量管理类
 * 统一管理应用中使用的常量值
 */
object Constants {
    
    // UI 相关常量
    object UI {
        const val BUTTON_HEIGHT_DP = 56
        const val CARD_ELEVATION_DP = 4
        const val CARD_ELEVATION_SMALL_DP = 2
        const val PADDING_STANDARD_DP = 16
        const val SPACING_STANDARD_DP = 16
        const val SPACING_SMALL_DP = 8
        const val PROGRESS_INDICATOR_SIZE_DP = 12
        const val PROGRESS_INDICATOR_STROKE_WIDTH_DP = 2
        const val BUTTON_WIDTH_RATIO = 0.8f
    }
    
    // 字体大小常量
    object FontSize {
        const val TITLE_SP = 18
        const val SUBTITLE_SP = 16
        const val BODY_SP = 14
        const val CAPTION_SP = 12
    }
    
    // 应用信息常量
    object App {
        const val VERSION = "1.0.0"
        const val SERVICE_NAME = "触摸屏蔽器"
    }
    
    // 权限相关常量
    object Permission {
        const val ACCESSIBILITY_SERVICE_PACKAGE = "com.example.myapplication"
        const val ACCESSIBILITY_SERVICE_CLASS = "com.example.myapplication.service.KeyListenerService"
    }
}