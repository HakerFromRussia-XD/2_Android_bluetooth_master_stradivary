package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color.*
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.R.drawable.*
import com.bailout.stickk.databinding.LayoutSecretSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity


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
    }

    private fun initializeUI() {

        try {
            binding.prosthesisModeSwapPsv.selectItemByIndex(mSettings!!.getInt(
                main?.mDeviceAddress + PreferenceKeys.SET_MODE_PROSTHESIS,
                0
            ))
        } catch (e: Exception) {
            e.printStackTrace()
        }


        binding.numberOfCyclesStandEt.setText((mSettings!!.getInt(
            main?.mDeviceAddress + PreferenceKeys.MAX_STAND_CYCLES,
            0
        )).toString())
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
            (numberOfCyclesStand/256).toByte(),
            numberOfCyclesStand.toByte()
            ), SET_REVERSE_NEW_VM, countRestart)
    }
}

