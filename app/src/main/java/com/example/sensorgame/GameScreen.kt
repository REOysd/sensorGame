package com.example.sensorgame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState
    var mapData by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.initializeSensors(context)
        viewModel.startSensorListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSensorListening()
        }
    }

    LaunchedEffect(Unit) {
        val inputStream = context.assets.open("stage1.txt")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        mapData = lines.map { line ->
            // カンマ区切りの場合の処理
            if (line.contains(',')) {
                line.split(',').map { it.trim().toInt() }
            } else {
                // スペース区切りまたは連続した数字の場合
                line.trim().split("\\s+".toRegex()).map { it.toInt() }
            }
        }
        reader.close()

        viewModel.loadMapData(mapData)
    }

    if (mapData.isNotEmpty()) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val tileSizeX = constraints.maxWidth.toFloat() / mapData[0].size
            val tileSizeY = constraints.maxHeight.toFloat() / mapData.size
            DrawGameMap(mapData)
            DrawBall(
                x = uiState.ballPositionX * tileSizeX + tileSizeX / 2,
                y = uiState.ballPositionY * tileSizeY + tileSizeY / 2,
                color = uiState.ballColor
            )
        }
    }
}

@Composable
fun DrawGameMap(mapData: List<List<Int>>) {
    Column(modifier = Modifier.fillMaxSize()) {
        for (row in mapData.indices) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (col in mapData[row].indices) {
                    val color = when (mapData[row][col]) {
                        0 -> Color.Black // 通路
                        1 -> Color.White // 壁
                        2 -> Color.Green // 出口 (ゴール)
                        3 -> Color.Gray   // 穴
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawBall(x: Float, y: Float, color: Color = Color.Red) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = color,
            radius = 16.dp.toPx(),
            center = Offset(x = x, y = y)
        )
    }
}