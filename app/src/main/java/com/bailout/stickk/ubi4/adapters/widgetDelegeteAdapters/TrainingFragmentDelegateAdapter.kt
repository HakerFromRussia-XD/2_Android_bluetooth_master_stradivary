package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import com.bailout.stickk.databinding.Ubi4WidgetTrainingOpticBinding
import com.bailout.stickk.ubi4.models.TrainingGestureItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class TrainingFragmentDelegateAdapter(
    val onConfirmClick: () -> Unit,
    val generateClick: () -> Unit,
    val showFileClick: () -> Unit
) :
    ViewBindingDelegateAdapter<TrainingGestureItem, Ubi4WidgetTrainingOpticBinding>(
        Ubi4WidgetTrainingOpticBinding::inflate
    ) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetTrainingOpticBinding.onBind(item: TrainingGestureItem) {
        trainingBtnTv.text = item.title


        trainingBtn.setOnClickListener {
            onConfirmClick()
        }
        generateBtn.setOnClickListener {
            generateClick()
        }

        showFileBtn.setOnClickListener {
            showFileClick()
        }
    }

    override fun isForViewType(item: Any): Boolean = item is TrainingGestureItem

    override fun TrainingGestureItem.getItemId(): Any = title
}