package com.dantariun.sat_demo_map.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dantariun.sat_demo_map.data.model.RouteItem
import com.dantariun.sat_demo_map.databinding.ItemRouteBinding
import com.dantariun.sat_demo_map.util.common.CommonUtil
import java.text.DecimalFormat
import kotlin.math.roundToInt

class RouteAdapter(
    private val onItemClick: (RouteItem) -> Unit
) : ListAdapter<RouteItem, RouteAdapter.RouteViewHolder>(RouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val currentItem = getItem(position)
        val isLastItem = position == itemCount - 1
        holder.bind(currentItem, isLastItem)
    }

    class RouteViewHolder(
        private val binding: ItemRouteBinding,
        private val onItemClick: (RouteItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(routeItem: RouteItem, isLastItem: Boolean) {
            val itemNumber = adapterPosition + 1
            binding.routeNumberText.text = itemNumber.toString()
            binding.placeTitleText.text =  routeItem.address
            val distanceMeters = routeItem.routeState.distance.roundToInt()
            binding.distanceText.text = if (distanceMeters >= 1000) {
                val km = distanceMeters / 1000.0
                val df = DecimalFormat("#.#")
                "${df.format(km)}km"
            } else {
                "${distanceMeters}m"
            }

            binding.timeText.text = CommonUtil.formatMillisToMinutesSeconds(routeItem.routeState.time)

            // isSelected 상태에 따라 배경을 활성화/비활성화
            itemView.isActivated = routeItem.isSelected
            binding.imageBottomLine.visibility = if (routeItem.isSelected || isLastItem) {
                View.GONE
            } else {
                View.VISIBLE
            }

            itemView.setOnClickListener {
                onItemClick(routeItem)
            }
        }
    }

    private class RouteDiffCallback : DiffUtil.ItemCallback<RouteItem>() {
        override fun areItemsTheSame(oldItem: RouteItem, newItem: RouteItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RouteItem, newItem: RouteItem): Boolean {
            return oldItem == newItem
        }
    }
}