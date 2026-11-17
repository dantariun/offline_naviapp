package com.dantariun.sat_demo_map.data.model
import org.osmdroid.util.GeoPoint

data class RouteDetails(
    val points: List<GeoPoint> = emptyList(),
    val distance: Double = 0.0, // 총 거리 (미터 단위)
    val time: Long = 0L,        // 총 소요 시간 (밀리초 단위)
)
