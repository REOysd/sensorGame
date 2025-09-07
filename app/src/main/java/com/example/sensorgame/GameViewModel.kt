package com.example.sensorgame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class GameViewModel : ViewModel(), SensorEventListener {
    private val _uiState = mutableStateOf(UiState())
    val uiState: State<UiState> = _uiState

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var mapData: List<List<Int>> = emptyList()
    private var startPosition: Pair<Int, Int>? = null

    private var ballVelocityX = 0f
    private var ballVelocityY = 0f
    private val friction = 0.95f
    private val gravity = 0.5f
    private val maxVelocity = 0.3f
    private val ballRadius = 0.4f

    private var timerJob: Job? = null // プレイ時間計測用のタイマージョブ

    // --- 公開関数 ---

    fun loadMapData(mapData: List<List<Int>>) {
        this.mapData = mapData
        this.startPosition = findStartPosition(mapData)
        resetGame()
    }

    fun initializeSensors(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        Log.d("GameViewModel", "Sensors initialized. Accelerometer available: ${accelerometer != null}")
    }

    fun startSensorListening() {
        accelerometer?.let {
            val result = sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("GameViewModel", "Started sensor listening. Success: $result")
        } ?: Log.e("GameViewModel", "Accelerometer not available")
    }

    fun stopSensorListening() {
        sensorManager?.unregisterListener(this)
        stopTimer() // センサー停止時にタイマーも停止
    }

    // ボールの色を変更する関数
    fun changeBallColor(color: Color) {
        _uiState.value = _uiState.value.copy(ballColor = color)
    }

    // ゲームの状態を初期化（リセット）する関数
    fun resetGame() {
        stopTimer() // 既存のタイマーを停止
        startPosition?.let {
            _uiState.value = UiState(
                ballPositionX = it.first.toFloat(),
                ballPositionY = it.second.toFloat(),
                ballColor = _uiState.value.ballColor // 色はリセットせず維持する
            )
        }
        // ゲームをアクティブにしてタイマーを開始
        _uiState.value = _uiState.value.copy(isGameActive = true)
        startTimer()
    }

    // --- センサーイベントリスナー ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (!_uiState.value.isGameActive) return // ゲームが終了していたら何もしない

        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val accelX = -it.values[0] * gravity
                val accelY = it.values[1] * gravity

                ballVelocityX += accelX
                ballVelocityY += accelY

                ballVelocityX = ballVelocityX.coerceIn(-maxVelocity, maxVelocity)
                ballVelocityY = ballVelocityY.coerceIn(-maxVelocity, maxVelocity)

                ballVelocityX *= friction
                ballVelocityY *= friction

                val currentState = _uiState.value
                var newX = currentState.ballPositionX + ballVelocityX
                var newY = currentState.ballPositionY + ballVelocityY

                if (mapData.isNotEmpty()) {
                    val collision = checkWallCollision(newX, newY)
                    if (collision.first) {
                        ballVelocityX *= -0.5f
                        newX = currentState.ballPositionX
                    }
                    if (collision.second) {
                        ballVelocityY *= -0.5f
                        newY = currentState.ballPositionY
                    }
                }

                _uiState.value = currentState.copy(
                    ballPositionX = newX,
                    ballPositionY = newY
                )

                // ゴールと穴の判定を毎フレーム行う
                checkGoalOrHole(newX, newY)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    // --- プライベート関数 ---

    private fun checkWallCollision(newX: Float, newY: Float): Pair<Boolean, Boolean> {
        if (mapData.isEmpty()) return Pair(false, false)

        val ballLeft = (newX - ballRadius).toInt()
        val ballRight = (newX + ballRadius).toInt()
        val ballTop = (newY - ballRadius).toInt()
        val ballBottom = (newY + ballRadius).toInt()

        var hitWallX = false
        var hitWallY = false

        // X方向の衝突
        val checkX = if (ballVelocityX > 0) ballRight else ballLeft
        if (checkX >= 0 && checkX < mapData[0].size && newY.toInt() >= 0 && newY.toInt() < mapData.size) {
            if (mapData[newY.toInt()][checkX] == 1) { // 1が壁
                hitWallX = true
            }
        }

        // Y方向の衝突
        val checkY = if (ballVelocityY > 0) ballBottom else ballTop
        if (checkY >= 0 && checkY < mapData.size && newX.toInt() >= 0 && newX.toInt() < mapData[0].size) {
            if (mapData[checkY][newX.toInt()] == 1) {
                hitWallY = true
            }
        }

        // 画面境界チェック
        if ((newX - ballRadius) < 0 || (newX + ballRadius) >= mapData[0].size) hitWallX = true
        if ((newY - ballRadius) < 0 || (newY + ballRadius) >= mapData.size) hitWallY = true

        return Pair(hitWallX, hitWallY)
    }

    // ボールがゴールまたは穴に到達したかを判定する関数
    private fun checkGoalOrHole(x: Float, y: Float) {
        if (mapData.isEmpty()) return
        val tileX = x.toInt()
        val tileY = y.toInt()

        if (tileY < 0 || tileY >= mapData.size || tileX < 0 || tileX >= mapData[0].size) return

        val tileType = mapData[tileY][tileX]
        val elapsedTimeFormatted = formatTime(_uiState.value.elapsedTime)

        if (tileType == 2) { // 2: ゴール
            stopTimer() // タイマーを停止
            _uiState.value = _uiState.value.copy(
                isGameClear = true,
                isGameActive = false, // ゲームを非アクティブに
                message = "🎉クリア！🎉\nプレイ時間: $elapsedTimeFormatted" // クリアメッセージを設定
            )
        } else if (tileType == 3) { // 3: 穴
            stopTimer() // タイマーを停止
            _uiState.value = _uiState.value.copy(
                isGameOver = true,
                isGameActive = false, // ゲームを非アクティブに
                message = "ゲームオーバー\nプレイ時間: $elapsedTimeFormatted" // ゲームオーバーメッセージを設定
            )
        }
    }


    private fun findStartPosition(mapData: List<List<Int>>): Pair<Int, Int>? {
        mapData.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, tile ->
                if (tile == 0) { // 最初の通路(0)をスタート地点とする
                    return Pair(colIndex, rowIndex)
                }
            }
        }
        return null
    }

    // プレイ時間計測タイマーを開始する関数
    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (_uiState.value.isGameActive) {
                delay(1000) // 1秒待機
                _uiState.value = _uiState.value.copy(
                    elapsedTime = _uiState.value.elapsedTime + 1 // 経過時間を1増やす
                )
            }
        }
    }

    // プレイ時間計測タイマーを停止する関数
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // 経過時間（秒）を mm:ss 形式の文字列にフォーマットする関数
    private fun formatTime(seconds: Long): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorListening()
    }
}

data class UiState(
    val ballPositionX: Float = 0f,
    val ballPositionY: Float = 0f,
    val ballColor: Color = Color.Red, // ボールの色を管理
    val isGameOver: Boolean = false, // ゲームオーバー状態を管理
    val isGameClear: Boolean = false, // ゲームクリア状態を管理
    val message: String? = null, // ゲーム終了時のメッセージ
    val elapsedTime: Long = 0L, // 経過時間（秒）
    val isGameActive: Boolean = false // ゲームがアクティブか（タイマーやセンサーを制御）
)