package me.start.motorica.new_electronic_by_Rodeon.ui.dialogs

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_change_value.*
import kotlinx.android.synthetic.main.dialog_change_value.view.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class CustomDialogChangeValue(keyValue: String): DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var changingValue = 0
    private var keyValue = keyValue
    private var timerChangeValue: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_change_value, container, false)
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


        rootView?.plus_btn?.setOnClickListener {
            changingValue += 1
            rootView?.my_wheel_hwv?.degreesAngle = ((changingValue*2.8)-359)
            System.err.println("lol plus_btn " + ((changingValue*2.8)-359))
//            setProgressInvisibleSB(changingValue)
//            animatedWheel(changingValue, 200)
        }


        rootView?.minus_btn?.setOnClickListener {
            changingValue -= 1
            rootView?.my_wheel_hwv?.degreesAngle = ((changingValue*2.8)-359)
            System.err.println("lol minus_btn " + ((changingValue*2.8)-359))
//            setProgressInvisibleSB(changingValue)
//            animatedWheel(changingValue, 200)
        }


        rootView?.dialog_confirm_change_value_confirm?.setOnClickListener {
            timerChangeValue?.cancel()
            dismiss()
        }


        rootView?.dialog_confirm_change_value_cancel?.setOnClickListener {
            timerChangeValue?.cancel()
            dismiss()
        }


        timerChangeValue = object : CountDownTimer(5000000, 10) {
            override fun onTick(millisUntilFinished: Long) {
                val wheelValue = rootView?.my_wheel_hwv?.degreesAngle?.toInt() ?: 0
                var convertedResult = 0
                convertedResult = ((wheelValue + 359) / 2.8).toInt()
//                changingValue = convertedResult
                rootView?.value_tv?.text =  convertedResult.toString()
            }

            override fun onFinish() {}
        }.start()


        rootView?.value_invisible_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rootView?.my_wheel_hwv?.degreesAngle = ((progress*2.8)-359)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }


    private fun setProgressInvisibleSB(process: Int) {
        rootView?.value_invisible_sb?.progress = process
    }
    private fun animatedWheel(position: Int, timeMs: Int) {
        ObjectAnimator.ofInt(
            value_invisible_sb,
            "progress",
            position
        ).setDuration(timeMs.toLong()).start()
    }
    private fun loadOldState() {
        changingValue = 255 -  mSettings!!.getInt(main?.mDeviceAddress + keyValue,127)
        animatedWheel(changingValue, 500)
    }
}