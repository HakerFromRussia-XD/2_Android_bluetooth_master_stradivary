package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.help

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.FragmentHelpMultyAdvancedSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

@Suppress("DEPRECATION")
class AdvancedSettingsFragmentMulty(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    private lateinit var binding: FragmentHelpMultyAdvancedSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHelpMultyAdvancedSettingsBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
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
            binding.imageView22.setImageDrawable(resources.getDrawable(R.drawable.help_image_22_ru))
            binding.imageView23.setImageDrawable(resources.getDrawable(R.drawable.help_image_23_ru))
            binding.imageView24.setImageDrawable(resources.getDrawable(R.drawable.help_image_24_ru))
            binding.imageView25.setImageDrawable(resources.getDrawable(R.drawable.help_image_25_ru))
            binding.imageView26.setImageDrawable(resources.getDrawable(R.drawable.help_image_26_ru))
            binding.imageView27.setImageDrawable(resources.getDrawable(R.drawable.help_image_27_ru))
            binding.imageView28.setImageDrawable(resources.getDrawable(R.drawable.help_image_28_ru))
            binding.imageView29.setImageDrawable(resources.getDrawable(R.drawable.help_image_29_ru))
            binding.imageView30.setImageDrawable(resources.getDrawable(R.drawable.help_image_30_ru))
            binding.imageView31.setImageDrawable(resources.getDrawable(R.drawable.help_image_31_ru))
            binding.imageView32.setImageDrawable(resources.getDrawable(R.drawable.help_image_32_ru))
            binding.imageView33.setImageDrawable(resources.getDrawable(R.drawable.help_image_33_ru))
            binding.imageView34.setImageDrawable(resources.getDrawable(R.drawable.help_image_34_ru))
            binding.imageView35.setImageDrawable(resources.getDrawable(R.drawable.help_image_35_ru))
            binding.imageView36.setImageDrawable(resources.getDrawable(R.drawable.help_image_36_ru))
            binding.imageView37.setImageDrawable(resources.getDrawable(R.drawable.help_image_37_ru))
            binding.imageView38.setImageDrawable(resources.getDrawable(R.drawable.help_image_38_ru))
            binding.imageView39.setImageDrawable(resources.getDrawable(R.drawable.help_image_49_ru))
        }

        //скрываем переключатель режимов ЕМГ для версий 2.37 и выше
        goneExtraView()
        System.err.println("driverVersionS AdvancedSettingsFragmentMulty ${main?.driverVersionS}")
        if (main?.driverVersionS != null) {
            val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
            if (driverNum.toInt() >= 237) {
                binding.textView8811111111112.visibility = View.VISIBLE
                binding.textView1711111111112.visibility = View.VISIBLE
                binding.imageView39.visibility = View.VISIBLE
            }
        }

        binding.sensorsSettingsBtn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }
        binding.settingsGestureBtn.setOnClickListener { navigator().showGesturesHelpScreen(chartFragmentClass) }

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }
    private fun goneExtraView() {
        binding.textView8811111111112.visibility = View.GONE
        binding.textView1711111111112.visibility = View.GONE
        binding.imageView39.visibility = View.GONE
    }
}