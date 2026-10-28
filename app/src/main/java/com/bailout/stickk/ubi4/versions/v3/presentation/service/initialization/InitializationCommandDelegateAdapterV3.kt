package com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization

import android.view.View
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ItemInitializationCommandBinding
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand.*
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class InitializationCommandDelegateAdapterV3(private val onClick: (V3InitializationCommand) -> Unit) :
    ViewBindingDelegateAdapter<V3InitializationCommandUiState, Ubi4ItemInitializationCommandBinding>(Ubi4ItemInitializationCommandBinding::inflate) {
    override fun Ubi4ItemInitializationCommandBinding.onBind(item: V3InitializationCommandUiState) {
        commandsHeader.visibility = if (item.step == 1) View.VISIBLE else View.GONE
        commandTitle.text = root.context.getString(R.string.v3_init_command_step, item.step, root.context.getString(title(item.packet.command)))
        commandHex.text = item.packet.hex
        sendCommand.isEnabled = item.isEnabled
        sendCommand.setOnClickListener { onClick(item.packet.command) }
        commandQueued.visibility = if (item.isLastQueued) View.VISIBLE else View.GONE
    }

    override fun Ubi4ItemInitializationCommandBinding.onRecycled() { sendCommand.setOnClickListener(null) }
    override fun isForViewType(item: Any) = item is V3InitializationCommandUiState
    override fun V3InitializationCommandUiState.getItemId(): Any = "initialization-command-${packet.command}"

    private fun title(command: V3InitializationCommand) = when (command) {
        DEVICE_DATA -> R.string.v3_init_device_data
        PHONE_DATE_TIME -> R.string.v3_init_phone_date_time
        THRESHOLD -> R.string.v3_init_threshold
        EMG_GAIN -> R.string.v3_init_emg_gain
        EMG_MODE -> R.string.v3_init_emg_mode
        EMG_MAX_GAIN -> R.string.v3_init_emg_max_gain
        EMG_CHANGE_GESTURE -> R.string.v3_init_emg_change_gesture
        SCREEN_TIMEOUT -> R.string.v3_init_screen_timeout
        MOVEMENT_LOCK -> R.string.v3_init_movement_lock
        HAND_SIDE -> R.string.v3_init_hand_side
        BATTERY -> R.string.v3_init_battery
        HAND_CONTROL_MODE -> R.string.v3_init_hand_control_mode
        GESTURE_CHANGE_MODE -> R.string.v3_init_gesture_change_mode
        SPEED -> R.string.v3_init_speed
        FORCE -> R.string.v3_init_force
        SERIAL_NUMBER -> R.string.v3_init_serial_number
        THUMB_POSITION -> R.string.v3_init_thumb_position
        FINGERS_POSITION -> R.string.v3_init_fingers_position
    }
}
