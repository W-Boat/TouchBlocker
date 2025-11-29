package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.presentation.viewmodel.MainViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import com.example.myapplication.utils.Constants
import com.example.myapplication.utils.DialogUtils

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity created")
        
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 当从其他界面返回时只刷新基本状态，不自动检查root权限
        viewModel.refreshBasicState()
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        LaunchedEffect(Unit) {
            // 只刷新基本状态，不自动检查root权限
            viewModel.refreshBasicState()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(
                            onClick = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 主开关 - 移到顶部
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.touch_blocking_function),
                            fontSize = Constants.FontSize.TITLE_SP.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val canEnable = uiState.touchBlockingState.isAvailable
                        
                        Button(
                            onClick = {
                                if (canEnable) {
                                    viewModel.toggleTouchBlocking()
                                }
                            },
                            enabled = canEnable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Constants.UI.BUTTON_HEIGHT_DP.dp),
                            colors = ButtonDefaults.buttonColors(
                                 containerColor = if (uiState.touchBlockingState.isEnabled) Color.Red else MaterialTheme.colorScheme.primary
                             )
                        ) {
                            Text(
                                text = if (uiState.touchBlockingState.isEnabled) 
                                    stringResource(R.string.disable_touch_blocking) 
                                else 
                                    stringResource(R.string.enable_touch_blocking),
                                fontSize = Constants.FontSize.SUBTITLE_SP.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (!canEnable) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = when {
                                         !uiState.touchBlockingState.isAccessibilityServiceEnabled -> stringResource(R.string.please_enable_accessibility_first)
                                         !uiState.touchBlockingState.canEnable -> stringResource(R.string.need_root_or_lsposed)
                                         else -> stringResource(R.string.function_unavailable)
                                     },
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = Constants.FontSize.BODY_SP.sp
                                )
                                
                                // 无障碍服务申请已移至设置界面
                            }
                        }
                    }
                }
                
                // 权限管理卡片 - 合并检查和申请Root功能
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.permission_management),
                            fontSize = Constants.FontSize.TITLE_SP.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.checkAndRequestRootPermission()
                                },
                                enabled = !uiState.isRequestingRoot,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isRequestingRoot) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("检查/申请Root", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.checkLSPosedStatus()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("检查LSPosed", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val intent = Intent(context, RegionSelectionActivity::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("设置屏蔽区域", fontSize = 14.sp)
                        }

                        // 调试无障碍服务按钮已移至设置界面
                    }
                }
                
                // 状态信息卡片 - 移到底部
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "系统状态",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        StatusRow(
                             stringResource(R.string.root_permission), 
                             if (uiState.hasRootPermission) stringResource(R.string.status_obtained) 
                             else if (uiState.isRootAvailable) stringResource(R.string.status_available) 
                             else stringResource(R.string.status_unavailable)
                         )
                         StatusRow(
                             stringResource(R.string.lsposed_framework), 
                             if (uiState.isLSPosedActive) stringResource(R.string.status_activated) 
                             else stringResource(R.string.status_not_activated)
                         )
                         StatusRow(
                             stringResource(R.string.accessibility_service), 
                             if (uiState.isAccessibilityServiceEnabled) stringResource(R.string.status_enabled) 
                             else stringResource(R.string.status_disabled)
                         )
                         StatusRow(stringResource(R.string.authorization_method), uiState.authorizationMethod.getDisplayName())
                         StatusRow(stringResource(R.string.device_model), "${Build.MANUFACTURER} ${Build.MODEL}")
                        StatusRow(stringResource(R.string.android_version), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                         StatusRow(stringResource(R.string.app_version), Constants.App.VERSION)
                    }
                }
            }
        }
    }
    
    @Composable
    fun StatusRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
    


}