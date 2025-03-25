package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Switch
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.WidgetState.switcherFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SwitchItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.MobileSettingsKey
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

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
    private val responseReceived = AtomicBoolean(false)

    private lateinit var _indicatorOpticStreamIv: ImageView
    private var timer: CountDownTimer? = null

    override fun Ubi4WidgetSwitcherBinding.onBind(item: SwitchItem) {
        onDestroyParent { onDestroy() }
        _indicatorOpticStreamIv = indicatorOpticStreamIv
        var addressDevice = 0
        var parameterID = 0
        var parameterIDSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(
            ParameterInfo(0, 0, 0, 0)
        )
        var switchChecked = false
        var keyMobileSettings = ""


        when (val widget = item.widget) {
            is SwitchParameterWidgetEStruct -> {
                addressDevice = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterID = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).parameterID
                parameterIDSet = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet
                switchChecked = widget.switchChecked
                keyMobileSettings = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.keyMobileSettings
            }
            is SwitchParameterWidgetSStruct -> {
                addressDevice = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterID = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).parameterID
                parameterIDSet = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                switchChecked = widget.switchChecked
                keyMobileSettings = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.keyMobileSettings
            }
        }



        // Здесь происходит начальная конфигурация UI
        setUIMobileSettings(keyMobileSettings, widgetSwitchSc)
        if (keyMobileSettings == ""){
            widgetSwitchSc.isChecked = switchChecked
        }
        widgetDescriptionTv.text = item.title
        Log.d("TestTitle", "${item.title}")
        if (item.title.contains("START LERNING")) {
            opticLearnCollect()
            indicatorOpticStreamIv.visibility = View.VISIBLE
        }


        widgetSwitchSc.setOnCheckedChangeListener { _, isChecked ->
            Log.d("sendSwitcherState", "setOnCheckedChangeListener addressDevice: $addressDevice, parameterID: $parameterID  parameterIDSet: $parameterIDSet}")
            onSwitchClick(addressDevice, parameterID, isChecked)
            processingMobileSettings(keyMobileSettings, widgetSwitchSc)
        }

        widgetSwitchInfo.add(WidgetSwitchInfo(addressDevice, parameterID, switchChecked, widgetSwitchSc))

        responseReceived.set(false)
        RetryUtils.sendRequestWithRetry(
            request = {
                Log.d("SwitcherRequest", "addressDevice = $addressDevice parameterID = $parameterID")
                main.bleCommandWithQueue(
                    BLECommands.requestSwitcher(addressDevice, parameterID),
                    MAIN_CHANNEL,
                    WRITE
                ){}
            },
            isResponseReceived = {
                responseReceived.get()
            },
            maxRetries = 5,
            delayMillis = 1000L
        )
//        Handler().postDelayed({
//            main.bleCommandWithQueue(
//                BLECommands.requestSwitcher(addressDevice, parameterID),
//                MAIN_CHANNEL,
//                SampleGattAttributes.WRITE)
//        }, 500)

        switchCollect()
    }

    private fun processingMobileSettings(keyMobileSettings: String, switch: Switch) {
        if (keyMobileSettings != ""){
            when (keyMobileSettings) {
                MobileSettingsKey.AUTO_LOGIN.key -> { main.saveBoolean(PreferenceKeys.SET_MODE_SMART_CONNECTION, switch.isChecked) }
            }
        }
    }

    private fun setUIMobileSettings(keyMobileSettings: String, @SuppressLint("UseSwitchCompatOrMaterialCode") switch: Switch) {
        if (keyMobileSettings != ""){
            when (keyMobileSettings) {
                MobileSettingsKey.AUTO_LOGIN.key -> {
                    if (main.getBoolean(PreferenceKeys.SET_MODE_SMART_CONNECTION, false)) {
                        Log.d("setUIMobileSettings", "keyMobileSettings $keyMobileSettings ${main.getBoolean(PreferenceKeys.SET_MODE_SMART_CONNECTION, false)}")
                        switch.isChecked = true
                    }
                }
            }
        }
    }

    private fun switchCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                switcherFlow.collect { parameterRef ->
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
                        try {
                            widgetSwitchInfo[getIndexWidgetSwitch(
                                parameterRef.addressDevice,
                                parameterRef.parameterID
                            )].isChecked = castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte()) != 0
                            widgetSwitchInfo[getIndexWidgetSwitch(parameterRef.addressDevice, parameterRef.parameterID)].widgetSwitch.isChecked = widgetSwitchInfo[getIndexWidgetSwitch(parameterRef.addressDevice, parameterRef.parameterID)].isChecked

                        } catch (e:Exception){
                            Log.d("switchCollect","$e")
                        }
                    }
                    responseReceived.set(true)
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
                Log.d("OPTIC DATA", "data = ${ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID).data}")

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
    var widgetSwitch: Switch,
    var isMobileSettings: Boolean = false
    //TODO добавить информацию - чей виджет? наш/не наш
)
