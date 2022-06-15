package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.layout_calibrating_le.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomInfoCalibrationDialogFragment: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var updatingUIThread: Thread? = null
    private var updateThreadFlag = true
    private var timer: CountDownTimer? = null

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


    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        if (main?.locate?.contains("ru")!!) {
            if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
                tv_update_dialog_layout_title?.text = "Калибровка правой руки"
            } else {
                tv_update_dialog_layout_title?.text = "Калибровка левой руки"
            }
        } else {
            if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
                tv_update_dialog_layout_title?.text = "Calibrating right hand"
            } else {
                tv_update_dialog_layout_title?.text = "Calibrating left hand"
            }
        }

        dialog!!.setCanceledOnTouchOutside(false)
        startUpdatingUIThread()
        timerDeactivation()
    }

    @SuppressLint("SetTextI18n")
    private fun startUpdatingUIThread() {
        updatingUIThread = Thread {
            while (updateThreadFlag) {
//                System.err.println("---> статус калибровки calibrationStage: " + main?.calibrationStage)
                if (main?.calibrationStage == 6) updateThreadFlag = false //если все пальцы успешно откалиброванны
                if (main?.calibrationStage == 5) updateThreadFlag = false //если хоть один палец слишком сильно затянут
                if (main?.calibrationStage == 4) updateThreadFlag = false //если хоть один палец прокручивается
                if (main?.calibrationStage == 3) updateThreadFlag = false //если хоть на одном пальце отключен энкодер
                if (main?.calibrationStage == 2) updateThreadFlag = false //если хоть на одном пальце отключен мотор
                if (main?.calibrationStage == 1) updateThreadFlag = false //если протез калибруется
                if (main?.calibrationStage == 0) updateThreadFlag = false //если протез не откалиброван

                if (!main?.calibrationDialogOpen!!) {
                    updateThreadFlag = false
                }
                try {
                    Thread.sleep(100)
                } catch (ignored: Exception) {}
            }
            saveInt(main?.mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 0)
            dismiss()
            if (main?.locate?.contains("ru")!!) {
                main?.showToast("Калибровка завершена!")
            } else {
                main?.showToast("Calibration completed!")
            }

        }
        updatingUIThread!!.start()
    }

    //скрывает окно калибровки в любом случае по прошествии пяти секунд
    private fun timerDeactivation() {
        timer?.cancel()
        timer = object : CountDownTimer(5000, 1) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                saveInt(main?.mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 0)
                dismiss()
            }
        }.start()
    }

    private fun saveInt(key: String, variable: Int) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putInt(key, variable)
        editor.apply()
    }
}