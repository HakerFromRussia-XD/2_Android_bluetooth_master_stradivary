package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSpinnerBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.state.WidgetState.spinnerFlowV3
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Collections

class SpinnerDelegateAdapterV3 (
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit
) : ViewBindingDelegateAdapter<SpinnerItemV3, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {
    private var collectJob: kotlinx.coroutines.Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val spinnerInfoList : ArrayList<WidgetSpinnerInfo> = ArrayList()
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value

    override fun Ubi4WidgetSpinnerBinding.onBind(item: SpinnerItemV3) {
        onDestroyParent { onDestroy() }
        // закрыть любые открытые попапы, чтобы не висели поверх при ребайнде
        dismissAll()

        var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(ParameterInfo(0,0,0,0))
        var selectedIndexFromWidget = 0
        var spinnerItems = mutableListOf<String>()
        var widgetPosition = 0

        
        when (val widget = item.widget) {
            is SpinnerParameterWidgetSStruct -> {
                parameterInfoSet = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                selectedIndexFromWidget = widget.dataSpinnerParameterWidgetStruct.selectedIndex
                spinnerItems = widget.dataSpinnerParameterWidgetStruct.spinnerItems as MutableList<String>
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
            }
        }
        val currentParameterInfo = parameterInfoSet.firstOrNull() ?: return
        val info = WidgetSpinnerInfo(
            parameterInfo = currentParameterInfo,
            spinner = spinnerPsv,
            items = spinnerItems,
            widgetPosition = widgetPosition
        )
        spinnerInfoList.removeAll {
            it.parameterInfo.deviceAddress == info.parameterInfo.deviceAddress &&
                it.parameterInfo.parameterID == info.parameterInfo.parameterID &&
                it.parameterInfo.dataCode == info.parameterInfo.dataCode &&
                it.widgetPosition == info.widgetPosition
        }
        spinnerInfoList.add(info)
        registerSpinner(spinnerPsv)
        spinnerPsv.setItems(spinnerItems)
        applySpinnerLockState(info)
        // стартовое состояние из структуры
        spinnerPsv.selectItemByIndex(selectedIndexFromWidget)
        spinnerTv.text = item.title
        spinnerPsv.apply {
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 12f
            typeface = ResourcesCompat.getFont(context, R.font.sf_pro_display_light)
            gravity = Gravity.CENTER
        }


        spinnerPsv.setOnSpinnerItemSelectedListener<String> { _, _, newIndex, _ ->
            if (!isInteractionEnabled) {
                spinnerPsv.dismiss()
                return@setOnSpinnerItemSelectedListener
            }
            // закрываем попап сразу
            spinnerPsv.dismiss()
            val pendingProgrammaticIndex = info.pendingProgrammaticIndex
            if (pendingProgrammaticIndex != null && pendingProgrammaticIndex == newIndex) {
                info.pendingProgrammaticIndex = null
                return@setOnSpinnerItemSelectedListener
            }
            info.pendingProgrammaticIndex = null
            sendValue(info, newIndex)
        }


        // BLE обновления — если у тебя реально приходят payload’ы
        spinnerCollect()
        setUI(currentParameterInfo)
        observeInteractionState()

        // при уходе элемента с экрана — закрыть попап
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                spinnerPsv.dismiss()
            }
        })
    }

    private fun spinnerCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            ParameterStoreV3.updates.collect { key ->
                spinnerInfoList.forEach { info ->
                    if (ParameterStoreV3.toKey(info.parameterInfo) == key) {
                        setUI(info.parameterInfo)
                    }
                }
            }
        }
    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return
        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                spinnerInfoList.forEach { applySpinnerLockState(it) }
            }
        }
    }

    private fun applySpinnerLockState(infoWidget: WidgetSpinnerInfo) {
        infoWidget.spinner.isEnabled = isInteractionEnabled
        infoWidget.spinner.isClickable = isInteractionEnabled
        infoWidget.spinner.isFocusable = isInteractionEnabled
        if (!isInteractionEnabled) {
            infoWidget.spinner.dismiss()
        }
    }

    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        spinnerInfoList.forEach { infoWidget ->
            val sameWidget =
                infoWidget.parameterInfo.deviceAddress == parameterInfo.deviceAddress &&
                    infoWidget.parameterInfo.parameterID == parameterInfo.parameterID &&
                    infoWidget.parameterInfo.dataCode == parameterInfo.dataCode
            if (!sameWidget) return@forEach

            val parameterMeta = ParameterInfoRegistry.getMeta(infoWidget.parameterInfo) ?: return@forEach
            val typedValue = ParameterStoreV3.get(infoWidget.parameterInfo)
                ?: run {
                    val serialized = ParameterProvider.getParameterV3(infoWidget.parameterInfo).data
                    ParameterCodecRegistryV3.decodeFromSerialized(parameterMeta.codecId, serialized)
                }
            val spinnerValue = (typedValue as? ParameterTypedValueV3.Spinner)
                ?.value
                ?.spinnerValue
                ?: return@forEach

            applyProgrammaticSelection(infoWidget, spinnerValue)
            platformLog("SpinnerDelegateAdapterV3", "принимаем spinnerValue=$spinnerValue")
        }

    }

    private fun applyProgrammaticSelection(infoWidget: WidgetSpinnerInfo, index: Int) {
        infoWidget.pendingProgrammaticIndex = index
        infoWidget.spinner.selectItemByIndex(index)
    }

    private fun sendValue(info: WidgetSpinnerInfo, value: Int) {
        if (!isInteractionEnabled) return
        platformLog("SpinnerDelegateAdapterV3", "sendValue info = $info  value = $value")
        main.bleCommandWithQueue(
            BLECommandsV3.sendCommand(
                info.parameterInfo.parameterID,
                info.parameterInfo.dataCode,
                value
            ),
            SERIALPORTCHAR_UUID,
            WRITE
        ){}
    }

    override fun isForViewType(item: Any): Boolean = item is SpinnerItemV3
    override fun SpinnerItemV3.getItemId(): Any = when (val w = widget) {
        is SpinnerParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val p = s.parameterInfoSet.firstOrNull()
            if (p != null) {
                "spinner-${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${s.widgetPosition}"
            } else {
                "spinner-$title"
            }
        }
        else -> "spinner-$title"
    }
    fun onDestroy() {
        spinnerInfoList.forEach { it.spinner.dismiss() }
        spinnerInfoList.clear()
        scope.cancel()
        collectJob?.cancel()
        collectJob = null
        interactionJob?.cancel()
        interactionJob = null
        Log.d("SpinnerDelegateAdapter", "onDestroy spinner")
    }

    companion object {
        private val spinners =
            Collections.synchronizedSet(mutableSetOf<WeakReference<PowerSpinnerView>>())

        private fun registerSpinner(spinner: PowerSpinnerView) {
            cleanupDeadRefs()
            spinners.add(WeakReference(spinner))
        }

        private fun cleanupDeadRefs() {
            val it = spinners.iterator()
            while (it.hasNext()) {
                if (it.next().get() == null) it.remove()
            }
        }

        fun dismissAll() {
            cleanupDeadRefs()
            spinners.forEach { ref -> ref.get()?.dismiss() }
        }
    }
}

data class WidgetSpinnerInfo(
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0,0,0,0),
    val spinner: PowerSpinnerView,
    var items: List<String>,
    var widgetPosition: Int = 0,
    var pendingProgrammaticIndex: Int? = null
)
