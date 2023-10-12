package com.bailout.stick.new_electronic_by_Rodeon.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bailout.stick.R
import com.bailout.stick.new_electronic_by_Rodeon.ble.SampleGattAttributes
import com.bailout.stick.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stick.new_electronic_by_Rodeon.ui.activities.main.MainActivity

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
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)



        //TODO первое падение на INDY
        rootView?.findViewById<View>(R.id.v_andex_alert_dialog_layout_confirm)?.setOnClickListener {
            System.err.println("ok")
            main?.bleCommandConnector(byteArrayOf(0x01), SampleGattAttributes.SET_START_UPDATE, SampleGattAttributes.WRITE, 18)
            main?.openFragmentInfoUpdate()
            dismiss()
        }
        rootView?.findViewById<View>(R.id.v_andex_alert_dialog_layout_cancel)?.setOnClickListener {
            System.err.println("Ne ok")
            dismiss()
        }
    }
}