package com.example.sensorgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.sensorgame.ui.theme.SensorGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorGameTheme {
                GameApp()
            }
        }
    }
}

enum class GameState {
    COLOR_SELECTION,
    PLAYING,
    RESULT
}

@Composable
fun GameApp() {
    var gameState by remember { mutableStateOf(GameState.COLOR_SELECTION) }
    val viewModel = remember { GameViewModel() }
    val uiState by viewModel.uiState
    
    // Watch for game end
    if (uiState.message != null && gameState == GameState.PLAYING) {
        gameState = GameState.RESULT
    }
    
    when (gameState) {
        GameState.COLOR_SELECTION -> {
            ColorSelectionScreen(
                selectedColor = uiState.ballColor,
                onColorSelected = { color -> viewModel.changeBallColor(color) },
                onStartGame = { 
                    viewModel.resetGame()
                    gameState = GameState.PLAYING 
                }
            )
        }
        GameState.PLAYING -> {
            GameScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )
        }
        GameState.RESULT -> {
            ResultScreen(
                message = uiState.message ?: "",
                isGameClear = uiState.isGameClear,
                onRestart = {
                    viewModel.resetGame()
                    gameState = GameState.COLOR_SELECTION
                }
            )
        }
    }
}