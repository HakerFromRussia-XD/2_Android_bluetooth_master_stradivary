package com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter

import com.bailout.stickk.databinding.TextItemBinding
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.TextItem


class TxtDelegateAdapter : ViewBindingDelegateAdapter<TextItem, TextItemBinding>(TextItemBinding::inflate) {

    override fun TextItemBinding.onBind(item: TextItem) {
        tvTitle.text = item.title
        tvDescription.text = item.description
    }

    override fun isForViewType(item: Any) = item is TextItem

    override fun TextItem.getItemId(): Any = title
}