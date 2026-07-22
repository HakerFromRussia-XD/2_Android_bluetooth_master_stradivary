package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

import com.bailout.stickk.databinding.Ubi4ItemBleLogButtonBinding
import com.bailout.stickk.ubi4.models.blelog.BleLogButtonItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class BleLogButtonDelegateAdapter(
    private val onClick: () -> Unit
) : ViewBindingDelegateAdapter<BleLogButtonItem, Ubi4ItemBleLogButtonBinding>(
    Ubi4ItemBleLogButtonBinding::inflate
) {
    override fun Ubi4ItemBleLogButtonBinding.onBind(item: BleLogButtonItem) {
        bleLogClick.setOnClickListener { onClick() }
    }

    override fun isForViewType(item: Any): Boolean = item is BleLogButtonItem

    override fun BleLogButtonItem.getItemId(): Any = "ble_log_button"
}
