package com.bailout.stickk.ubi4.di

import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import java.util.WeakHashMap
import com.bailout.stickk.ubi4.ble.BleCommandWriter
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.data.network.TelemetryCoordinator
import com.bailout.stickk.ubi4.versions.v3.data.telemetry.V3TelemetryRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.telemetry.SendTelemetryUseCaseV3
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.device.DeviceConnectionInitializer
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.data.state.BLEState.bleParser
import com.bailout.stickk.ubi4.data.state.BLEState.bleParserV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleEnvironment
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendNextChunkFlagFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.data.transport.V3CommandTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

internal object BleDependencies {
    val v3CommandTransport = V3CommandTransport { MainActivityUBI4.main }
    // ViewModelStore survives recreation; weak keys do not retain finished Activity scopes.
    private val deviceIdentities = WeakHashMap<ViewModelStore, V3DeviceIdentityStore>()

    @Synchronized
    fun deviceIdentity(owner: ViewModelStoreOwner): V3DeviceIdentityStore =
        deviceIdentities.getOrPut(owner.viewModelStore) { V3DeviceIdentityStore() }

    fun updateLaunchIntent(owner: ViewModelStoreOwner, intent: Intent) {
        if (UiState.isInterfaceV3Activated) {
            deviceIdentity(owner).intentDeviceName = intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME)
        }
    }

    fun createCommandWriter(dispatch: (ByteArray?, String, String) -> Boolean) =
        BleCommandWriter(dispatch)

    fun bindTelemetry(
        owner: ViewModelStoreOwner,
        scope: CoroutineScope,
        preferences: SharedPreferences,
        controller: BLEController,
        showToast: (String) -> Unit,
    ) {
        val repository = V3TelemetryRepositoryImpl(preferences, controller::requestTelemetryDataV3, deviceIdentity(owner))
        val coordinator = TelemetryCoordinator(scope, SendTelemetryUseCaseV3(repository), showToast)
        controller.setOnConnectedListener {
            if (UiState.isInterfaceV3Activated) coordinator.sendTelemetry(showResultToast = false)
        }
    }

    /** Initialize the launch context and shared graph before BLEController starts connecting. */
    fun initializeSession(
        intent: Intent,
        scope: CoroutineScope,
        manager: BleManagerKmm,
        executor: BleCommandExecutor,
        commandWriter: BleCommandWriter,
        owner: ViewModelStoreOwner,
    ) {
        DeviceConnectionInitializer.initialize(intent)
        updateLaunchIntent(owner, intent)
        canSendNextChunkFlagFlow = MutableSharedFlow()
        commandWriter.reset()
        manager.setBleCommandExecutor(executor)
        bleParser = BLEParser(scope, bleCommandExecutor = executor, bleManager = manager)
        bleParserV3 = BLEParserV3(scope, bleCommandExecutor = executor, bleManager = manager)
        BleEnvironment.register(manager, executor, bleParser, bleParserV3)
    }
}
