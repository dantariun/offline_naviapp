package com.dantariun.sat_demo_map.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.data.model.Screens
import com.dantariun.sat_demo_map.databinding.FragmentSettingsBinding // 달라진 점: ActivitySettingsBinding -> FragmentSettingsBinding
import com.dantariun.sat_demo_map.ui.adapter.SettingsAdapter
import com.dantariun.sat_demo_map.ui.adapter.VerticalSpaceItemDecoration
import com.dantariun.sat_demo_map.ui.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() { // 달라진 점: AppCompatActivity -> Fragment

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var settingsAdapter: SettingsAdapter

    // 달라진 점: onCreateView, onViewCreated 생명주기 사용
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 달라진 점: 뒤로가기 버튼 클릭 시 NavController 사용
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter(mutableListOf()) { key, isEnabled ->
            mapViewModel.onSettingChanged(key, isEnabled)
        }
        binding.rvSettings.apply {
            adapter = settingsAdapter
            // 달라진 점: Context를 this@SettingsActivity -> requireContext()로 변경
            layoutManager = LinearLayoutManager(requireContext())
            val spacingInPixels = resources.getDimensionPixelSize(R.dimen.recycler_item_spacing)

            addItemDecoration(VerticalSpaceItemDecoration(spacingInPixels))
        }
    }

    private fun observeViewModel() {
        // 달라진 점: lifecycleScope -> viewLifecycleOwner.lifecycleScope
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel.settingsList.collect { settings ->
                    if (settings.isNotEmpty()) {
                        settingsAdapter.updateList(settings)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapViewModel.setCurrentFragmentTitle(Screens.SETTING.toString())
    }

    // 달라진 점: ViewBinding 객체 메모리 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}