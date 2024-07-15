package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.help

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.FragmentSensorSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.DecoratorChange
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main.ChartFragment

@Suppress("DEPRECATION")
class SensorsFragment(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private val decoratorChanger: DecoratorChange = chartFragmentClass
    private var multigrib = false

    private lateinit var binding: FragmentSensorSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSensorSettingsBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        multigrib = (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_A)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_BT05)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_MY_IPHONE)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        initializeUI()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initializeUI() {

        binding.titleClickBlockBtn.setOnClickListener {  }
        if (main?.locate?.contains("ru")!!) {
            binding.imageView9.setImageDrawable(resources.getDrawable(R.drawable.help_image_9_ru))
            binding.imageView10.setImageDrawable(resources.getDrawable(R.drawable.help_image_10_ru))
        }

        binding.showInteractiveInstructionBtn.setOnClickListener { decoratorChanger.setStartDecorator() }
        binding.settingsGestureBtn.setOnClickListener { navigator().showGesturesHelpScreen(chartFragmentClass) }
        binding.advancedSettingsBtn.setOnClickListener {
            if (multigrib) { navigator().showHelpMultyAdvancedSettingsScreen(chartFragmentClass) }
            else { navigator().showHelpMonoAdvancedSettingsScreen(chartFragmentClass) }
        }

        // регуляция показа кнопок из подвала в зависимости от ситуации (односхват/многосхват,
        // расширенные настройки открыты/закрыты)
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            binding.appInstructionTitle2Tv.visibility = View.VISIBLE
            binding.mainControlsCv.visibility = View.VISIBLE
            binding.advancedSettingsRl.visibility = View.VISIBLE
        }
        if (multigrib) {
            binding.appInstructionTitle2Tv.visibility = View.VISIBLE
            binding.mainControlsCv.visibility = View.VISIBLE
            binding.gestureCustomizationRl.visibility =View.VISIBLE
        }

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }
}