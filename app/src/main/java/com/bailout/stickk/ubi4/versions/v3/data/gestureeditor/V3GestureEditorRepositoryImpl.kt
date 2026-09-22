package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import android.content.SharedPreferences
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureCommand
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureEditorRepository
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettings
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class V3GestureEditorRepositoryImpl(
    private val preferences: SharedPreferences,
    private val enqueuePacket: (ByteArray) -> Unit,
    private val callbackScheduler: Scheduler = AndroidSchedulers.mainThread(),
    private val saveProfile: (ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3) -> Unit = SettingsProfileManager::saveBleValue,
) : V3GestureEditorRepository {
    private val json = Json { encodeDefaults = true }

    override fun getHandSide(): Int {
        val info = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        val storedSide = (ParameterStoreV3.get(info) as? ParameterTypedValueV3.Spinner)
            ?.value?.spinnerValue?.coerceIn(0, 1)
        return storedSide ?: preferences.getInt(
            preferences.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE,
            1,
        )
    }



    override fun observeSettings() = callbackFlow {
        val subscription = RxUpdateMainEventUbi4.getInstance().uiGestureSettingsV3Observable
            .observeOn(callbackScheduler)
            .subscribe { info ->
                val data = ParameterProvider.getParameterV3(info).data
                val gesture = if (data.isBlank()) null else runCatching {
                    json.decodeFromString<GestureV3>(data)
                }.getOrNull()
                trySend(gesture?.let {
                    V3GestureSettings(
                        gestureId = it.gestureId,
                        openPositions = listOf(it.openPosition1, it.openPosition2, it.openPosition3, it.openPosition4, it.openPosition5, it.openPosition6),
                        closePositions = listOf(it.closePosition1, it.closePosition2, it.closePosition3, it.closePosition4, it.closePosition5, it.closePosition6),
                        openToCloseDelays = listOf(it.openToCloseTimeShift1, it.openToCloseTimeShift2, it.openToCloseTimeShift3, it.openToCloseTimeShift4, it.openToCloseTimeShift5, it.openToCloseTimeShift6),
                        closeToOpenDelays = listOf(it.closeToOpenTimeShift1, it.closeToOpenTimeShift2, it.closeToOpenTimeShift3, it.closeToOpenTimeShift4, it.closeToOpenTimeShift5, it.closeToOpenTimeShift6),
                    )
                })
            }
        awaitClose { subscription.dispose() }
    }.buffer(Channel.UNLIMITED)

    override suspend fun awaitReady() {
        BLEState.state.first { it == BLEState.State.READY }
    }

    override fun requestSettings(gestureId: Int) {
        enqueuePacket(BLECommandsV3.requestGestureInfo(gestureId))
    }

    override fun writeSettings(settings: V3GestureSettings, command: V3GestureCommand, name: String) {
        val gesture = Gesture(
            settings.gestureId,
            settings.openPositions[0], settings.openPositions[1], settings.openPositions[2],
            settings.openPositions[3], settings.openPositions[4], settings.openPositions[5],
            settings.closePositions[0], settings.closePositions[1], settings.closePositions[2],
            settings.closePositions[3], settings.closePositions[4], settings.closePositions[5],
            settings.openToCloseDelays[0], settings.openToCloseDelays[1], settings.openToCloseDelays[2],
            settings.openToCloseDelays[3], settings.openToCloseDelays[4], settings.openToCloseDelays[5],
            settings.closeToOpenDelays[0], settings.closeToOpenDelays[1], settings.closeToOpenDelays[2],
            settings.closeToOpenDelays[3], settings.closeToOpenDelays[4], settings.closeToOpenDelays[5], name, 0,
        )
        val typedValue = ParameterTypedValueV3.GestureSettings(GestureV3(
            gestureId = gesture.gestureId,
            openPosition1 = gesture.openPosition1, openPosition2 = gesture.openPosition2,
            openPosition3 = gesture.openPosition3, openPosition4 = gesture.openPosition4,
            openPosition5 = gesture.openPosition5, openPosition6 = gesture.openPosition6,
            closePosition1 = gesture.closePosition1, closePosition2 = gesture.closePosition2,
            closePosition3 = gesture.closePosition3, closePosition4 = gesture.closePosition4,
            closePosition5 = gesture.closePosition5, closePosition6 = gesture.closePosition6,
            openToCloseTimeShift1 = gesture.openToCloseTimeShift1, openToCloseTimeShift2 = gesture.openToCloseTimeShift2,
            openToCloseTimeShift3 = gesture.openToCloseTimeShift3, openToCloseTimeShift4 = gesture.openToCloseTimeShift4,
            openToCloseTimeShift5 = gesture.openToCloseTimeShift5, openToCloseTimeShift6 = gesture.openToCloseTimeShift6,
            closeToOpenTimeShift1 = gesture.closeToOpenTimeShift1, closeToOpenTimeShift2 = gesture.closeToOpenTimeShift2,
            closeToOpenTimeShift3 = gesture.closeToOpenTimeShift3, closeToOpenTimeShift4 = gesture.closeToOpenTimeShift4,
            closeToOpenTimeShift5 = gesture.closeToOpenTimeShift5, closeToOpenTimeShift6 = gesture.closeToOpenTimeShift6,
        ))
        val baseInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val info = ParameterInfo(baseInfo.parameterID, baseInfo.dataCode, baseInfo.deviceAddress, gesture.gestureId)
        ParameterStoreV3.put(info, typedValue)
        saveProfile(info, typedValue)
        // Address/parameter are unused by the V3 codec; the common UBI4 path retains its own values.
        enqueuePacket(BLECommandsV3.sendGestureInfo(GestureWithAddress(0, baseInfo.dataCode, gesture, command.code)))
    }
}
