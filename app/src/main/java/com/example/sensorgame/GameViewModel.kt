package com.example.sensorgame

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    private val _uiState = mutableStateOf(UiState())
    val uiState: State<UiState> = _uiState

    fun loadMapData(mapData: List<List<Int>>) {
        val startPosition = findStartPosition(mapData)
        if (startPosition != null) {
            _uiState.value = _uiState.value.copy(
                ballPositionX = startPosition.first.toFloat(),
                ballPositionY = startPosition.second.toFloat()
            )
        }
    }

    private fun findStartPosition(mapData: List<List<Int>>): Pair<Int, Int>? {
        mapData.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, tile ->
                if (tile == 0) {
                    // 最初の通路タイルを見つけたら、その行と列を返す
                    return Pair(colIndex, rowIndex)
                }
            }
        }
        return null
    }
}

data class UiState(
    val ballPositionX: Float = 0f,
    val ballPositionY: Float = 0f,
    val score: Int = 0,
    val isGameOver: Boolean = false
)