package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.presentation.viewmodel.MainViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.Constants
import com.example.myapplication.utils.DialogUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("SettingsActivity created")
        
        setContent {
            MyApplicationTheme {
                SettingsScreen()
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen() {
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        LaunchedEffect(Unit) {
            viewModel.refreshState()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                // 按键监听服务卡片（无障碍服务）
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.key_listening_service),
                            fontSize = Constants.FontSize.TITLE_SP.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.status),
                                    fontSize = Constants.FontSize.BODY_SP.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (uiState.touchBlockingState.isAccessibilityServiceEnabled) 
                                        stringResource(R.string.status_enabled) 
                                    else 
                                        stringResource(R.string.status_disabled),
                                    fontSize = Constants.FontSize.BODY_SP.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.touchBlockingState.isAccessibilityServiceEnabled) Color.Green else Color.Red
                                )
                            }
                            
                            Button(
                                onClick = {
                                    if (!uiState.touchBlockingState.isAccessibilityServiceEnabled) {
                                        DialogUtils.showAccessibilityDialog(this@SettingsActivity)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.touchBlockingState.isAccessibilityServiceEnabled) Color.Green else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (uiState.touchBlockingState.isAccessibilityServiceEnabled) 
                                        stringResource(R.string.status_enabled) 
                                    else 
                                        stringResource(R.string.go_enable),
                                    color = Color.White
                                )
                            }
                        }
                        
                        // 添加详细说明和额外的申请按钮
                        if (!uiState.touchBlockingState.isAccessibilityServiceEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "无障碍服务是应用正常工作的必要条件，请点击下方按钮前往系统设置开启。",
                                fontSize = Constants.FontSize.BODY_SP.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    DialogUtils.showAccessibilityDialog(this@SettingsActivity)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.enable_accessibility_service))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.service_description),
                            fontSize = Constants.FontSize.CAPTION_SP.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 添加调试按钮
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.debugAccessibilityService()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("调试无障碍服务", fontSize = Constants.FontSize.BODY_SP.sp)
                        }
                    }
                }
                
                // 触摸屏蔽功能已移至主界面，此处只保留无障碍服务申请功能
                
                // 应用信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_info),
                            fontSize = Constants.FontSize.TITLE_SP.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        StatusRow(stringResource(R.string.app_version), Constants.App.VERSION)
                         StatusRow(stringResource(R.string.package_name), context.packageName)
                         StatusRow(
                             stringResource(R.string.lsposed_module), 
                             if (uiState.isLSPosedActive) 
                                 stringResource(R.string.status_activated) 
                             else 
                                 stringResource(R.string.status_not_activated)
                         )
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
                fontSize = Constants.FontSize.BODY_SP.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = Constants.FontSize.BODY_SP.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
    


}