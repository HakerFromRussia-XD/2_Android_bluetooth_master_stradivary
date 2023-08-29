package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class HelpFragment(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private val reactivatedInterface: ReactivatedChart = chartFragmentClass
    private var mSettings: SharedPreferences? = null
    private var multigrib = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_help, container, false)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.rootView = rootView
        this.rootView!!.isFocusableInTouchMode = true
        this.rootView!!.requestFocus()
        this.rootView!!.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                navigator().goingBack()
                reactivatedInterface.reactivatedChart()
                requireFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                return@OnKeyListener true
            }
            false
        })
        multigrib = (main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X))
        this.mContext = context
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        initializeUI()
    }


    private fun initializeUI() {
        rootView?.findViewById<View>(R.id.title_click_block_btn)?.setOnClickListener {  }

        rootView?.findViewById<View>(R.id.back_btn)?.setOnClickListener {
            navigator().goingBack()
            reactivatedInterface.reactivatedChart()
        }

        rootView?.findViewById<View>(R.id.sensors_settings_btn)?.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }
        rootView?.findViewById<View>(R.id.settings_gesture_btn)?.setOnClickListener { navigator().showGesturesHelpScreen(chartFragmentClass) }
        rootView?.findViewById<View>(R.id.advanced_settings_btn)?.setOnClickListener {
            if (multigrib) { navigator().showHelpMultyAdvancedSettingsScreen(chartFragmentClass) }
            else { navigator().showHelpMonoAdvancedSettingsScreen(chartFragmentClass) }
        }
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            rootView?.findViewById<RelativeLayout>(R.id.advanced_settings_rl)?.visibility = View.VISIBLE
        } else {
            rootView?.findViewById<RelativeLayout>(R.id.advanced_settings_rl)?.visibility = View.GONE
        }
        if (multigrib) {  rootView?.findViewById<RelativeLayout>(R.id.gesture_customization_rl)?.visibility =View.VISIBLE }
        else {  rootView?.findViewById<RelativeLayout>(R.id.gesture_customization_rl)?.visibility =View.GONE }


        rootView?.findViewById<View>(R.id.how_prostheses_works_btn)?.setOnClickListener {
            if (multigrib) { navigator().showHowProsthesesWorksScreen() }
            else { navigator().showHowProsthesesWorksMonoScreen() }
        }
        rootView?.findViewById<View>(R.id.how_to_put_on_a_prostheses_socket_btn)?.setOnClickListener { navigator().showHowPutOnTheProsthesesSocketScreen() }
        rootView?.findViewById<View>(R.id.complectation_btn)?.setOnClickListener { navigator().showCompleteSetScreen() }
        rootView?.findViewById<View>(R.id.prostheses_charge_btn)?.setOnClickListener { navigator().showChargingTheProsthesesScreen() }
        rootView?.findViewById<View>(R.id.prostheses_care_btn)?.setOnClickListener { navigator().showProsthesesCareScreen() }
        rootView?.findViewById<View>(R.id.service_and_warranty_btn)?.setOnClickListener { navigator().showServiceAndWarrantyScreen() }

        rootView?.findViewById<View>(R.id.contact_support_btn)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:88007077197"))
            if (intent.resolveActivity( main!!.packageManager ) != null) {
                startActivity(intent)
            }
        }

        rootView?.findViewById<ImageView>(R.id.vk_btn)?.setOnClickListener {
            val url = "https://vk.com/motorica"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
        rootView?.findViewById<ImageView>(R.id.telegramm_btn)?.setOnClickListener {
            val url = "https://t.me/motoricans"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
    }
}