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
import com.bailout.stickk.databinding.FragmentHelpMonoAdvancedSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main.ChartFragment

@Suppress("DEPRECATION")
class AdvancedSettingsFragmentMono(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    private lateinit var binding: FragmentHelpMonoAdvancedSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHelpMonoAdvancedSettingsBinding.inflate(layoutInflater)
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
            binding.imageView39.setImageDrawable(resources.getDrawable(R.drawable.help_image_39_ru))
            binding.imageView40.setImageDrawable(resources.getDrawable(R.drawable.help_image_40_ru))
            binding.imageView41.setImageDrawable(resources.getDrawable(R.drawable.help_image_41_ru))
            binding.imageView42.setImageDrawable(resources.getDrawable(R.drawable.help_image_42_ru))
            binding.imageView43.setImageDrawable(resources.getDrawable(R.drawable.help_image_43_ru))
            binding.imageView44.setImageDrawable(resources.getDrawable(R.drawable.help_image_44_ru))
            binding.imageView45.setImageDrawable(resources.getDrawable(R.drawable.help_image_49_ru))
        }

        //скрываем переключатель режимов ЕМГ для версий 2.37 и выше
        goneExtraView()
        if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.DRIVER_NUM, 1) >= 237) {
            binding.textView8811111111112.visibility = View.VISIBLE
            binding.textView1711111111112.visibility = View.VISIBLE
            binding.imageView45.visibility = View.VISIBLE
        }

        binding.sensorsSettingsBtn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }
    private fun goneExtraView() {
        binding.textView8811111111112.visibility = View.GONE
        binding.textView1711111111112.visibility = View.GONE
        binding.imageView39.visibility = View.GONE
    }
}