package com.bailout.stickk.ubi4.ui.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import androidx.core.graphics.drawable.toDrawable

data class DataCollectionSettings(
    val nCycles: Int,
    val baselineDuration: Int,
    val preGestDuration: Int,
    val atGestDuration: Int,
    val postGestDuration: Int
)

class EngineerModeFragment : BaseWidgetsFragment() {
    private var dataCollectionDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_engineer_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dataCollectionBtn = view.findViewById<View>(R.id.dataCollectionBtn)
        dataCollectionBtn?.setOnClickListener {
            showDataCollectionDialog()
        }
    }

    private fun showDataCollectionDialog() {
        if (dataCollectionDialog?.isShowing == true)
            return

        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_data_collection_settings, null)
        val dialog = Dialog(requireContext())

        dialog.setContentView(dialogBinding)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dataCollectionDialog = dialog
        dialog.show()

        setupDialogControls(dialogBinding)
        setupDialogButtons(dialogBinding, dialog)
    }

    private fun setupDialogControls(dialogBinding: View) {
        // N_CYCLES
        setupSlider(
            dialogBinding,
            R.id.nCyclesSliderSb,
            R.id.nCyclesSliderNumTv,
            R.id.nCyclesMinusBtnRipple,
            R.id.nCyclesPlusBtnRipple,
            minValue = 1,
            maxValue = 10,
            defaultValue = 3
        )

        // BASELINE_DURATION
        setupSlider(
            dialogBinding,
            R.id.baselineDurationSliderSb,
            R.id.baselineDurationSliderNumTv,
            R.id.baselineDurationMinusBtnRipple,
            R.id.baselineDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 10,
            defaultValue = 5
        )

        // PRE_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.preGestDurationSliderSb,
            R.id.preGestDurationSliderNumTv,
            R.id.preGestDurationMinusBtnRipple,
            R.id.preGestDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 5,
            defaultValue = 2
        )

        // AT_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.atGestDurationSliderSb,
            R.id.atGestDurationSliderNumTv,
            R.id.atGestDurationMinusBtnRipple,
            R.id.atGestDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 5,
            defaultValue = 2
        )

        // POST_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.postGestDurationSliderSb,
            R.id.postGestDurationSliderNumTv,
            R.id.postGestDurationMinusBtnRipple,
            R.id.postGestDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 5,
            defaultValue = 2
        )
    }

    private fun setupSlider(
        dialogBinding: View,
        seekBarId: Int,
        textViewId: Int,
        minusBtnId: Int,
        plusBtnId: Int,
        minValue: Int,
        maxValue: Int,
        defaultValue: Int
    ) {
        val seekBar = dialogBinding.findViewById<SeekBar>(seekBarId)
        val textView = dialogBinding.findViewById<TextView>(textViewId)
        val minusBtn = dialogBinding.findViewById<View>(minusBtnId)
        val plusBtn = dialogBinding.findViewById<View>(plusBtnId)

        seekBar.max = maxValue - minValue
        seekBar.progress = defaultValue - minValue
        textView.text = defaultValue.toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + minValue
                textView.text = value.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        minusBtn.setOnClickListener {
            val currentValue = seekBar.progress + minValue
            if (currentValue > minValue)
                seekBar.progress = currentValue - 1 - minValue
        }

        plusBtn.setOnClickListener {
            val currentValue = seekBar.progress + minValue
            if (currentValue < maxValue)
                seekBar.progress = currentValue + 1 - minValue
        }
    }

    private fun setupDialogButtons(dialogBinding: View, dialog: Dialog) {
        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsSaveBtn)
        saveBtn.setOnClickListener {
            val settings = collectDialogSettings(dialogBinding)
//            saveDataCollectionSettings(settings)
            dialog.dismiss()
        }

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsCancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun getSliderValue(dialogBinding: View, seekBarId: Int, minValue: Int = 1): Int {
        val seekBar = dialogBinding.findViewById<SeekBar>(seekBarId)
        return seekBar.progress + minValue
    }

    private fun collectDialogSettings(dialogBinding: View): DataCollectionSettings {
        return DataCollectionSettings(
            nCycles = getSliderValue(dialogBinding, R.id.nCyclesSliderSb),
            baselineDuration = getSliderValue(dialogBinding, R.id.baselineDurationSliderSb),
            preGestDuration = getSliderValue(dialogBinding, R.id.preGestDurationSliderSb),
            atGestDuration = getSliderValue(dialogBinding, R.id.atGestDurationSliderSb),
            postGestDuration = getSliderValue(dialogBinding, R.id.postGestDurationSliderSb)
        )
    }
}