package com.dantariun.sat_demo_map.ui.adapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceDrawItemDecoration(
    private val verticalSpaceHeight: Int,
    color: Int
) : RecyclerView.ItemDecoration() {

    // ✅ 1. 여백에 칠할 색상을 가진 Paint 객체 생성
    private val paint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        // 이 부분은 기존과 동일하게 유지하여 공간을 확보합니다.
        if (parent.getChildAdapterPosition(view) != parent.adapter!!.itemCount - 1) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    // ✅ 2. 확보된 공간에 색상을 그리는 onDraw 메서드 추가
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        // RecyclerView의 좌우 패딩을 고려하여 구분선이 그려질 좌우 좌표를 계산합니다.
        val left = parent.paddingLeft.toFloat()
        val right = (parent.width - parent.paddingRight).toFloat()

        // 화면에 보이는 아이템들을 순회합니다.
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)

            // 마지막 아이템 다음에는 그리지 않도록 합니다.
            if (parent.getChildAdapterPosition(child) == parent.adapter!!.itemCount - 1) {
                continue
            }

            // 아이템의 아래쪽에 사각형을 그립니다.
            val top = child.bottom.toFloat()
            val bottom = top + verticalSpaceHeight

            // 계산된 좌표에 사각형을 그립니다.
            c.drawRect(left, top, right, bottom, paint)
        }
    }
}