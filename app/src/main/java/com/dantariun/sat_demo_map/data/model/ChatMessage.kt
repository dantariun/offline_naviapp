package com.dantariun.sat_demo_map.data.model

data class ChatMessage(
    val id: Long, // 메시지 고유 ID (DiffUtil에서 사용)
    val message: String, // 메시지 내용
    val timestamp: String, // 보낸 시간
    val sender: Sender // 보낸 사람 (ME 또는 OTHER)
)
