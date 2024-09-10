package com.bailout.stickk.ubi4.adapters.testDelegeteAdapter

import android.view.View
import com.bailout.stickk.databinding.ImageItemBinding
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.ImageItem


class ImageDelegateAdapter(private val clickListener: View.OnClickListener) :
    ViewBindingDelegateAdapter<ImageItem, ImageItemBinding>(ImageItemBinding::inflate) {

    override fun ImageItemBinding.onBind(item: ImageItem) {
        tvTitle.text = item.title
        imgBg.setOnClickListener(clickListener)
        imgBg.setImageResource(item.imageRes)
    }

    override fun isForViewType(item: Any): Boolean = item is ImageItem

    override fun ImageItem.getItemId(): Any = title
}