package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BluetoothLeService.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.data.local.OpticTrainingStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SwitchItem
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SwitcherDelegateAdapter(
    val onSwitchClick: (addressDevice: Int, parameterID: Int, switchState: Boolean) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<SwitchItem, Ubi4WidgetSwitcherBinding>(
        Ubi4WidgetSwitcherBinding::inflate
    ) {
    // RxJava
    private val disposables = CompositeDisposable()
    private val rxUpdateMainEvent = RxUpdateMainEventUbi4.getInstance()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSwitchInfo: ArrayList<WidgetSwitchInfo> = ArrayList()

    private lateinit var _indicatorOpticStreamIv: ImageView
    private var timer: CountDownTimer? = null

    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItem) {
        onDestroyParent { onDestroy() }
        _indicatorOpticStreamIv = indicatorOpticStreamIv
        var addressDevice = 0
        var parameterID = 0
        var switchChecked = false


        when (item.widget) {
            is SwitchParameterWidgetEStruct -> {
                addressDevice = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                switchChecked = item.widget.switchChecked
            }

            is SwitchParameterWidgetSStruct -> {
                addressDevice = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                switchChecked = item.widget.switchChecked
            }
        }

        // Здесь происходит начальная конфигурация UI
        widgetSwitchSc.isChecked = switchChecked
        widgetDescriptionTv.text = item.title
        Log.d("TestTitle", "${item.title}")
        if (item.title.contains("START LERNING")) {
            opticLearnCollect()
            indicatorOpticStreamIv.visibility = View.VISIBLE
        }


        widgetSwitchSc.setOnCheckedChangeListener { _, isChecked ->
            onSwitchClick(addressDevice, parameterID, isChecked)
        }

        widgetSwitchInfo.add(WidgetSwitchInfo(addressDevice, parameterID, switchChecked, widgetSwitchSc))

        Handler().postDelayed({
//            main.bleCommandWithQueue(
//                BLECommands.requestSwitcher(addressDevice, parameterID),
//                MAIN_CHANNEL,
//                SampleGattAttributes.WRITE)
        }, 500)

        switchCollect()
    }

    private fun switchCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.switcherFlow.collect { parameterRef ->
                    val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
                    Log.d(
                        "SwitcherCollect",
                        "addressDevice = ${parameterRef.addressDevice}, parameterID = ${parameterRef.parameterID}, parameter.data = ${parameter.data}"
                    )
                    Log.d(
                        "SwitcherCollect",
                        "значение свича ${castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte()) != 0}"
                    )
                    Log.d(
                        "SwitcherCollect",
                        "Index меняемого свича ${getIndexWidgetSwitch(parameterRef.addressDevice, parameterRef.parameterID)}"
                    )
                    if (parameter.data.isNotEmpty()) {
                        widgetSwitchInfo[getIndexWidgetSwitch(
                            parameterRef.addressDevice,
                            parameterRef.parameterID
                        )].isChecked = castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte()) != 0
                        widgetSwitchInfo[getIndexWidgetSwitch(parameterRef.addressDevice, parameterRef.parameterID)].widgetSwitch.isChecked = widgetSwitchInfo[getIndexWidgetSwitch(parameterRef.addressDevice, parameterRef.parameterID)].isChecked
                    }
                }
            }
        }
    }
    private fun opticLearnCollect() {
        val opticStreamDisposable = rxUpdateMainEvent.uiOpticTrainingObservable
            .compose(main.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameterRef ->
                onDataPacketReceived()
            }
        disposables.add(opticStreamDisposable)
    }
    private fun onDataPacketReceived() {
        // Сбрасываем предыдущий таймер
        _indicatorOpticStreamIv.setImageDrawable(main.resources.getDrawable(R.drawable.circle_16_green))
        timer?.cancel()
        // Запускаем новый таймер на 100 мс
        timer = object : CountDownTimer(100, 100) {
            override fun onTick(millisUntilFinished: Long) = Unit

            override fun onFinish() {
                _indicatorOpticStreamIv.setImageDrawable(main.resources.getDrawable(R.drawable.circle_16_red))
            }
        }.start()
    }

    private fun getIndexWidgetSwitch(addressDevice: Int, parameterID: Int): Int {
        widgetSwitchInfo.forEachIndexed { index, widgetSliderInfo ->
            if (widgetSliderInfo.addressDevice == addressDevice && widgetSliderInfo.parameterID == parameterID) {
                return index
            }
        }
        return -1
    }

    override fun isForViewType(item: Any): Boolean = item is SwitchItem
    override fun SwitchItem.getItemId(): Any = title
    fun onDestroy() {
        scope.cancel()
        disposables.clear()
        Log.d("onDestroy" , "onDestroy swich")
    }
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
data class WidgetSwitchInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var isChecked: Boolean = false,
    var widgetSwitch: Switch
)
