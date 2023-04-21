package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_help_mono_advanced_settings.*
import kotlinx.android.synthetic.main.fragment_sensor_settings.back_btn
import kotlinx.android.synthetic.main.fragment_sensor_settings.title_click_block_btn
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.DecoratorChange
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class AdvancedSettingsFragmentMono(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_help_mono_advanced_settings, container, false)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.rootView = rootView
        this.mContext = context
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        initializeUI()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initializeUI() {
        title_click_block_btn.setOnClickListener {  }
        if (main?.locate?.contains("ru")!!) {
            imageView39.setImageDrawable(resources.getDrawable(R.drawable.help_image_39_ru))
            imageView40.setImageDrawable(resources.getDrawable(R.drawable.help_image_40_ru))
            imageView41.setImageDrawable(resources.getDrawable(R.drawable.help_image_41_ru))
            imageView42.setImageDrawable(resources.getDrawable(R.drawable.help_image_42_ru))
            imageView43.setImageDrawable(resources.getDrawable(R.drawable.help_image_43_ru))
            imageView44.setImageDrawable(resources.getDrawable(R.drawable.help_image_44_ru))
        }

        sensors_settings_btn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }


        back_btn.setOnClickListener { navigator().goingBack() }
    }
}