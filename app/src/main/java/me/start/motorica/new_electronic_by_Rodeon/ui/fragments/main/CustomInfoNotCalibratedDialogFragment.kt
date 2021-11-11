package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_not_calibrating_le.view.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity


@Suppress("DEPRECATION")
class CustomInfoNotCalibratedDialogFragment: DialogFragment()  {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_not_calibrating_le, container, false)
        rootView = view
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        dialog!!.setCanceledOnTouchOutside(false)

        rootView?.v_andex_alert_dialog_layout_confirm_not_calibrated?.setOnClickListener {
            dismiss()
        }
    }
}