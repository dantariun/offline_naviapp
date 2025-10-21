package com.dantariun.sat_demo_map.ui.dialog.inner

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.databinding.FragmentRescueRequestBinding
import com.dantariun.sat_demo_map.ui.viewmodel.AppDialogViewModel

class CancelledRescueFragment : Fragment(R.layout.fragment_rescue_request) {

    // parentFragment(AppDialog)의 ViewModel을 공유
    private val viewModel: AppDialogViewModel by viewModels({requireParentFragment()})
    private lateinit var binding: FragmentRescueRequestBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRescueRequestBinding.bind(view)

//        binding.btnAgree.setOnClickListener {
//            // 로직 처리를 직접 하지 않고, ViewModel에 이벤트를 전달
//            // viewModel.onRescueConfirmed()
//        }
//
//        binding.btnDisagree.setOnClickListener {
//            // ViewModel에 '취소' 이벤트 전달
//            viewModel.onRescueCancelled()
//        }
    }
}