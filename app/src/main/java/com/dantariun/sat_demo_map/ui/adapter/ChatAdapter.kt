package com.dantariun.sat_demo_map.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.data.model.ChatMessage
import com.dantariun.sat_demo_map.data.model.Sender

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    // 뷰 타입을 구분하기 위한 상수
    companion object {
        private const val VIEW_TYPE_ME = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    // 1. ViewHolder 정의 (2종류)
    // 내가 보낸 메시지 뷰홀더
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            timestampText.text = chatMessage.timestamp
        }
    }

    // 상대방이 보낸 메시지 뷰홀더
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            timestampText.text = chatMessage.timestamp
        }
    }

    inner class SystemMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            timestampText.text = chatMessage.timestamp
            messageText.setTextColor(Color.YELLOW)
        }
    }

    // 2. getItemViewType(): 아이템의 송신자에 따라 뷰 타입을 반환
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when (message.sender) {
            Sender.ME -> VIEW_TYPE_ME
            Sender.OTHER -> VIEW_TYPE_OTHER
            Sender.SYSTEM -> VIEW_TYPE_SYSTEM
        }
    }

    // 3. onCreateViewHolder(): 뷰 타입에 따라 다른 레이아웃과 뷰홀더를 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ME -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_me, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_me, parent, false)
                SystemMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_other, parent, false)
                ReceivedMessageViewHolder(view)
            }
        }
    }

    // 4. onBindViewHolder(): 생성된 뷰홀더에 데이터를 바인딩
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SystemMessageViewHolder -> holder.bind(message)
        }
    }
}

// DiffUtil: 리스트 업데이트 시 성능 향상을 위해 사용
class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}