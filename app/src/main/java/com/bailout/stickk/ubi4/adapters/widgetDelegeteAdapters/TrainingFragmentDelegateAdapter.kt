package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.provider.Settings.Global.getString
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetTrainingOpticBinding
import com.bailout.stickk.ubi4.models.TrainingGestureItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
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
    var onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit
) :
    ViewBindingDelegateAdapter<TrainingGestureItem, Ubi4WidgetTrainingOpticBinding>(
        Ubi4WidgetTrainingOpticBinding::inflate
    ) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _trainingSubTitleTv: TextView
    private lateinit var  _trainingAnnotationIv: ImageView
    private lateinit var _trainingBtn: View
    private lateinit var _lottieAnimationLoading: com.airbnb.lottie.LottieAnimationView




    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetTrainingOpticBinding.onBind(item: TrainingGestureItem) {
        onDestroyParent{ onDestroy() }
        _trainingSubTitleTv = trainingSubTitleTv
        _trainingAnnotationIv = trainingAnnotationIv
        _trainingBtn = trainingBtn
        _lottieAnimationLoading = lottieAnimationLoading

        generateBtn.setOnClickListener {
            onGenerateClick()

        }
        showFileBtn.setOnClickListener {
            onShowFileClick()
        }
        stateFlowCollect()
    }

//    private fun stateFlowCollect() {
//        lifecycleOwner.lifecycleScope.launch {
//            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.stateOpticTrainingFlow.collect { state ->
//                    updateUIBasedOnState(state)
//                }
//            }
//        }
//    }

    private fun stateFlowCollect() {

        scope.launch {
            MainActivityUBI4.stateOpticTrainingFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    updateUIBasedOnState(state)
                }
            }
        }
    }

    private fun updateUIBasedOnState(state: Int) {
        when (state) {
            0 -> {
                _trainingAnnotationIv.visibility = View.VISIBLE
                _lottieAnimationLoading.visibility = View.GONE
                _lottieAnimationLoading.cancelAnimation()
                _trainingBtn.isEnabled = true
                _trainingBtn.setOnClickListener {
                    onConfirmClick()
                }
            }

            1 -> {
                _trainingSubTitleTv.text =
                    main.getString(R.string.model_training_in_progress_please_wait)
                _trainingAnnotationIv.visibility = View.GONE
                _lottieAnimationLoading.visibility = View.VISIBLE
                _lottieAnimationLoading.playAnimation()
                _trainingBtn.isEnabled = true
                _trainingBtn.setOnClickListener {
                    Toast.makeText(
                        main,
                        main.getString(R.string.wait_until_the_model_file_is_created),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            2 -> {
                _trainingSubTitleTv.text =
                    main.getString(R.string.the_gesture_model_file_is_ready_load)
                _trainingAnnotationIv.visibility = View.VISIBLE
                _lottieAnimationLoading.visibility = View.GONE
                _lottieAnimationLoading.cancelAnimation()
                _trainingBtn.isEnabled = true
                _trainingBtn.setOnClickListener {
                    onConfirmClick()
                }
            }
        }
    }

//    private fun stateFlowCollect(){
//        scope.launch(Dispatchers.IO) {
//            withContext(Dispatchers.Main) {
//                MainActivityUBI4.stateOpticTrainingFlow.collect { state ->
//                    when (state) {
//                        0 -> {
//                            _trainingAnnotationIv.visibility = View.VISIBLE
//                            _lottieAnimationLoading.visibility = View.GONE
//                            _lottieAnimationLoading.cancelAnimation()
//                            _trainingBtn.isEnabled = true
//                            _trainingBtn.setOnClickListener {
//                                onConfirmClick()
//
//                            }
//                        }
//
//                        1 -> {
//                            _trainingSubTitleTv.text =
//                                main.getString(R.string.model_training_in_progress_please_wait)
//                            _trainingAnnotationIv.visibility = View.GONE
//                            _lottieAnimationLoading.visibility = View.VISIBLE
//                            _lottieAnimationLoading.playAnimation()
//
//                            _trainingBtn.isEnabled = true
//                            _trainingBtn.setOnClickListener {
//                                Toast.makeText(
//                                    main,
//                                    main.getString(R.string.wait_until_the_model_file_is_created),
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        }
//
//                        2 -> {
//                            _trainingSubTitleTv.text =
//                                main.getString(R.string.the_gesture_model_file_is_ready_load)
//                            _trainingAnnotationIv.visibility = View.VISIBLE
//                            _lottieAnimationLoading.visibility = View.GONE
//                            _lottieAnimationLoading.cancelAnimation()
//                            _trainingBtn.isEnabled = true
//                            _trainingBtn.setOnClickListener {
//                                onConfirmClick()
//
//                            }
//
//                        }
//                    }
//                }
//            }
//        }
//    }
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