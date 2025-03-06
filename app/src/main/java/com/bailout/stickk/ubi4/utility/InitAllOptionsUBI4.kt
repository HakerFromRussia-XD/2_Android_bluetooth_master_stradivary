package com.bailout.stickk.ubi4.utility

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.ubi4.models.AllOptions
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4


import kotlin.properties.Delegates

class InitAllOptionsUBI4(context: Context, mDeviceAddress: String) {
    private var mSettings: SharedPreferences? = null
    private var gameLaunchRate = 0
    private var maximumPoints = 0
    private var numberOfCups = 0

    init {
        mSettings = context.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        gameLaunchRate = mSettings!!.getInt(mDeviceAddress + PreferenceKeysUBI4.GAME_LAUNCH_RATE, 0)
        maximumPoints = mSettings!!.getInt(mDeviceAddress + PreferenceKeysUBI4.MAXIMUM_POINTS, 0)
        numberOfCups = mSettings!!.getInt(mDeviceAddress + PreferenceKeysUBI4.NUMBER_OF_CUPS, 0)

        myAllOptions = AllOptions(
            gameLaunchRate = "$gameLaunchRate",
            maximumPoints = "$maximumPoints",
            numberOfCups = "$numberOfCups")
    }


    companion object {
        var myAllOptions by Delegates.notNull<AllOptions>()
    }
}