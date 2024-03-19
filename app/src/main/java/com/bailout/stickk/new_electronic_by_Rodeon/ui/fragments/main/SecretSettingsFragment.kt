package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bailout.stickk.R.drawable.*
import com.bailout.stickk.databinding.LayoutSecretSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers


@Suppress("DEPRECATION")
class SecretSettingsFragment: Fragment(){

    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private val countRestart = 5

    private lateinit var binding: LayoutSecretSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutSecretSettingsBinding.inflate(layoutInflater)
        if (activity != null) { main = activity as MainActivity? }
        return binding.root
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        initializeUI()

        binding.lockSecretSettingsBtn.setOnClickListener {
            if (mSettings?.getBoolean(PreferenceKeys.ENTER_SECRET_PIN, false) == true) {
                main?.saveBool(PreferenceKeys.ENTER_SECRET_PIN, false)
                binding.lockSecretSettingsBtn.setImageResource(ic_lock)
                main?.showToast("Настройки заблокированы")
            } else {
                main?.saveBool(PreferenceKeys.ENTER_SECRET_PIN, true)
                binding.lockSecretSettingsBtn.setImageResource(ic_unlock)
                main?.showToast("Настройки разблокированы")
            }
        }

        binding.prosthesisModeSwapPsv.setOnSpinnerItemSelectedListener<String> { _, _, prosthesisMode, _ ->
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_PROSTHESIS, prosthesisMode)
            sendProsthesisMode()
        }

        binding.autocalibrationIndyPsv.setOnSpinnerItemSelectedListener<String> { _, _, gestureType, _ ->
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.AUTOCALIBRATION_MODE, gestureType)
            sendGestureType(gestureType.toByte())
        }

        binding.setNumberOfCyclesStandBtn.setOnClickListener {
            var numberOfCyclesStand = 0
            try {
                numberOfCyclesStand = binding.numberOfCyclesStandEt.text.toString().toInt()
                if (numberOfCyclesStand > 65535) {
                    numberOfCyclesStand = 65535
                    main?.showToast("Вы ввели слишком большое число")
                    binding.numberOfCyclesStandEt.setText(numberOfCyclesStand.toString())
                }
                when (numberOfCyclesStand) {
                    0 -> {main?.showToast("Бесконечное количество циклов")}
                    else -> {main?.showToast("Количество циклов: ${numberOfCyclesStand*10}")}
                }
            } catch (err : Exception)  {
                main?.showToast("Введите число без пробелов и спецсимволов")
            }

            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.MAX_STAND_CYCLES, numberOfCyclesStand)
            sendProsthesisMode()
        }

        binding.fullResetBtn.setOnClickListener {
            main?.showHardResetDialog()
        }

        binding.autocalibrationBtn.setOnClickListener {
            sendAutocalibration()
        }


        RxUpdateMainEvent.getInstance().uiSecretSettings
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                initializeUI()
            }
    }

    private fun initializeUI() {
        when {
            main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X) -> {
                System.err.println("DEVICE_TYPE FEST_X")
                binding.autocalibrationIndyRl.visibility = View.GONE
                binding.gestureTypeRl.visibility = View.GONE
            }
            else -> {
                System.err.println("DEVICE_TYPE НЕ FEST_X")
                binding.numberOfCyclesStandRl.visibility = View.GONE
                binding.prosthesisModeRl.visibility = View.GONE
                binding.fullResetRl.visibility = View.GONE
                binding.autocalibrationRl.visibility = View.GONE
            }
        }

        try {
            binding.prosthesisModeSwapPsv.selectItemByIndex(mSettings!!.getInt(
                main?.mDeviceAddress + PreferenceKeys.SET_MODE_PROSTHESIS,
                0
            ))
//            binding.autocalibrationIndyPsv.selectItemByIndex(mSettings!!.getInt(
//                main?.mDeviceAddress + PreferenceKeys.AUTOCALIBRATION_MODE, 0
//            ))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.gestureTypeNumTv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GESTURE_TYPE, 0).toString()
        binding.autocalibrationIndyNumTv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.AUTOCALIBRATION_MODE, 0).toString()

        binding.numberOfCyclesStandEt.setText((mSettings!!.getInt(
            main?.mDeviceAddress + PreferenceKeys.MAX_STAND_CYCLES,
            0
        )).toString())
    }

    private fun sendGestureType(gestureType: Byte) {
        main?.bleCommandConnector(byteArrayOf(gestureType), SET_AUTOCALIBRATION, WRITE, 19)
    }
    private fun sendProsthesisMode() {
        val setReverse = if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)) { 1 } else { 0 }
        val numActiveGestures = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 8)
        val prosthesisMode = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_PROSTHESIS, 0)
        val numberOfCyclesStand = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.MAX_STAND_CYCLES, 0)


        main?.runSendCommand(byteArrayOf(
            setReverse.toByte(),
            numActiveGestures.toByte(),
            prosthesisMode.toByte(),
            numberOfCyclesStand.toByte(),
            (numberOfCyclesStand/256).toByte(),
            ), SET_REVERSE_NEW_VM, countRestart)
    }
    private fun sendAutocalibration() {
        val correlatorNoiseThreshold1 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, 16)
        val correlatorNoiseThreshold2 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, 16)
        val modeEMG = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,9)

        main?.runSendCommand(byteArrayOf(
            correlatorNoiseThreshold1.toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
            64, correlatorNoiseThreshold2.toByte(), 6, 1, 0x10, 36, 18,
            44, 52, 64, 72, 0x40, 5, 64, modeEMG.toByte(), 0x01
        ), SENS_OPTIONS_NEW_VM, countRestart)
    }
}

