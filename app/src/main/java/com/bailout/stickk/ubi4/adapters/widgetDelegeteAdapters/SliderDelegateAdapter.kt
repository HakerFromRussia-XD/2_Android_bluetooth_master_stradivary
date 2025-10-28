package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.AndroidContextProvider.context
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState.slidersFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.widgets.SliderItem
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.logging.systemLang
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancelChildren
import java.util.concurrent.atomic.AtomicBoolean
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4


class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: ArrayList<Int>) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0
    private var timer: CountDownTimer? = null
    private var isAttached = false

    private var collectJob: kotlinx.coroutines.Job? = null



    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        Log.d("SliderAdapterTest", "onBind RUN")
        onDestroyParent { onDestroy() }
        isAttached = true

        var addressDevice = 0
        var parameterID = 0
        var dataCode = 0
        val dataOffset: ArrayList<Int> = ArrayList()
        var minProgress = 0
        var maxProgress = 0
        var widgetPosition = 0

        when (val widget = item.widget) {
            is SliderParameterWidgetEStruct -> {
                // Берем данные из первого элемента набора параметров
                addressDevice = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).parameterID

                dataCode = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).dataCode
                // Собираем данные для всех параметров (например, dataOffset и dataCode)
                widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    dataOffset.add(it.dataOffset)

                }
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                widgetPosition = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition

                Log.d(
                    "addressDevice",
                    "E struct: addressDevice = $addressDevice   ${widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId}"
                )
            }
            is SliderParameterWidgetSStruct -> {
                addressDevice = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).parameterID
                dataCode = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).dataCode
                widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    dataOffset.add(it.dataOffset)
                }
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress

                Log.d(
                    "addressDevice",
                    "S struct: addressDevice = $addressDevice   ${widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId}"
                )
            }
        }

        // Количество параметров определяем количеством dataOffset
        val paramCount = dataOffset.size
        val initialProgress = MutableList(paramCount) { 0 }
        val currentSliderInfo = WidgetSliderInfo(
            addressDevice = addressDevice,
            parameterID = parameterID,
            dataOffset = dataOffset,
            minProgress = minProgress,
            maxProgress = maxProgress,
            progress = ArrayList(initialProgress),
            widgetSlidersSb = arrayListOf(widgetSliderSb, widgetSlider2Sb),
            widgetSliderNumTv = arrayListOf(widgetSliderNumTv, widgetSliderNum2Tv),
            widgetPosition = widgetPosition
        )

//        currentSliderInfo.instanceId = sliderInfoCounter++
//        widgetSlidersInfo.removeAll { it.widgetPosition == widgetPosition }
//        widgetSlidersInfo.sortBy { it.widgetPosition }
//        widgetSlidersInfo.add(currentSliderInfo)

        currentSliderInfo.instanceId = sliderInfoCounter++
// Не удаляем по widgetPosition — разные слайдеры могут делить одну позицию!
        val removed = widgetSlidersInfo.removeAll {
            it.addressDevice == addressDevice && it.parameterID == parameterID
        }
        if (removed) Log.d("SliderMap", "Replaced widget for addr=$addressDevice pid=$parameterID")
        widgetSlidersInfo.add(currentSliderInfo)
        Log.d("SliderMap", "Added widget: addr=$addressDevice pid=$parameterID pos=$widgetPosition; total=${widgetSlidersInfo.size}")

        // Cache-first draw to avoid showing 0 if the event already arrived earlier
        run {
            val ref = ParameterRef(addressDevice, parameterID, dataCode)
            val cached = ParameterProvider.getParameter(addressDevice, parameterID)
            if (cached.data.isNotEmpty()) {
                setUI(ref)
            }
        }

        sliderCollect()

        // Получаем индекс текущего виджета по значению device и parameter
        val indexWidgetSlider = getIndexWidgetSlider(addressDevice, parameterID)
        val range = if (maxProgress == minProgress) 100 else maxProgress - minProgress

        // Настраиваем слайдеры: если параметров больше одного, показываем второй слайдер
        widgetSliderSb.max = range
        if (paramCount > 1) {
            widgetSlider2Sb.max = range
            secondSliderCl.visibility = View.VISIBLE
            widgetSlider2Sb.progress = currentSliderInfo.progress[1]
            widgetSliderNum2Tv.text = currentSliderInfo.progress[1].toString()
        } else {
            secondSliderCl.visibility = View.GONE
        }

        widgetSliderSb.progress = currentSliderInfo.progress[0]
        widgetSliderNumTv.text = currentSliderInfo.progress[0].toString()
