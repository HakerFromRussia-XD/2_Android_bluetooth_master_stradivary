package com.bailout.stickk.ubi4.ui.fragments.account.statisticsFragmentV3

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentAccountStatisticsV3Binding
import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.telemetryGestureCountersFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.widgets.GestureUsageChartItem
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider
import kotlinx.coroutines.launch

class AccountFragmentStatisticsV3 : Fragment() {

    private var _binding: Ubi4FragmentAccountStatisticsV3Binding? = null
    private val binding get() = requireNotNull(_binding)
    private var latestCounters = TelemetryGestureCounters()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentAccountStatisticsV3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gestureUsageChart.setTitle(getString(R.string.gesture_usage_chart_title))
        subscribeToData()
        requestTelemetryData()
    }

    private fun subscribeToData() {
        viewLifecycleOwner.lifecycleScope.launch {
            telemetryGestureCountersFlow.collect { counters ->
                latestCounters = counters
                renderChart()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            updateFlow.collect { renderChart() }
        }
    }

    private fun requestTelemetryData() {
        if (!UiState.isInterfaceV3Activated) return
        (activity as? MainActivityUBI4)?.getBLEController()?.requestTelemetryDataV3()
    }

    private fun renderChart() {
        binding.gestureUsageChart.setItems(latestCounters.toChartItems())
    }

    private fun TelemetryGestureCounters.toChartItems(): List<GestureUsageChartItem> {
        val baseItems = baseGestureMovementCount.mapIndexedNotNull { index, count ->
            val gestureId = BASE_GESTURE_IDS.getOrNull(index) ?: return@mapIndexedNotNull null
            if (gestureId == GestureEnum.GESTURE_NO_GESTURE.number || count <= 0L) return@mapIndexedNotNull null
            GestureUsageChartItem(gestureId, baseGestureName(gestureId), count)
        }
        val customItems = customGestureMovementCount.mapIndexedNotNull { index, count ->
            if (count <= 0L) return@mapIndexedNotNull null
            GestureUsageChartItem(
                gestureId = GestureEnum.GESTURE_CUSTOM_0.number + index,
                title = customGestureName(index),
                count = count
            )
        }
        return (baseItems + customItems)
            .sortedWith(compareByDescending<GestureUsageChartItem> { it.count }.thenBy { it.gestureId })
    }

    private fun baseGestureName(gestureId: Int): String =
        CollectionGesturesProvider.getGesture(gestureId).gestureName.takeIf { it.isNotBlank() }
            ?: "Gesture $gestureId"

    private fun customGestureName(index: Int): String {
        val defaultName = getString(CUSTOM_GESTURE_NAME_RES.getOrNull(index) ?: R.string.gesture_1_btn)
        val preferences = requireContext().getSharedPreferences(
            PreferenceKeysUbi4.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        val macKey = preferences.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "").orEmpty()
        return preferences.getString(
            PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + index,
            defaultName
        ) ?: defaultName
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        val BASE_GESTURE_IDS = listOf(
            GestureEnum.GESTURE_NO_GESTURE.number,
            GestureEnum.GESTURE_FIST.number,
            GestureEnum.GESTURE_POINT.number,
            GestureEnum.GESTURE_PINCH.number,
            GestureEnum.GESTURE_FIST_THUMB_OVER.number,
            GestureEnum.GESTURE_KEY.number,
            GestureEnum.GESTURE_ROCK.number,
            GestureEnum.GESTURE_TWIZZERS.number,
            GestureEnum.GESTURE_CUPHOLDER.number,
            GestureEnum.GESTURE_HALF_GRAB.number,
            GestureEnum.GESTURE_OK.number,
            GestureEnum.GESTURE_THUMB_UP.number,
            GestureEnum.GESTURE_MIDDLE_FINGER.number,
            GestureEnum.GESTURE_DOUBLE_POINT.number,
            GestureEnum.GESTURE_CALL_ME.number,
            GestureEnum.GESTURE_NATURAL_POSITION.number
        )

        val CUSTOM_GESTURE_NAME_RES = listOf(
            R.string.gesture_1_btn,
            R.string.gesture_2_btn,
            R.string.gesture_3_btn,
            R.string.gesture_4_btn,
            R.string.gesture_5_btn,
            R.string.gesture_6_btn,
            R.string.gesture_7_btn,
            R.string.gesture_8_btn,
            R.string.gesture_9_btn,
            R.string.gesture_10_btn,
            R.string.gesture_11_btn,
            R.string.gesture_12_btn,
            R.string.gesture_13_btn,
            R.string.gesture_14_btn,
            R.string.gesture_15_btn
        )
    }
}
