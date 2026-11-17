package com.dantariun.sat_demo_map.data.model

/**
 * UI에 표시될 주요 경로 안내 정보를 담는 데이터 클래스
 *
 * @param turnType 안내 아이콘 표시를 위한 TurnType (예: 좌회전, 직진)
 * @param totalDistance 이 안내 구간의 총 거리 (미터 단위)
 * @param description UI에 표시될 최종 텍스트 (예: "3.2km 동안 직진", "300m 후 좌회전")
 */
data class MajorTurnInstruction(
    val turnType: TurnType,
    val totalDistance: Int,
    val description: String
)