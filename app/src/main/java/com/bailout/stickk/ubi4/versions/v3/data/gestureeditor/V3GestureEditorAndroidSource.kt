package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import android.content.SharedPreferences
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

/** Android preferences and response delivery used by the shared editor repository. */
class V3GestureEditorAndroidSource(
    private val preferences: SharedPreferences,
    private val callbackScheduler: Scheduler = AndroidSchedulers.mainThread(),
) {
    fun readSavedHandSide(): Int = preferences.getInt(
        preferences.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE,
        1,
    )

    // Invoke the reader on the original scheduler, before the repository buffers the result.
    fun subscribeSettingsUpdates(onUpdate: (ParameterInfo<Int, Int, Int, Int>) -> Unit): () -> Unit {
        val subscription = RxUpdateMainEventUbi4.getInstance().uiGestureSettingsV3Observable
            .observeOn(callbackScheduler)
            .subscribe { info -> onUpdate(info) }
        return subscription::dispose
    }
}
