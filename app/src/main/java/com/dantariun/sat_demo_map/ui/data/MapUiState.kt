package com.dantariun.sat_demo_map.ui.data

import com.dantariun.sat_demo_map.data.model.Screens
import com.dantariun.sat_demo_map.data.model.TurnInstruction
import org.osmdroid.util.GeoPoint

// 지도 UI 상태를 나타내는 데이터 클래스
data class MapUiState(
    val isLoading: Boolean = false,
    val route: List<GeoPoint>? = null,
    val instructions: List<TurnInstruction>? = null,
    val autoStartAnimation: Boolean = false,
    val currentScreen:Screens = Screens.MAP,
    val address:String? = null
)