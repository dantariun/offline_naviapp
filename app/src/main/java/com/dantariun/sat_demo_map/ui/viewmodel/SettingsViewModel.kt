package com.dantariun.sat_demo_map.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantariun.sat_demo_map.data.SettingsManager
import com.dantariun.sat_demo_map.ui.data.SettingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val settingsList: StateFlow<List<SettingItem>> = settingsManager.settingsFlow
        .map { preferences ->
            createSettingItems(preferences)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSettingChanged(key: String, isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setBoolean(key, isEnabled) // SettingsManager에 키와 값을 전달
        }
    }

    private fun createSettingItems(preferences: androidx.datastore.preferences.core.Preferences): List<SettingItem> {
        return listOf(
            SettingItem(
                key = "private_location_agree",
                title = "개인정보 위치동의",
                content = "위치 기반 서비스 사용을 위해 사용자의 현재 위치를 수집 및 이용합니다.",
                isEnabled = preferences[SettingsManager.PRIVATE_LOCATION_KEY] ?: false
            ),
            SettingItem(
                key = "satellite_communication_agree",
                title = "위성통신 정보동의",
                content = "",
                isEnabled = preferences[SettingsManager.SATELLITE_COMMUNICATION_KEY] ?: true
            ) ,
            SettingItem(
                key = "generative_ai_information_agree",
                title = "생성형 AI 정보동의",
                content = "",
                isEnabled = preferences[SettingsManager.GENERATIVE_AI_INFORMATION_KEY] ?: true
            ),
            SettingItem(
                key = "route_detour_service_agree",
                title = "경로 우회 서비스 이용동의",
                content = "",
                isEnabled = preferences[SettingsManager.ROUTE_DETOUR_SERVICE_KEY] ?: true
            ),
            SettingItem(
                key = "emergency_analysis_agree",
                title = "위성 이미지기반 긴급 상황 분석동의",
                content = "",
                isEnabled = preferences[SettingsManager.EMERGENCY_ANALYSIS_KEY] ?: true
            )
        )
    }
}