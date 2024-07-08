package com.bailout.stickk.new_electronic_by_Rodeon.utils

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.new_electronic_by_Rodeon.models.AllOptions
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import kotlin.properties.Delegates

class InitAllOptions(context: Context, mDeviceAddress: String) {
    private var mSettings: SharedPreferences? = null
    private var gameLaunchRate = 0
    private var maximumPoints = 0
    private var numberOfCups = 0

    init {
        mSettings = context.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        gameLaunchRate = mSettings!!.getInt(mDeviceAddress + PreferenceKeys.GAME_LAUNCH_RATE, 0)
        maximumPoints = mSettings!!.getInt(mDeviceAddress + PreferenceKeys.MAXIMUM_POINTS, 0)
        numberOfCups = mSettings!!.getInt(mDeviceAddress + PreferenceKeys.NUMBER_OF_CUPS, 0)

        myAllOptions = AllOptions(
            gameLaunchRate = "$gameLaunchRate",
            maximumPoints = "$maximumPoints",
            numberOfCups = "$numberOfCups")
    }


    companion object {
        var myAllOptions by Delegates.notNull<AllOptions>()
    }
}