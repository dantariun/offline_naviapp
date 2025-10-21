package com.dantariun.sat_demo_map.ui.helper

import com.dantariun.sat_demo_map.data.model.MajorTurnInstruction
import com.dantariun.sat_demo_map.data.model.TurnInstruction
import com.dantariun.sat_demo_map.data.model.TurnType

class NavigationGuideGenerator {

    /**
     * 상세한 TurnInstruction 목록을 사용자가 보기 편한 주요 경로 안내 목록으로 변환합니다.
     * 연속된 '직진' 안내는 하나의 긴 직진 안내로 통합됩니다.
     *
     * @param detailedInstructions RouteAnalyzer에 의해 생성된 TurnInstruction 목록
     * @return UI에 표시하기 좋은 MajorTurnInstruction 목록
     */
    fun create(detailedInstructions: List<TurnInstruction>): List<MajorTurnInstruction> {
        // 경로 데이터가 너무 짧으면 빈 목록 반환
        if (detailedInstructions.size <= 1) return emptyList()

        val majorInstructions = mutableListOf<MajorTurnInstruction>()

        // START, FINISH를 제외한 실제 경로 안내(1번 인덱스부터)를 순회합니다.
        for (i in 1 until detailedInstructions.size) {
            val currentInstruction = detailedInstructions[i]
            val distanceForStep = currentInstruction.distanceToNextTurnMeters

            if (distanceForStep <= 0) continue // 거리가 0인 안내는 무시

            if (currentInstruction.turnType == TurnType.STRAIGHT) {
                // ✅ 연속된 직진(STRAIGHT) 합치기 로직
                // 주요 안내 목록의 마지막 항목이 이미 '직진'이라면 거리를 합칩니다.
                if (majorInstructions.isNotEmpty() && majorInstructions.last().turnType == TurnType.STRAIGHT) {
                    val lastMajorInstruction = majorInstructions.last()
                    val newTotalDistance = lastMajorInstruction.totalDistance + distanceForStep

                    // 기존 마지막 항목을 새로운 거리 정보로 업데이트하여 교체합니다.
                    majorInstructions[majorInstructions.size - 1] = lastMajorInstruction.copy(
                        totalDistance = newTotalDistance,
                        description = formatDistanceDescription("동안 직진", newTotalDistance)
                    )
                } else {
                    // 새로운 '직진' 안내를 추가합니다.
                    majorInstructions.add(
                        MajorTurnInstruction(
                            turnType = TurnType.STRAIGHT,
                            totalDistance = distanceForStep,
                            description = formatDistanceDescription("동안 직진", distanceForStep)
                        )
                    )
                }
            } else {
                // ✅ 좌/우회전, 유턴, 도착 등 다른 타입의 안내 처리
                val turnActionText = getTurnActionText(currentInstruction.turnType)
                majorInstructions.add(
                    MajorTurnInstruction(
                        turnType = currentInstruction.turnType,
                        totalDistance = distanceForStep,
                        description = formatDistanceDescription("후 $turnActionText", distanceForStep)
                    )
                )
            }
        }
        return majorInstructions
    }

    /**
     * 거리를 km 또는 m 단위로 변환하고, 안내 문구와 결합하여 최종 설명 텍스트를 만듭니다.
     * @param actionText "동안 직진", "후 좌회전" 과 같은 동작 설명
     * @param distance 미터 단위의 거리
     */
    private fun formatDistanceDescription(actionText: String, distance: Int): String {
        return if (distance >= 1000) {
            val km = distance / 1000.0
            // 소수점 첫째 자리가 0이면 정수로 표현 (예: 2.0km -> 2km)
            val formattedKm = if (km % 1 == 0.0) km.toInt().toString() else String.format("%.1f", km)
            "${formattedKm}km $actionText"
        } else {
            "${distance}m $actionText"
        }
    }

    /**
     * TurnType에 맞는 기본 안내 문구를 반환합니다.
     */
    private fun getTurnActionText(turnType: TurnType): String {
        return when (turnType) {
            TurnType.LEFT -> "좌회전"
            TurnType.RIGHT -> "우회전"
            TurnType.U_TURN -> "유턴"
            TurnType.FINISH -> "목적지 도착"
            else -> "" // STRAIGHT, START 등은 여기서 처리되지 않음
        }
    }
}