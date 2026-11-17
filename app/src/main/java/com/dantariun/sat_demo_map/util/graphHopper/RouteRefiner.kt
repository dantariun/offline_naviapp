package com.dantariun.sat_demo_map.util.graphHopper

import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * GeoPoint 리스트를 받아서, 두 지점 사이의 거리가 지정된 최대 거리를 초과하는 경우
 * 중간 지점들을 추가하여 경로를 더 부드럽게 만드는 유틸리티 클래스입니다.
 *
 * @property maxDistanceMeters 두 지점 사이에 허용되는 최대 거리 (미터 단위). 이 거리보다 길면 중간 지점이 생성됩니다.
 */
class RouteRefiner(private val maxDistanceMeters: Double = 3.0) {

    /**
     * 원본 GeoPoint 리스트를 기반으로 중간 지점이 추가된 새로운 리스트를 반환합니다.
     *
     * @param originalRoute 원본 경로 리스트.
     * @return 중간 지점이 추가된 새로운 경로 리스트.
     */
    fun refineRoute(originalRoute: List<GeoPoint>): List<GeoPoint> {
        // 경로에 지점이 2개 미만이면 보정할 필요가 없으므로 그대로 반환합니다.
        if (originalRoute.size < 2) {
            return originalRoute
        }

        val refinedRoute = mutableListOf<GeoPoint>()

        // 경로의 각 구간(segment)에 대해 반복합니다.
        for (i in 0 until originalRoute.size - 1) {
            val startPoint = originalRoute[i]
            val endPoint = originalRoute[i + 1]

            // 항상 현재 구간의 시작점을 리스트에 추가합니다.
            refinedRoute.add(startPoint)

            // 두 지점 사이의 거리를 미터 단위로 계산합니다.
            val distance = startPoint.distanceToAsDouble(endPoint)

            // 계산된 거리가 설정된 최대 거리보다 길 경우에만 중간 지점을 추가합니다.
            if (distance > maxDistanceMeters) {
                // 최대 거리를 기준으로 몇 개의 구간으로 나눌지 계산합니다. (올림 처리)
                val segments = ceil(distance / maxDistanceMeters).toInt()

                if (segments > 1) {
                    // 삽입할 중간 지점들을 계산하고 리스트에 추가합니다.
                    for (j in 1 until segments) {
                        val fraction = j.toDouble() / segments.toDouble()
                        val intermediatePoint = getIntermediatePoint(startPoint, endPoint, fraction)
                        refinedRoute.add(intermediatePoint)
                    }
                }
            }
        }

        // 전체 경로의 가장 마지막 지점을 리스트에 추가합니다.
        originalRoute.lastOrNull()?.let {
            refinedRoute.add(it)
        }

        return refinedRoute
    }

    private fun getIntermediatePoint(start: GeoPoint, end: GeoPoint, fraction: Double): GeoPoint {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1

        val bX = cos(lat2) * cos(dLon)
        val bY = cos(lat2) * sin(dLon)

        val lat3 = atan2(
            sin(lat1) + sin(lat2),
            sqrt((cos(lat1) + bX) * (cos(lat1) + bX) + bY * bY)
        )
        val lon3 = lon1 + atan2(bY, cos(lat1) + bX)

        // 위 코드는 간단한 구형 모델 보간입니다.
        // 더 정확한 Slerp(구면 선형 보간) 공식을 적용하면 아래와 같습니다.

        // 각도 거리 계산
        val angle = acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(dLon))

        // 보간 계수
        val a = sin((1 - fraction) * angle) / sin(angle)
        val b = sin(fraction * angle) / sin(angle)

        // 카테시안 좌표로 변환
        val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
        val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
        val z = a * sin(lat1) + b * sin(lat2)

        // 다시 위도/경도로 변환
        val finalLat = atan2(z, sqrt(x * x + y * y))
        val finalLon = atan2(y, x)

        return GeoPoint(Math.toDegrees(finalLat), Math.toDegrees(finalLon))
    }
}