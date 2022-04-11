package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_updating_le.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

@Suppress("DEPRECATION")
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


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        dialog!!.setCanceledOnTouchOutside(false)
        pb_update.progressTintList = ColorStateList.valueOf(this.resources.getColor(R.color.darkOrange))
        startUpdatingUIThread()
    }

    @SuppressLint("SetTextI18n", "Recycle")
    private fun startUpdatingUIThread() {
        var count = 0
        updatingUIThread = Thread {
            while (updateThreadFlag) {
                if (main?.getProgressUpdate() == 100) updateThreadFlag = false
                tv_update_dialog_layout_title2.text = tv_update_dialog_layout_title2.text.split(" ")[0] + "  " + main?.getProgressUpdate()!! + "%"
                count++
                if (count >= 10) {
                    main?.runOnUiThread {
                        ObjectAnimator.ofInt(pb_update, "progress", main?.getProgressUpdate()!!).setDuration(1000).start()
                    }
                    count = 0
                }
                try {
                    Thread.sleep(100)
                } catch (ignored: Exception) {}
            }
            dismiss()
            if (main?.locate?.contains("ru")!!) {
                main?.showToast("Обновление установлено!")
            } else {
                main?.showToast("Update installed!")
            }

        }
        updatingUIThread!!.start()
    }
}