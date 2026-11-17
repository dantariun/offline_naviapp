// util/map/MapViewExtensions.kt

package com.dantariun.sat_demo_map.util.map

import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.ln
import org.osmdroid.util.MyMath // ✅ gudermannInverse 함수를 사용하기 위해 임포트

/**
 * 주어진 지점들을 모두 포함하는 최적의 줌 레벨을 계산하여 반환합니다.
 *
 * @param points 표시할 GeoPoint 목록
 * @param paddingDp 화면 가장자리로부터의 여백 (DP 단위)
 * @return 계산된 줌 레벨 (Double)
 */
fun MapView.zoomLevelThatFits(points: List<GeoPoint>, paddingDp: Int): Double {
    if (points.isEmpty()) {
        return this.zoomLevelDouble
    }

    val boundingBox = BoundingBox.fromGeoPoints(points)

    // DP를 Pixel로 변환
    val paddingPx = (paddingDp * this.context.resources.displayMetrics.density).toInt()

    val availableWidth = this.width - 2 * paddingPx
    val availableHeight = this.height - 2 * paddingPx

    if (availableWidth <= 0 || availableHeight <= 0) {
        return this.zoomLevelDouble
    }

    // 1. 위도(Latitude) 기준 줌 레벨 계산 (메르카토르 투영법 고려)
    val latNorth = boundingBox.latNorth
    val latSouth = boundingBox.latSouth
    // gudermannInverse는 위도를 메르카토르 투영의 Y좌표로 변환합니다.
    val latitudeZoom = ln(availableHeight / (
            MyMath.gudermannInverse(latNorth) - MyMath.gudermannInverse(latSouth)
            ) * 2 * Math.PI / 256) / ln(2.0)

    // 2. 경도(Longitude) 기준 줌 레벨 계산
    val longitudeSpan = boundingBox.longitudeSpanWithDateLine
    val longitudeZoom = if (longitudeSpan == 0.0) {
        21.0 // 너비가 0일 경우 최대 줌 레벨 근사치 반환
    } else {
        ln(availableWidth * 360 / longitudeSpan / 256) / ln(2.0)
    }

    // 3. 두 줌 레벨 중 더 작은 값을 선택 (더 축소된 뷰를 선택해야 모든 영역이 보임)
    return Math.min(latitudeZoom, longitudeZoom)

}