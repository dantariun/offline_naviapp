package com.dantariun.ecall.presentation.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.O)
private fun getPopUpLayoutParams(context: Context): WindowManager.LayoutParams {
    val orientation = context.resources.configuration.orientation

    val params = WindowManager.LayoutParams(
        // 팝업 크기 설정
        // 가로 모드일 때는 화면 높이 전체를, 세로 모드일 때는 화면 너비 전체를 차지하도록 설정
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) WRAP_CONTENT else MATCH_PARENT,
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) MATCH_PARENT else WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // API 26 이상
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    )

    // 화면 방향에 따라 위치 및 애니메이션 설정
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // 가로 모드: 좌/우에서 슬라이드
        params.gravity = Gravity.END // 또는 Gravity.START
//        params.windowAnimations = R.style.Animation_SlideFromEnd
    } else {
        // 세로 모드: 위/아래에서 슬라이드
        params.gravity = Gravity.BOTTOM // 또는 Gravity.TOP
//        params.windowAnimations = R.style.Animation_SlideFromBottom
    }

    return params
}