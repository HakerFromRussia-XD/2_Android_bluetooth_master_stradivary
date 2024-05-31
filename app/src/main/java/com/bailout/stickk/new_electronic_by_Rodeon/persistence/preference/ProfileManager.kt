package com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.GestureStateWithEncoders
import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.ProfileSettings
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class ProfileManager(mContext: Context, private val main: MainActivity) {
    private var mSettings: SharedPreferences? = null
    private val gestures = ArrayList<GestureStateWithEncoders>()
    private var gestureState = 0 // 0 - close     1 - open

    init {
        mSettings = mContext.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    fun loadProfileSettings(): ProfileSettings {
        val text = "load not work"
        for (i in 0..13) {
            val gestureStateWithEncoders = GestureStateWithEncoders(
                gestureNumber = 0,
                openStage1 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + i, 0),
                openStage2 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + i, 0),
                openStage3 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + i, 0),
                openStage4 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + i, 0),
                openStage5 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + i, 0),
                openStage6 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + i, 0),
                closeStage1 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + i, 0),
                closeStage2 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + i, 0),
                closeStage3 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + i, 0),
                closeStage4 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + i, 0),
                closeStage5 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + i, 0),
                closeStage6 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + i, 0),
                openStageDelay1 = mSettings!!.getInt(PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 1, 0),
                openStageDelay2 = mSettings!!.getInt(PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 2, 0),
                openStageDelay3 = 3,
                openStageDelay4 = 4,
                openStageDelay5 = 5,
                openStageDelay6 = 6,
                closeStageDelay1 = 1,
                closeStageDelay2 = 2,
                closeStageDelay3 = 3,
                closeStageDelay4 = 4,
                closeStageDelay5 = 5,
                closeStageDelay6 = 6,
                state = gestureState,
                withChangeGesture = false,
                onlyNumberGesture = false
            )
            gestures.add(gestureStateWithEncoders)
        }

        return ProfileSettings(
            gesturesStateWithEncoders = gestures,
            openChNum = mSettings!!.getInt(main.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, 0),
            closeChNum = mSettings!!.getInt(main.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, 0),
            correlatorNoiseThreshold1 = mSettings!!.getInt(main.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, 16),
            correlatorNoiseThreshold2 = mSettings!!.getInt(main.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, 16),
            setReverseNum = mSettings!!.getBoolean(main.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false),
            thresholdsBlocking = mSettings!!.getBoolean(main.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)
        )
    }

    fun showVersion(): String {
        return mSettings?.getInt(
            main.mDeviceAddress + PreferenceKeys.DRIVER_NUM,
            0).toString()
    }
    fun showVersionS(): String {
        return mSettings?.getString(
            mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") +
                    PreferenceKeys.DRIVER_VERSION_STRING, "1234").toString()
    }
}