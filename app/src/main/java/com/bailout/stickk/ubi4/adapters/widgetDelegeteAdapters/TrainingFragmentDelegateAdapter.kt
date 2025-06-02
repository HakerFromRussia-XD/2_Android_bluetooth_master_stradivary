package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetTrainingOpticBinding

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.TrainingModelState
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.TrainingGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.BaseUrlUtilsUBI4.API_KEY
import com.bailout.stickk.ubi4.utility.TrainingUploadManager
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class TrainingFragmentDelegateAdapter(
    var onConfirmClick: () -> Unit,
    var onShowFileClick: (addressDevice: Int, ) -> Unit,
    var onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<TrainingGestureItem, Ubi4WidgetTrainingOpticBinding>(
        Ubi4WidgetTrainingOpticBinding::inflate
    ) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetTrainingOpticBinding.onBind(item: TrainingGestureItem) {
        // 1) сразу подписываемся на прогресс и состояние
        main.lifecycleScope.launchWhenStarted {
            TrainingUploadManager.stateFlow.collect { state ->
                when (state) {
                    TrainingUploadManager.State.RUNNING -> {
                        trainingSubTitleTv.text = main.getString(R.string.model_training_in_progress_please_wait)
                        trainingTitleTv.text = main.getString(R.string.spppr_is_learning)
                        trainingAnnotationIv.visibility = View.GONE
                        lottieAnimationLoading.visibility = View.VISIBLE
                        percentLearningTv.visibility = View.VISIBLE
                        trainingBtn.isEnabled = true
                        percentLearningTv.text = ""
                        trainingBtn.setOnClickListener {
                            Toast.makeText(main, main.getString(R.string.wait_until_the_model_file_is_created), Toast.LENGTH_SHORT).show()
                        }
                    }
                    TrainingUploadManager.State.EXPORTING -> {
                        trainingSubTitleTv.text = main.getString(R.string.the_model_file_is_saved_to_the_device_memory_please_wait)
                        trainingTitleTv.text = main.getString(R.string.spppr_is_saving)
                        percentLearningTv.visibility = View.GONE
                        lottieAnimationLoading.visibility = View.GONE
                        trainingAnnotationIv.visibility = View.VISIBLE
                        trainingBtn.isEnabled = true
                        trainingBtn.setOnClickListener { onConfirmClick() }
                    }
                    else -> { // BASE
                        trainingSubTitleTv.text = main.getString(R.string.follow_the_gestures_on_the_screen_and_keep_track_of_the_time)
                        trainingTitleTv.text = main.getString(R.string.let_s_start_training_spr)
                        trainingAnnotationIv.visibility = View.VISIBLE
                        lottieAnimationLoading.visibility = View.GONE
                        percentLearningTv.visibility = View.GONE
                        trainingBtn.isEnabled = true
                        trainingBtn.setOnClickListener { onConfirmClick() }
                    }
                }
            }
        }
        main.lifecycleScope.launchWhenStarted {
            TrainingUploadManager.progressFlow.collect { pct ->
                percentLearningTv.text = "$pct %"
            }
        }
        Log.d("TestWidgetView", "Start onBind, item: ${item} item.getItemId() = ${item.getItemId()}")
        onDestroyParent { onDestroy() }
//        main.getPercentProgressLearningModel()
//        Log.d("TestWidgetViewdfsghg", "test int ${main.getPercentProgressLearningModel()}")
        var addressDevice = 0
        var parameterId = 0

        when (val widget = item.widget) {
            is OpticStartLearningWidgetEStruct -> {
                addressDevice = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterId = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).parameterID
            }

            is OpticStartLearningWidgetSStruct -> {
                addressDevice = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterId = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).parameterID
            }
        }

        showFileBtn.setOnClickListener {
            Log.d("TestWidgetView", "setOnClickListener OK")
            onShowFileClick(addressDevice)
        }
    }




    override fun isForViewType(item: Any): Boolean = item is TrainingGestureItem

    override fun TrainingGestureItem.getItemId(): Any = title

    private fun onDestroy() {
        onConfirmClick = {}
        onShowFileClick = {}
        onDestroyParent = {}
        scope.cancel()
    }
}

