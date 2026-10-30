package com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization

import com.bailout.stickk.databinding.Ubi4ItemInitializationCommandsEntryBinding
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

data object V3InitializationCommandsEntry

class InitializationCommandsEntryAdapterV3(private val onClick: () -> Unit) :
    ViewBindingDelegateAdapter<V3InitializationCommandsEntry, Ubi4ItemInitializationCommandsEntryBinding>(
        Ubi4ItemInitializationCommandsEntryBinding::inflate
    ) {
    override fun Ubi4ItemInitializationCommandsEntryBinding.onBind(item: V3InitializationCommandsEntry) {
        initializationCommandsEntryClick.setOnClickListener { onClick() }
    }

    override fun Ubi4ItemInitializationCommandsEntryBinding.onRecycled() {
        initializationCommandsEntryClick.setOnClickListener(null)
    }

    override fun isForViewType(item: Any): Boolean = item is V3InitializationCommandsEntry
    override fun V3InitializationCommandsEntry.getItemId(): Any = "initialization_commands_entry"
}
