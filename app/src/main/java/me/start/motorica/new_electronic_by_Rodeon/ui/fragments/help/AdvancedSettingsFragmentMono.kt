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
import me.start.motorica.databinding.FragmentHelpMonoAdvancedSettingsBinding
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

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
        }

        binding.sensorsSettingsBtn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }
}