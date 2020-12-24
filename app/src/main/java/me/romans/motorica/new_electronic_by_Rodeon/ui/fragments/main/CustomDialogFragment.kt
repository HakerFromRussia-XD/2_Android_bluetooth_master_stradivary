package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_gripper_settings_le2.view.*
import me.romans.motorica.R
import org.jetbrains.anko.sdk25.coroutines.onClick

class CustomDialogFragment: DialogFragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_gripper_settings_le2, container, false)
        view.v_andex_alert_dialog_layout_one.setOnClickListener() { Toast.makeText(context, "One Click", Toast.LENGTH_SHORT).show() }
        view.v_andex_alert_dialog_layout_two.setOnClickListener() { Toast.makeText(context, "Two Click", Toast.LENGTH_SHORT).show() }
        view.v_andex_alert_dialog_layout_three.setOnClickListener() { Toast.makeText(context, "Three Click", Toast.LENGTH_SHORT).show() }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        tv_andex_alert_dialog_layout_two_text.text = "One"
    }
}