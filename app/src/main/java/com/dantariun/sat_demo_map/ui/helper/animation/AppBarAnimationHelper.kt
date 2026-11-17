package com.dantariun.sat_demo_map.ui.helper.animation

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.dantariun.sat_demo_map.R

/**
 * HomeActivity의 AppBar 레이아웃 상태를 정의합니다.
 */
enum class AppBarState {
    FULL_WIDTH, // AppBar가 전체 너비를 차지하는 기본 상태
    SHRUNK      // AppBar가 축소되어 우측에 정렬된 상태
}

/**
 * HomeActivity의 AppBar 애니메이션을 관리하는 헬퍼 클래스.
 * MapFragment의 사이드 패널 등장에 맞춰 AppBar의 너비와 위치를 동적으로 변경합니다.
 *
 * @param rootLayout 애니메이션을 적용할 부모 ConstraintLayout (예: HomeActivity의 main 레이아웃)
 */
class AppBarAnimationHelper(private val rootLayout: ConstraintLayout) {

    // 각 레이아웃 상태에 대한 ConstraintSet을 저장할 맵
    private val constraintSets = mutableMapOf<AppBarState, ConstraintSet>()
    private var isInitialized = false

    /**
     * 애니메이션에 필요한 모든 ConstraintSet들을 초기화합니다.
     * 이 메서드는 HomeActivity의 onCreate 등에서 한 번만 호출되어야 합니다.
     */
    fun initialize() {
        if (isInitialized) return

        // 상태 1: AppBar가 전체 너비인 상태 (기본값)
        constraintSets[AppBarState.FULL_WIDTH] = ConstraintSet().apply {
            clone(rootLayout)
            // XML에 정의된 기본 제약 조건이므로 추가 설정은 필요 없음
            // 명확성을 위해 제약 조건을 다시 설정할 수도 있음
            connect(R.id.app_bar_layout, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(R.id.app_bar_layout, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constrainPercentWidth(R.id.app_bar_layout, 1.0f)
        }

        // 상태 2: AppBar가 축소되어 우측에 정렬된 상태
        constraintSets[AppBarState.SHRUNK] = ConstraintSet().apply {
            clone(rootLayout)
            // 1. 너비를 부모의 60%로 설정
            constrainPercentWidth(R.id.app_bar_layout, 0.6f)

            // 2. 왼쪽 제약(Start)을 해제하여 우측 정렬이 가능하게 함
            clear(R.id.app_bar_layout, ConstraintSet.START)

            // 3. 오른쪽 제약(End)은 부모의 오른쪽에 유지
            connect(R.id.app_bar_layout, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }

        isInitialized = true
    }

    /**
     * 지정된 AppBar 상태로 화면을 전환합니다.
     * @param state 전환할 목표 레이아웃 상태 (AppBarState)
     */
    fun transitionTo(state: AppBarState) {
        if (!isInitialized) return

        val targetConstraintSet = constraintSets[state] ?: return

        targetConstraintSet.applyTo(rootLayout)
    }
}