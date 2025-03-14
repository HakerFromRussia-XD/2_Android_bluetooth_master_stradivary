package com.bailout.stickk.new_electronic_by_Rodeon.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.ADD_GESTURE
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity

@Suppress("DEPRECATION")
class CustomDialogFragment: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private var oldOpenStage = 0b00000000
    private var oldCloseStage = 0b00000000
    private val openState: Byte = 1
    private val closeState: Byte = 0
    private var side: Int = 1


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_gripper_settings_le, container, false)
        rootView = view
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        loadOldState()

        //info block
        if (main?.locate?.contains("ru")!!) {
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_title)?.text  = "Кофигуратор жеста " + main?.getMNumberGesture()
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_title2)?.textSize = 10f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_title3)?.textSize = 10f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_message)?.textSize = 14f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_message_2)?.textSize = 14f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_message_3)?.textSize = 14f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_message_4)?.textSize = 14f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_message_5)?.textSize = 14f
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_message_6)?.textSize = 14f
        } else {
            rootView?.findViewById<TextView>(R.id.tv_andex_alert_dialog_layout_title)?.text  = "Gesture " + main?.getMNumberGesture()+ " configurator"
        }

        //control block
        if (!main?.lockWriteBeforeFirstRead!!) {
            if (side == 1) {
                rootView?.findViewById<Switch>(R.id.finger_1_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_1_open_sb)!!.isChecked) openStage or 0b00000001
                    else openStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_1_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_1_close_sb)!!.isChecked) closeStage or 0b00000001
                    else closeStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_2_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_2_open_sb)!!.isChecked) openStage or 0b00000010
                    else openStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }

                rootView?.findViewById<Switch>(R.id.finger_2_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_2_close_sb)!!.isChecked) closeStage or 0b00000010
                    else closeStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_3_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_3_open_sb)!!.isChecked) openStage or 0b00000100
                    else openStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)

                }
                rootView?.findViewById<Switch>(R.id.finger_3_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_3_close_sb)!!.isChecked) closeStage or 0b00000100
                    else closeStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_4_open_sb)?.setOnClickListener {
                    openStage = if ( rootView?.findViewById<Switch>(R.id.finger_4_open_sb)!!.isChecked) openStage or 0b00001000
                    else openStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_4_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_4_close_sb)!!.isChecked) closeStage or 0b00001000
                    else closeStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_5_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_5_open_sb)!!.isChecked) openStage or 0b00010000
                    else openStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_5_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_5_close_sb)!!.isChecked) closeStage or 0b00010000
                    else closeStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_6_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_6_open_sb)!!.isChecked) openStage or 0b00100000
                    else openStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_6_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_6_close_sb)!!.isChecked) closeStage or 0b00100000
                    else closeStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
            } else {
                rootView?.findViewById<Switch>(R.id.finger_4_open_sb)?.setOnClickListener {
                    openStage = if ( rootView?.findViewById<Switch>(R.id.finger_4_open_sb)!!.isChecked) openStage or 0b00000001
                    else openStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_4_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_4_close_sb)!!.isChecked) closeStage or 0b00000001
                    else closeStage and 0b11111110
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_3_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_3_open_sb)!!.isChecked) openStage or 0b00000010
                    else openStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_3_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_3_close_sb)!!.isChecked) closeStage or 0b00000010
                    else closeStage and 0b11111101
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_2_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_2_open_sb)!!.isChecked) openStage or 0b00000100
                    else openStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_2_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_2_close_sb)!!.isChecked) closeStage or 0b00000100
                    else closeStage and 0b11111011
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_1_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_1_open_sb)!!.isChecked) openStage or 0b00001000
                    else openStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_1_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_1_close_sb)!!.isChecked) closeStage or 0b00001000
                    else closeStage and 0b11110111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_5_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_5_open_sb)!!.isChecked) openStage or 0b00010000
                    else openStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_5_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_5_close_sb)!!.isChecked) closeStage or 0b00010000
                    else closeStage and 0b11101111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_6_open_sb)?.setOnClickListener {
                    openStage = if (rootView?.findViewById<Switch>(R.id.finger_6_open_sb)!!.isChecked) openStage or 0b00100000
                    else openStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                }
                rootView?.findViewById<Switch>(R.id.finger_6_close_sb)?.setOnClickListener {
                    closeStage = if (rootView?.findViewById<Switch>(R.id.finger_6_close_sb)!!.isChecked) closeStage or 0b00100000
                    else closeStage and 0b11011111
                    main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), openStage.toByte(), closeStage.toByte(), closeState), ADD_GESTURE, WRITE, 12)
                }
            }


            rootView?.findViewById<Button>(R.id.v_andex_alert_dialog_layout_confirm)
            rootView?.findViewById<Button>(R.id.v_andex_alert_dialog_layout_confirm)?.setOnClickListener {
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_NUM + (main?.getMNumberGesture()!! - 1), openStage)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + (main?.getMNumberGesture()!! - 1), closeStage)
                dismiss()
            }
            rootView?.findViewById<Button>(R.id.v_andex_alert_dialog_layout_cancel)?.setOnClickListener {
                main?.bleCommandConnector(byteArrayOf((main?.getMNumberGesture()!! - 1).toByte(), oldOpenStage.toByte(), oldCloseStage.toByte(), openState), ADD_GESTURE, WRITE, 12)
                dismiss()
            }
        } else {
            main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
        }
    }

    private fun loadOldState() {
        openStage = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_NUM + (main?.getMNumberGesture()!!-1), 0)
        closeStage = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + (main?.getMNumberGesture()!!-1), 0)
        side = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
        oldOpenStage = openStage
        oldCloseStage = closeStage
        if (side == 1) {
            rootView?.findViewById<Switch>(R.id.finger_1_open_sb)?.isChecked = ((oldOpenStage shr 0 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_2_open_sb)?.isChecked = ((oldOpenStage shr 1 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_3_open_sb)?.isChecked = ((oldOpenStage shr 2 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_4_open_sb)?.isChecked = ((oldOpenStage shr 3 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_5_open_sb)?.isChecked = ((oldOpenStage shr 4 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_6_open_sb)?.isChecked = ((oldOpenStage shr 5 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_1_close_sb)?.isChecked = ((oldCloseStage shr 0 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_2_close_sb)?.isChecked = ((oldCloseStage shr 1 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_3_close_sb)?.isChecked = ((oldCloseStage shr 2 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_4_close_sb)?.isChecked = ((oldCloseStage shr 3 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_5_close_sb)?.isChecked = ((oldCloseStage shr 4 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_6_close_sb)?.isChecked = ((oldCloseStage shr 5 and 0b00000001) == 1)
        } else {
            rootView?.findViewById<Switch>(R.id.finger_4_open_sb)?.isChecked = ((oldOpenStage shr 0 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_3_open_sb)?.isChecked = ((oldOpenStage shr 1 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_2_open_sb)?.isChecked = ((oldOpenStage shr 2 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_1_open_sb)?.isChecked = ((oldOpenStage shr 3 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_5_open_sb)?.isChecked = ((oldOpenStage shr 4 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_6_open_sb)?.isChecked = ((oldOpenStage shr 5 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_4_close_sb)?.isChecked = ((oldCloseStage shr 0 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_3_close_sb)?.isChecked = ((oldCloseStage shr 1 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_2_close_sb)?.isChecked = ((oldCloseStage shr 2 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_1_close_sb)?.isChecked = ((oldCloseStage shr 3 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_5_close_sb)?.isChecked = ((oldCloseStage shr 4 and 0b00000001) == 1)
            rootView?.findViewById<Switch>(R.id.finger_6_close_sb)?.isChecked = ((oldCloseStage shr 5 and 0b00000001) == 1)
        }
    }
}