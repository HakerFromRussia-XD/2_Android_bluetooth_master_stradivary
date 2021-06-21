package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_chart.*
import kotlinx.android.synthetic.main.layout_updating_le.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomInfoUpdateDialogFragment: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var updatingUIThread: Thread? = null
    private var updateThreadFlag = true

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_updating_le, container, false)
        rootView = view
        if (activity != null) { main = activity as MainActivity? }
        return view
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        startUpdatingUIThread()
    }

    @SuppressLint("SetTextI18n")
    private fun startUpdatingUIThread() {
        updatingUIThread = Thread {
            while (updateThreadFlag) {
                main?.runOnUiThread {
                    if (main?.getProgressUpdate() == 100) updateThreadFlag = false
                    ObjectAnimator.ofInt(pb_update, "progress", main?.getProgressUpdate()!!).setDuration(200).start()
                    tv_update_dialog_layout_title2.text = tv_update_dialog_layout_title2.text.split(" ")[0] + "  " + main?.getProgressUpdate()!! + "%"
                }
                try {
                    Thread.sleep(100)
                } catch (ignored: Exception) { }
            }
            dismiss()
        }
        updatingUIThread?.start()
    }
}