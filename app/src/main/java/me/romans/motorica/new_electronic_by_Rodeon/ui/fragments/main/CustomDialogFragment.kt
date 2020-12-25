package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_gripper_settings_le.view.*
import me.romans.motorica.R

class CustomDialogFragment: DialogFragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_gripper_settings_le, container, false)

        //info block
        view.v_andex_alert_dialog_layout_one.setOnClickListener {Toast.makeText(context, "Блок настройки состояний первого пальца", Toast.LENGTH_SHORT).show()}
        view.v_andex_alert_dialog_layout_two.setOnClickListener {Toast.makeText(context, "Блок настройки состояний второго пальца", Toast.LENGTH_SHORT).show()}
        view.v_andex_alert_dialog_layout_three.setOnClickListener {Toast.makeText(context, "Блок настройки состояний третьего пальца", Toast.LENGTH_SHORT).show()}
        view.v_andex_alert_dialog_layout_four.setOnClickListener {Toast.makeText(context, "Блок настройки состояний четвёртого пальца", Toast.LENGTH_SHORT).show()}
        view.v_andex_alert_dialog_layout_five.setOnClickListener {Toast.makeText(context, "Блок настройки состояний пятого пальца", Toast.LENGTH_SHORT).show()}

        //control block
        view.finger_1_open_sb.setOnClickListener {
            if (view.finger_1_open_sb.isChecked) Toast.makeText(context, "finger_1_open_sb Click 1", Toast.LENGTH_SHORT).show()
            if (!view.finger_1_open_sb.isChecked) Toast.makeText(context, "finger_1_open_sb Click 0", Toast.LENGTH_SHORT).show()
        }

        view.finger_1_close_sb.setOnClickListener { Toast.makeText(context, "finger_1_close_sb Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }

        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }м
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }//        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
////        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        //        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
//        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }




















        return view
    }
}