package com.bailout.stickk.ubi4.di

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import com.bailout.stickk.ubi4.ble.BleCommandWriter
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.data.network.TelemetryCoordinator
import com.bailout.stickk.ubi4.data.network.Ubi4SettingsProfileReceiver
import com.bailout.stickk.ubi4.versions.v3.di.createSettingsProfileValueApplier
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
import com.bailout.stickk.ubi4.versions.v3.data.transport.V3CommandTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

internal object BleDependencies {
    @Volatile private var commandExecutorRef: WeakReference<BleCommandExecutor>? = null
    @Volatile private var sensorsRefresh: (() -> Unit)? = null
    @Volatile private var telemetryRequest: (() -> Unit)? = null
    @Volatile var currentDeviceIdentity: V3DeviceIdentityStore? = null
        private set
    val v3CommandTransport = V3CommandTransport {
        commandExecutorRef?.get() ?: error("BLE command executor is not registered")
    }

    @Synchronized
    fun bindCommandExecutor(executor: BleCommandExecutor, identity: V3DeviceIdentityStore? = null) {
        sensorsRefresh = null
        telemetryRequest = null
        commandExecutorRef = WeakReference(executor)
        currentDeviceIdentity = identity
    }

    @Synchronized
    fun unbindCommandExecutor(executor: BleCommandExecutor) {
        // A previous Activity can finish after the next session has already registered.
        if (commandExecutorRef?.get() === executor) {
            sensorsRefresh = null
            telemetryRequest = null
            commandExecutorRef = null
            currentDeviceIdentity = null
        }
    }

    @Synchronized
    fun bindSensorsRefresh(
        executor: BleCommandExecutor,
        controller: BLEController,
        observeSyncProgress: () -> Unit,
    ) {
        check(commandExecutorRef?.get() === executor) { "Cannot bind sensors refresh outside the active BLE session" }
        // These callbacks belong to this session and are released on replacement or unbind.
        sensorsRefresh = {
            observeSyncProgress()
            controller.refreshWidgetsV3BySwipe()
        }
    }

    fun refreshSensors() {
        val refresh = sensorsRefresh ?: error("Sensors refresh is not registered")
        refresh()
    }

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

    fun requestV3TelemetryData() {
        // Statistics previously skipped the request when there was no current Activity.
        telemetryRequest?.invoke()
    }

    @Synchronized
    fun bindTelemetry(
        owner: ViewModelStoreOwner,
        scope: CoroutineScope,
        preferences: SharedPreferences,
        controller: BLEController,
        showToast: (String) -> Unit,
        executor: BleCommandExecutor,
    ) {
        check(commandExecutorRef?.get() === executor) { "Cannot bind telemetry outside the active BLE session" }
        telemetryRequest = controller::requestTelemetryDataV3
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
        context: Context,
    ) {
        bindCommandExecutor(executor, deviceIdentity(owner))
        Ubi4SettingsProfileReceiver.defaultApplyProfileValues = createSettingsProfileValueApplier(context)
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
