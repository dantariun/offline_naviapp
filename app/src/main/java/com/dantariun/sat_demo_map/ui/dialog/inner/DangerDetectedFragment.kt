package com.dantariun.sat_demo_map.ui.dialog.inner

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.databinding.FragmentDangerDetectedBinding
import com.dantariun.sat_demo_map.ui.viewmodel.AppDialogViewModel

class DangerDetectedFragment : Fragment(R.layout.fragment_danger_detected) {

    // parentFragment(AppDialog)의 ViewModel을 공유
    private val viewModel: AppDialogViewModel by viewModels({requireParentFragment()})
    private lateinit var binding: FragmentDangerDetectedBinding
    private var progressBarAnimator: ObjectAnimator? = null
    private var wasCancelledManually = false
    private val questionCountdownDuration = 10000L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDangerDetectedBinding.bind(view)
        setupButtons()
        startProgressBar()
    }

    private fun setupButtons() {
        binding.btnAppDialogAgree.setOnClickListener {
            viewModel.onDangerDetected()
        }

        binding.btnAppDialogDisagree.setOnClickListener {
            viewModel.notifyDismiss()
        }
    }

    private fun startProgressBar() {
        val progressBar = binding.progressBar
        progressBarAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            duration = questionCountdownDuration
            doOnEnd {
                if (isAdded && !wasCancelledManually) {
                    viewModel.onRescueConfirmed()
                }
            }
        }
        progressBarAnimator?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressBarAnimator?.cancel()
        progressBarAnimator = null
    }
}