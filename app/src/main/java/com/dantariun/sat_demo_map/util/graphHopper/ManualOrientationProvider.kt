package com.dantariun.sat_demo_map.util.graphHopper

import org.osmdroid.views.overlay.compass.IOrientationConsumer
import org.osmdroid.views.overlay.compass.IOrientationProvider

/**
 * 하드웨어 센서 대신, 외부에서 수동으로 방향 값을 주입받는 OrientationProvider.
 * RouteAnimator가 계산한 경로의 방향을 CompassOverlay에 전달하는 다리 역할을 한다.
 */
class ManualOrientationProvider : IOrientationProvider {

    private var orientation: Float = 0f
    private var orientationConsumer: IOrientationConsumer? = null

    /**
     * 마지막으로 알려진 방향 값을 반환합니다. CompassOverlay가 이 값을 사용해 자신을 그립니다.
     */
    override fun getLastKnownOrientation(): Float {
        return orientation
    }

    override fun destroy() {
    }

    /**
     * 외부(RouteAnimator)에서 새로운 방향 값을 설정하기 위한 함수.
     * @param newOrientation 새로운 방향(bearing) 값
     */
    fun setOrientation(newOrientation: Float) {
        this.orientation = newOrientation

        // Consumer(CompassOverlay)가 있다면 방향이 변경되었음을 알려줍니다.
        orientationConsumer?.onOrientationChanged(orientation, this)
    }

    override fun startOrientationProvider(orientationConsumer: IOrientationConsumer): Boolean {
        this.orientationConsumer = orientationConsumer
        return true // 항상 성공
    }

    override fun stopOrientationProvider() {
        this.orientationConsumer = null
    }
}