//        widgetSliderTitleTv.text = item.title
        val sliderE = item.widget as? SliderParameterWidgetEStruct
        if (sliderE != null) {
            val b = sliderE.baseParameterWidgetEStruct.baseParameterWidgetStruct
            val labelKey = (b.deviceId shl 16) or (b.widgetId shl 8) or b.widgetCode
            val labelsByOffset = UiState.labelCodesByOffset[labelKey]

            val langKey = if (systemLang().startsWith("ru", true)) "ru" else "en"
            val dict = PreferenceKeysUbi4.parameterWidgetLabel[langKey]
                ?: PreferenceKeysUbi4.parameterWidgetLabel["en"].orEmpty()

            fun resolve(off: Int?): String? =
                off?.let { labelsByOffset?.get(it) }?.let { code -> dict[code.toString()] }

            // Поддерживаем столько заголовков, сколько есть TextView
            val titleViews = listOf(widgetSliderTitleTv, widgetSliderTitle2Tv)
            titleViews.forEachIndexed { idx, tv ->
                val text = resolve(dataOffset.getOrNull(idx))
                    ?: if (idx == 0) item.title else tv.text
                runCatching { tv.text = text }
            }
        } else {
            // S‑структуры: берём заголовок как есть
            widgetSliderTitleTv.text = item.title
        }





        // Обработчик первого слайдера
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = (seekBar.progress + widgetSlidersInfo[indexWidgetSlider].minProgress).toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                widgetSlidersInfo[indexWidgetSlider].progress[0]  = seekBar.progress + widgetSlidersInfo[indexWidgetSlider].minProgress
                Log.d("SliderSend", "→ send onSetProgress(address=$addressDevice, param=$parameterID, progress=${widgetSlidersInfo[indexWidgetSlider].progress})")
                onSetProgress(addressDevice, parameterID,  widgetSlidersInfo[indexWidgetSlider].progress )
            }
        })

        // Обработчик второго слайдера (если доступен)
        if (paramCount > 1) {
            widgetSlider2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val idx = getIndexWidgetSlider(addressDevice, parameterID)
                    if (idx != -1 && widgetSlidersInfo.getOrNull(idx)?.progress?.size ?: 0 > 1) {
                        widgetSliderNum2Tv.text = (seekBar.progress + widgetSlidersInfo[idx].minProgress).toString()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val idx = getIndexWidgetSlider(addressDevice, parameterID)
                    val info = widgetSlidersInfo.getOrNull(idx)
                    if (info != null && info.progress.size > 1) {
                        val min = info.minProgress
                        info.progress[1] = seekBar.progress + min
                        Log.d("SliderSend", "→ send onSetProgress(address=$addressDevice, param=$parameterID, progress=${info.progress})")
                        onSetProgress(addressDevice, parameterID, info.progress)
                    } else {
                        Log.w("Slider", "Ignore second slider touch: paramCount <= 1 or missing info")
                    }
                }
            })
        }

        // Кнопки инкремента и декремента для каждого слайдера
        minusBtnRipple.setOnClickListener {
            updateSliderProgress(widgetPosition, sliderIndex = 0, step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple.setOnClickListener {
            updateSliderProgress(widgetPosition, sliderIndex = 0, step = +1, indexWidgetSlider = indexWidgetSlider)
        }
        if (paramCount > 1) {
            minusBtnRipple2.setOnClickListener {
                updateSliderProgress(widgetPosition, sliderIndex = 1, step = -1, indexWidgetSlider = indexWidgetSlider)
            }
            plusBtnRipple2.setOnClickListener {
                updateSliderProgress(widgetPosition, sliderIndex = 1, step = +1, indexWidgetSlider = indexWidgetSlider)
            }
        } else {
            minusBtnRipple2.setOnClickListener(null)
            plusBtnRipple2.setOnClickListener(null)
        }
        // Cache-first + Refresh: if cache is empty -> request with retries; otherwise UI already set
        run {
            val cachedNow = ParameterProvider.getParameter(addressDevice, parameterID)
            if (cachedNow.data.isEmpty()) {
                currentSliderInfo.responseReceived.set(false)
                RetryUtils.sendRequestWithRetry(
                    request = {
                        Log.d("SliderDebugTest!", "=============================================")
                        Log.d("SliderDebugTest!", "1 - addressDevice = $addressDevice, parameterID = $parameterID")
                        main.bleCommandWithQueue(
                            BLECommands.requestSlider(addressDevice, parameterID),
                            MAIN_CHANNEL_CHARACTERISTIC,
                            SampleGattAttributes.WRITE
                        ) {}
                    },
                    isResponseReceived = { currentSliderInfo.responseReceived.get() },
                    maxRetries = 5,
                    delayMillis = 400L,
                    scope = scope
                )
                Log.d("RequestUtils", "Запрос отправлен: кэш пуст. deviceAddress=$addressDevice, parameterId=$parameterID")
            } else {
                // Данные уже есть – убеждаемся, что не будем ретраить зря
                currentSliderInfo.responseReceived.set(true)
                Log.d("RequestUtils", "Кэш уже заполнен: deviceAddress=$addressDevice, parameterId=$parameterID, data='${cachedNow.data}'")
            }
        }

    }

    private fun updateSliderProgress(widgetPosition: Int, sliderIndex: Int, step: Int, indexWidgetSlider: Int) {
        val sliderInfo = widgetSlidersInfo.find { it.widgetPosition == widgetPosition }
        if (sliderInfo == null) {
            Log.e("updateSliderProgress", "Не найден sliderInfo для widgetPosition = $widgetPosition")
            return
        }
        val currentValue = sliderInfo.progress.getOrNull(sliderIndex)
        if (currentValue == null) {
            Log.e("updateSliderProgress", "Нет значения progress для sliderIndex = $sliderIndex")
            return
        }
        var newValue = currentValue + step
        val minProgress = sliderInfo.minProgress
        val effectiveMax = if (minProgress == sliderInfo.maxProgress) 100 else sliderInfo.maxProgress
        newValue = newValue.coerceIn(minProgress, effectiveMax)
        sliderInfo.progress[sliderIndex] = newValue
        sliderInfo.widgetSlidersSb.getOrNull(sliderIndex)?.progress = newValue - minProgress
        sliderInfo.widgetSliderNumTv.getOrNull(sliderIndex)?.text = (newValue).toString()
        timer?.cancel()
        timer = object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                onSetProgress(sliderInfo.addressDevice, sliderInfo.parameterID, sliderInfo.progress)
            }
        }.start()

    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            slidersFlow.collect { parameterRef ->
                val idx = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
                val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)


                val info = widgetSlidersInfo.getOrNull(idx)
                Log.d(
                    "SliderProbe",
                    "flow addr=${parameterRef.addressDevice} pid=${parameterRef.parameterID} idx=$idx " +
                            "params=${info?.dataOffset?.size ?: -1} offsets=${info?.dataOffset}"
                )

                Log.d("SliderDebugTest!", "2 - addressDevice = ${parameterRef.addressDevice}, parameterID = ${parameterRef.parameterID} parameterData = ${parameter.data}")
                Log.d("SliderFlow", "addr=${parameterRef.addressDevice}, pid=${parameterRef.parameterID}, data=${parameter.data}, hasIdx=${idx != -1}")
                if (idx != -1) setUI(parameterRef) else Log.d("SliderFlow", "skip update: adapter doesn't have widget for addr=${parameterRef.addressDevice}, pid=${parameterRef.parameterID}")
            }
        }
    }

    private fun setUI(parameterRef: ParameterRef) {
        val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
        Log.d("setUITest", "ParameterRef = $parameterRef, parameter = $parameter")
        Log.d("parameter sliderCollect", "перед обновлением слайдера: addressDevice = ${parameterRef.addressDevice}, parameterID = ${parameterRef.parameterID}, data=${parameter.data}")

        val indexWidgetSlider = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
        if (indexWidgetSlider != -1 && indexWidgetSlider < widgetSlidersInfo.size) {
            try {
                val sizeOf = PreferenceKeysUbi4.ParameterTypeEnum.entries[parameter.type].sizeOf
                widgetSlidersInfo[indexWidgetSlider].dataOffset.forEachIndexed { index, dataOffset ->
                    Log.d("SliderDebug", "Слайдер[$index]: sizeOf=$sizeOf, data.length=${parameter.data.length}")
                    if (parameter.data.isNotEmpty()) {
                        val oldProgress = widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index].progress

                        var newValue = castUnsignedCharToInt(
                            parameter.data.substring((sizeOf * dataOffset) * 2, sizeOf * (dataOffset + 1) * 2).toInt(16).toByte()
                        )
                        if (parameter.type == PreferenceKeysUbi4.ParameterTypeEnum.PARTE_INT8_TYPE.number){
                            newValue = parameter.data.substring((sizeOf * dataOffset) * 2, sizeOf * (dataOffset + 1) * 2).toInt(16).toByte().toInt()
                        }
                        widgetSlidersInfo[indexWidgetSlider].progress[index] = newValue
                        animateProgressBar(widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index], oldProgress, newValue - widgetSlidersInfo[indexWidgetSlider].minProgress)
                        widgetSlidersInfo[indexWidgetSlider].widgetSliderNumTv[index].text = newValue.toString()
                    }
                    // Обновляем отображение
                    widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index].progress =
                        widgetSlidersInfo[indexWidgetSlider].progress[index] - widgetSlidersInfo[indexWidgetSlider].minProgress
                }
            } catch (e: Exception) {
                Log.e("SliderDebug", "Ошибка при обработке данных: ${e.message}", e)
            } finally {
                widgetSlidersInfo[indexWidgetSlider].responseReceived.set(true)
                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.forEach { it?.cancel() }
                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.clear()
                Log.d("SliderDebug", "Установлен флаг responseReceived=true для слайдера с индексом $indexWidgetSlider")
            }
        } else {
            Log.d("parameter sliderCollect", "НЕТ слайдера для: addr=${parameterRef.addressDevice}, pid=${parameterRef.parameterID}. Всего=${widgetSlidersInfo.size}")
            widgetSlidersInfo.forEachIndexed { i, it ->
                Log.d("SliderMap", "MISS setUI for addr=${parameterRef.addressDevice} pid=${parameterRef.parameterID}")
                Log.d("parameter sliderCollect", "[$i] addr=${it.addressDevice}, pid=${it.parameterID}, pos=${it.widgetPosition}, min=${it.minProgress}, max=${it.maxProgress}")
            }
        }
        Log.d("SliderDebug", "Received parameter.data = '${parameter.data}', длина = ${parameter.data.length}")
    }

    private fun animateProgressBar(progressBar: ProgressBar, from: Int, to: Int) {
        if (from == to) return
        ValueAnimator.ofInt(from, to).apply {
            duration = DURATION_ANIMATION
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }

private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
    val idx = widgetSlidersInfo.indexOfFirst {
        it.addressDevice == addressDevice && it.parameterID == parameterID
    }
    if (idx == -1) {
        Log.d("SliderMap", "Not found: addr=$addressDevice pid=$parameterID; listSize=${widgetSlidersInfo.size}")
    }
    return idx
}

    override fun isForViewType(item: Any): Boolean = item is SliderItem
    override fun SliderItem.getItemId(): Any = when (val w = widget) {
        is SliderParameterWidgetEStruct -> {
            val s = w.baseParameterWidgetEStruct.baseParameterWidgetStruct
            val addr = s.parameterInfoSet.elementAt(0).deviceAddress
            val pid = s.parameterInfoSet.elementAt(0).parameterID
            val pos = s.widgetPosition
            "slider-$addr-$pid-$pos"
        }
        is SliderParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val addr = s.parameterInfoSet.elementAt(0).deviceAddress
            val pid = s.parameterInfoSet.elementAt(0).parameterID
            val pos = s.widgetPosition
            "slider-$addr-$pid-$pos"
        }
        else -> title
    }

    fun onDestroy() {
        Log.d("SliderAdapterTest", "onDestroy slider")
        isAttached = false
        timer?.cancel()
        timer = null
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
    }
}

data class WidgetSliderInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var dataOffset: ArrayList<Int> = ArrayList(),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var progress: ArrayList<Int> = ArrayList(),
    var widgetSlidersSb: ArrayList<ProgressBar>,
    var widgetSliderNumTv: ArrayList<TextView>,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ArrayList<ValueAnimator?> = ArrayList()
)