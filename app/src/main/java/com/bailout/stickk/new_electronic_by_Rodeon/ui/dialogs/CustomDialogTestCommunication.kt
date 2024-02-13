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
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView
import io.reactivex.android.schedulers.AndroidSchedulers
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity


@Suppress("DEPRECATION")
class CustomDialogTestCommunication: DialogFragment() {
    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var countReceivedCommandToCommunicationTest = 0
    private var percentageCommunicationQuality = 0


    @SuppressLint("CheckResult")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_progress_communication_test, container, false)
        rootView = view
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (activity != null) {
            main = activity as MainActivity?
        }


        val test = RxUpdateMainEvent.getInstance().communicationTestResult

        test
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { attempt ->
                countReceivedCommandToCommunicationTest += 1
                when (attempt) {
                    1 -> { percentageCommunicationQuality += 10 }
                    2 -> { percentageCommunicationQuality += 8 }
                    3 -> { percentageCommunicationQuality += 6 }
                    4 -> { percentageCommunicationQuality += 4 }
                    5 -> { percentageCommunicationQuality += 2 }
                }
                System.err.println("CustomDialogTestCommunication attempt: $attempt   countReceivedCommandToCommunicationTest: $countReceivedCommandToCommunicationTest")
                changeLoader((countReceivedCommandToCommunicationTest + 1) * 90)
                if (countReceivedCommandToCommunicationTest == 10) {
                    main?.openTestConnectionResultDialog(percentageCommunicationQuality)
                    percentageCommunicationQuality = 0
                    main?.testingConnection = false

                    test
                        .compose(main?.bindToLifecycle())
                        .observeOn(AndroidSchedulers.mainThread())
                        .unsubscribeOn(AndroidSchedulers.mainThread())

                    dismiss()
                    System.err.println("Test completed!!!!!!!  attempt percentageCommunicationQuality: $percentageCommunicationQuality")
                }
            }
        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        rootView?.findViewById<ProgressBar>(R.id.pb_test_communication)?.progressTintList = ColorStateList.valueOf(this.resources.getColor(R.color.dark_orange))
        changeLoader(100)

        rootView?.findViewById<LottieAnimationView>(R.id.test_communication_animation_view)?.setAnimation(R.raw.test_communication_animation)

        rootView?.findViewById<View>(R.id.dialog_test_communication_cancel)?.setOnClickListener {
            countReceivedCommandToCommunicationTest = 0
            percentageCommunicationQuality = 0
            main?.testingConnection = false
            dismiss()
        }
    }


    private fun changeLoader(value: Int) {
        main?.runOnUiThread {
            ObjectAnimator.ofInt(rootView?.findViewById<ProgressBar>(R.id.pb_test_communication), "progress", value).setDuration(1000).start()
        }
    }
}