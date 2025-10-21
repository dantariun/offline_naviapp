package com.dantariun.sat_demo_map.ui.adapter

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.ui.data.SettingItem

// [수정] 스위치 변경 이벤트를 Activity/Fragment로 전달하기 위한 람다 추가
class SettingsAdapter(
    private val items: MutableList<SettingItem>,
    private val onSwitchChanged: (key: String, isEnabled: Boolean) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    inner class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerLayout: ConstraintLayout = itemView.findViewById(R.id.header_layout)
        private val expandableLayout: LinearLayout = itemView.findViewById(R.id.expandable_layout)
        private val title: TextView = itemView.findViewById(R.id.text_title)
        private val content: TextView = itemView.findViewById(R.id.content_text_view)
        private val chevronIcon: ImageView = itemView.findViewById(R.id.iv_chevron)
        private val customSwitch: SwitchCompat = itemView.findViewById(R.id.customSwitch)

        fun bind(settingItem: SettingItem) {
            title.text = settingItem.title
            content.text = settingItem.content

            // 확장/축소 상태 UI 업데이트
            expandableLayout.visibility = if (settingItem.isExpanded) View.VISIBLE else View.GONE
            chevronIcon.rotation = if (settingItem.isExpanded) 180f else 0f

            // [수정] 스위치 상태 UI 업데이트
            // 리스너를 null로 설정 후 값을 변경해야 onBind 과정에서 이벤트가 불필요하게 호출되는 것을 막을 수 있음
            customSwitch.setOnCheckedChangeListener(null)
            customSwitch.isChecked = settingItem.isEnabled
            customSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged(settingItem.key, isChecked)
            }

            // 확장/축소 클릭 리스너
            headerLayout.setOnClickListener {
                settingItem.isExpanded = !settingItem.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.expandable_item_layout, parent, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // [수정] Activity에서 새로운 리스트를 받았을 때 업데이트하는 함수
    fun updateList(newItems: List<SettingItem>) {
        // isExpanded 상태는 UI에서만 관리되므로 이전 상태를 유지
        newItems.forEach { newItem ->
            items.find { it.key == newItem.key }?.let { oldItem ->
                newItem.isExpanded = oldItem.isExpanded
            }
        }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        // 마지막 아이템이 아닌 경우에만 아래쪽 여백을 추가합니다.
        if (parent.getChildAdapterPosition(view) != parent.adapter!!.itemCount - 1) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}