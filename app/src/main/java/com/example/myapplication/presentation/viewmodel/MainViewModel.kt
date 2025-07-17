package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.TouchBlockingState
import com.example.myapplication.data.repository.TouchBlockingRepository
import com.example.myapplication.service.PermissionService
import com.example.myapplication.domain.model.AuthorizationMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for MainActivity with improved state management
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TouchBlockingRepository,
    private val permissionService: PermissionService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _touchBlockingState = MutableStateFlow(TouchBlockingState())
    val touchBlockingState: StateFlow<TouchBlockingState> = _touchBlockingState.asStateFlow()
    
    init {
        observeTouchBlockingState()
        refreshBasicState()
    }
    
    /**
     * Observe touch blocking state changes
     */
    private fun observeTouchBlockingState() {
        repository.getTouchBlockingState()
            .catch { e ->
                Timber.e(e, "Error observing touch blocking state")
                _touchBlockingState.value = _touchBlockingState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
            .onEach { state ->
                _touchBlockingState.value = state
                updateUiState()
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Refresh current state
     */
    fun refreshState() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                updateUiState()
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing state")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Refresh basic state without checking root permission
     */
    fun refreshBasicState() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                updateBasicUiState()
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing basic state")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Toggle touch blocking state
     */
    fun toggleTouchBlocking() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val success = repository.toggleTouchBlocking()
                if (!success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to toggle touch blocking"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling touch blocking")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Request root permission
     */
    fun requestRootPermission() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRequestingRoot = true)
                
                val hasPermission = permissionService.requestRootPermission()
                _uiState.value = _uiState.value.copy(
                    isRequestingRoot = false,
                    hasRootPermission = hasPermission,
                    error = if (!hasPermission) "Failed to obtain root permission" else null
                )
                
                if (hasPermission) {
                    refreshState()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error requesting root permission")
                _uiState.value = _uiState.value.copy(
                    isRequestingRoot = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Check root permission without requesting
     */
    fun checkRootPermission() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val hasPermission = permissionService.requestRootPermission()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasRootPermission = hasPermission
                )
            } catch (e: Exception) {
                Timber.e(e, "Error checking root permission")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Check and request root permission if available
     * 提供更详细的错误信息，帮助用户理解问题
     */
    fun checkAndRequestRootPermission() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRequestingRoot = true)
                
                // 首先检查是否已有root权限
                val hasPermission = permissionService.hasRootPermission()
                if (hasPermission) {
                    _uiState.value = _uiState.value.copy(
                        isRequestingRoot = false,
                        hasRootPermission = true,
                        error = null
                    )
                    refreshState()
                    return@launch
                }
                
                // 检查root是否可用
                val isRootAvailable = permissionService.isRootAvailable()
                if (!isRootAvailable) {
                    _uiState.value = _uiState.value.copy(
                        isRequestingRoot = false,
                        isRootAvailable = false,
                        error = "设备未Root：请先Root您的设备或使用LSPosed框架"
                    )
                    return@launch
                }
                
                // 尝试申请root权限
                val requestResult = permissionService.requestRootPermission()
                _uiState.value = _uiState.value.copy(
                    isRequestingRoot = false,
                    hasRootPermission = requestResult,
                    isRootAvailable = true,
                    error = if (!requestResult) "Root权限申请被拒绝：请在Root管理应用中授予本应用权限" else null
                )
                
                if (requestResult) {
                    refreshState()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking and requesting root permission")
                // 提供更具体的错误信息，而不仅仅是显示异常消息
                val errorMessage = when {
                    e.message?.contains("timeout") == true -> "Root权限请求超时：请检查Root管理应用是否正常运行"
                    e.message?.contains("permission") == true -> "权限错误：Root管理应用拒绝了请求"
                    e.message?.contains("not found") == true -> "未找到su命令：设备可能未正确Root"
                    else -> "Root权限请求失败：${e.message ?: "未知错误"}"
                }
                
                _uiState.value = _uiState.value.copy(
                    isRequestingRoot = false,
                    error = errorMessage
                )
            }
        }
    }
    
    /**
     * Check LSPosed status
     */
    fun checkLSPosedStatus() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val isActive = permissionService.isLSPosedActive()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLSPosedActive = isActive
                )
            } catch (e: Exception) {
                Timber.e(e, "Error checking LSPosed status")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Debug accessibility service status
     */
    fun debugAccessibilityService() {
        viewModelScope.launch {
            try {
                val isEnabled = permissionService.isAccessibilityServiceEnabled()
                Timber.d("Accessibility service debug check: $isEnabled")
                
                // 强制刷新状态
                refreshBasicState()
                
                _uiState.value = _uiState.value.copy(
                    error = if (isEnabled) "无障碍服务检测：已启用" else "无障碍服务检测：未启用"
                )
            } catch (e: Exception) {
                Timber.e(e, "Error debugging accessibility service")
                _uiState.value = _uiState.value.copy(
                    error = "调试无障碍服务失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private suspend fun updateUiState() {
        try {
            val hasRoot = permissionService.hasRootPermission()
            val authMethod = permissionService.getAuthorizationMethod()
            val isLSPosedActive = authMethod == AuthorizationMethod.LSPOSED
            val isAccessibilityEnabled = permissionService.isAccessibilityServiceEnabled()
            val isRootAvailable = permissionService.isRootAvailable()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasRootPermission = hasRoot,
                isLSPosedActive = isLSPosedActive,
                isAccessibilityServiceEnabled = isAccessibilityEnabled,
                authorizationMethod = authMethod,
                isRootAvailable = isRootAvailable,
                touchBlockingState = _touchBlockingState.value,
                error = null
            )
        } catch (e: Exception) {
            Timber.e(e, "Error updating UI state")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }
    
    /**
     * Update basic UI state without actively requesting root permission
     */
    private suspend fun updateBasicUiState() {
        try {
            val authMethod = permissionService.getAuthorizationMethod()
            val isLSPosedActive = authMethod == AuthorizationMethod.LSPOSED
            val isAccessibilityEnabled = permissionService.isAccessibilityServiceEnabled()
            val isRootAvailable = permissionService.isRootAvailable()
            // 只检查是否已有root权限，不主动申请
            val hasRoot = if (isRootAvailable) {
                try {
                    permissionService.hasRootPermission()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to check existing root permission")
                    false
                }
            } else false

            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasRootPermission = hasRoot,
                isLSPosedActive = isLSPosedActive,
                isAccessibilityServiceEnabled = isAccessibilityEnabled,
                authorizationMethod = authMethod,
                isRootAvailable = isRootAvailable,
                touchBlockingState = _touchBlockingState.value,
                error = null
            )
        } catch (e: Exception) {
            Timber.e(e, "Error updating UI state")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }
}

/**
 * UI state for MainActivity
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val hasRootPermission: Boolean = false,
    val isLSPosedActive: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false,
    val authorizationMethod: AuthorizationMethod = AuthorizationMethod.NONE,
    val isRootAvailable: Boolean = false,
    val isRequestingRoot: Boolean = false,
    val touchBlockingState: TouchBlockingState = TouchBlockingState(),
    val error: String? = null
)