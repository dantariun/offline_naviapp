package com.dantariun.sat_demo_map.ui.dialog

sealed class AppDialogResult {
    // 구조 요청에 동의했을 때
    object RescueConfirmed : AppDialogResult()

    // 최종적으로 다이얼로그를 닫았을 때 (예: 도착 확인)
    object DangerDetected : AppDialogResult()

    // 취소 이유를 선택하고 다이얼로그를 닫았을 때
    data class RescueCancelled(val reason: String) : AppDialogResult()

    // 아무 행동 없이 그냥 닫았을 때 (예: 백버튼)
    object Dismissed : AppDialogResult()
}