package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomInfoCalibrationDialogFragment: DialogFragment() {
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
        val view = inflater.inflate(R.layout.layout_calibrating_le, container, false)
        rootView = view
        if (activity != null) { main = activity as MainActivity? }
        return view
    }


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

        dialog!!.setCanceledOnTouchOutside(false)
        startUpdatingUIThread()
    }

    @SuppressLint("SetTextI18n")
    private fun startUpdatingUIThread() {
        updatingUIThread = Thread {
            while (updateThreadFlag) {
                System.err.println("---> калибровка идёт calibrationStage: " + main?.calibrationStage)
                if (main?.calibrationStage == 6) updateThreadFlag = false //если все пальцы успешно откалиброванны
                if (main?.calibrationStage == 5) updateThreadFlag = false //если хоть один палец слишком сильно затянут
                if (main?.calibrationStage == 4) updateThreadFlag = false //если хоть один палец прокручивается
                if (main?.calibrationStage == 3) updateThreadFlag = false //если хоть на одном пальце отключен энкодер
                if (main?.calibrationStage == 2) updateThreadFlag = false //если хоть на одном пальце отключен мотор
                if (main?.calibrationStage == 1) updateThreadFlag = false //если протез калибруется //TODO удалить когда мы поймём, почему у нас
                if (main?.calibrationStage == 0) updateThreadFlag = false //если протез не откалиброван

                try {
                    Thread.sleep(100)
                } catch (ignored: Exception) {}
            }
            dismiss()
            if (main?.locate?.contains("ru")!!) {
                main?.showToast("Калибровка завершена!")
            } else {
                main?.showToast("Calibration completed!")
            }

        }
        updatingUIThread!!.start()
    }
}