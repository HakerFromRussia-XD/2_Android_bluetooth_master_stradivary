package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.os.Handler
import android.util.Log
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSpinnerBinding
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.PlotThresholds
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SliderItem
import com.bailout.stickk.ubi4.models.SpinnerItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
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

class SpinnerDelegateAdapter(
    private val onSpinnerItemSelected: (addressDevice: Int, parameterID: Int, newIndex: Int) -> Unit,
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit
) : ViewBindingDelegateAdapter<SpinnerItem, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {
    private val disposables = CompositeDisposable()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _psvGesturesSpinner: PowerSpinnerView

    private val spinnerInfoList = mutableListOf<WidgetSpinnerInfo>()

    override fun Ubi4WidgetSpinnerBinding.onBind(item: SpinnerItem) {
        _psvGesturesSpinner = psvGesturesSpinner
        onDestroyParent { onDestroy() }

        val addressDeviceList = mutableListOf<Int>()
        val parameterIDList = mutableListOf<Int>()
        var spinnerItems: List<String> = emptyList()
        var selectedIndex = 0

        when (item.widget) {
            is SpinnerParameterWidgetEStruct -> {
                item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    addressDeviceList.add(it.deviceAddress)
                    parameterIDList.add(it.parameterID)
                }
//                spinnerItems = item.widget.dataSpinnerParameterWidgetEStruct.spinnerItems
                spinnerItems =
                    main.baseContext.resources.getStringArray(R.array.gesture_loop).toList()
                selectedIndex = item.widget.dataSpinnerParameterWidgetStruct.selectedIndex

                Log.d(
                    "SpinnerDelegateAdapter",
                    "E struct: addressDevice = $addressDeviceList, deviceId = ${item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId}"
                )
            }
            is SpinnerParameterWidgetSStruct -> {
                item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    addressDeviceList.add(it.deviceAddress)
                    parameterIDList.add(it.parameterID)
                }
                spinnerItems = item.widget.dataSpinnerParameterWidgetStruct.spinnerItems
                selectedIndex = item.widget.dataSpinnerParameterWidgetStruct.selectedIndex
                Log.d(
                    "SpinnerDelegateAdapter",
                    "S struct: addressDevice = $addressDeviceList, deviceId = ${item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId}"
                )
            }
            else -> {
                Log.w("SpinnerDelegateAdapter", "Unknown widget type: ${item.widget}")
            }
        }

        // Берем первый элемент из списков (если они пустые, используем 0)
        val addressDevice = addressDeviceList.firstOrNull() ?: 0
        val parameterID = parameterIDList.firstOrNull() ?: 0

        spinnerTv.text = item.title
        Log.d("SpinnerDelegateAdapter", "Bind spinner: address=$addressDevice, param=$parameterID, items=$spinnerItems")

        // Заполняем PowerSpinnerView элементами
        psvGesturesSpinner.setItems(spinnerItems)
        psvGesturesSpinner.apply {
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 12f
            typeface = ResourcesCompat.getFont(context, R.font.sf_pro_display_light)
            gravity = Gravity.CENTER

        }

        // Ставим выбранный индекс, если он валиден
        if (selectedIndex in spinnerItems.indices) {
            psvGesturesSpinner.selectItemByIndex(selectedIndex)
        }

        psvGesturesSpinner.setOnSpinnerItemSelectedListener<String> { _, _, newIndex, newItem ->
            Log.d("SpinnerDelegateAdapter", "User selected $newItem (pos=$newIndex) from $spinnerItems")
            onSpinnerItemSelected(addressDevice, parameterID, newIndex)
        }

        // Сохраняем информацию для обновления (например, при поступлении BLE-данных)
        spinnerInfoList.add(WidgetSpinnerInfo(addressDevice, parameterID, psvGesturesSpinner, spinnerItems))

        Handler().postDelayed({
            // Пример вызова BLE-команды (закомментирован)
            // main.bleCommandWithQueue(BLECommands.requestSpinnerData(addressDevice, parameterID), MAIN_CHANNEL, SampleGattAttributes.WRITE) {}
        }, 300)

        collectSpinnerUpdates()
    }

    private fun collectSpinnerUpdates() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.spinnerFlow.collect { parameterRef ->
                    // Ищем нужный элемент по адресу и parameterID
                    val index = getIndexWidgetSpinner(parameterRef.addressDevice, parameterRef.parameterID)
                    if (index >= 0) {
                        val param = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
                        Log.d("SpinnerCollect", "Got data = ${param.data} for device=${parameterRef.addressDevice}, paramID=${parameterRef.parameterID}")
                        // Предполагаем, что param.data содержит индекс в hex, например "03" означает индекс 3
                        val selectedIndex = Json.decodeFromString<DataSpinnerParameterWidgetStruct>("\"${param.data}\"").selectedIndex
                        spinnerInfoList[index].items = Json.decodeFromString<DataSpinnerParameterWidgetStruct>("\"${param.data}\"").spinnerItems
                        _psvGesturesSpinner.setItems(spinnerInfoList[index].items)
                        if (selectedIndex in spinnerInfoList[index].items.indices) {
                            _psvGesturesSpinner.selectItemByIndex(selectedIndex)
                        }

                    }
                }
            }
        }
    }

    private fun getIndexWidgetSpinner(addressDevice: Int, parameterID: Int): Int {
        spinnerInfoList.forEachIndexed { i, info ->
            if (info.addressDevice == addressDevice && info.parameterID == parameterID) {
                return i
            }
        }
        return -1
    }

    override fun isForViewType(item: Any): Boolean = item is SpinnerItem
    override fun SpinnerItem.getItemId(): Any = title

    fun onDestroy() {
        spinnerInfoList.forEach { it.spinner.dismiss() }
        scope.cancel()
        disposables.clear()
        Log.d("SpinnerDelegateAdapter", "onDestroy spinner")
    }
}

data class WidgetSpinnerInfo(
    val addressDevice: Int,
    val parameterID: Int,
    val spinner: com.skydoves.powerspinner.PowerSpinnerView,
    var items: List<String>
)