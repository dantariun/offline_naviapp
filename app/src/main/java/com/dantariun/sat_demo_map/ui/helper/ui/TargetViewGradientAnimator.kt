package com.dantariun.sat_demo_map.ui.helper.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView

/**
 * 특정 ImageView를 대상으로 그라데이션 알파 애니메이션을 제어하는 클래스.
 *
 * @param targetView 애니메이션을 적용할 ImageView
 */
class TargetViewGradientAnimator(private val targetView: ImageView) {

    private var fadeAnimator: ValueAnimator? = null

    init {
        // 제어를 시작하기 전에 View를 숨김 상태로 설정
        targetView.visibility = View.GONE
        targetView.alpha = 0f

        // 반복되는 알파 애니메이션 생성
        setupFadeAnimator()
    }

    private fun setupFadeAnimator() {
        fadeAnimator = ValueAnimator.ofFloat(0.3f, 0.8f).apply {
            duration = 1500L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                // 애니메이션 값에 따라 타겟 View의 알파(투명도)를 변경
                targetView.alpha = animation.animatedValue as Float
            }
        }
    }

    /**
     * 타겟 View를 화면에 부드럽게 표시하고 애니메이션을 시작합니다.
     */
    fun show() {
        // 이미 보이거나 애니메이션 중이라면 중복 실행 방지
        if (targetView.visibility == View.VISIBLE) return

        // 1. View를 보이도록 설정
        targetView.visibility = View.VISIBLE

        // 2. 부드럽게 나타나는 애니메이션 (Fade In)
        targetView.animate()
            .alpha(0.3f) // 애니메이션 시작 알파 값까지 밝아짐
            .setDuration(400)
            .withEndAction {
                // 3. Fade In이 끝나면, 반복되는 애니메이션 시작
                fadeAnimator?.start()
            }
            .start()
    }

    /**
     * 타겟 View를 화면에서 부드럽게 제거하고 애니메이션을 중지합니다.
     */
    fun hide() {
        // 이미 사라진 상태라면 중복 실행 방지
        if (targetView.visibility == View.GONE) return

        // 1. 부드럽게 사라지는 애니메이션 (Fade Out)
        targetView.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                // 2. 반복 애니메이션 중지 (메모리 누수 방지)
                fadeAnimator?.cancel()
                // 3. 애니메이션이 끝나면 View를 완전히 숨김
                targetView.visibility = View.GONE
            }
            .start()
    }
}