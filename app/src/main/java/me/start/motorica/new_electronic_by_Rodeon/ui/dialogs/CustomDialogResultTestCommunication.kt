package me.start.motorica.new_electronic_by_Rodeon.ui.dialogs

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
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

@Suppress("DEPRECATION")
class CustomDialogResultTestCommunication(private val percentageCommunicationQuality: Int): DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null


    @SuppressLint("CheckResult")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_result_communication_test, container, false)
        rootView = view
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_percent)?.text = "$percentageCommunicationQuality%"
        when(percentageCommunicationQuality) {
            0 -> { rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_title2)?.text = getString(R.string.no_link) }
            in 1..20 -> { rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_title2)?.text = getString(R.string.terrible_quality) }
            in 20..40 -> { rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_title2)?.text = getString(R.string.poor_quality) }
            in 40..60 -> { rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_title2)?.text = getString(R.string.satisfactory_quality) }
            in 60..80 -> { rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_title2)?.text = getString(R.string.high_quality) }
            in 80..100 -> { rootView?.findViewById<TextView>(R.id.tv_result_test_communication_dialog_title2)?.text = getString(R.string.excellent_quality) }
        }


        rootView?.findViewById<Button>(R.id.dialog_result_test_communication_cancel)?.setOnClickListener {
            dismiss()
        }
    }
}