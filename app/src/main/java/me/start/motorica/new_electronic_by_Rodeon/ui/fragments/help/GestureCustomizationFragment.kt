package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_gesture_customization.*
import kotlinx.android.synthetic.main.fragment_gesture_customization.advanced_settings_btn
import kotlinx.android.synthetic.main.fragment_gesture_customization.gesture_customization_rl
import kotlinx.android.synthetic.main.fragment_gesture_customization.sensors_settings_btn
import kotlinx.android.synthetic.main.fragment_help.*
import kotlinx.android.synthetic.main.fragment_sensor_settings.advanced_settings_rl
import kotlinx.android.synthetic.main.fragment_sensor_settings.back_btn
import kotlinx.android.synthetic.main.fragment_sensor_settings.title_click_block_btn
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class GestureCustomizationFragment(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var multigrib = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_gesture_customization, container, false)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.rootView = rootView
        this.mContext = context
        multigrib = (main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X))
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
            imageView14.setImageDrawable(resources.getDrawable(R.drawable.help_image_14_ru))
            imageView15.setImageDrawable(resources.getDrawable(R.drawable.help_image_15_ru))
            imageView16.setImageDrawable(resources.getDrawable(R.drawable.help_image_16_ru))
            imageView18.setImageDrawable(resources.getDrawable(R.drawable.help_image_18_ru))
            imageView19.setImageDrawable(resources.getDrawable(R.drawable.help_image_19_ru))
            imageView20.setImageDrawable(resources.getDrawable(R.drawable.help_image_20_ru))
            imageView21.setImageDrawable(resources.getDrawable(R.drawable.help_image_21_ru))
        }

        sensors_settings_btn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }
        advanced_settings_btn.setOnClickListener { navigator().showHelpMultyAdvancedSettingsScreen(chartFragmentClass) }

        // регуляция показа кнопок из подвала в зависимости от ситуации (односхват/многосхват,
        // расширенные настройки открыты/закрыты)
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            app_instruction_title_2_tv.visibility = View.VISIBLE
            main_controls_cv.visibility = View.VISIBLE
            advanced_settings_rl.visibility = View.VISIBLE
        }
        if (multigrib) {
            app_instruction_title_2_tv.visibility = View.VISIBLE
            main_controls_cv.visibility = View.VISIBLE
            gesture_customization_rl.visibility =View.VISIBLE
        }

        back_btn.setOnClickListener { navigator().goingBack() }
    }
}