package com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter

import com.bailout.stickk.databinding.ImageItemBinding
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.ImageItem

class GenerateItemsDelegateAdapter(private val generateNewItems: () -> Unit) :
    ViewBindingDelegateAdapter<ImageItem, ImageItemBinding>(ImageItemBinding::inflate) {

    override fun ImageItemBinding.onBind(item: ImageItem) {
        tvTitle.text = item.title
        imgBg.setImageResource(item.imageRes)
        llRoot.setOnClickListener { generateNewItems() }
    }

    override fun isForViewType(item: Any): Boolean = item is ImageItem

    override fun ImageItem.getItemId(): Any = title
}