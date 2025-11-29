package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.datastore.PreferencesManager
import com.example.myapplication.data.model.BlockRegion
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegionSelectionActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                RegionSelectionScreen()
            }
        }
    }

    @Composable
    fun RegionSelectionScreen() {
        val config = LocalConfiguration.current
        val screenWidth = config.screenWidthDp.dp
        val screenHeight = config.screenHeightDp.dp

        var regions by remember { mutableStateOf<List<BlockRegion>>(emptyList()) }
        var currentStart by remember { mutableStateOf<Offset?>(null) }
        var currentEnd by remember { mutableStateOf<Offset?>(null) }

        LaunchedEffect(Unit) {
            preferencesManager.blockRegions.collect { regions = it }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentStart = offset },
                            onDrag = { _, _ -> },
                            onDragEnd = {
                                if (currentStart != null && currentEnd != null) {
                                    val start = currentStart!!
                                    val end = currentEnd!!
                                    val newRegion = BlockRegion(
                                        minOf(start.x, end.x),
                                        minOf(start.y, end.y),
                                        maxOf(start.x, end.x),
                                        maxOf(start.y, end.y)
                                    )
                                    regions = regions + newRegion
                                    currentStart = null
                                    currentEnd = null
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            currentEnd = change.position
                        }
                    }
            ) {
                regions.forEach { region ->
                    drawRect(
                        color = Color.Red.copy(alpha = 0.3f),
                        topLeft = Offset(region.left, region.top),
                        size = Size(region.right - region.left, region.bottom - region.top)
                    )
                }

                if (currentStart != null && currentEnd != null) {
                    val start = currentStart!!
                    val end = currentEnd!!
                    drawRect(
                        color = Color.Yellow.copy(alpha = 0.5f),
                        topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                        size = Size(
                            maxOf(start.x, end.x) - minOf(start.x, end.x),
                            maxOf(start.y, end.y) - minOf(start.y, end.y)
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    lifecycleScope.launch {
                        preferencesManager.setBlockRegions(regions)
                        finish()
                    }
                }) {
                    Text("保存")
                }
                Button(onClick = { regions = emptyList() }) {
                    Text("清除")
                }
                Button(onClick = { finish() }) {
                    Text("取消")
                }
            }
        }
    }
}
