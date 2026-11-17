package com.dantariun.sat_demo_map.ui.helper.animation

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.dantariun.sat_demo_map.R

enum class LayoutState {
    FULL_MAP,
    MAP_WITH_CHAT,
    MAP_WITH_DIALOG,
    MAP_WITH_ROUTE,
}

class MapPopAnimationHelper(private val rootLayout: ConstraintLayout) {

    private lateinit var constraintSetFullMap: ConstraintSet
    private lateinit var constraintSetPopUp: ConstraintSet
    
    fun initConstraintSets( ) {
        constraintSetFullMap = ConstraintSet().apply {
            clone(rootLayout)
            connect(R.id.map_view_container, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            setHorizontalWeight(R.id.map_view_container, 1f)
            setHorizontalWeight(R.id.view_pop_up, 0f)
            setVisibility(R.id.view_pop_up, View.GONE)
        }

        constraintSetPopUp = ConstraintSet().apply {
            clone(rootLayout)
            connect(R.id.view_pop_up, ConstraintSet.END, R.id.map_view_container, ConstraintSet.START) // view_pop_up
            setHorizontalWeight(R.id.view_pop_up, 0.4f)
            setHorizontalWeight(R.id.map_view_container, 0.6f)
            setVisibility(R.id.view_pop_up, View.VISIBLE)
        }

        constraintSetFullMap.applyTo(rootLayout)
    }

    fun toggleChatVisibility(state: LayoutState) {
        val transition = TransitionSet().apply {
            duration = 300
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Fade())
            addTransition(ChangeBounds())
        }

        TransitionManager.beginDelayedTransition(rootLayout, transition)

        if (state == LayoutState.FULL_MAP) {
            constraintSetFullMap.applyTo(rootLayout)
        } else {
            constraintSetPopUp.applyTo(rootLayout)
        }
    }
}