package com.dantariun.sat_demo_map.ui.dialog

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import androidx.fragment.app.DialogFragment
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.databinding.DialogDangerDetectedBinding
import androidx.core.graphics.drawable.toDrawable

enum class DialogMode {
    ARRIVAL, // 목적지 도착 (기존 WITHOUT_MAP)
    RESCUE,   // 구조 요청
    DANGER_DETECTED, // 위험 감지
}

class AppDialog : DialogFragment() {

    private var _binding: DialogDangerDetectedBinding? = null
    private val binding get() = _binding!!

    private var progressBarAnimator: ObjectAnimator? = null
    private var wasCancelledManually = false

    private val questionCountdownDuration = 5000L

    interface OnDialogButtonClickListener {
        fun onAgreeClicked()
        fun onDisagreeClicked()
    }

    private var buttonClickListener: OnDialogButtonClickListener? = null

    fun setDialogButtonClickListener(listener: OnDialogButtonClickListener) {
        this.buttonClickListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // [변경] 새로운 레이아웃(DialogDangerDetectedBinding)으로 inflate
        _binding = DialogDangerDetectedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // arguments에서 mode를 가져와 분기 처리
        val mode = arguments?.getSerializable(ARG_MODE) as? DialogMode ?: DialogMode.ARRIVAL

        // [변경] 모드에 따라 UI 설정 함수를 호출하는 방식으로 구조 개선
        when (mode) {
            DialogMode.ARRIVAL -> {}
            DialogMode.RESCUE -> {}
            DialogMode.DANGER_DETECTED -> setupUiForRescue()
        }

        setupButtons()
        startProgressBar()
    }

    // [추가] 구조 요청 모드 UI 설정 함수
    private fun setupUiForRescue() {
//        binding.txtDialogTitle.text = "충돌이 감지되었습니다."
//        binding.txtDialogDescription.text = "운전자 상태 확인 중...\n괜찮다면 화면을 탭하세요."
//        binding.tvSubMessage.text = "( 일정 기간 동안 미응답 시 자동으로 도움 요청 )"
//        binding.tvSubMessage.visibility = View.VISIBLE
//
//        binding.btnAgree.text = "구조요청"
//        binding.btnDisagree.text = "취소"
//
//        binding.btnAgree.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
    }


    private fun setupButtons() {
        binding.btnAppDialogAgree.setOnClickListener {
            performAgreeAction()
        }

        binding.btnAppDialogDisagree.setOnClickListener {
            performDisagreeAction()
        }
    }

    private fun performAgreeAction() {
        if (buttonClickListener == null) return
        wasCancelledManually = true
        progressBarAnimator?.cancel()
        buttonClickListener?.onAgreeClicked()
        buttonClickListener = null
        dismiss()
    }

    private fun performDisagreeAction() {
        if (buttonClickListener == null) return
        wasCancelledManually = true
        progressBarAnimator?.cancel()
        buttonClickListener?.onDisagreeClicked()
        buttonClickListener = null
        dismiss()
    }

    private fun startProgressBar() {
        val progressBar = binding.progressBar
        progressBarAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            duration = questionCountdownDuration
            doOnEnd {
                if (isAdded && !wasCancelledManually) {
                    performAgreeAction()
                }
            }
        }
        progressBarAnimator?.start()
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return

        window.setBackgroundDrawable(resources.getColor(R.color.danger_dialog_bg).toDrawable())
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressBarAnimator?.cancel()
        progressBarAnimator = null
        _binding = null
    }

    companion object {
        // [삭제] 지도 관련 상수 제거
        private const val ARG_MODE = "dialog_mode"

        fun newInstance(mode: DialogMode): AppDialog { // [변경] 반환 타입을 AppDialog로 변경
            val args = Bundle().apply {
                putSerializable(ARG_MODE, mode)
            }
            val fragment = AppDialog() // [변경] 생성자를 AppDialog로 변경
            fragment.arguments = args
            return fragment
        }
    }
}