package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_gripper_settings_le.view.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.ADD_GESTURE
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomDialogFragment: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private var oldOpenStage = 0b00000000
    private var oldCloseStage = 0b00000000
    private val openState: Byte = 1
    private val closeState: Byte = 0
    private var mSettings: SharedPreferences? = null

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
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        loadOldState()

        //info block
        rootView?.tv_andex_alert_dialog_layout_title?.text  = rootView?.tv_andex_alert_dialog_layout_title?.text.toString() + main?.getMNumberGesture()+ " configurator"
//        rootView?.v_andex_alert_dialog_layout_one?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний первого пальца", Toast.LENGTH_SHORT).show()}
//        rootView?.v_andex_alert_dialog_layout_two?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний второго пальца", Toast.LENGTH_SHORT).show()}
//        rootView?.v_andex_alert_dialog_layout_three?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний третьего пальца", Toast.LENGTH_SHORT).show()}
//        rootView?.v_andex_alert_dialog_layout_four?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний четвёртого пальца", Toast.LENGTH_SHORT).show()}
//        rootView?.v_andex_alert_dialog_layout_five?.setOnClickListener {Toast.makeText(context, "Блок настройки состояний пятого пальца", Toast.LENGTH_SHORT).show()}

        //control block
        rootView?.finger_1_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_1_open_sb!!.isChecked) openStage or 0b00000001
            else openStage and 0b11111110
//            Toast.makeText(context, "finger_1_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_1_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_1_close_sb!!.isChecked) closeStage or 0b00000001
            else closeStage and 0b11111110
//            Toast.makeText(context, "finger_1_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_2_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_2_open_sb!!.isChecked) openStage or 0b00000010
            else openStage and 0b11111101
//            Toast.makeText(context, "finger_2_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_2_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_2_close_sb!!.isChecked) closeStage or 0b00000010
            else closeStage and 0b11111101
//            Toast.makeText(context, "finger_2_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_3_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_3_open_sb!!.isChecked) openStage or 0b00000100
            else openStage and 0b11111011
//            Toast.makeText(context, "finger_3_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_3_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_3_close_sb!!.isChecked) closeStage or 0b00000100
            else closeStage and 0b11111011
//            Toast.makeText(context, "finger_3_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_4_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_4_open_sb!!.isChecked) openStage or 0b00001000
            else openStage and 0b11110111
//            Toast.makeText(context, "finger_4_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_4_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_4_close_sb!!.isChecked) closeStage or 0b00001000
            else closeStage and 0b11110111
//            Toast.makeText(context, "finger_4_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_5_open_sb?.setOnClickListener {
            openStage = if (rootView?.finger_5_open_sb!!.isChecked) openStage or 0b00010000
            else openStage and 0b11101111
//            Toast.makeText(context, "finger_5_open_sb Click $openStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }
        rootView?.finger_5_close_sb?.setOnClickListener {
            closeStage = if (rootView?.finger_5_close_sb!!.isChecked) closeStage or 0b00010000
            else closeStage and 0b11101111
//            Toast.makeText(context, "finger_5_close_sb Click $closeStage", Toast.LENGTH_SHORT).show()
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
        }


        rootView?.v_andex_alert_dialog_layout_confirm?.setOnClickListener {
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_NUM + (main?.getMNumberGesture()!!-1), openStage)
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + (main?.getMNumberGesture()!!-1), closeStage)
            dismiss()
        }
        rootView?.v_andex_alert_dialog_layout_cancel?.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!!-1).toByte(), oldOpenStage.toByte(), oldCloseStage.toByte(), openState), ADD_GESTURE, WRITE,12)
            main?.incrementCountCommand()
            dismiss()
        }
    }

    private fun loadOldState() {
        openStage = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_NUM + (main?.getMNumberGesture()!!-1), 0)
        closeStage = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + (main?.getMNumberGesture()!!-1), 0)
        oldOpenStage = openStage
        oldCloseStage = closeStage
        System.err.println("oldOpenStage: $oldOpenStage")
        System.err.println("(oldOpenStage and 0b00000010) == 1: " + ((oldOpenStage shr 1 and 0b00000001) == 1))
        System.err.println("oldOpenStage: $oldOpenStage")
        rootView?.finger_1_open_sb?.isChecked = ((oldOpenStage shr 0 and 0b00000001) == 1)
        rootView?.finger_2_open_sb?.isChecked = ((oldOpenStage shr 1 and 0b00000001) == 1)
        rootView?.finger_3_open_sb?.isChecked = ((oldOpenStage shr 2 and 0b00000001) == 1)
        rootView?.finger_4_open_sb?.isChecked = ((oldOpenStage shr 3 and 0b00000001) == 1)
        rootView?.finger_5_open_sb?.isChecked = ((oldOpenStage shr 4 and 0b00000001) == 1)
        rootView?.finger_1_close_sb?.isChecked = ((oldCloseStage shr 0 and 0b00000001) == 1)
        rootView?.finger_2_close_sb?.isChecked = ((oldCloseStage shr 1 and 0b00000001) == 1)
        rootView?.finger_3_close_sb?.isChecked = ((oldCloseStage shr 2 and 0b00000001) == 1)
        rootView?.finger_4_close_sb?.isChecked = ((oldCloseStage shr 3 and 0b00000001) == 1)
        rootView?.finger_5_close_sb?.isChecked = ((oldCloseStage shr 4 and 0b00000001) == 1)
    }
}