package com.dantariun.sat_demo_map.domain.usecase

import com.dantariun.sat_demo_map.data.model.RouteDetails
import com.dantariun.sat_demo_map.util.graphHopper.GraphHopperManager
import org.osmdroid.util.GeoPoint

class FindRouteUseCase(private val graphHopperManager: GraphHopperManager) {
    suspend operator fun invoke(startPoint: GeoPoint, endPoint: GeoPoint): Result<RouteDetails> {
        return try {
            if (!graphHopperManager.isReady) {
                throw IllegalStateException("경로 탐색기가 준비되지 않았습니다.")
            }
            val points = graphHopperManager.findRoute(startPoint, endPoint)
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}