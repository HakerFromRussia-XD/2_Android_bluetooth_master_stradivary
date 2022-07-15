package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import androidx.constraintlayout.solver.state.ConstraintReference
import androidx.fragment.app.DialogFragment
//import kotlinx.android.synthetic.main.layout_gripper_settings_le.*
import kotlinx.android.synthetic.main.layout_gripper_settings_le.view.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

@Suppress("DEPRECATION")
class CustomUpdateDialogFragment: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_request_updating_le, container, false)
        rootView = view
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)


        rootView?.v_andex_alert_dialog_layout_confirm?.setOnClickListener {
            System.err.println("ok")
            main?.bleCommandConnector(byteArrayOf(0x01), SampleGattAttributes.SET_START_UPDATE, SampleGattAttributes.WRITE, 18)
            main?.openFragmentInfoUpdate()
            dismiss()
        }
        rootView?.v_andex_alert_dialog_layout_cancel?.setOnClickListener {
            System.err.println("Ne ok")
            dismiss()
        }
    }
}