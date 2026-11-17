package com.dantariun.sat_demo_map.util.map

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.asin

/**
 * 디바이스 센서를 사용하여 MapView의 회전 및 기울기를 제어하는 헬퍼 클래스.
 * @param context 애플리케이션 컨텍스트
 */
class MapSensorController(context: Context) : SensorEventListener {

    /**
     * 센서 값 업데이트를 수신하는 리스너 인터페이스
     */
    interface OnSensorUpdateListener {
        /**
         * 디바이스 방향에 따른 지도 회전 각도가 업데이트될 때 호출됩니다.
         * @param rotationAngle 지도에 적용할 회전 각도 (0-360)
         */
        fun onRotationUpdate(rotationAngle: Float)

        /**
         * 디바이스 기울기에 따른 지도 기울기 각도가 업데이트될 때 호출됩니다.
         * @param tiltAngle 지도에 적용할 기울기 각도 (0-60)
         */
        fun onTiltUpdate(tiltAngle: Float)
    }

    private var listener: OnSensorUpdateListener? = null
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // 센서 객체
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // 센서 데이터를 저장할 배열
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var isRotationEnabled = false
    private var isTiltEnabled = false
    private var isStarted = false

    /**
     * 센서 업데이트를 받을 리스너를 설정합니다.
     */
    fun setOnSensorUpdateListener(listener: OnSensorUpdateListener?) {
        this.listener = listener
    }

    /**
     * 디바이스 방향에 따른 지도 자동 회전 기능을 활성화/비활성화합니다.
     */
    fun enableRotation(enable: Boolean) {
        isRotationEnabled = enable
        if (isStarted) { // 이미 동작 중이면 센서 재시작
            stop()
            start()
        }
    }

    /**
     * 디바이스 기울기에 따른 지도 자동 기울이기 기능을 활성화/비활성화합니다.
     */
    fun enableTilt(enable: Boolean) {
        isTiltEnabled = enable
        if (isStarted) { // 이미 동작 중이면 센서 재시작
            stop()
            start()
        }
    }

    /**
     * 센서 리스너 등록을 시작합니다. Activity/Fragment의 onResume()에서 호출하세요.
     */
    fun start() {
        if (isTiltEnabled) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
        if (isRotationEnabled) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
        isStarted = true
    }

    /**
     * 센서 리스너 등록을 해제합니다. Activity/Fragment의 onPause()에서 호출하세요.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
        isStarted = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.size)
            if (isTiltEnabled) updateTilt()
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
        }

        if (isRotationEnabled) updateRotation()
    }

    private fun updateRotation() {
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val screenAdjustment = when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_90 -> 90f
                Surface.ROTATION_180 -> 180f
                Surface.ROTATION_270 -> 270f
                else -> 0f
            }
            listener?.onRotationUpdate(-azimuth - screenAdjustment)
        }
    }

    private fun updateTilt() {
        val normOfG = kotlin.math.sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        val normalizedY = gravity[1] / normOfG
        val pitchInDegrees = Math.toDegrees(asin(normalizedY).toDouble()).toFloat()
        val tilt = (90 - abs(pitchInDegrees)) * (60f / 90f)
        listener?.onTiltUpdate(tilt)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}