package com.dantariun.sat_demo_map.ui.helper.scenario

import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.dantariun.sat_demo_map.data.model.ChatMessage
import com.dantariun.sat_demo_map.data.model.Sender
import com.dantariun.sat_demo_map.ui.adapter.ChatAdapter
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChatAdapter와 RecyclerView를 제어하는 헬퍼 클래스.
 *
 * @param recyclerView 메시지를 표시할 RecyclerView
 * @param adapter RecyclerView에 연결된 ChatAdapter
 */
class ChatHelper(
    private val recyclerView: RecyclerView,
    private val adapter: ChatAdapter,
    private val progressBar: ProgressBar? = null
) {
    // 메시지 고유 ID를 생성하기 위한 카운터 (실제 앱에서는 서버 ID나 UUID 사용 권장)
    private var messageIdCounter = System.currentTimeMillis()

    /**
     * 새로운 메시지를 채팅 목록에 추가하고 맨 아래로 스크롤합니다.
     *
     * @param messageText 추가할 메시지의 텍스트
     * @param sender 메시지를 보낸 주체 (ME 또는 OTHER)
     */
    fun addMessage(messageText: String, sender: Sender) {
        // 1. 새 ChatMessage 객체 생성
        val newMessage = ChatMessage(
            id = messageIdCounter++,
            message = messageText,
            timestamp = getCurrentTimestamp(),
            sender = sender
        )

        // 2. 기존 리스트를 복사하여 새 메시지를 추가한 새 리스트 생성
        // ListAdapter는 불변성을 유지하는 것이 중요하므로 항상 새 리스트를 전달해야 합니다.
        val currentList = adapter.currentList.toMutableList()
        currentList.add(newMessage)

        // 3. Adapter에 새 리스트를 제출하여 UI 갱신
        adapter.submitList(currentList) {
            // 4. 리스트 업데이트가 완료된 후, 마지막 아이템으로 스크롤
            scrollToBottom()
        }
    }

    fun showLoadingBar(){
        progressBar?.visibility = View.VISIBLE
    }

    fun hideLoadingBar(){
        progressBar?.visibility = View.GONE
    }

    /**
     * [추가] 모든 대화 내용을 삭제합니다.
     */
    fun clearAllMessages() {
        adapter.submitList(emptyList())
    }

    /**
     * 채팅 목록을 맨 아래로 스크롤합니다.
     */
    private fun scrollToBottom() {
        val lastItemPosition = adapter.itemCount - 1
        if (lastItemPosition >= 0) {
            recyclerView.smoothScrollToPosition(lastItemPosition)
        }
    }

    /**
     * 현재 시간을 "오후 2:30" 형식의 문자열로 반환합니다.
     */
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("a h:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}