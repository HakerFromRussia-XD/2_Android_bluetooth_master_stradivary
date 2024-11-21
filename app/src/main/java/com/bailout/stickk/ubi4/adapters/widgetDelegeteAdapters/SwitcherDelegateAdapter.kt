package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SwitchItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class SwitcherDelegateAdapter(
    val onSwitchClick: (isChecked:Boolean) -> Unit
) :
    ViewBindingDelegateAdapter<SwitchItem, Ubi4WidgetSwitcherBinding>(
        Ubi4WidgetSwitcherBinding::inflate
    ) {

    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItem) {

        var addressDevice = 0
        var parameterID = 0
        var switchChecked = false


        when(item.widget) {
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


        widgetSwitchSc.setOnClickListener {
            onSwitchClick(
                widgetSwitchSc.isChecked
            )
        }
    }

    override fun isForViewType(item: Any): Boolean = item is SwitchItem

    override fun SwitchItem.getItemId(): Any = title
}