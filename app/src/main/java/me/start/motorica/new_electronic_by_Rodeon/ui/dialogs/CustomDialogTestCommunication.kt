package me.start.motorica.new_electronic_by_Rodeon.ui.dialogs

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_progress_communication_test.view.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomDialogTestCommunication: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_progress_communication_test, container, false)
        rootView = view
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        loadOldState()

        rootView?.test_communication_animation_view?.setAnimation(R.raw.test_communication_animation)

        rootView?.dialog_test_communication_cancel?.setOnClickListener {
            dismiss()
        }
    }


    private fun loadOldState() {
//        changingValue = 255 -  mSettings!!.getInt(main?.mDeviceAddress + keyValue,127)
//        animatedWheel(changingValue, 500)
    }
}