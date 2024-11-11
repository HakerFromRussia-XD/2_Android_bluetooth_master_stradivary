package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.models.SwitchItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class SwitcherDelegateAdapter(
    val onSwitchClick: () -> Unit
) :
    ViewBindingDelegateAdapter<SwitchItem, Ubi4WidgetSwitcherBinding>(
        Ubi4WidgetSwitcherBinding::inflate
    ) {

    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItem) {
        widgetDescriptionTv.text = item.text

        widgetSwitchBtn.setOnClickListener {
            onSwitchClick()
        }
    }

    override fun isForViewType(item: Any): Boolean = item is SwitchItem

    override fun SwitchItem.getItemId(): Any = text
}