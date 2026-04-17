package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.Switch
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.SwitcherV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SwitchItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SwitcherDelegateAdapterV3(
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SwitchItemV3, Ubi4WidgetSwitcherBinding>(
    Ubi4WidgetSwitcherBinding::inflate
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetInfoList: ArrayList<WidgetSwitchInfoV3> = ArrayList()
    private var switchInfoCounter = 0
    private var isAttached = false


    private var collectJob: kotlinx.coroutines.Job? = null
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var programmaticChange = false
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItemV3) {
        Log.d("SwitcherDelegateAdapterV3", "onBind RUN")
        onDestroyParent { onDestroy() }
        isAttached = true

        indicatorOpticStreamIv.visibility = View.GONE

        var parameterInfo: ParameterInfo<Int, Int, Int, Int>? = null
        var switchChecked = false
        var keyMobileSettings = ""
        var widgetPosition = 0

        when (val widget = item.widget) {
            is SwitchParameterWidgetSStruct -> {
                parameterInfo = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0)
                switchChecked = widget.switchChecked
                keyMobileSettings = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.keyMobileSettings
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
            }
        }

        val currentParameterInfo = parameterInfo ?: return
        val isMobileSetting = keyMobileSettings.isNotEmpty()

        val currentSwitchInfo = WidgetSwitchInfoV3(
            parameterInfo = currentParameterInfo,
            isChecked = switchChecked,
            widgetSwitch = widgetSwitchSc,
            widgetPosition = widgetPosition,
            isMobileSettings = isMobileSetting,
            keyMobileSettings = keyMobileSettings
        )
        currentSwitchInfo.instanceId = switchInfoCounter++
        widgetInfoList.removeAll {
            it.parameterInfo.deviceAddress == currentSwitchInfo.parameterInfo.deviceAddress &&
                it.parameterInfo.parameterID == currentSwitchInfo.parameterInfo.parameterID &&
                it.parameterInfo.dataCode == currentSwitchInfo.parameterInfo.dataCode &&
                it.parameterInfo.dataOffsets == currentSwitchInfo.parameterInfo.dataOffsets &&
                it.widgetPosition == currentSwitchInfo.widgetPosition
        }
        widgetInfoList.add(currentSwitchInfo)

        if (isMobileSetting) {
            val saved = main.getBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, false)
            updateSwitchState(saved, widgetSwitchSc)
        } else {
            updateSwitchState(switchChecked, widgetSwitchSc)
        }

        widgetDescriptionTv.text = item.title

        widgetSwitchSc.setOnCheckedChangeListener { _, isChecked ->
            if (programmaticChange) return@setOnCheckedChangeListener
            if (!isInteractionEnabled) {
                updateSwitchState(false, widgetSwitchSc)
                return@setOnCheckedChangeListener
            }

            Log.d(
                "sendSwitcherStateV3",
                "setOnCheckedChangeListener parameterInfo=$currentParameterInfo isChecked=$isChecked"
            )
            currentSwitchInfo.isChecked = isChecked

            if (!isMobileSetting) {
                sendStateSwitcher(currentParameterInfo, isChecked)
            }

            processingMobileSettings(keyMobileSettings, widgetSwitchSc)
        }

        if (!isMobileSetting) {
            currentSwitchInfo.responseReceived.set(false)

            if (RetryUtils.canSendRequestWithFirstReceiveDataFlag(
                    currentParameterInfo.deviceAddress,
                    currentParameterInfo.parameterID
                )
            ) {
                RetryUtils.sendRequestWithRetry(
                    request = {
                        Log.d(
                            "SwitcherRequestV3",
                            "parameterInfo = $currentParameterInfo"
                        )
                        main.bleCommandWithQueue(
                            BLECommandsV3.request(currentParameterInfo.dataCode),
                            SERIALPORTCHAR_UUID,
                            WRITE
                        ) {}
                    },
                    isResponseReceived = {
                        currentSwitchInfo.responseReceived.get()
                    },
                    maxRetries = 5,
                    delayMillis = 1000L,
                    scope = scope
                )
            } else { setUI(currentParameterInfo, widgetPosition = currentSwitchInfo.widgetPosition) }

            switchCollect()
        }

        observeInteractionState()
        applySwitchLockState(currentSwitchInfo)
    }

    private fun setUI(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>? = null,
        widgetPosition: Int? = null
    ) {
        widgetInfoList.forEach { widgetInfo ->
            applySwitchLockState(widgetInfo)
            val sameWidget = parameterInfo == null ||
                    (
                            widgetInfo.parameterInfo.deviceAddress == parameterInfo.deviceAddress &&
                                    widgetInfo.parameterInfo.parameterID == parameterInfo.parameterID &&
                                    widgetInfo.parameterInfo.dataCode == parameterInfo.dataCode &&
                                    (widgetPosition == null || widgetInfo.widgetPosition == widgetPosition)
                            )
            if (!sameWidget) return@forEach

            val parameterMeta = ParameterInfoRegistry.getMeta(widgetInfo.parameterInfo) ?: return@forEach
            val typedValue = ParameterStoreV3.get(widgetInfo.parameterInfo)
                ?: run {
                    val serialized = ParameterProvider.getParameterV3(widgetInfo.parameterInfo).data
                    ParameterCodecRegistryV3.decodeFromSerialized(parameterMeta.codecId, serialized)
                }
            val switcherV3 = (typedValue as? ParameterTypedValueV3.Switcher)?.value ?: return@forEach
            widgetInfo.isChecked = switcherV3.checked
            updateSwitchState(widgetInfo.isChecked, widgetInfo.widgetSwitch)
            widgetInfo.responseReceived.set(true)
        }
    }

    private fun sendStateSwitcher(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        checked: Boolean
    ) {
        val subcommand = parameterInfo.dataCode
        val typedValue = ParameterTypedValueV3.Switcher(SwitcherV3(checked = checked))
        ParameterStoreV3.put(parameterInfo, typedValue)
        val parameterMeta = ParameterInfoRegistry.getMeta(parameterInfo)
        if (parameterMeta != null) {
            ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
                ParameterProvider.getParameterV3(parameterInfo).data = encoded
                platformLog("sendSwitcher", "parameter.data: $encoded")
            }
        }
        main.bleCommandWithQueue(
            BLECommandsV3.sendSwitcher(subcommand, checked),
            SERIALPORTCHAR_UUID, WRITE
        ) {}
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun updateSwitchState(newState: Boolean, switch: Switch) {
        programmaticChange = true
        switch.isChecked = newState

        if (WidgetState.dbSnapshotAppliedWithCrc) {
            switch.jumpDrawablesToCurrentState()
        }

        programmaticChange = false
    }

    private fun processingMobileSettings(keyMobileSettings: String, switch: Switch) {
        if (keyMobileSettings.isNotEmpty()) {
            when (keyMobileSettings) {
                MobileSettingsKey.AUTO_LOGIN.key -> {
                    main.saveBoolean(
                        PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION,
                        switch.isChecked
                    )
                }
            }
        }
    }

    private fun switchCollect() {
        if (collectJob?.isActive == true) return

        collectJob = scope.launch(Dispatchers.Main) {
            try {
                ParameterStoreV3.updates.collect { key ->
                    widgetInfoList.forEach { info ->
                        if (ParameterStoreV3.toKey(info.parameterInfo) == key) {
                            setUI(info.parameterInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("switchCollectTestV3", "${e.message}")
            }
        }
    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return

        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                widgetInfoList.forEach { applySwitchLockState(it) }
            }
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun applySwitchLockState(widgetInfo: WidgetSwitchInfoV3) {
        val switch = widgetInfo.widgetSwitch
        if (!isInteractionEnabled) {
            updateSwitchState(false, switch)
            switch.isEnabled = false
            switch.isClickable = false
            return
        }

        val targetState = if (widgetInfo.isMobileSettings) {
            main.getBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, false)
        } else {
            widgetInfo.isChecked
        }

        switch.isEnabled = true
        switch.isClickable = true
        updateSwitchState(targetState, switch)
    }


    override fun isForViewType(item: Any): Boolean =
        item is SwitchItemV3 && item.widget is SwitchParameterWidgetSStruct
    override fun SwitchItemV3.getItemId(): Any = when (val w = widget) {
        is SwitchParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val p = s.parameterInfoSet.elementAt(0)
            val pos = s.widgetPosition
            "switch-${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${pos}"
        }
        else -> title
    }
    fun onDestroy() {
        Log.d("SwitcherDelegateAdapterV3", "onDestroy switch")
        isAttached = false
        widgetInfoList.clear()
        switchInfoCounter = 0
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
        interactionJob?.cancel()
        interactionJob = null
    }
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
data class WidgetSwitchInfoV3(
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0, 0, 0, 0),
    var isChecked: Boolean = false,
    var widgetSwitch: Switch,
    var widgetPosition: Int = 0,
    var isMobileSettings: Boolean = false,
    var keyMobileSettings: String = "",
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false)
)
