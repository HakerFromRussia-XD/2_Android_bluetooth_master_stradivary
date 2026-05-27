package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentServiceBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters.GestureUsageWidgetItem
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters.SpinnerDelegateAdapter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.telemetryGestureCountersFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.widgets.GestureUsageChartItem
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceFragment: BaseWidgetsFragment() {

    private var _binding: Ubi4FragmentServiceBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 4
    private var latestGestureUsageItems: List<GestureUsageChartItem> = emptyList()


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentServiceBinding.inflate(inflater, container, false)

        setupRecycler()
        adapterWidgets.swapData(buildServiceData())
        subscribeUpdates()
        subscribeTelemetry()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestTelemetryData()
    }


    override fun onPause() {
        super.onPause()
        SpinnerDelegateAdapter.dismissAll()
    }

    private fun setupRecycler() {
        binding.serviceFragmentRv.layoutManager = LinearLayoutManager(requireContext())
        binding.serviceFragmentRv.adapter = adapterWidgets
    }

    private fun buildServiceData(): List<Any> =
        listOf(
            GestureUsageWidgetItem(
                title = getString(R.string.gesture_usage_chart_title),
                items = latestGestureUsageItems
            )
        ) + mDataFactory.prepareData(display)

    private fun updateServiceData() {
        val data = buildServiceData()
        if (binding.serviceFragmentRv.isComputingLayout) {
            binding.serviceFragmentRv.post { adapterWidgets.swapData(data) }
        } else {
            adapterWidgets.swapData(data)
        }
    }

    private fun subscribeUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            updateFlow.collect {
                withContext(Dispatchers.Main) {
                    updateServiceData()
                }
            }
        }
    }

    private fun subscribeTelemetry() {
        viewLifecycleOwner.lifecycleScope.launch {
            telemetryGestureCountersFlow.collect { counters ->
                latestGestureUsageItems = counters.toChartItems()
                updateServiceData()
            }
        }
    }

    private fun requestTelemetryData() {
        if (!UiState.isInterfaceV3Activated) return
        (activity as? MainActivityUBI4)?.getBLEController()?.requestTelemetryDataV3()
    }

    private fun TelemetryGestureCounters.toChartItems(): List<GestureUsageChartItem> {
        val baseItems = baseGestureMovementCount.mapIndexedNotNull { index, count ->
            val gestureId = BASE_GESTURE_IDS.getOrNull(index) ?: return@mapIndexedNotNull null
            if (gestureId == GestureEnum.GESTURE_NO_GESTURE.number || count <= 0L) return@mapIndexedNotNull null
            GestureUsageChartItem(
                gestureId = gestureId,
                title = baseGestureName(gestureId),
                count = count
            )
        }

        val customItems = customGestureMovementCount.mapIndexedNotNull { index, count ->
            if (count <= 0L) return@mapIndexedNotNull null
            val gestureId = GestureEnum.GESTURE_CUSTOM_0.number + index
            GestureUsageChartItem(
                gestureId = gestureId,
                title = customGestureName(index),
                count = count
            )
        }

        return (baseItems + customItems)
            .sortedWith(compareByDescending<GestureUsageChartItem> { it.count }.thenBy { it.gestureId })
    }

    private fun baseGestureName(gestureId: Int): String {
        return CollectionGesturesProvider.getGesture(gestureId)
            .gestureName
            .takeIf { it.isNotBlank() }
            ?: "Gesture $gestureId"
    }

    private fun customGestureName(index: Int): String {
        val defaultName = getString(CUSTOM_GESTURE_NAME_RES.getOrNull(index) ?: R.string.gesture_1_btn)
        val prefs = requireContext().getSharedPreferences(
            PreferenceKeysUbi4.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        val macKey = prefs.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "") ?: ""
        return prefs.getString(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + index, defaultName)
            ?: defaultName
    }

    companion object {
        private val BASE_GESTURE_IDS = listOf(
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

        private val CUSTOM_GESTURE_NAME_RES = listOf(
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

        fun newInstance(): ServiceFragment = ServiceFragment()
    }

    override fun onDestroyView() {
        binding.serviceFragmentRv.adapter = null
        _binding = null
        super.onDestroyView()
    }

}
