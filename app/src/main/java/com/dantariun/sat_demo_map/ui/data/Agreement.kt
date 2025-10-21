package com.dantariun.sat_demo_map.ui.data

data class Agreement(
    val title: String,
    val content: String,
    var isExpanded: Boolean = false // 확장 상태를 저장
)