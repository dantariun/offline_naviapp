package com.dantariun.sat_demo_map.data.model

import com.dantariun.sat_demo_map.data.repository.RouteState

data class RouteItem(
    val id: Long, // 아이템을 고유하게 식별할 ID
    val name: String = "",
    val address: String = "",
    val distance : Double = 0.0,
    val routeState: RouteState,
    var isSelected: Boolean = false
)
