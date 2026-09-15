package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.view.View
import android.widget.CompoundButton
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.versions.v3.presentation.autologin.V3AutoLoginUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class AutoLoginDelegateAdapterV3(
    private val onCheckedChanged: (Boolean) -> Unit,
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val animationsEnabled: () -> Boolean,
) : ViewBindingDelegateAdapter<V3SpecialSettingsWidget.Switch, Ubi4WidgetSwitcherBinding>(Ubi4WidgetSwitcherBinding::inflate) {
    private class Holder(val binding: Ubi4WidgetSwitcherBinding) {
        lateinit var listener: CompoundButton.OnCheckedChangeListener
    }

    private val holders = mutableMapOf<CompoundButton, Holder>()
    private var state = V3AutoLoginUiState()

    override fun Ubi4WidgetSwitcherBinding.onBind(item: V3SpecialSettingsWidget.Switch) {
        release(widgetSwitchSc)
        onDestroyParent(::onDestroy)
        indicatorOpticStreamIv.visibility = View.GONE
        widgetDescriptionTv.text = item.info.title
        val holder = Holder(this)
        holders[widgetSwitchSc] = holder
        holder.listener = CompoundButton.OnCheckedChangeListener { _, checked ->
            if (holders[widgetSwitchSc] !== holder) return@OnCheckedChangeListener
            if (state.isEnabled) onCheckedChanged(checked) else render(holder)
        }
        render(holder)
    }

    fun render(state: V3AutoLoginUiState) {
        this.state = state
        holders.values.forEach(::render)
    }

    private fun render(holder: Holder) = with(holder.binding.widgetSwitchSc) {
        setOnCheckedChangeListener(null)
        try {
            isChecked = state.isChecked
            isEnabled = state.isEnabled
            isClickable = state.isEnabled
            if (!animationsEnabled()) jumpDrawablesToCurrentState()
        } finally {
            setOnCheckedChangeListener(holder.listener)
        }
    }

    override fun Ubi4WidgetSwitcherBinding.onRecycled() = release(widgetSwitchSc)

    private fun release(button: CompoundButton) {
        button.setOnCheckedChangeListener(null)
        holders.remove(button)
    }

    private fun onDestroy() {
        holders.keys.toList().forEach(::release)
        state = V3AutoLoginUiState()
    }

    override fun isForViewType(item: Any) = item is V3SpecialSettingsWidget.Switch && item.info.key == MobileSettingsKey.AUTO_LOGIN.key
    override fun V3SpecialSettingsWidget.Switch.getItemId(): Any = info.key
}
