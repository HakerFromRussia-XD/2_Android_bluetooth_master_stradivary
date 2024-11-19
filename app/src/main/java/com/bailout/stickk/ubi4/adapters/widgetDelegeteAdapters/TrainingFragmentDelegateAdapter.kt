package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bailout.stickk.databinding.Ubi4WidgetTrainingOpticBinding
import com.bailout.stickk.ubi4.models.TrainingGestureItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrainingFragmentDelegateAdapter(
    var onConfirmClick: () -> Unit,
    var onGenerateClick: () -> Unit,
    var onShowFileClick: () -> Unit,
    var onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<TrainingGestureItem, Ubi4WidgetTrainingOpticBinding>(
        Ubi4WidgetTrainingOpticBinding::inflate
    ) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _trainingSubTitleTv: TextView
    private lateinit var  _trainingAnnotationIv: ImageView




    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetTrainingOpticBinding.onBind(item: TrainingGestureItem) {
        onDestroyParent{ onDestroy() }
        _trainingSubTitleTv = trainingSubTitleTv
        _trainingAnnotationIv = trainingAnnotationIv
        trainingBtn.setOnClickListener {
            onConfirmClick()

        }
        generateBtn.setOnClickListener {
            onGenerateClick()

        }
        showFileBtn.setOnClickListener {
            onShowFileClick()
        }
        stateFlowCollect()
    }

    private fun stateFlowCollect(){
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.stateOpticTrainingFlow.collect { state ->
                    if (state == 0) { _trainingAnnotationIv.visibility = View.VISIBLE }
                    if (state == 1) {
                        _trainingSubTitleTv.text = "Модель выполняется, пожалуйста, подождите..."
                        _trainingAnnotationIv.visibility = View.GONE
                        Log.d("StateCallBack", "In if:$state")
                    }
                    if (state == 2) {
                        _trainingSubTitleTv.text =
                            "СПР файл выполняется, пожалуйста, подождите..."
                        _trainingAnnotationIv.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    override fun isForViewType(item: Any): Boolean = item is TrainingGestureItem

    override fun TrainingGestureItem.getItemId(): Any = title

    private fun onDestroy() {
        onConfirmClick = {}
        onGenerateClick = {}
        onShowFileClick = {}
        onDestroyParent = {}
        scope.cancel()
    }
}