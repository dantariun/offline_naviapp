package com.dantariun.sat_demo_map.data.model

enum class Screens {
    MAP,
    SEARCH,
    SETTING
}

enum class LayoutState {
    FULL_MAP,
    MAP_WITH_CHAT,
    MAP_WITH_DIALOG
}

enum class DialogTypes {
    SAT, // 위성통신
    SAT_COMPLETE,
    CHANGE_ROUTE, //재해 알림
}