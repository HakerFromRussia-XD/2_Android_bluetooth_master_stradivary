package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_gripper_settings_le.view.*
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.ADD_GESTURE
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomDialogFragment: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private val openState: Byte = 1
    private val closeState: Byte = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_gripper_settings_le, container, false)
        rootView = view
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //info block
        rootView?.tv_andex_alert_dialog_layout_title?.text  = rootView?.tv_andex_alert_dialog_layout_title?.text.toString() + main?.getMNumberGesture()+ " configurator"
        rootView?.v_andex_alert_dialog_layout_one?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний первого пальца", Toast.LENGTH_SHORT).show()}
        rootView?.v_andex_alert_dialog_layout_two?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний второго пальца", Toast.LENGTH_SHORT).show()}
        rootView?.v_andex_alert_dialog_layout_three?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний третьего пальца", Toast.LENGTH_SHORT).show()}
        rootView?.v_andex_alert_dialog_layout_four?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний четвёртого пальца", Toast.LENGTH_SHORT).show()}
        rootView?.v_andex_alert_dialog_layout_five?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний пятого пальца", Toast.LENGTH_SHORT).show()}

        //control block
        rootView?.finger_1_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_1_open_sb!!.isChecked) openStage or 0b00000001
            else openStage and 0b11111110
            Toast.makeText(context, "finger_1_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_1_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_1_close_sb!!.isChecked) closeStage or 0b00000001
            else closeStage and 0b11111110
            Toast.makeText(context, "finger_1_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_2_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_2_open_sb!!.isChecked) openStage or 0b00000010
            else openStage and 0b11111101
            Toast.makeText(context, "finger_2_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_2_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_2_close_sb!!.isChecked) closeStage or 0b00000010
            else closeStage and 0b11111101
            Toast.makeText(context, "finger_2_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_3_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_3_open_sb!!.isChecked) openStage or 0b00000100
            else openStage and 0b11111011
            Toast.makeText(context, "finger_3_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_3_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_3_close_sb!!.isChecked) closeStage or 0b00000100
            else closeStage and 0b11111011
            Toast.makeText(context, "finger_3_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_4_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_4_open_sb!!.isChecked) openStage or 0b00001000
            else openStage and 0b11110111
            Toast.makeText(context, "finger_4_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_4_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_4_close_sb!!.isChecked) closeStage or 0b00001000
            else closeStage and 0b11110111
            Toast.makeText(context, "finger_4_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_5_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_5_open_sb!!.isChecked) openStage or 0b00010000
            else openStage and 0b11101111
            Toast.makeText(context, "finger_5_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE)
        }
        rootView?.finger_5_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_5_close_sb!!.isChecked) closeStage or 0b00010000
            else closeStage and 0b11101111
            Toast.makeText(context, "finger_5_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommand(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE)
        }


        rootView?.v_andex_alert_dialog_layout_confirm?.setOnClickListener { dismiss() }
        rootView?.v_andex_alert_dialog_layout_cancel?.setOnClickListener { dismiss() }
    }
}