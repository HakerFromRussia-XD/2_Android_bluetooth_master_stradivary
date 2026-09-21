package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.BLEState
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
    private val enqueuePacket: (ByteArray) -> Unit,
    private val callbackScheduler: Scheduler = AndroidSchedulers.mainThread(),
) : V3GestureEditorRepository {
    private val json = Json { encodeDefaults = true }

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
}
