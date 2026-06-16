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
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentState
import com.bailout.stickk.ubi4.ui.dashboard.DashboardSlotContentAction
import com.bailout.stickk.ubi4.ui.dashboard.DashboardSlotContentScreen
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardSlotContentFragment : Fragment() {
    private val deviceAddress: Int
        get() = requireArguments().getInt(ARG_DEVICE_ADDRESS)

    private val dataCode: Int
        get() = requireArguments().getInt(ARG_DATA_CODE)

    private val slotTitle: String
        get() = requireArguments().getString(ARG_TITLE).orEmpty()

    private val version: Int
        get() = requireArguments().getInt(ARG_VERSION)

    private val subVersion: Int
        get() = requireArguments().getInt(ARG_SUB_VERSION)

    private val declaredSize: Int
        get() = requireArguments().getInt(ARG_DECLARED_SIZE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by DashboardSlotContentState.stateFlow.collectAsState()
                DashboardSlotContentScreen(
                    state = state,
                    onActionClick = ::handleActionClick
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSlotContent()
    }

    private fun handleActionClick(action: DashboardSlotContentAction) {
        when (action) {
            DashboardSlotContentAction.Refresh -> requestSlotContent()
            DashboardSlotContentAction.Send -> sendCurrentSlotData()
            is DashboardSlotContentAction.ParameterChanged -> DashboardSlotContentState.updateParameterValue(
                path = action.path,
                value = action.value
            )
            DashboardSlotContentAction.Save -> sendCommand(
                commandName = "SAVE_DATA",
                packet = BLECommandsV3.saveSlots(deviceAddress)
            )
            DashboardSlotContentAction.Reset -> sendCommand(
                commandName = "RESET_TO_FACTORY",
                packet = BLECommandsV3.resetSlot(deviceAddress, dataCode)
            )
            DashboardSlotContentAction.ResetAll -> sendCommand(
                commandName = "RESET_TO_FACTORY_ALL",
                packet = BLECommandsV3.resetAllSlots(deviceAddress)
            )
        }
    }

    private fun requestSlotContent() {
        DashboardSlotContentState.requestStarted(
            deviceAddress = deviceAddress,
            dataCode = dataCode,
            title = slotTitle,
            version = version,
            subVersion = subVersion,
            declaredSize = declaredSize
        )
        if (declaredSize > CHUNK_SIZE_THRESHOLD) {
            sendReadDataParts()
        } else {
            sendCommand(
                commandName = "READ_DATA",
                packet = BLECommandsV3.requestSlotData(deviceAddress, dataCode)
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SLOT_CONTENT_REQUEST_TIMEOUT_MS + chunkCount(declaredSize) * SLOT_CONTENT_CHUNK_TIMEOUT_MS)
            DashboardSlotContentState.requestFailed(
                deviceAddress = deviceAddress,
                dataCode = dataCode,
                message = "Плата не ответила на запрос содержимого"
            )
        }
    }

    private fun sendReadDataParts() {
        var offset = 0
        while (offset < declaredSize) {
            val chunkSize = minOf(CHUNK_DATA_SIZE, declaredSize - offset)
            sendCommand(
                commandName = "READ_DATA_PART",
                packet = BLECommandsV3.requestSlotDataPart(
                    deviceAddress = deviceAddress,
                    dataCode = dataCode,
                    offset = offset,
                    size = chunkSize
                )
            )
            offset += chunkSize
        }
    }

    private fun sendCurrentSlotData() {
        val current = DashboardSlotContentState.stateFlow.value
        if (current.data.isEmpty()) {
            DashboardSlotContentState.updateStatus("Нет данных для отправки")
            return
        }
        val data = current.data.toByteArray()
        if (data.size > CHUNK_SIZE_THRESHOLD) {
            sendWriteDataParts(data)
        } else {
            sendCommand(
                commandName = "WRITE_DATA",
                packet = BLECommandsV3.writeSlotData(
                    deviceAddress = deviceAddress,
                    dataCode = dataCode,
                    data = data
                )
            )
        }
        DashboardSlotContentState.updateStatus("Данные отправлены")
    }

    private fun sendWriteDataParts(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(CHUNK_DATA_SIZE, data.size - offset)
            sendCommand(
                commandName = "WRITE_DATA_PART",
                packet = BLECommandsV3.writeSlotDataPart(
                    deviceAddress = deviceAddress,
                    dataCode = dataCode,
                    offset = offset,
                    data = data.copyOfRange(offset, offset + chunkSize)
                )
            )
            offset += chunkSize
        }
    }

    private fun sendCommand(commandName: String, packet: ByteArray) {
        platformLog(
            DASHBOARD_SLOT_CONTENT_LOG_TAG,
            "TX $commandName deviceAddress=0x${deviceAddress.toHexByte()} " +
                "dataCode=0x${dataCode.toHexByte()} packet=${packet.toHexLog()}"
        )
        (activity as? MainActivityUBI4)?.bleCommandWithQueue(
            packet,
            SERIALPORTCHAR_UUID,
            WRITE
        ) {}
    }

    companion object {
        private const val ARG_DEVICE_ADDRESS = "device_address"
        private const val ARG_DATA_CODE = "data_code"
        private const val ARG_TITLE = "title"
        private const val ARG_VERSION = "version"
        private const val ARG_SUB_VERSION = "sub_version"
        private const val ARG_DECLARED_SIZE = "declared_size"
        private const val SLOT_CONTENT_REQUEST_TIMEOUT_MS = 5_000L
        private const val SLOT_CONTENT_CHUNK_TIMEOUT_MS = 1_000L
        private const val CHUNK_SIZE_THRESHOLD = 250
        private const val CHUNK_DATA_SIZE = 200
        private const val DASHBOARD_SLOT_CONTENT_LOG_TAG = "DASHBOARD_SLOT_CONTENT"

        fun newInstance(
            deviceAddress: Int,
            dataCode: Int,
            title: String,
            version: Int,
            subVersion: Int,
            declaredSize: Int
        ): DashboardSlotContentFragment =
            DashboardSlotContentFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DEVICE_ADDRESS, deviceAddress)
                    putInt(ARG_DATA_CODE, dataCode)
                    putString(ARG_TITLE, title)
                    putInt(ARG_VERSION, version)
                    putInt(ARG_SUB_VERSION, subVersion)
                    putInt(ARG_DECLARED_SIZE, declaredSize)
                }
            }
    }
}

private fun chunkCount(size: Int): Int =
    if (size <= 0) 1 else (size + 199) / 200

private fun List<Int>.toByteArray(): ByteArray =
    ByteArray(size) { index -> this[index].toByte() }

private fun ByteArray.toHexLog(): String =
    joinToString(" ") { (it.toInt() and 0xFF).toHexByte() }

private fun Int.toHexByte(): String =
    toString(16).uppercase().padStart(2, '0')
