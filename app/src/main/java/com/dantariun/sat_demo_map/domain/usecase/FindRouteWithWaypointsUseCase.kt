package com.dantariun.sat_demo_map.domain.usecase

import com.dantariun.sat_demo_map.data.model.RouteDetails
import com.dantariun.sat_demo_map.util.graphHopper.GraphHopperManager
import org.osmdroid.util.GeoPoint

class FindRouteWithWaypointsUseCase(private val graphHopperManager: GraphHopperManager) {
    suspend operator fun invoke(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        waypoints: List<GeoPoint>
    ): Result<RouteDetails> {
        if (waypoints.isEmpty()){
            return Result.failure(IllegalArgumentException("경유지가 비어있습니다. FindRouteUseCase를 사용하세요."))
        }

        return try {
            if (!graphHopperManager.isReady) {
                throw IllegalStateException("경로 탐색기가 준비되지 않았습니다.")
            }
            val points = graphHopperManager.findRouteWithWaypoints(startPoint, endPoint, waypoints)
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}