//package com.bailout.stickk.ubi4.adapters.testDelegeteAdapter
//
//import android.util.Log
//import com.bailout.stickk.databinding.CheckItemBinding
//import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.CheckItem
//
//
//class CheckDelegateAdapter : ViewBindingDelegateAdapter<CheckItem, CheckItemBinding>(CheckItemBinding::inflate) {
//
//    override fun CheckItemBinding.onBind(item: CheckItem) = with(checkBox) {
//        text = item.title
//        isChecked = item.isChecked
//        setOnCheckedChangeListener { _, isChecked ->
//            item.isChecked = isChecked
//        }
//    }
//
//    override fun isForViewType(item: Any): Boolean = item is CheckItem
//
//    override fun CheckItem.getItemId(): Any = title
//
//    override fun CheckItemBinding.onRecycled() {
//        checkBox.setOnCheckedChangeListener(null)
//    }
//
//    override fun CheckItemBinding.onAttachedToWindow() {
//        Log.d(CheckDelegateAdapter::class.java.simpleName, "onAttachedToWindow")
//    }
//
//    override fun CheckItemBinding.onDetachedFromWindow() {
//        Log.d(CheckDelegateAdapter::class.java.simpleName, "onDetachedFromWindow")
//    }
//}