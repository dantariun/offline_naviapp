// src/main/java/com/fu_it/sat_demo_map/ui/helper/RouteOverlayManager.kt

package com.dantariun.sat_demo_map.ui.helper.ui

import android.graphics.Color
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class RouteOverlayManager(private val mapView: MapView) {

    private var currentRoutePolyline: Polyline? = null

    /**
     * 지도에 경로를 그리고, 해당 경로가 모두 보이도록 줌과 위치를 조절합니다.
     * @param routePoints 경로를 구성하는 GeoPoint 리스트
     */
    fun drawRouteAndZoom(routePoints: List<GeoPoint>) {
        // 유효한 경로 데이터가 없으면 아무것도 하지 않음
        if (routePoints.isEmpty()) return

        // 1. 기존에 그려진 경로가 있다면 제거
        clearRoute()

        // 2. 새로운 Polyline 생성 및 설정
        currentRoutePolyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = ROUTE_COLOR
            outlinePaint.strokeWidth = ROUTE_WIDTH
        }

        // 3. 지도에 Polyline 추가
        mapView.overlays.add(currentRoutePolyline)

        // 4. 경로가 모두 보이도록 BoundingBox를 계산하여 지도 뷰 조절
        // mapView.post를 사용하여 UI 렌더링이 끝난 후 실행되도록 보장
        mapView.post {
            val boundingBox = BoundingBox.fromGeoPoints(routePoints)
            mapView.zoomToBoundingBox(boundingBox, true, MAP_PADDING)
        }

        mapView.invalidate()
    }

    /**
     * 지도에 그려진 경로를 제거합니다.
     */
    fun clearRoute() {
        currentRoutePolyline?.let { mapView.overlays.remove(it) }
        currentRoutePolyline = null
        mapView.invalidate()
    }

    companion object {
        private const val ROUTE_COLOR = Color.BLUE
        private const val ROUTE_WIDTH = 10.0f
        private const val MAP_PADDING = 100 // BoundingBox 주변의 여백 (pixel)
    }
}