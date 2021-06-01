package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_gripper_settings_le.*
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
    private var side: Int = 1
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
        if (main?.locate?.contains("ru")!!) {
            rootView?.tv_andex_alert_dialog_layout_title?.text  = "Кофигуратор жеста " + main?.getMNumberGesture()
            tv_andex_alert_dialog_layout_title2.textSize = 10f
            tv_andex_alert_dialog_layout_title3.textSize = 10f
            tv_andex_alert_dialog_layout_message.textSize = 14f
            tv_andex_alert_dialog_layout_message_2.textSize = 14f
            tv_andex_alert_dialog_layout_message_3.textSize = 14f
            tv_andex_alert_dialog_layout_message_4.textSize = 14f
            tv_andex_alert_dialog_layout_message_5.textSize = 14f
            tv_andex_alert_dialog_layout_message_6.textSize = 14f
        } else {
            rootView?.tv_andex_alert_dialog_layout_title?.text  = "Gesture " + main?.getMNumberGesture()+ " configurator"
        }


        //control block
        if (!main?.lockWriteBeforeFirstRead!!) {
            if (side == 1) {
                rootView?.finger_1_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_1_open_sb!!.isChecked) openStage or 0b00000001
                    else openStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_1_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_1_close_sb!!.isChecked) closeStage or 0b00000001
                    else closeStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_2_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_2_open_sb!!.isChecked) openStage or 0b00000010
                    else openStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_2_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_2_close_sb!!.isChecked) closeStage or 0b00000010
                    else closeStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_3_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_3_open_sb!!.isChecked) openStage or 0b00000100
                    else openStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_3_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_3_close_sb!!.isChecked) closeStage or 0b00000100
                    else closeStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_4_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_4_open_sb!!.isChecked) openStage or 0b00001000
                    else openStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_4_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_4_close_sb!!.isChecked) closeStage or 0b00001000
                    else closeStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_5_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_5_open_sb!!.isChecked) openStage or 0b00010000
                    else openStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_5_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_5_close_sb!!.isChecked) closeStage or 0b00010000
                    else closeStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_6_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_6_open_sb!!.isChecked) openStage or 0b00100000
                    else openStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_6_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_6_close_sb!!.isChecked) closeStage or 0b00100000
                    else closeStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
            } else {
                rootView?.finger_4_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_4_open_sb!!.isChecked) openStage or 0b00000001
                    else openStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_4_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_4_close_sb!!.isChecked) closeStage or 0b00000001
                    else closeStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_3_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_3_open_sb!!.isChecked) openStage or 0b00000010
                    else openStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_3_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_3_close_sb!!.isChecked) closeStage or 0b00000010
                    else closeStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_2_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_2_open_sb!!.isChecked) openStage or 0b00000100
                    else openStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_2_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_2_close_sb!!.isChecked) closeStage or 0b00000100
                    else closeStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_1_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_1_open_sb!!.isChecked) openStage or 0b00001000
                    else openStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_1_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_1_close_sb!!.isChecked) closeStage or 0b00001000
                    else closeStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_5_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_5_open_sb!!.isChecked) openStage or 0b00010000
                    else openStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_5_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_5_close_sb!!.isChecked) closeStage or 0b00010000
                    else closeStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_6_open_sb?.setOnClickListener {
                    openStage = if (rootView?.finger_6_open_sb!!.isChecked) openStage or 0b00100000
                    else openStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
                rootView?.finger_6_close_sb?.setOnClickListener {
                    closeStage = if (rootView?.finger_6_close_sb!!.isChecked) closeStage or 0b00100000
                    else closeStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                    main?.incrementCountCommand()
                }
            }



            rootView?.v_andex_alert_dialog_layout_confirm?.setOnClickListener {
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_NUM + (main?.getMNumberGesture()!! - 1), openStage)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + (main?.getMNumberGesture()!! - 1), closeStage)
                dismiss()
            }
            rootView?.v_andex_alert_dialog_layout_cancel?.setOnClickListener {
                main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), oldOpenStage.toByte(), oldCloseStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                main?.incrementCountCommand()
                dismiss()
            }
        }
    }

    private fun loadOldState() {
        openStage = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_NUM + (main?.getMNumberGesture()!!-1), 0)
        closeStage = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + (main?.getMNumberGesture()!!-1), 0)
        side = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
        oldOpenStage = openStage
        oldCloseStage = closeStage
        if (side == 1) {
            rootView?.finger_1_open_sb?.isChecked = ((oldOpenStage shr 0 and 0b00000001) == 1)
            rootView?.finger_2_open_sb?.isChecked = ((oldOpenStage shr 1 and 0b00000001) == 1)
            rootView?.finger_3_open_sb?.isChecked = ((oldOpenStage shr 2 and 0b00000001) == 1)
            rootView?.finger_4_open_sb?.isChecked = ((oldOpenStage shr 3 and 0b00000001) == 1)
            rootView?.finger_5_open_sb?.isChecked = ((oldOpenStage shr 4 and 0b00000001) == 1)
            rootView?.finger_6_open_sb?.isChecked = ((oldOpenStage shr 5 and 0b00000001) == 1)
            rootView?.finger_1_close_sb?.isChecked = ((oldCloseStage shr 0 and 0b00000001) == 1)
            rootView?.finger_2_close_sb?.isChecked = ((oldCloseStage shr 1 and 0b00000001) == 1)
            rootView?.finger_3_close_sb?.isChecked = ((oldCloseStage shr 2 and 0b00000001) == 1)
            rootView?.finger_4_close_sb?.isChecked = ((oldCloseStage shr 3 and 0b00000001) == 1)
            rootView?.finger_5_close_sb?.isChecked = ((oldCloseStage shr 4 and 0b00000001) == 1)
            rootView?.finger_6_close_sb?.isChecked = ((oldCloseStage shr 5 and 0b00000001) == 1)
        } else {
            rootView?.finger_4_open_sb?.isChecked = ((oldOpenStage shr 0 and 0b00000001) == 1)
            rootView?.finger_3_open_sb?.isChecked = ((oldOpenStage shr 1 and 0b00000001) == 1)
            rootView?.finger_2_open_sb?.isChecked = ((oldOpenStage shr 2 and 0b00000001) == 1)
            rootView?.finger_1_open_sb?.isChecked = ((oldOpenStage shr 3 and 0b00000001) == 1)
            rootView?.finger_5_open_sb?.isChecked = ((oldOpenStage shr 4 and 0b00000001) == 1)
            rootView?.finger_6_open_sb?.isChecked = ((oldOpenStage shr 5 and 0b00000001) == 1)
            rootView?.finger_4_close_sb?.isChecked = ((oldCloseStage shr 0 and 0b00000001) == 1)
            rootView?.finger_3_close_sb?.isChecked = ((oldCloseStage shr 1 and 0b00000001) == 1)
            rootView?.finger_2_close_sb?.isChecked = ((oldCloseStage shr 2 and 0b00000001) == 1)
            rootView?.finger_1_close_sb?.isChecked = ((oldCloseStage shr 3 and 0b00000001) == 1)
            rootView?.finger_5_close_sb?.isChecked = ((oldCloseStage shr 4 and 0b00000001) == 1)
            rootView?.finger_6_close_sb?.isChecked = ((oldCloseStage shr 5 and 0b00000001) == 1)
        }
    }
}