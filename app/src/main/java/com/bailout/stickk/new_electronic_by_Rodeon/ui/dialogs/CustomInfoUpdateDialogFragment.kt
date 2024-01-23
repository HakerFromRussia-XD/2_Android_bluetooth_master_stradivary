package com.bailout.stickk.new_electronic_by_Rodeon.ui.dialogs

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity

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
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (activity != null) { main = activity as MainActivity? }
        return view
    }


    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        dialog!!.setCanceledOnTouchOutside(false)
        rootView?.findViewById<ProgressBar>(R.id.pb_update)?.progressTintList = ColorStateList.valueOf(this.resources.getColor(R.color.dark_orange))
        startUpdatingUIThread()
    }

    @SuppressLint("SetTextI18n", "Recycle")
    private fun startUpdatingUIThread() {
        var count = 0
        updatingUIThread = Thread {
            while (updateThreadFlag) {
                if (main?.getProgressUpdate() == 100) updateThreadFlag = false
                rootView?.findViewById<TextView>(R.id.tv_update_dialog_layout_title2)?.text = rootView?.findViewById<TextView>(R.id.tv_update_dialog_layout_title2)!!.text.split(" ")[0] + "  " + main?.getProgressUpdate()!! + "%"
                count++
                if (count >= 10) {
                    main?.runOnUiThread {
                        ObjectAnimator.ofInt(rootView?.findViewById<ProgressBar>(R.id.pb_update), "progress", main?.getProgressUpdate()!!).setDuration(1000).start()
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