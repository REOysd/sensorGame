package com.example.sensorgame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel(), SensorEventListener {
    private val _uiState = mutableStateOf(UiState())
    val uiState: State<UiState> = _uiState
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var mapData: List<List<Int>> = emptyList()
    
    private var ballVelocityX = 0f
    private var ballVelocityY = 0f
    private val friction = 0.95f
    private val gravity = 0.5f
    private val maxVelocity = 0.3f
    private val ballRadius = 0.4f

    fun loadMapData(mapData: List<List<Int>>) {
        this.mapData = mapData
        val startPosition = findStartPosition(mapData)
        if (startPosition != null) {
            _uiState.value = _uiState.value.copy(
                ballPositionX = startPosition.first.toFloat(),
                ballPositionY = startPosition.second.toFloat()
            )
        }
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
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // 加速度センサーの値を取得（重力を含む）
                val accelX = -it.values[0] * gravity
                val accelY = it.values[1] * gravity
                
                Log.d("GameViewModel", "Sensor values - X: ${it.values[0]}, Y: ${it.values[1]}, accelX: $accelX, accelY: $accelY")
                
                // 速度を更新
                ballVelocityX += accelX
                ballVelocityY += accelY
                
                // 最大速度を制限
                ballVelocityX = ballVelocityX.coerceIn(-maxVelocity, maxVelocity)
                ballVelocityY = ballVelocityY.coerceIn(-maxVelocity, maxVelocity)
                
                // 摩擦を適用
                ballVelocityX *= friction
                ballVelocityY *= friction
                
                // 新しい位置を計算
                val currentState = _uiState.value
                var newX = currentState.ballPositionX + ballVelocityX
                var newY = currentState.ballPositionY + ballVelocityY
                
                Log.d("GameViewModel", "Ball position - Current: (${currentState.ballPositionX}, ${currentState.ballPositionY}), New: ($newX, $newY)")
                
                // mapDataが空でない場合のみ衝突判定
                if (mapData.isNotEmpty()) {
                    // 壁との衝突判定
                    val collision = checkWallCollision(newX, newY)
                    if (collision.first) {
                        ballVelocityX *= -0.5f // 反発係数を小さく
                        newX = currentState.ballPositionX // X座標は元の位置に戻す
                    }
                    if (collision.second) {
                        ballVelocityY *= -0.5f // 反発係数を小さく
                        newY = currentState.ballPositionY // Y座標は元の位置に戻す
                    }
                    
                    // 画面境界での反発
                    if (newX <= 0 || newX >= mapData[0].size - 1) {
                        ballVelocityX *= -0.7f
                        newX = maxOf(0f, minOf(newX, mapData[0].size - 1f))
                    }
                    if (newY <= 0 || newY >= mapData.size - 1) {
                        ballVelocityY *= -0.7f
                        newY = maxOf(0f, minOf(newY, mapData.size - 1f))
                    }
                }
                
                _uiState.value = currentState.copy(
                    ballPositionX = newX,
                    ballPositionY = newY
                )
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    
    private fun checkWallCollision(newX: Float, newY: Float): Pair<Boolean, Boolean> {
        if (mapData.isEmpty()) return Pair(false, false)
        
        val currentState = _uiState.value
        
        val ballLeft = newX - ballRadius
        val ballRight = newX + ballRadius
        val ballTop = newY - ballRadius
        val ballBottom = newY + ballRadius
        
        var hitWallX = false
        var hitWallY = false
        
        val currentTileY = currentState.ballPositionY.toInt()
        val nextTileX = if (ballVelocityX > 0) ballRight.toInt() else ballLeft.toInt()
        
        if (nextTileX >= 0 && nextTileX < mapData[0].size && 
            currentTileY >= 0 && currentTileY < mapData.size) {
            if (mapData[currentTileY][nextTileX] == 1) { // 1が壁
                hitWallX = true
            }
        }
        
        val currentTileX = currentState.ballPositionX.toInt()
        val nextTileY = if (ballVelocityY > 0) ballBottom.toInt() else ballTop.toInt()
        
        if (currentTileX >= 0 && currentTileX < mapData[0].size && 
            nextTileY >= 0 && nextTileY < mapData.size) {
            if (mapData[nextTileY][currentTileX] == 1) { // 1が壁
                hitWallY = true
            }
        }
        
        if (ballLeft < 0 || ballRight >= mapData[0].size) {
            hitWallX = true
        }
        if (ballTop < 0 || ballBottom >= mapData.size) {
            hitWallY = true
        }
        
        return Pair(hitWallX, hitWallY)
    }

    private fun findStartPosition(mapData: List<List<Int>>): Pair<Int, Int>? {
        mapData.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, tile ->
                if (tile == 0) {
                    return Pair(colIndex, rowIndex)
                }
            }
        }
        return null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopSensorListening()
    }
}

data class UiState(
    val ballPositionX: Float = 0f,
    val ballPositionY: Float = 0f,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val isGameClear: Boolean = false,
    val message: String? = null,
    val elapsedTime: Long = 0L,
    val isGameActive: Boolean = false
)