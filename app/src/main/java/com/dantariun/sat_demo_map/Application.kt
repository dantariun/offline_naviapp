package com.dantariun.sat_demo_map

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences: SharedPreferences = applicationContext.getSharedPreferences(
            "${applicationContext.packageName}_preferences",
            Context.MODE_PRIVATE
        )
        Configuration.getInstance().load(applicationContext,sharedPreferences)
        Configuration.getInstance().userAgentValue = "com.fu_it.sat_demo_map"

    }
}