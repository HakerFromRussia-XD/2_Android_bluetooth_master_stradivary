package com.bailout.stickk.ubi4.ui.fragments.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.data.state.DashboardSlotsState
import com.bailout.stickk.ubi4.ui.dashboard.DashboardSlotsScreen
import com.bailout.stickk.ubi4.ui.dashboard.toDashboardSlotUiItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardSlotsFragment : Fragment() {
    private val deviceAddress: Int
        get() = requireArguments().getInt(ARG_DEVICE_ADDRESS)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by DashboardSlotsState.stateFlow.collectAsState()
                DashboardSlotsScreen(
                    slots = state.slots.map { it.toDashboardSlotUiItem() },
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onSlotClick = { slot ->
                        navigator().showDashboardSlotContentScreen(
                            deviceAddress = slot.deviceAddress,
                            dataCode = slot.dataCode,
                            title = slot.title,
                            version = slot.version,
                            subVersion = slot.subVersion,
                            declaredSize = slot.dataSize
                        )
                    }
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSlots()
    }

    private fun requestSlots() {
        DashboardSlotsState.requestStarted(deviceAddress)
        val packet = BLECommandsV3.requestAvailableSlots(deviceAddress)
        platformLog(
            DASHBOARD_SLOTS_LOG_TAG,
            "TX READ_AVAILABLE_SLOTS deviceAddress=0x${deviceAddress.toHexByte()} " +
                "uuid=$SERIALPORTCHAR_UUID packet=${packet.toHexLog()}"
        )
        (activity as? MainActivityUBI4)?.bleCommandWithQueue(
            packet,
            SERIALPORTCHAR_UUID,
            WRITE
        ) {}
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SLOTS_REQUEST_TIMEOUT_MS)
            DashboardSlotsState.requestFailed(
                deviceAddress = deviceAddress,
                message = "Плата не ответила на запрос слотов"
            )
        }
    }

    companion object {
        private const val ARG_DEVICE_ADDRESS = "device_address"
        private const val SLOTS_REQUEST_TIMEOUT_MS = 5_000L
        private const val DASHBOARD_SLOTS_LOG_TAG = "DASHBOARD_SLOTS"

        fun newInstance(deviceAddress: Int): DashboardSlotsFragment =
            DashboardSlotsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DEVICE_ADDRESS, deviceAddress)
                }
            }
    }
}

private fun ByteArray.toHexLog(): String =
    joinToString(" ") { (it.toInt() and 0xFF).toHexByte() }

private fun Int.toHexByte(): String =
    toString(16).uppercase().padStart(2, '0')
