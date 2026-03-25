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
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.state.WidgetState.spinnerFlowV3
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.guiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference
import java.util.Collections

class SpinnerDelegateAdapterV3 (
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit
) : ViewBindingDelegateAdapter<SpinnerItemV3, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {
    private val json = Json { encodeDefaults = true }
    private var collectJob: kotlinx.coroutines.Job? = null
    private val disposables = CompositeDisposable()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val spinnerInfoList : ArrayList<WidgetSpinnerInfo> = ArrayList()

    override fun Ubi4WidgetSpinnerBinding.onBind(item: SpinnerItemV3) {
        onDestroyParent { onDestroy() }
        // закрыть любые открытые попапы, чтобы не висели поверх при ребайнде
        dismissAll()

        var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(ParameterInfo(0,0,0,0))
        var selectedIndexFromWidget = 0
        var spinnerItems = mutableListOf<String>()

        
        when (val widget = item.widget) {
            is SpinnerParameterWidgetSStruct -> {
                parameterInfoSet = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                selectedIndexFromWidget = widget.dataSpinnerParameterWidgetStruct.selectedIndex
                spinnerItems = widget.dataSpinnerParameterWidgetStruct.spinnerItems as MutableList<String>
            }
        }
        val info = WidgetSpinnerInfo(
            parameterInfoSet = parameterInfoSet,
            spinner = spinnerPsv,
            items = spinnerItems
        )
        spinnerInfoList.add(info)
        registerSpinner(spinnerPsv)
        spinnerPsv.setItems(spinnerItems)
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
            // закрываем попап сразу
            spinnerPsv.dismiss()
            sendValue(info, newIndex)
        }


        // BLE обновления — если у тебя реально приходят payload’ы
        spinnerCollect()

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
            spinnerFlowV3.collect { parameterInfo -> setUI(parameterInfo) }
        }
    }


    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        val parameter = ParameterProvider.getParameterV3(parameterInfo)
        val spinnerValue = parseSpinnerSafely(parameter.data)?.spinnerValue
        spinnerInfoList.forEachIndexed { index, info ->
            val subcommand = parameterInfo.dataCode
            when (subcommand) {
                PWCE_SET_HAND_CONTROL_MODE.number.toInt() -> {
                   info.spinner.selectItemByIndex(spinnerValue ?: 0)
                }
                GMCE_SET_LEFT_RIGHT_HAND.number.toInt() -> {
                    info.spinner.selectItemByIndex(spinnerValue ?: 0)
                }
            }
        }

    }

    private fun sendValue(info: WidgetSpinnerInfo, value: Int) {
        platformLog("SpinnerDelegateAdapterV3", "sendValue info = $info  value = $value")
        main.bleCommandWithQueue(BLECommandsV3.sendCommand(info.parameterInfoSet.elementAt(0).parameterID, info.parameterInfoSet.elementAt(0).dataCode, value), SERIALPORTCHAR_UUID, WRITE){}
    }

    private fun parseSpinnerSafely(data: String): SpinnerV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<SpinnerV3>(data) }
            .onFailure { platformLog("SpinnerDelegateAdapterV3", "Failed to decode SpinnerV3: ${it.message}") }
            .getOrNull()
    }
    override fun isForViewType(item: Any): Boolean = item is SpinnerItemV3
    override fun SpinnerItemV3.getItemId(): Any = title
    fun onDestroy() {
        spinnerInfoList.forEach { it.spinner.dismiss() }
        spinnerInfoList.clear()
        scope.cancel()
        disposables.clear()
        collectJob?.cancel()
        collectJob = null
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
    var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(ParameterInfo(0,0,0,0)),
    val spinner: PowerSpinnerView,
    var items: List<String>
)