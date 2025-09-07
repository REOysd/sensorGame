package com.example.sensorgame

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    private val _uiState = mutableStateOf(UiState())
    val uiState: State<UiState> = _uiState
}

data class UiState(
    val playerX: Float = 0f,
    val playerY: Float = 0f,
    val score: Int = 0,
    val isGameOver: Boolean = false
)