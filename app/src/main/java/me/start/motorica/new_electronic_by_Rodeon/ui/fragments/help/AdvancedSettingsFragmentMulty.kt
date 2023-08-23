package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class AdvancedSettingsFragmentMulty(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_help_multy_advanced_settings, container, false)
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
        rootView?.findViewById<Button>(R.id.title_click_block_btn)?.setOnClickListener {  }
        if (main?.locate?.contains("ru")!!) {
            rootView?.findViewById<ImageView>(R.id.imageView22)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_22_ru))
            rootView?.findViewById<ImageView>(R.id.imageView23)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_23_ru))
            rootView?.findViewById<ImageView>(R.id.imageView24)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_24_ru))
            rootView?.findViewById<ImageView>(R.id.imageView25)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_25_ru))
            rootView?.findViewById<ImageView>(R.id.imageView26)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_26_ru))
            rootView?.findViewById<ImageView>(R.id.imageView27)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_27_ru))
            rootView?.findViewById<ImageView>(R.id.imageView28)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_28_ru))
            rootView?.findViewById<ImageView>(R.id.imageView29)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_29_ru))
            rootView?.findViewById<ImageView>(R.id.imageView30)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_30_ru))
            rootView?.findViewById<ImageView>(R.id.imageView31)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_31_ru))
            rootView?.findViewById<ImageView>(R.id.imageView32)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_32_ru))
            rootView?.findViewById<ImageView>(R.id.imageView33)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_33_ru))
            rootView?.findViewById<ImageView>(R.id.imageView34)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_34_ru))
            rootView?.findViewById<ImageView>(R.id.imageView35)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_35_ru))
            rootView?.findViewById<ImageView>(R.id.imageView36)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_36_ru))
            rootView?.findViewById<ImageView>(R.id.imageView37)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_37_ru))
            rootView?.findViewById<ImageView>(R.id.imageView38)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_38_ru))

        }

        rootView?.findViewById<Button>(R.id.sensors_settings_btn)?.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }
        rootView?.findViewById<Button>(R.id.settings_gesture_btn)?.setOnClickListener { navigator().showGesturesHelpScreen(chartFragmentClass) }

        rootView?.findViewById<Button>(R.id.back_btn)?.setOnClickListener { navigator().goingBack() }
    }
}