package com.dantariun.sat_demo_map.ui.data

import org.osmdroid.util.GeoPoint

/**
 * 음영 지역 관련 UI 상태를 나타내는 Sealed Class
 */
sealed class ShadowZoneUiState {
    // 기본 상태 (음영 지역이 아니거나, 벗어났을 때)
    object Default : ShadowZoneUiState()

    // 음영 지역에 진입하여 머무는 중인 상태
    data class InProgress(val startPoint: GeoPoint) : ShadowZoneUiState()
}