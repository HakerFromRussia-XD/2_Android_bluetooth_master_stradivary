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
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.github.shchurov.horizontalwheelview.HorizontalWheelView
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import java.math.RoundingMode

class CustomDialogChangeValue(private var keyValue: String, private val callbackChartFragment: ChartFragmentCallback? = null): DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var changingValue = 0
    private var timerChangeValue: CountDownTimer? = null
    private val coefficient = 2.82

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

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        loadOldState()


        rootView?.findViewById<View>(R.id.plus_btn)?.setOnClickListener {
            changingValue += 1
            changingValue = checkRange(changingValue)
            rootView?.findViewById<HorizontalWheelView>(R.id.my_wheel_hwv)?.degreesAngle = ((changingValue*coefficient)-359)
            System.err.println("lol plus_btn " + ((changingValue*coefficient)-359) + "  changingValue= "+changingValue)
        }

        rootView?.findViewById<View>(R.id.minus_btn)?.setOnClickListener {
            changingValue -= 1
            changingValue = checkRange(changingValue)
            rootView?.findViewById<HorizontalWheelView>(R.id.my_wheel_hwv)?.degreesAngle = ((changingValue*coefficient)-359)
            System.err.println("lol minus_btn " + ((changingValue*coefficient)-359) + "  changingValue= "+changingValue)
        }


        rootView?.findViewById<View>(R.id.dialog_confirm_change_value_confirm)?.setOnClickListener {
            if (callbackChartFragment != null) {
                changeValue(keyValue, callbackChartFragment)
            }

            timerChangeValue?.cancel()
            dismiss()
        }


        rootView?.findViewById<View>(R.id.dialog_confirm_change_value_cancel)?.setOnClickListener {
            timerChangeValue?.cancel()
            dismiss()
        }


        timerChangeValue = object : CountDownTimer(5000000, 10) {
            override fun onTick(millisUntilFinished: Long) {
                val wheelValue: Double = rootView?.findViewById<HorizontalWheelView>(R.id.my_wheel_hwv)?.degreesAngle ?: 0.0
                val convertedResult: Int = ((wheelValue + 359) / coefficient).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toInt()//((wheelValue + 359) / coefficient).toInt()
                changingValue = convertedResult
                rootView?.findViewById<TextView>(R.id.value_tv)?.text =  convertedResult.toString()
            }

            override fun onFinish() {}
        }.start()

        rootView?.findViewById<SeekBar>(R.id.value_invisible_sb)?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rootView?.findViewById<HorizontalWheelView>(R.id.my_wheel_hwv)?.degreesAngle = ((progress*coefficient)-359)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }

    private fun changeValue(keyValue: String, callbackChartFragment: ChartFragmentCallback? = null) {
        when (keyValue) {
            PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM -> {
                callbackChartFragment?.changeCorrelatorNoiseThreshold1(changingValue)
            }
            PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM -> {
                callbackChartFragment?.changeCorrelatorNoiseThreshold2(changingValue)
            }
        }
    }
    private fun checkRange(value: Int): Int {
        if (value > 255) { return 255 }
        if (value < 0) { return 0 }
        return changingValue
    }
    private fun animatedWheel(position: Int, timeMs: Int) {
        ObjectAnimator.ofInt(
            rootView?.findViewById<SeekBar>(R.id.value_invisible_sb),
            "progress",
            position
        ).setDuration(timeMs.toLong()).start()
    }
    private fun loadOldState() {
        System.err.println("test save correlator value" + main?.mDeviceAddress + keyValue)
        changingValue = 255 -  mSettings!!.getInt(main?.mDeviceAddress + keyValue,127)
        animatedWheel(changingValue, 500)
    }
}