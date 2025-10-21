package com.dantariun.sat_demo_map.ui.dialog

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.databinding.DialogUseSafetyCompleteBinding
import com.dantariun.sat_demo_map.databinding.DialogUseSafetyQuestionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface OnSatDialogInteractionListener {
    fun onAgreementAction()
    fun onDisagreementAction()
}

class DialogSatManager(
    private val context: Context,
    private val questionBinding: DialogUseSafetyQuestionBinding,
    private val completeBinding: DialogUseSafetyCompleteBinding,
    private val scope: CoroutineScope,
    private val listener: OnSatDialogInteractionListener
) {
    private var questionCountdownJob: Job? = null
    private val questionCountdownDuration = 10000L

    private var completionCloseJob: Job? = null
    private val completionCloseDuration = 3000L

    init {
        questionBinding.btnSatAgree.setOnClickListener {
            stopQuestionCountdown()
            showCompletionView()
        }

        questionBinding.btnSatDisagree.setOnClickListener {
            stopAllTimers()
            listener.onDisagreementAction()
        }

        questionBinding.progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rescue_red)));
    }

    fun showQuestionView() {
        stopAllTimers() // 새로 시작할 때 모든 타이머 정리
        questionBinding.root.visibility = View.VISIBLE
        completeBinding.root.visibility = View.GONE
        startQuestionCountdown()
    }

    fun showCompletionView() {
        listener.onAgreementAction()
        questionBinding.root.visibility = View.GONE
        completeBinding.root.visibility = View.VISIBLE
        // [추가] 완료 화면이 보이면, 자동으로 닫는 타이머 시작
        startCompletionCloseTimer()
    }

    private fun startQuestionCountdown() {
        questionCountdownJob = scope.launch {
            val progressBar = questionBinding.progressBar
            ObjectAnimator.ofInt(progressBar, "progress", 100, 0).apply {
                duration = questionCountdownDuration
                start()
            }
            delay(questionCountdownDuration)
            // 시간 초과 시 '동의'와 동일하게 완료 화면 보여주기
            showCompletionView()
        }
    }

    // [추가] 완료 화면을 5초 뒤에 자동으로 닫는 타이머
    private fun startCompletionCloseTimer() {
        completionCloseJob = scope.launch {
            val progressBar = completeBinding.progressBar
            ObjectAnimator.ofInt(progressBar, "progress", 100, 0).apply {
                duration = completionCloseDuration
                start()
            }
            delay(completionCloseDuration)
            // 5초가 지나면 동의 액션을 호출하여 다이얼로그를 닫도록 함
            listener.onDisagreementAction()
        }
    }

    private fun stopQuestionCountdown() {
        questionCountdownJob?.cancel()
        questionCountdownJob = null
        questionBinding.progressBar.progress = 100
    }

    // [추가] 모든 타이머를 한 번에 중지하는 함수
    private fun stopAllTimers() {
        questionCountdownJob?.cancel()
        questionCountdownJob = null
        completionCloseJob?.cancel()
        completionCloseJob = null
        questionBinding.progressBar.progress = 100
    }

    // [추가] Fragment의 onDestroyView에서 호출할 정리 함수
    fun cleanup() {
        stopAllTimers() // [수정] 모든 타이머를 중지하도록 변경
        questionBinding.root.visibility = View.GONE
        completeBinding.root.visibility = View.GONE
    }
}