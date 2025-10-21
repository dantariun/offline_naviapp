package com.dantariun.sat_demo_map.util.graphHopper

import com.dantariun.sat_demo_map.data.model.TurnInstruction
import com.dantariun.sat_demo_map.data.model.TurnType
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

class RouteAnalyzer {

    companion object {
        // 회전으로 감지할 최소 각도 (예: 35도)
        private const val TURN_ANGLE_THRESHOLD = 25.0
        // 유턴으로 감지할 최소 각도 (예: 160도)
        private const val U_TURN_ANGLE_THRESHOLD = 160.0
        // ✅ 1. 직진으로 판단할 최소 거리 임계값 추가 (단위: 미터)
        private const val STRAIGHT_DISTANCE_THRESHOLD_METERS = 200.0
    }

    fun analyze(routePoints: List<GeoPoint>): List<TurnInstruction> {
        if (routePoints.size < 2) return emptyList()

        val instructions = mutableListOf<TurnInstruction>()
        var distanceSinceLastTurn = 0.0

        // 1. 출발 지점 추가
        instructions.add(
            TurnInstruction(routePoints.first(), TurnType.START, 0)
        )

        // 2. 경로 중간 지점 분석 (세 점(p1, p2, p3)을 이용해 각도 계산)
        for (i in 1 until routePoints.size - 1) {
            val p1 = routePoints[i - 1]
            val p2 = routePoints[i]
            val p3 = routePoints[i + 1]

            // p1 -> p2, p2 -> p3 사이의 거리를 누적
            distanceSinceLastTurn += p2.distanceToAsDouble(p1)

            val bearing1 = p1.bearingTo(p2)
            val bearing2 = p2.bearingTo(p3)
            var turnAngle = bearing2 - bearing1

            // 각도를 -180 ~ 180 범위로 정규화
            if (turnAngle > 180) turnAngle -= 360
            if (turnAngle < -180) turnAngle += 360

            val turnType = when {
                abs(turnAngle) > U_TURN_ANGLE_THRESHOLD -> TurnType.U_TURN
                turnAngle > TURN_ANGLE_THRESHOLD -> TurnType.RIGHT
                turnAngle < -TURN_ANGLE_THRESHOLD -> TurnType.LEFT
                else -> null // 회전으로 간주하지 않음
            }

            // ✅ 2. 로직 수정: 회전을 감지하거나, 직진 거리가 임계값을 넘으면 안내 추가
            if (turnType != null) {
                // 회전이 감지된 경우
                instructions.add(
                    TurnInstruction(p2, turnType, distanceSinceLastTurn.toInt())
                )
                distanceSinceLastTurn = 0.0 // 거리 초기화
            } else if (distanceSinceLastTurn >= STRAIGHT_DISTANCE_THRESHOLD_METERS) {
                // 회전은 없지만, 직진 거리가 300m 이상인 경우
                instructions.add(
                    TurnInstruction(p2, TurnType.STRAIGHT, distanceSinceLastTurn.toInt())
                )
                distanceSinceLastTurn = 0.0 // 거리 초기화
            }
        }

        // 3. 도착 지점 추가
        val lastPoint = routePoints.last()
        distanceSinceLastTurn += lastPoint.distanceToAsDouble(routePoints[routePoints.size - 2])
        instructions.add(
            TurnInstruction(lastPoint, TurnType.FINISH, distanceSinceLastTurn.toInt())
        )

        return instructions
    }
}