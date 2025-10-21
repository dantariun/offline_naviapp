package com.dantariun.sat_demo_map.ui.dialog

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.databinding.DialogChangeRouteBinding
import com.dantariun.sat_demo_map.domain.ZoneType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface OnRouteDialogInteractionListener {
    fun onAgreementAction()
    fun onDisagreementAction()
}

class DialogChangeRoute(
private val context: Context,
private val binding: DialogChangeRouteBinding,
private val scope: CoroutineScope,
private val listener: OnRouteDialogInteractionListener
) {
    private var questionCountdownJob: Job? = null
    private val questionCountdownDuration = 10000L

    init {
        binding.btnChangeRoute.setOnClickListener {
            listener.onAgreementAction()
            questionCountdownJob?.cancel()
            questionCountdownJob = null
        }

        binding.btnStay.setOnClickListener {
            stopQuestionCountdown()
            listener.onDisagreementAction()
        }

        binding.progressBar.setProgressTintList(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.rescue_red
                )
            )
        )
    }

    fun showChangeRoute(type:ZoneType){
        when(type){
            ZoneType.FIRE ->{
                binding.imageWarningType.setImageResource(R.drawable.icon_fire)
                binding.txtWarningDescription.text = context.getString(R.string.warning_fire)
            }
            ZoneType.FLOOD -> {
                binding.imageWarningType.setImageResource(R.drawable.icon_disaster)
                binding.txtWarningDescription.text = context.getString(R.string.warning_fire)
            }
            ZoneType.ACCIDENT -> {
                binding.imageWarningType.setImageResource(R.drawable.icon_road_construction)
                binding.txtWarningDescription.text = context.getString(R.string.warning_fire)
            }
            ZoneType.ROAD_CONSTRUCTION -> {
                binding.imageWarningType.setImageResource(R.drawable.icon_road_construction)
                binding.txtWarningDescription.text = context.getString(R.string.warning_road_construction)
            }
            ZoneType.DISASTER -> {
                binding.imageWarningType.setImageResource(R.drawable.icon_disaster)
                binding.txtWarningDescription.text = context.getString(R.string.warning_disaster)
            }
        }
        binding.root.visibility = View.VISIBLE
        startQuestionCountdown()
    }

    private fun startQuestionCountdown() {
        questionCountdownJob = scope.launch {
            val progressBar = binding.progressBar
            ObjectAnimator.ofInt(progressBar, "progress", 100, 0).apply {
                duration = questionCountdownDuration
                start()
            }
            delay(questionCountdownDuration)
            // 시간 초과 시 '동의'와 동일하게 완료 화면 보여주기

            listener.onAgreementAction()
            questionCountdownJob?.cancel()
            questionCountdownJob = null
        }
    }

    private fun stopQuestionCountdown() {
        questionCountdownJob?.cancel()
        questionCountdownJob = null
        binding.progressBar.progress = 100
    }

    // [추가] Fragment의 onDestroyView에서 호출할 정리 함수
    fun cleanup() {
        binding.root.visibility = View.GONE
        stopQuestionCountdown()
    }
}