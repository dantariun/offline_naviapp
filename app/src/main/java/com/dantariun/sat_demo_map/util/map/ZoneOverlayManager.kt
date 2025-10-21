package com.dantariun.sat_demo_map.util.map

import android.graphics.Color
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.domain.ZoneType
import com.dantariun.sat_demo_map.util.common.CommonUtil
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


object ZoneOverlayManager {

    /**
     * 지도에 특정 유형의 재난 구역을 그립니다.
     *
     * @param mapView MapView 인스턴스
     * @param zonePoints 다각형을 구성하는 GeoPoint 리스트
     * @param zoneType 재난 유형 (ZoneType.FIRE, ZoneType.FLOOD 등)
     */
    fun drawZone(mapView: MapView, zonePoints: List<GeoPoint>, zoneType: ZoneType) {
        if (zonePoints.size < 3) return // 다각형을 만들려면 최소 3개의 점이 필요

        val polygon = Polygon().apply {
            // 1. 다각형의 꼭짓점 설정
            points = zonePoints

            // 2. 재난 유형에 따라 색상 스타일 설정
//            val (fillColor, strokeColor) = getZoneColors(zoneType)
            val (fillColor, strokeColor) =
                Pair(Color.argb(50, 255, 0, 0), Color.RED)

            // 3. 내부 채우기 스타일 설정 (핵심: 반투명 색상 적용)
            fillPaint.apply {
                style = android.graphics.Paint.Style.FILL
                color = fillColor
            }

            // 4. 외곽선 스타일 설정
            outlinePaint.apply {
                style = android.graphics.Paint.Style.STROKE
                color = strokeColor
                strokeWidth = 8f // 선 굵기
            }
        }

        // 5. 지도에 다각형 오버레이 추가 및 갱신
        mapView.overlays.add(polygon)

        // --- 2. 중심 마커(Marker) 생성 및 추가 (추가된 부분) ---

        // 2-1. 폴리곤의 경계 상자(BoundingBox)를 이용해 중심점 계산
        val centerPoint = polygon.bounds.centerWithDateLine

        // 2-2. 마커 생성
        val iconMarker = Marker(mapView).apply {
            position = centerPoint
            // 아이콘의 정중앙이 좌표에 위치하도록 앵커 설정
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            // 재난 유형에 맞는 아이콘 Drawable 설정
            icon = CommonUtil.resizeIcon(mapView.context, getZoneIcon(zoneType), 36, 36)
            // 아이콘이 다른 오버레이에 가려지지 않도록 Z-Index 우선순위를 높게 설정 (선택 사항)
            // setPanToView(false) // 마커 클릭 시 지도가 이동하지 않도록 설정
        }
        mapView.overlays.add(iconMarker)
        mapView.invalidate()
    }

    /**
     * 재난 유형에 맞는 색상 쌍을 반환합니다.
     * @return Pair<채우기 색상, 외곽선 색상>
     */
    private fun getZoneColors(zoneType: ZoneType): Pair<Int, Int> {
        return when (zoneType) {
            // 화재: 반투명 빨강 채우기 + 진한 빨강 외곽선
            ZoneType.FIRE -> Pair(Color.argb(50, 255, 0, 0), Color.RED)
            // 침수: 반투명 파랑 채우기 + 진한 파랑 외곽선
            ZoneType.FLOOD -> Pair(Color.argb(50, 0, 0, 255), Color.BLUE)
            // 사고: 반투명 노랑 채우기 + 진한 노랑 외곽선
            ZoneType.ACCIDENT -> Pair(Color.argb(50, 255, 255, 0), Color.YELLOW)
            ZoneType.ROAD_CONSTRUCTION -> Pair(Color.argb(50, 255, 0, 0), Color.RED)
            ZoneType.DISASTER -> Pair(Color.argb(50, 255, 0, 0), Color.RED)
        }
    }

    /**
     * 재난 유형에 맞는 아이콘 리소스 ID를 반환합니다. (추가된 함수)
     * @return Drawable Resource ID
     */
    private fun getZoneIcon(zoneType: ZoneType): Int {
        return when (zoneType) {
            ZoneType.FIRE -> R.drawable.icon_fire // 화재 아이콘
            ZoneType.FLOOD -> R.drawable.icon_flood // 침수 아이콘
            ZoneType.ACCIDENT -> R.drawable.icon_accident // 사고 아이콘
            ZoneType.ROAD_CONSTRUCTION -> R.drawable.icon_road_construction
            ZoneType.DISASTER -> R.drawable.icon_disaster
        }
    }
}