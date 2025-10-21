package com.dantariun.sat_demo_map.domain.usecase

import com.dantariun.sat_demo_map.data.model.RouteDetails
import com.dantariun.sat_demo_map.util.graphHopper.GraphHopperManager
import org.osmdroid.util.GeoPoint

/**
 * 특정 지역을 회피하는 경로를 찾는 UseCase 입니다.
 *
 * @property graphHopperManager GraphHopper 엔진을 관리하는 매니저 클래스
 */
class FindRouteAvoidingAreaUseCase(private val graphHopperManager: GraphHopperManager) {

    /**
     * UseCase를 함수처럼 호출할 수 있게 해주는 operator fun 입니다.
     * @param startPoint 시작점
     * @param endPoint 도착점
     * @param avoidArea 회피할 지역의 꼭지점 리스트 (Polygon)
     * @return 경로 탐색 결과를 담은 Result 객체. 성공 시 경로 좌표 리스트, 실패 시 Exception을 포함합니다.
     */
    suspend operator fun invoke(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        avoidArea: List<GeoPoint>
    ): Result<RouteDetails> {
        // 1. 입력값 유효성 검사
        if (avoidArea.size < 3) {
            return Result.failure(IllegalArgumentException("회피 지역은 최소 3개 이상의 꼭지점으로 구성되어야 합니다."))
        }

        // 2. 경로 탐색 로직 실행 및 예외 처리
        return try {
            if (!graphHopperManager.isReady) {
                // GraphHopperManager가 준비되지 않았을 경우의 예외 처리
                throw IllegalStateException("경로 탐색기가 아직 준비되지 않았습니다.")
            }
            // GraphHopperManager의 함수를 호출하여 경로 데이터 요청
            val points = graphHopperManager.findRouteAvoidingArea(startPoint, endPoint, avoidArea)
            Result.success(points)
        } catch (e: Exception) {
            // 경로 탐색 중 발생할 수 있는 모든 예외를キャッチ하여 Result.failure로 반환
            Result.failure(e)
        }
    }
}