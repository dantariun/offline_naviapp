package com.dantariun.sat_demo_map.util.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlin.math.min

object CommonUtil {

    fun formatMillisToMinutesSeconds(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%02dmin", minutes)
    }

    fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun resizeIcon(context: Context, drawableId: Int, widthDp: Int, heightDp: Int): Drawable? {
        // 1. 원본 Drawable 리소스 가져오기
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null

        // 2. 목표 사이즈(dp)를 픽셀(px)로 변환
        val targetWidthPx = dpToPx(context, widthDp)
        val targetHeightPx = dpToPx(context, heightDp)

        // 3. 원본 Drawable의 고유 너비와 높이 가져오기
        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            // 고유 크기가 없는 벡터 드로어블 등의 경우 처리
            return drawable
        }

        // 4. CenterInside 로직: 원본 비율을 유지하는 새로운 크기 계산
        val ratio = min(targetWidthPx.toFloat() / intrinsicWidth, targetHeightPx.toFloat() / intrinsicHeight)
        val newWidth = (intrinsicWidth * ratio).toInt()
        val newHeight = (intrinsicHeight * ratio).toInt()

        // 5. 최종 비트맵 생성 및 캔버스 설정
        val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 6. 캔버스의 중앙에 위치하도록 좌표 계산
        val left = (targetWidthPx - newWidth) / 2
        val top = (targetHeightPx - newHeight) / 2

        // 7. 계산된 위치와 크기로 Drawable 그리기
        drawable.setBounds(left, top, left + newWidth, top + newHeight)
        drawable.draw(canvas)

        return BitmapDrawable(context.resources, bitmap)
    }
}