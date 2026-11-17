package com.dantariun.sat_demo_map.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.osmdroid.util.GeoPoint

// 회전 종류를 나타내는 열거형 클래스
enum class TurnType {
    START,          // 출발
    STRAIGHT,       // 직진
    LEFT,           // 좌회전
    RIGHT,          // 우회전
    U_TURN,         // 유턴
    FINISH          // 도착
}

// 하나의 회전 안내 정보를 담는 데이터 클래스

@Parcelize
data class TurnInstruction(
    val turnPoint: GeoPoint,            // 회전이 발생하는 지점의 GeoPoint
    val turnType: TurnType,             // 회전 종류
    val distanceToNextTurnMeters: Int,   // *이전* 회전 지점부터 현재 회전 지점까지의 거리
    val turnPointAddress:String? = null
) : Parcelable