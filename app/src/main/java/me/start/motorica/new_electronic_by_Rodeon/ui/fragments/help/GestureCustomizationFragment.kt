package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.start.motorica.R
import me.start.motorica.databinding.FragmentGestureCustomizationBinding
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

@Suppress("DEPRECATION")
class GestureCustomizationFragment(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var multigrib = false

    private lateinit var binding: FragmentGestureCustomizationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGestureCustomizationBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        multigrib = (main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
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
            binding.imageView14.setImageDrawable(resources.getDrawable(R.drawable.help_image_14_ru))
            binding.imageView15.setImageDrawable(resources.getDrawable(R.drawable.help_image_15_ru))
            binding.imageView16.setImageDrawable(resources.getDrawable(R.drawable.help_image_16_ru))
            binding.imageView18.setImageDrawable(resources.getDrawable(R.drawable.help_image_18_ru))
            binding.imageView19.setImageDrawable(resources.getDrawable(R.drawable.help_image_19_ru))
            binding.imageView20.setImageDrawable(resources.getDrawable(R.drawable.help_image_20_ru))
            binding.imageView21.setImageDrawable(resources.getDrawable(R.drawable.help_image_21_ru))
        }

        binding.sensorsSettingsBtn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }
        binding.advancedSettingsBtn.setOnClickListener { navigator().showHelpMultyAdvancedSettingsScreen(chartFragmentClass) }

        // регуляция показа кнопок из подвала в зависимости от ситуации (односхват/многосхват,
        // расширенные настройки открыты/закрыты)
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            binding.appInstructionTitle2Tv.visibility = View.VISIBLE
            binding.mainControlsCv.visibility = View.VISIBLE
            binding.advancedSettingsBtn.visibility = View.VISIBLE
        }
        if (multigrib) {
            binding.appInstructionTitle2Tv.visibility = View.VISIBLE
            binding.mainControlsCv.visibility = View.VISIBLE
            binding.gestureCustomizationRl.visibility = View.VISIBLE
        }

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }
}