package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import com.bailout.stickk.databinding.Widget1ButtonBinding
import com.bailout.stickk.ubi4.adapters.models.OneButtonItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class OneButtonDelegateAdapter(private val onButtonClick: (oneButtonItem: OneButtonItem) -> Unit) :
    ViewBindingDelegateAdapter<OneButtonItem, Widget1ButtonBinding>(Widget1ButtonBinding::inflate) {

    override fun Widget1ButtonBinding.onBind(item: OneButtonItem) {
        widget1Button.text = item.title
        widget1Button.setOnClickListener { onButtonClick(item) }
    }

    override fun isForViewType(item: Any): Boolean = item is OneButtonItem

    override fun OneButtonItem.getItemId(): Any = title
}