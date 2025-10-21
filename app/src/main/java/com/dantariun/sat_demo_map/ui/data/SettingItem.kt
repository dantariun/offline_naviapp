package com.dantariun.sat_demo_map.ui.data

data class SettingItem(
    val key: String, // DataStore에 저장될 고유 키
    val title: String,
    val content: String,
    val isEnabled: Boolean, // 스위치의 현재 값
    var isExpanded: Boolean = false // UI 확장 상태
)
