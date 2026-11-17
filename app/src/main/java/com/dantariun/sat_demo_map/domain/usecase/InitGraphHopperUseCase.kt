package com.dantariun.sat_demo_map.domain.usecase

import com.dantariun.sat_demo_map.util.graphHopper.GraphHopperManager

class InitGraphHopperUseCase(private val graphHopperManager: GraphHopperManager) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            graphHopperManager.init()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}