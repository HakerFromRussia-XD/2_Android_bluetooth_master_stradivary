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
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.switcherFlowV3
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.SwitcherV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SwitchItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

class SwitcherDelegateAdapterV3(
    val onClearCache: (onClearCache: (() -> Unit)) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SwitchItemV3, Ubi4WidgetSwitcherBinding>(
    Ubi4WidgetSwitcherBinding::inflate
) {

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSwitchInfo: ArrayList<WidgetSwitchInfoV3> = ArrayList()
    private var switchInfoCounter = 0
    private var isAttached = false


    private var collectJob: kotlinx.coroutines.Job? = null
    private var programmaticChange = false

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItemV3) {
        Log.d("SwitcherDelegateAdapterV3", "onBind RUN")
        onDestroyParent { onDestroy() }
        onClearCache { clearCache() }
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
        widgetSwitchInfo.add(currentSwitchInfo)

        if (isMobileSetting) {
            val saved = main.getBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, false)
            updateSwitchState(saved, widgetSwitchSc)
        } else {
            updateSwitchState(switchChecked, widgetSwitchSc)
        }

        widgetDescriptionTv.text = item.title

        widgetSwitchSc.setOnCheckedChangeListener { _, isChecked ->
            if (programmaticChange) return@setOnCheckedChangeListener

            Log.d(
                "sendSwitcherStateV3",
                "setOnCheckedChangeListener parameterInfo=$currentParameterInfo isChecked=$isChecked"
            )

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
            } else {
                setUI(currentParameterInfo)
            }

            switchCollect()
        }
    }

    private fun clearCache() {
        widgetSwitchInfo.clear()
    }

    private fun sendStateSwitcher(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        checked: Boolean
    ) {
        val subcommand = parameterInfo.dataCode
        val parameter = ParameterProvider.getParameterV3(parameterInfo)
        when (subcommand) {
            ProsthesisModuleControlEnum.PWCE_TEST_SWITCHER.number.toInt() -> {
                val switcherV3 = parseTestResultSafely(parameter.data) ?: SwitcherV3()
                switcherV3.checked = checked
                parameter.data = json.encodeToString(switcherV3)
                platformLog("sendSwitcher", "parameter.data: ${parameter.data}")
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
                switcherFlowV3.collect { parameterInfo ->
                    setUI(parameterInfo)
                }
            } catch (e: Exception) {
                Log.d("switchCollectTestV3", "${e.message}")
            }
        }
    }

    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        // [new widgets V3] тут добавляем ветки расфасовки пришедших данных SwitcherDelegateAdapterV3 1
        val parameter = ParameterProvider.getParameterV3(parameterInfo)

        val indexWidgetSwitchArray = getIndexWidgetSwitch(parameterInfo.parameterID)
        indexWidgetSwitchArray.forEach { indexWidgetSwitch ->
            val subcommand = parameterInfo.dataCode
            when (subcommand) {
                ProsthesisModuleControlEnum.PWCE_TEST_SWITCHER.number.toInt() -> {
                    val switcherV3 = parseTestResultSafely(parameter.data) ?: SwitcherV3()
                    widgetSwitchInfo[indexWidgetSwitch].isChecked = switcherV3.checked
                    updateSwitchState(
                        widgetSwitchInfo[indexWidgetSwitch].isChecked,
                        widgetSwitchInfo[indexWidgetSwitch].widgetSwitch
                    )
                }
                else -> {
                    if (parameter.data.isNotEmpty()) {
                        try {
                            val switcherV3 = parseTestResultSafely(parameter.data) ?: SwitcherV3()
                            widgetSwitchInfo[indexWidgetSwitch].isChecked = switcherV3.checked
                            updateSwitchState(
                                widgetSwitchInfo[indexWidgetSwitch].isChecked,
                                widgetSwitchInfo[indexWidgetSwitch].widgetSwitch
                            )
                        } catch (e: Exception) {
                            Log.d("switchCollectV3", "$e")
                        } finally {
                            widgetSwitchInfo[indexWidgetSwitch].responseReceived.set(true)
                        }
                    }
                }
            }
        }
    }

    private fun parseTestResultSafely(data: String): SwitcherV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<SwitcherV3>(data) }
            .onFailure { platformLog("SwitcherDelegateAdapterV3", "Failed to decode TestToggleV3: ${it.message}") }
            .getOrNull()
    }

    private fun getIndexWidgetSwitch(parameterID: Int): IntArray {
        platformLog(
            "SwitcherDelegateAdapterV3",
            "getIndexWidgetSwitch из ${widgetSwitchInfo.size}"
        )

        return widgetSwitchInfo.mapIndexedNotNull { index, item ->
            if (item.parameterInfo.parameterID == parameterID) {
                index
            } else {
                null
            }
        }.toIntArray()
    }

    override fun isForViewType(item: Any): Boolean =
        item is SwitchItemV3 && item.widget is SwitchParameterWidgetSStruct

    override fun SwitchItemV3.getItemId(): Any = when (val w = widget) {
        is SwitchParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val p = s.parameterInfoSet.elementAt(0)
            val pos = s.widgetPosition
            "switch-${p.deviceAddress}-${p.parameterID}-$pos"
        }
        else -> title
    }

    fun onDestroy() {
        Log.d("SwitcherDelegateAdapterV3", "onDestroy switch")
        isAttached = false
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
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