package com.dantariun.sat_demo_map.util.etc

import android.content.Context
import android.os.Build
import android.view.WindowManager

object VersionValidationUtil {



    fun getDisplayRotation(context: Context) : Int{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.rotation
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.rotation
        }
    }
}