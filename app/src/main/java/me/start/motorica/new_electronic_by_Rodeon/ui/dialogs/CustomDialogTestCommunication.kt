package me.start.motorica.new_electronic_by_Rodeon.ui.dialogs

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
import androidx.fragment.app.DialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_progress_communication_test.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import java.util.concurrent.Flow


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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        pb_test_communication?.progressTintList = ColorStateList.valueOf(this.resources.getColor(R.color.darkOrange))
        changeLoader(100)

        test_communication_animation_view.setAnimation(R.raw.test_communication_animation)

        dialog_test_communication_cancel.setOnClickListener {
            countReceivedCommandToCommunicationTest = 0
            percentageCommunicationQuality = 0
            main?.testingConnection = false
            dismiss()
        }
    }


    private fun changeLoader(value: Int) {
        main?.runOnUiThread {
            ObjectAnimator.ofInt(pb_test_communication, "progress", value).setDuration(1000).start()
        }
    }
}