package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetTrainingOpticBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.models.TrainingGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.stateOpticTrainingFlow
import com.bailout.stickk.ubi4.utility.TrainingModelHandler
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrainingFragmentDelegateAdapter(
    var onConfirmClick: () -> Unit,
    var onShowFileClick: (addressDevice: Int, ) -> Unit,
    var onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<TrainingGestureItem, Ubi4WidgetTrainingOpticBinding>(
        Ubi4WidgetTrainingOpticBinding::inflate
    ) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _trainingTitleTv: TextView
    private lateinit var _trainingSubTitleTv: TextView
    private lateinit var _trainingAnnotationIv: ImageView
    private lateinit var _trainingBtn: View
    private lateinit var _lottieAnimationLoading: com.airbnb.lottie.LottieAnimationView
    private lateinit var _percentLearning: TextView


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetTrainingOpticBinding.onBind(item: TrainingGestureItem) {
        Log.d("TestWidgetView", "Start onBind, item: ${item}")
        onDestroyParent { onDestroy() }
        _trainingTitleTv = trainingTitleTv
        _trainingSubTitleTv = trainingSubTitleTv
        _trainingAnnotationIv = trainingAnnotationIv
        _trainingBtn = trainingBtn
        _lottieAnimationLoading = lottieAnimationLoading
        _percentLearning = percentLearningTv
        main.getPercentProgressLearningModel()
        Log.d("TestWidgetViewdfsghg", "test int ${main.getPercentProgressLearningModel()}")
        var addressDevice = 0
        var parameterId = 0

        when (item.widget) {
            is OpticStartLearningWidgetEStruct -> {
                addressDevice = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterId = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
            }

            is OpticStartLearningWidgetSStruct -> {
                addressDevice = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterId = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
            }
        }


        showFileBtn.setOnClickListener {
            Log.d("TestWidgetView", "setOnClickListener OK")
            onShowFileClick(addressDevice)
        }
        startStateFlowCollector()
    }

    private fun startStateFlowCollector() {
        scope.launch(Dispatchers.Main) {
            stateOpticTrainingFlow.collect { state ->
                Log.d("StateFlowCollector", "Collected state: $state")
                updateUI(state)
                scope.launch(Dispatchers.Main) {
                    while (stateOpticTrainingFlow.value == PreferenceKeysUBI4.TrainingModelState.RUN) {
                        _percentLearning.text = main?.getPercentProgressLearningModel().toString() + " %"
                        Log.d("StateFlowCollector", "scope 2 run")
                        delay(100)
                    }
                }
            }
        }
    }



    /**
     * Обновляет UI в зависимости от текущего состояния.
     */
    private fun updateUI(state: PreferenceKeysUBI4.TrainingModelState) {
        when (state) {
            PreferenceKeysUBI4.TrainingModelState.BASE -> {
                _trainingSubTitleTv.text = main.getString(R.string.follow_the_gestures_on_the_screen_and_keep_track_of_the_time)
                _trainingTitleTv.text = main.getString(R.string.let_s_start_training_spr)
                _trainingAnnotationIv.visibility = View.VISIBLE
                _lottieAnimationLoading.visibility = View.GONE
                _percentLearning.visibility = View.GONE
                _lottieAnimationLoading.cancelAnimation()
                _trainingBtn.isEnabled = true
                _trainingBtn.setOnClickListener {
                    onConfirmClick()
                }
            }

            PreferenceKeysUBI4.TrainingModelState.RUN -> {
                _trainingSubTitleTv.text = main.getString(R.string.model_training_in_progress_please_wait)
                _trainingTitleTv.text = main.getString(R.string.spppr_is_learning)
                _trainingAnnotationIv.visibility = View.GONE
                _lottieAnimationLoading.visibility = View.VISIBLE
                _percentLearning.visibility = View.VISIBLE
                _lottieAnimationLoading.playAnimation()
                _trainingBtn.isEnabled = true
                _percentLearning.text = ""
                _trainingBtn.setOnClickListener {
                    Toast.makeText(
                        main,
                        main.getString(R.string.wait_until_the_model_file_is_created),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            PreferenceKeysUBI4.TrainingModelState.EXPORT -> {
                _trainingSubTitleTv.text = main.getString(R.string.the_model_file_is_saved_to_the_device_memory_please_wait)
                _trainingTitleTv.text = main.getString(R.string.spppr_is_saving)
//                _trainingAnnotationIv.visibility = View.VISIBLE
//                _lottieAnimationLoading.visibility = View.GONE
                _percentLearning.visibility = View.GONE
//                _lottieAnimationLoading.cancelAnimation()
                _trainingBtn.isEnabled = true
                _trainingBtn.setOnClickListener {
                    onConfirmClick()
                }
            }
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

