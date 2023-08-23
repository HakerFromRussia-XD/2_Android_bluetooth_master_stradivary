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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.DecoratorChange
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class SensorsFragment(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private val decoratorChanger: DecoratorChange = chartFragmentClass
    private var multigrib = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_sensor_settings, container, false)
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
        rootView?.findViewById<Button>(R.id.title_click_block_btn)?.setOnClickListener {  }
        if (main?.locate?.contains("ru")!!) {
            rootView?.findViewById<ImageView>(R.id.imageView9)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_9_ru))
            rootView?.findViewById<ImageView>(R.id.imageView10)?.setImageDrawable(resources.getDrawable(R.drawable.help_image_10_ru))
        }

        rootView?.findViewById<Button>(R.id.show_interactive_instruction_btn)?.setOnClickListener { decoratorChanger.setStartDecorator() }
        rootView?.findViewById<Button>(R.id.settings_gesture_btn)?.setOnClickListener { navigator().showGesturesHelpScreen(chartFragmentClass) }
        rootView?.findViewById<Button>(R.id.advanced_settings_btn)?.setOnClickListener {
            if (multigrib) { navigator().showHelpMultyAdvancedSettingsScreen(chartFragmentClass) }
            else { navigator().showHelpMonoAdvancedSettingsScreen(chartFragmentClass) }
        }

        // регуляция показа кнопок из подвала в зависимости от ситуации (односхват/многосхват,
        // расширенные настройки открыты/закрыты)
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            rootView?.findViewById<TextView>(R.id.app_instruction_title_2_tv)?.visibility = View.VISIBLE
            rootView?.findViewById<CardView>(R.id.main_controls_cv)?.visibility = View.VISIBLE
            rootView?.findViewById<RelativeLayout>(R.id.advanced_settings_rl)?.visibility = View.VISIBLE
        }
        if (multigrib) {
            rootView?.findViewById<TextView>(R.id.app_instruction_title_2_tv)?.visibility = View.VISIBLE
            rootView?.findViewById<CardView>(R.id.main_controls_cv)?.visibility = View.VISIBLE
            rootView?.findViewById<RelativeLayout>(R.id.gesture_customization_rl)?.visibility =View.VISIBLE
        }

        rootView?.findViewById<Button>(R.id.back_btn)?.setOnClickListener { navigator().goingBack() }
    }
}