package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Switch
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BluetoothLeService.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SwitchItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwitcherDelegateAdapter(
    val onSwitchClick: (addressDevice: Int, parameterID: Int, switchState: Boolean) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<SwitchItem, Ubi4WidgetSwitcherBinding>(
        Ubi4WidgetSwitcherBinding::inflate
    ) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var addressDevice = 0
    private var parameterID = 0
    private var switchChecked = false

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var _widgetSwitchSc: Switch


    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItem) {
        _widgetSwitchSc = widgetSwitchSc
        onDestroyParent { onDestroy() }

        when (item.widget) {
            is SwitchParameterWidgetEStruct -> {
                addressDevice = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                switchChecked = item.widget.switchChecked
            }

            is SwitchParameterWidgetSStruct -> {
                addressDevice = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                switchChecked = item.widget.switchChecked

            }
        }

        // Здесь происходит начальная конфигурация UI
        widgetSwitchSc.isChecked = switchChecked
        widgetDescriptionTv.text = item.title


        widgetSwitchSc.setOnCheckedChangeListener { _, isChecked ->
            onSwitchClick(addressDevice, parameterID, isChecked)
        }

        main.bleCommand(
            BLECommands.requestSwitcher(addressDevice, parameterID),
            MAIN_CHANNEL,
            SampleGattAttributes.WRITE
        )

        switchCollect()
    }

    private fun switchCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.switcherFlow.collect { _ ->
                    val parameter = ParameterProvider.getParameter(addressDevice, parameterID)
                    Log.d(
                        "SwitcherCollect",
                        "addressDevice = $addressDevice, parameterID = $parameterID, parameter.data = ${parameter.data}"
                    )
                    if (parameter.data.isNotEmpty()) {
                        _widgetSwitchSc.isChecked = castUnsignedCharToInt(
                            parameter.data.substring(0, 2).toInt(16).toByte()
                        ) != 0
                    }
                }
            }
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
    override fun isForViewType(item: Any): Boolean = item is SwitchItem

    override fun SwitchItem.getItemId(): Any = title
}