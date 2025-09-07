package com.example.sensorgame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun GameScreen(viewModel: GameViewModel = GameViewModel()) {
    val context = LocalContext.current
    var mapData by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

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
    }

    if (mapData.isNotEmpty()) {
        DrawGameMap(mapData)
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