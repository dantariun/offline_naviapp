package com.dantariun.sat_demo_map.data.repository


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.dantariun.sat_demo_map.data.model.TurnInstruction
import org.osmdroid.util.GeoPoint

@Parcelize
data class RouteState(
    val route: List<GeoPoint> = emptyList(),
    val instructions: List<TurnInstruction> = emptyList(), // 실제로는 Instruction 객체 리스트가 될 수 있음
    val isLoading: Boolean = false,
    val error: String? = null,
    val autoStartAnimation: Boolean = false,
    val address: String? = null,
    val distance: Double = 0.0, // 총 거리 (미터 단위)
    val time: Long = 0L,        // 총 소요 시간 (밀리초 단위)
) : Parcelable