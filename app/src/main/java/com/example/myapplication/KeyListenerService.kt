package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.myapplication.data.repository.TouchBlockingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class KeyListenerService : AccessibilityService() {
    
    @Inject
    lateinit var repository: TouchBlockingRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val handler = Handler(Looper.getMainLooper())
    private var starKeyPressTime = 0L
    private var isStarKeyPressed = false
    
    private val longPressRunnable = Runnable {
        if (isStarKeyPressed && 
            System.currentTimeMillis() - starKeyPressTime >= STAR_KEY_HOLD_DURATION) {
            // 长按*键3秒，切换功能
            toggleTouchBlocking()
        }
    }
    
    companion object {
        private const val STAR_KEY_HOLD_DURATION = 3000L // 3秒
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "按键监听服务已启动", Toast.LENGTH_SHORT).show()
    }
    
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // 监听*键（通常是KeyEvent.KEYCODE_STAR）
        if (event.keyCode == KeyEvent.KEYCODE_STAR) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isStarKeyPressed) {
                        isStarKeyPressed = true
                        starKeyPressTime = System.currentTimeMillis()
                        
                        // 设置3秒后的检查
                        handler.postDelayed(longPressRunnable, STAR_KEY_HOLD_DURATION)
                    }
                    return true
                }
                
                KeyEvent.ACTION_UP -> {
                    if (isStarKeyPressed) {
                        handler.removeCallbacks(longPressRunnable)
                        isStarKeyPressed = false
                    }
                    return true
                }
            }
        }
        
        return super.onKeyEvent(event)
    }
    
    private fun toggleTouchBlocking() {
        serviceScope.launch {
            try {
                val success = repository.toggleTouchBlocking()
                val message = if (success) {
                    "触摸屏蔽状态已切换"
                } else {
                    "切换触摸屏蔽失败"
                }
                
                Toast.makeText(this@KeyListenerService, message, Toast.LENGTH_LONG).show()
                Timber.d("Touch blocking toggled: $success")
            } catch (e: Exception) {
                Timber.e(e, "Error toggling touch blocking")
                Toast.makeText(this@KeyListenerService, "操作失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理无障碍事件
    }
    
    override fun onInterrupt() {
        Timber.d("KeyListenerService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Timber.d("KeyListenerService destroyed")
    }
}