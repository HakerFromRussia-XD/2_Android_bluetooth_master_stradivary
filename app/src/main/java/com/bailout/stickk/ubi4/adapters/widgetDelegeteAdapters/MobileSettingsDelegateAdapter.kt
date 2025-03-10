package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.content.Context
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.models.MobileSettingsItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class MobileSettingsDelegateAdapter (
    private val context: Context,
    private val onSwitchToggled: (Boolean) -> Unit
) : ViewBindingDelegateAdapter<MobileSettingsItem, Ubi4WidgetSwitcherBinding>(
    Ubi4WidgetSwitcherBinding::inflate
) {
    override fun Ubi4WidgetSwitcherBinding.onBind(item: MobileSettingsItem) {
        widgetDescriptionTv.text = item.title
        widgetSwitchSc.isChecked = item.settings.autoLogin
        widgetSwitchSc.setOnCheckedChangeListener { _, isChecked ->
            onSwitchToggled(isChecked)
        }

    }

    override fun isForViewType(item: Any): Boolean = item is MobileSettingsItem

    override fun MobileSettingsItem.getItemId(): Any = title
}