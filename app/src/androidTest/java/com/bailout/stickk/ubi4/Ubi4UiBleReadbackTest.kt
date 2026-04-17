package com.bailout.stickk.ubi4

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.bailout.stickk.R
import com.bailout.stickk.scan.view.ScanActivity
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.PlotThresholds
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.PlotItem
import com.bailout.stickk.ubi4.models.widgets.SliderItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.serialization.json.Json
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class Ubi4UiBleReadbackTest {

    @get:org.junit.Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    @Test
    fun changeOpenThreshold_andVerifyReadback() {
        connectToDeviceFromScan()
        val thresholdBinding = resolveOpenThresholdBinding()

        onView(withId(R.id.homeRv)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(withId(R.id.open_CH_v))
            )
        )
        val uiSet = trySetOpenThresholdUiToTarget(TARGET_VALUE, timeoutMs = 45_000)
        if (!uiSet) {
            val closeThresholdUi = readTextFromView(withId(R.id.close_threshold_tv)).toIntOrNull() ?: 0
            sendOpenThresholdViaBle(thresholdBinding, TARGET_VALUE, closeThresholdUi)
        }

        waitForCondition("ожидание UI порога открытия = $TARGET_VALUE", 90_000) {
            readTextFromView(withId(R.id.open_threshold_tv)).toIntOrNull() == TARGET_VALUE
        }
        SystemClock.sleep(600)

        val bleValue = requestThresholdAndReadBack(thresholdBinding, expectedValue = TARGET_VALUE)
        val uiValue = readTextFromView(withId(R.id.open_threshold_tv)).toInt()

        assertEquals("UI значение порога не совпало с BLE read-back", uiValue, bleValue)
        assertEquals("Порог должен быть установлен в $TARGET_VALUE", TARGET_VALUE, bleValue)
    }

    @Test
    fun changeGlobalSensitivity_andVerifyReadback() {
        runSliderScenario(title = GLOBAL_SENSITIVITY_TITLE)
    }

    @Test
    fun changeGlobalForce_andVerifyReadback() {
        runSliderScenario(title = GLOBAL_FORCE_TITLE)
    }

    private fun runSliderScenario(title: String) {
        connectToDeviceFromScan()
        waitForView(withText(title), 90_000)

        val binding = resolveSliderBinding(title)
        val initialBleValue = requestSliderAndReadBack(binding, expectedDisplayValue = null)
        waitForCondition("синхронизация UI '$title' с BLE", 45_000) {
            readSliderUiValue(title) == initialBleValue
        }
        val uiSet = trySetSliderUiToTarget(title, TARGET_VALUE, timeoutMs = 45_000)
        if (!uiSet) {
            sendSliderValueViaBle(binding, TARGET_VALUE)
        }
        waitForCondition("ожидание UI '$title' = $TARGET_VALUE", 120_000) {
            readSliderUiValue(title) == TARGET_VALUE
        }
        SystemClock.sleep(700)

        val bleValue = requestSliderAndReadBack(binding, expectedDisplayValue = TARGET_VALUE)
        val uiValue = readSliderUiValue(title)

        assertEquals("UI значение '$title' не совпало с BLE read-back", uiValue, bleValue)
        assertEquals("Параметр '$title' должен быть установлен в $TARGET_VALUE", TARGET_VALUE, bleValue)
    }

    private fun connectToDeviceFromScan() {
        ActivityScenario.launch(ScanActivity::class.java)
        waitForCondition("старт ScanActivity или авто-переход в MainActivityUBI4", 30_000) {
            resumedActivity<MainActivityUBI4>() != null || resumedActivity<ScanActivity>() != null
        }

        if (resumedActivity<MainActivityUBI4>() == null) {
            waitForCondition("обнаружение целевого устройства в scan_list/данных ScanActivity", 90_000) {
                hasScanCandidateInData() || isScanCandidateVisible()
            }

            val clicked = clickAnyScanCandidate()
            if (!clicked) {
                forceConnectFromScanActivity()
            }

            val transitionedAfterUiClick = waitUntil(20_000) {
                resumedActivity<MainActivityUBI4>() != null
            }

            if (!transitionedAfterUiClick) {
                waitForCondition("fallback-подключение к UBI4 из ScanActivity", 30_000) {
                    resumedActivity<MainActivityUBI4>() != null || forceConnectFromScanActivity()
                }
            }
        }

        waitForCondition("переход в MainActivityUBI4", 90_000) {
            resumedActivity<MainActivityUBI4>() != null
        }
        waitForView(withId(R.id.homeRv), 90_000)
        waitForCondition("инициализация UBI4 виджетов", 90_000) {
            readPreparedData(DISPLAY_SENSORS).isNotEmpty()
        }
    }

    private fun clickAnyScanCandidate(): Boolean {
        SCAN_DEVICE_CANDIDATES.forEach { candidate ->
            try {
                onView(withId(R.id.scan_list)).perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(allOf(withId(R.id.title_tv), withText(containsString(candidate)))),
                        ViewActions.click()
                    )
                )
                return true
            } catch (_: Throwable) {
                // try next candidate
            }
        }
        return false
    }

    private fun hasScanCandidateInData(): Boolean {
        val scan = resumedActivity<ScanActivity>() ?: return false
        var found = false
        instrumentation.runOnMainSync {
            found = findScanCandidateDevice(scan) != null
        }
        return found
    }

    private fun isScanCandidateVisible(): Boolean {
        return SCAN_DEVICE_CANDIDATES.any { candidate ->
            try {
                onView(withId(R.id.scan_list)).check(
                    matches(
                        hasDescendant(
                            allOf(withId(R.id.title_tv), withText(containsString(candidate)))
                        )
                    )
                )
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun forceConnectFromScanActivity(): Boolean {
        val scan = resumedActivity<ScanActivity>() ?: return false
        var connected = false
        instrumentation.runOnMainSync {
            val device = findScanCandidateDevice(scan)
            if (device != null) {
                scan.navigateToLEChart("device", device)
                connected = true
            }
        }
        return connected
    }

    private fun findScanCandidateDevice(scan: ScanActivity): BluetoothDevice? {
        return scan.leDevices.firstOrNull { device ->
            val name = device.name ?: return@firstOrNull false
            SCAN_DEVICE_CANDIDATES.any { candidate -> name.contains(candidate) }
        }
    }

    private fun requestSliderAndReadBack(
        binding: SliderBinding,
        expectedDisplayValue: Int?
    ): Int {
        var lastKnown = Int.MIN_VALUE

        waitForCondition("BLE read-back слайдера pid=${binding.parameterId}", 30_000) {
            requestSlider(binding.addressDevice, binding.parameterId)
            val rawValue = parseSliderRawValue(binding)
            if (rawValue != null) {
                val displayValue = binding.toDisplayValue(rawValue)
                lastKnown = displayValue
                expectedDisplayValue == null || displayValue == expectedDisplayValue
            } else {
                false
            }
        }

        return lastKnown
    }

    private fun trySetSliderUiToTarget(title: String, target: Int, timeoutMs: Long): Boolean {
        return waitUntil(timeoutMs) {
            val current = readSliderUiValue(title)
            if (current == target) return@waitUntil true
            val buttonId = if (current < target) R.id.plusBtnRipple else R.id.minusBtnRipple
            onView(withId(R.id.homeRv)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(title)),
                    clickChildViewWithId(buttonId)
                )
            )
            SystemClock.sleep(220)
            false
        }
    }

    private fun requestThresholdAndReadBack(
        binding: ThresholdBinding,
        expectedValue: Int?
    ): Int {
        var lastKnown = Int.MIN_VALUE

        waitForCondition("BLE read-back порога pid=${binding.parameterId}", 40_000) {
            requestThreshold(binding.addressDevice, binding.parameterId)
            val value = parseOpenThreshold(binding)
            if (value != null) {
                lastKnown = value
                expectedValue == null || value == expectedValue
            } else {
                false
            }
        }

        return lastKnown
    }

    private fun trySetOpenThresholdUiToTarget(target: Int, timeoutMs: Long): Boolean {
        return waitUntil(timeoutMs) {
            val current = readTextFromView(withId(R.id.open_threshold_tv)).toIntOrNull() ?: return@waitUntil false
            if (current == target) return@waitUntil true

            val correction = (target - current).coerceIn(-30, 30)
            val calibrationTarget = (target + correction).coerceIn(0, 255)
            onView(withId(R.id.open_CH_v)).perform(clickAtThresholdValue(calibrationTarget))
            SystemClock.sleep(300)
            false
        }
    }

    private fun sendSliderValueViaBle(binding: SliderBinding, targetDisplayValue: Int) {
        val rawTarget = binding.toRawValue(targetDisplayValue).coerceIn(binding.minProgress, binding.maxProgress)
        withMainActivity { main ->
            main.bleCommandWithQueue(
                BLECommands.sendSliderCommand(
                    binding.addressDevice,
                    binding.parameterId,
                    arrayListOf(rawTarget)
                ),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}
        }
        SystemClock.sleep(500)
    }

    private fun sendOpenThresholdViaBle(binding: ThresholdBinding, openThreshold: Int, closeThreshold: Int) {
        withMainActivity { main ->
            main.bleCommandWithQueue(
                BLECommands.sendThresholdsCommand(
                    binding.addressDevice,
                    binding.parameterId,
                    arrayListOf(openThreshold, 0, closeThreshold, 0)
                ),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}
        }
        SystemClock.sleep(700)
    }

    private fun requestSlider(addressDevice: Int, parameterId: Int) {
        withMainActivity { main ->
            main.bleCommandWithQueue(
                BLECommands.requestSlider(addressDevice, parameterId),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}
        }
        SystemClock.sleep(400)
    }

    private fun requestThreshold(addressDevice: Int, parameterId: Int) {
        withMainActivity { main ->
            main.bleCommandWithQueue(
                BLECommands.requestThresholds(addressDevice, parameterId),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}
        }
        SystemClock.sleep(400)
    }

    private fun parseSliderRawValue(binding: SliderBinding): Int? {
        val parameter = ParameterProvider.getParameter(binding.addressDevice, binding.parameterId)
        val raw = parameter.data
        if (raw.isBlank()) return null

        val sizeOf = PreferenceKeysUbi4.ParameterTypeEnum.entries
            .getOrNull(parameter.type)
            ?.sizeOf
            ?: return null
        if (sizeOf <= 0) return null

        val start = (sizeOf * binding.dataOffset) * 2
        val end = (sizeOf * (binding.dataOffset + 1)) * 2
        if (start < 0 || end > raw.length || start >= end) return null

        val hex = raw.substring(start, end)
        val byteValue = hex.toInt(16).toByte()

        return if (parameter.type == PreferenceKeysUbi4.ParameterTypeEnum.PARTE_INT8_TYPE.number) {
            byteValue.toInt()
        } else {
            byteValue.toInt() and 0xFF
        }
    }

    private fun parseOpenThreshold(binding: ThresholdBinding): Int? {
        val raw = ParameterProvider.getParameter(binding.addressDevice, binding.parameterId).data
        if (raw.isBlank()) return null
        return runCatching {
            Json.decodeFromString<PlotThresholds>("\"$raw\"").threshold1
        }.getOrNull() ?: parseHexThresholdByte(raw, byteOffset = 0)
    }

    private fun parseHexThresholdByte(raw: String, byteOffset: Int): Int? {
        val compact = raw.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        val start = byteOffset * 2
        val end = start + 2
        if (end > compact.length) return null
        return compact.substring(start, end).toIntOrNull(16)
    }

    private fun readSliderUiValue(title: String): Int {
        var valueText: String? = null
        onView(withId(R.id.homeRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(title)),
                captureChildText(R.id.widgetSliderNumTv) { valueText = it }
            )
        )

        val raw = requireNotNull(valueText) { "UI значение для '$title' не найдено" }
        val parsed = parseDisplayedInt(raw)
        Log.d("Ubi4UiBleReadbackTest", "UI[$title] raw='$raw' parsed=$parsed")
        return parsed
    }

    private fun resolveSliderBinding(title: String): SliderBinding {
        val sliderItem = readPreparedData(DISPLAY_SENSORS)
            .filterIsInstance<SliderItem>()
            .firstOrNull { it.title == title }
            ?: error("Не найден слайдер '$title' в UiState.listWidgets")

        var info: com.bailout.stickk.ubi4.models.commonModels.ParameterInfo<Int, Int, Int, Int>? = null
        var increment = 1.0f
        var minProgress = 0
        var maxProgress = 100
        when (val widget = sliderItem.widget) {
            is SliderParameterWidgetEStruct -> {
                info = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
                increment = widget.increment
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
            }
            is SliderParameterWidgetSStruct -> {
                info = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
                increment = widget.increment
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
            }
        }
        val parameterInfo = info ?: error("Не найдены BLE параметры для '$title'")

        return SliderBinding(
            addressDevice = parameterInfo.deviceAddress,
            parameterId = parameterInfo.parameterID,
            dataOffset = parameterInfo.dataOffsets,
            increment = increment,
            minProgress = minProgress,
            maxProgress = maxProgress
        )
    }

    private fun resolveOpenThresholdBinding(): ThresholdBinding {
        val plotItem = readPreparedData(DISPLAY_SENSORS)
            .filterIsInstance<PlotItem>()
            .firstOrNull { item ->
                extractPlotParameterInfos(item.widget).any { it.dataCode == PDCE_OPEN_CLOSE_THRESHOLD.number }
            }
            ?: error("Не найден plot-виджет с порогами открытия/закрытия")

        val info = extractPlotParameterInfos(plotItem.widget)
            .firstOrNull { it.dataCode == PDCE_OPEN_CLOSE_THRESHOLD.number }
            ?: error("Не найден parameterInfo для OPEN_CLOSE_THRESHOLD")

        return ThresholdBinding(
            addressDevice = info.deviceAddress,
            parameterId = info.parameterID
        )
    }

    private fun extractPlotParameterInfos(widget: Any): Set<com.bailout.stickk.ubi4.models.commonModels.ParameterInfo<Int, Int, Int, Int>> {
        return when (widget) {
            is PlotParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet
            is PlotParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
            else -> emptySet()
        }
    }

    private fun withMainActivity(action: (MainActivityUBI4) -> Unit) {
        val activity = resumedActivity<MainActivityUBI4>()
            ?: error("MainActivityUBI4 не находится в стадии RESUMED")
        instrumentation.runOnMainSync { action(activity) }
    }

    private fun <T : Activity> resumedActivity(clazz: Class<T>): T? {
        var result: T? = null
        instrumentation.runOnMainSync {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            result = resumed.firstOrNull { clazz.isInstance(it) } as? T
        }
        return result
    }

    private inline fun <reified T : Activity> resumedActivity(): T? = resumedActivity(T::class.java)

    private fun waitForView(matcher: Matcher<View>, timeoutMs: Long) {
        waitForCondition("view $matcher", timeoutMs) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun waitForCondition(description: String, timeoutMs: Long, check: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var lastError: Throwable? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                if (check()) return
            } catch (t: Throwable) {
                lastError = t
            }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        if (lastError != null) {
            throw AssertionError("Таймаут ожидания: $description", lastError)
        }
        throw AssertionError("Таймаут ожидания: $description")
    }

    private fun waitUntil(timeoutMs: Long, check: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                if (check()) return true
            } catch (_: Throwable) {
                // ignore and keep waiting
            }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun readPreparedData(display: Int): List<Any> {
        var data: List<Any> = emptyList()
        instrumentation.runOnMainSync {
            data = DataFactory().prepareData(display)
        }
        return data
    }

    private fun readTextFromView(matcher: Matcher<View>): String {
        var text: String? = null
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(TextView::class.java)
            override fun getDescription(): String = "Read text from TextView"
            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: View
            ) {
                text = (view as TextView).text?.toString()
            }
        })
        return requireNotNull(text)
    }

    private fun parseDisplayedInt(raw: String): Int {
        val normalized = raw.replace(',', '.')
        val number = Regex("-?\\d+(\\.\\d+)?").find(normalized)?.value
            ?: error("Не удалось распарсить число из '$raw'")
        return number.toFloat().toInt()
    }

    private fun clickChildViewWithId(@IdRes childId: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = "Click child view with id=$childId"
            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: View
            ) {
                val child = view.findViewById<View>(childId)
                    ?: throw NoMatchingViewException.Builder()
                        .withRootView(view)
                        .withViewMatcher(withId(childId))
                        .build()
                ViewActions.click().perform(uiController, child)
            }
        }
    }

    private fun captureChildText(@IdRes childId: Int, callback: (String) -> Unit): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = "Capture child text from id=$childId"
            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: View
            ) {
                val child = view.findViewById<TextView>(childId)
                    ?: throw AssertionError("TextView id=$childId не найден в itemView")
                callback(child.text?.toString().orEmpty())
            }
        }
    }

    private fun clickAtThresholdValue(targetValue: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isAssignableFrom(View::class.java))
            override fun getDescription(): String = "Click on threshold lane to set value=$targetValue"
            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: View
            ) {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val x = location[0] + (view.width / 2f)
                val fraction = (1f - (targetValue / 255f)).coerceIn(0f, 1f)
                val y = location[1] + (view.height * fraction)

                val clickAction = GeneralClickAction(
                    Tap.SINGLE,
                    CoordinatesProvider { floatArrayOf(x, y) },
                    Press.FINGER,
                    InputDevice.SOURCE_TOUCHSCREEN,
                    MotionEvent.BUTTON_PRIMARY
                )
                clickAction.perform(uiController, view)
                uiController.loopMainThreadForAtLeast(500)
            }
        }
    }

    private data class SliderBinding(
        val addressDevice: Int,
        val parameterId: Int,
        val dataOffset: Int,
        val increment: Float,
        val minProgress: Int,
        val maxProgress: Int
    ) {
        fun toDisplayValue(rawValue: Int): Int {
            return (rawValue * increment).toInt()
        }

        fun toRawValue(displayValue: Int): Int {
            if (increment == 0f) return displayValue
            return (displayValue / increment).roundToInt()
        }
    }

    private data class ThresholdBinding(
        val addressDevice: Int,
        val parameterId: Int
    )

    private companion object {
        private val SCAN_DEVICE_CANDIDATES = listOf(
            "UBIv4_CPU6",
            "CPU6",
            "FTHS3-Роман",
            "FTHS3-Roman",
            "Роман",
            "Roman",
            "UBIv4"
        )
        private const val GLOBAL_SENSITIVITY_TITLE = "Общая чувствительность"
        private const val GLOBAL_FORCE_TITLE = "Общая сила"
        private const val TARGET_VALUE = 50
        private const val DISPLAY_SENSORS = 1
        private const val POLL_INTERVAL_MS = 600L

        private val instrumentation get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
    }
}
