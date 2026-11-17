package com.dantariun.sat_demo_map.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.data.model.Screens
import com.dantariun.sat_demo_map.data.model.UserStatus
import com.dantariun.sat_demo_map.databinding.ActivityHomeBinding
import com.dantariun.sat_demo_map.ui.data.ShadowZoneUiState
import com.dantariun.sat_demo_map.ui.helper.animation.AppBarAnimationHelper
import com.dantariun.sat_demo_map.ui.helper.animation.LayoutState
import com.dantariun.sat_demo_map.ui.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.dantariun.sat_demo_map.ui.helper.animation.AppBarState
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity: AppCompatActivity(){

    private lateinit var binding: ActivityHomeBinding
    private val mapViewModel: MapViewModel by viewModels()

    private lateinit var appBarAnimationHelper: AppBarAnimationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appBarAnimationHelper = AppBarAnimationHelper(binding.main)
        appBarAnimationHelper.initialize()

        setupWindowInsets()
        setupClickListeners() // [추가] 클릭 리스너 설정 함수 호출
        observeViewModel()    // [추가] ViewModel 관찰 함수 호출
    }

    // [추가] 상단 바의 클릭 리스너들을 MainActivity에서 설정
    private fun setupClickListeners() {
        binding.appBarDisplay.btnRouteStop.setOnClickListener {
            mapViewModel.stopNavigation()
        }
        binding.appBarDisplay.btnNetworkState.setOnClickListener {
            mapViewModel.toggleSatComMode()
        }
        binding.appBarDisplay.btnMic.setOnClickListener {
            mapViewModel.toggleChatMode()
        }
        binding.appBarDisplay.btnEmergencyTest.setOnClickListener {
            mapViewModel.triggerEmergencyAlert()
        }

        binding.appBarDisplay.btnRefresh.setOnClickListener {
            mapViewModel.triggerMapReset()
        }

        binding.appBarDisplay.btnBarMenu.setOnClickListener {
            mapViewModel.toggleHiddenView()
        }

        binding.btnStatusData.setOnClickListener {
            mapViewModel.toggleDataStatus()
        }

        binding.viewUserState.setOnClickListener {
            mapViewModel.cycleUserStatus()
        }
    }

    // [추가] MainActivity에서 상단 바 UI와 관련된 LiveData/Flow를 관찰
    private fun observeViewModel() {
        // 현재 시간 관찰
        mapViewModel.currentTime.observe(this) { timeString ->
            binding.appBarDisplay.textTime.text = timeString
        }

        // 위성 통신 모드 관찰
        mapViewModel.isSatComMode.observe(this) {
            updateSatComRelatedUi()
        }

        mapViewModel.totalRemainingDistance.observe(this) { distanceText ->
            binding.appBarDisplay.textDistanceKm.text = distanceText
        }

        // 경로 안내 상태에 따른 UI 변경 (예시)
        lifecycleScope.launch {
            mapViewModel.isNavigating.collect { isNavigating ->
                val visibility = if (isNavigating) View.VISIBLE else View.GONE
//                binding.appBarDisplay.btnDisasterTest.visibility = visibility
//                binding.appBarDisplay.btnEmergencyTest.visibility = visibility
                binding.appBarDisplay.navigationMenu.visibility = visibility
            }
        }

        mapViewModel.uiState.observe(this) { state ->
            if (!state.route.isNullOrEmpty() && !state.instructions.isNullOrEmpty()) {
                if( mapViewModel.isNavigating.value){
                    binding.appBarDisplay.textDestination.text = state.address
                }else{
                    binding.appBarDisplay.textDestination.text = ""
                }
            }
        }

        mapViewModel.currentFragmentTitle.observe(this) { state ->
            when(state) {
                Screens.SEARCH.toString()->{
                    binding.appBarDisplay.root.visibility = View.GONE
                    binding.appBarDisplay.root.visibility = View.GONE
                    binding.statusView.setBackgroundResource(android.R.color.transparent)
                }
                Screens.MAP.toString() -> {
                    binding.appBarDisplay.root.visibility = View.VISIBLE
                    binding.appBarDisplay.root.visibility = View.VISIBLE
                    binding.statusView.setBackgroundResource(R.drawable.bg_status_white)
                }
                Screens.SETTING.toString() -> {
                    binding.appBarDisplay.root.visibility = View.GONE
                    binding.appBarDisplay.root.visibility = View.GONE
                    binding.statusView.setBackgroundResource(android.R.color.transparent)
                }
            }
        }

        lifecycleScope.launch {
            mapViewModel.layoutState.collect { state ->
                val appBarState = when (state) {
                    LayoutState.FULL_MAP -> AppBarState.FULL_WIDTH
                    LayoutState.MAP_WITH_CHAT, LayoutState.MAP_WITH_DIALOG, LayoutState.MAP_WITH_ROUTE -> AppBarState.SHRUNK
                }
                appBarAnimationHelper.transitionTo(appBarState)
            }
        }

        mapViewModel.userStatus.observe(this){ status ->
            binding.textStatusUser.text = status.name
            when(status){
                UserStatus.GOOD -> {
                    binding.imgStatusUser.setImageResource(R.drawable.icon_good)
                }
//                UserStatus.NORMAL -> {
//                    binding.imgStatusUser.setImageResource(R.drawable.icon_usually)
//                }
                UserStatus.BAD -> {
                    binding.imgStatusUser.setImageResource(R.drawable.icon_bad)
                }
            }
        }
    }

    private fun updateSatComRelatedUi() {
        val isNetworkState = mapViewModel.shadowZoneState.value ?: ShadowZoneUiState.Default  // 네트워크 상태

        val isSatComSettingEnabled = mapViewModel.settingsList.value
            .find { it.key == "satellite_communication_agree" }?.isEnabled ?: false // 위성통신 사용 권한

        when (isNetworkState) {
            is ShadowZoneUiState.InProgress -> {  // 데이터 상태 악화
                binding.btnStatusData.setImageResource(R.drawable.data_lvl_0)
                if (isSatComSettingEnabled) { //
                    binding.btnStatusData.setImageResource(R.drawable.icon_network_satellite)
                }
            }
            is ShadowZoneUiState.Default -> {
                binding.btnStatusData.setImageResource(R.drawable.data_lvl_4)
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }
}