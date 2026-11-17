package com.dantariun.sat_demo_map.util.graphHopper

import android.location.Location
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.util.GeoPoint
import java.util.ArrayDeque

/**
 * [상태 저장 방식] 외부에서 위치 데이터를 받아 안정적인 이동 평균 속력을 계산하고,
 * StateFlow를 통해 외부에 단일 값을 제공하는 헬퍼 클래스
 */
class SpeedCalculator {

    // 위치와 시간을 함께 저장하는 데이터 클래스
    private data class LocationData(val location: Location, val timestamp: Long)

    // 최근 위치 데이터를 저장할 버퍼 (Queue처럼 사용)
    private val locationBuffer = ArrayDeque<LocationData>()

    // 계산된 속도를 저장하고 외부에 노출할 StateFlow
    private val _speedStateFlow = MutableStateFlow(0.0f)
    val speedFlow: StateFlow<Float> = _speedStateFlow

    companion object {
        private const val TAG = "SpeedCalculator"
        // 평균을 계산할 때 사용할 데이터의 수
        private const val WINDOW_SIZE = 10
        // 주행이 멈췄다고 판단하는 시간 (ms). 이 시간 이상 위치 정보가 없으면 버퍼를 초기화한다.
        private const val STALL_THRESHOLD_MS = 3000L
    }

    /**
     * 외부(ViewModel)에서 새로운 GeoPoint를 받아 속력 계산을 트리거하는 유일한 진입점
     */
    fun addGeoPoint(geoPoint: GeoPoint) {
        val newLocation = Location("fromGeoPoint").apply {
            latitude = geoPoint.latitude
            longitude = geoPoint.longitude
        }
        addLocation(newLocation)
    }

    /**
     * ✅ [새로운 핵심 로직] 새로운 위치를 받아 버퍼를 관리하고, 단 한번의 속도 계산을 수행
     */
    private fun addLocation(newLocation: Location) {
        val newLocationData = LocationData(newLocation, System.currentTimeMillis())

        // 마지막 위치 정보가 너무 오래 전이면(정지 후 다시 출발), 이전 기록을 초기화하여 속도가 급격히 튀는 것을 방지
        if (locationBuffer.isNotEmpty() && (newLocationData.timestamp - locationBuffer.last().timestamp > STALL_THRESHOLD_MS)) {
            Log.d(TAG, "주행이 오래 멈춘 후 재시작되어 위치 버퍼를 초기화합니다.")
            locationBuffer.clear()
        }

        // 새로운 위치 데이터를 버퍼에 추가
        locationBuffer.add(newLocationData)

        // 버퍼의 크기가 지정된 WINDOW_SIZE를 초과하면 가장 오래된 데이터를 삭제
        if (locationBuffer.size > WINDOW_SIZE) {
            locationBuffer.removeFirst()
        }

        // 계산을 위한 최소 데이터 수(2개)가 확보되었는지 확인
        if (locationBuffer.size < 2) {
            Log.d(TAG, "데이터 수집 중... (${locationBuffer.size}/$WINDOW_SIZE)")
            _speedStateFlow.value = 0.0f
            return
        }

        val oldestData = locationBuffer.first()
        val newestData = locationBuffer.last()

        val totalDistanceInMeters = oldestData.location.distanceTo(newestData.location)
        val totalTimeInSeconds = (newestData.timestamp - oldestData.timestamp) / 1000.0f

        // 거의 움직이지 않았거나 시간이 비정상적일 때
        if (totalDistanceInMeters < 1.0f || totalTimeInSeconds <= 0) {
            _speedStateFlow.value = 0.0f
            return
        }

        val averageSpeedInMps = totalDistanceInMeters / totalTimeInSeconds
        val averageSpeedInKmh = averageSpeedInMps * 3.6f

        // 계산된 최종 속도 값을 StateFlow에 업데이트 (이때 UI가 갱신됨)
        _speedStateFlow.value = averageSpeedInKmh
    }
}