package com.dantariun.sat_demo_map.ui.helper

import android.view.View
import android.view.animation.DecelerateInterpolator

class RouteUIManager(
    private val topRightInfoView: View,
) {

    init {
        topRightInfoView.visibility = View.GONE
    }

    fun showViews() {
        topRightInfoView.visibility = View.INVISIBLE
        topRightInfoView.post {
            topRightInfoView.translationX = -topRightInfoView.width.toFloat()
            topRightInfoView.visibility = View.VISIBLE
            topRightInfoView.animate()
                .translationX(0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(400)
                .start()
        }
    }

    fun hideViews() {
        topRightInfoView.animate()
            .translationX(-topRightInfoView.width.toFloat())
            .setInterpolator(DecelerateInterpolator())
            .setDuration(400)
            .withEndAction { topRightInfoView.visibility = View.GONE }
            .start()
    }
}