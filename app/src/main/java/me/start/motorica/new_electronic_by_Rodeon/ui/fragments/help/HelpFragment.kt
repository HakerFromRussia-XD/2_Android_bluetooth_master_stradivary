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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_help.*
import kotlinx.android.synthetic.main.fragment_help.back_btn
import kotlinx.android.synthetic.main.fragment_help.complectation_btn
import kotlinx.android.synthetic.main.fragment_help.contact_support_btn
import kotlinx.android.synthetic.main.fragment_help.how_prosthesis_works_btn
import kotlinx.android.synthetic.main.fragment_help.how_to_put_on_a_prosthesis_socket_btn
import kotlinx.android.synthetic.main.fragment_help.prosthesis_care_btn
import kotlinx.android.synthetic.main.fragment_help.prosthesis_charge_btn
import kotlinx.android.synthetic.main.fragment_help.sensors_settings_btn
import kotlinx.android.synthetic.main.fragment_help.service_and_warranty_btn
import kotlinx.android.synthetic.main.fragment_help.telegramm_btn
import kotlinx.android.synthetic.main.fragment_help.title_click_block_btn
import kotlinx.android.synthetic.main.fragment_help.vk_btn
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class HelpFragment(chartFragmentClass: ChartFragment) : Fragment() {
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
        title_click_block_btn.setOnClickListener {  }

        back_btn.setOnClickListener {
            navigator().goingBack()
            reactivatedInterface.reactivatedChart()
        }

        sensors_settings_btn.setOnClickListener { navigator().showSensorsHelpScreen() }
        settings_gesture_btn.setOnClickListener { navigator().showGesturesHelpScreen() }
        advanced_settings_btn.setOnClickListener {  }
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            advanced_settings_rl.visibility = View.VISIBLE
        } else {
            advanced_settings_rl.visibility = View.GONE
        }
        if (multigrib) { gesture_customization_rl.visibility =View.VISIBLE }
        else { gesture_customization_rl.visibility =View.GONE }


        how_prosthesis_works_btn.setOnClickListener {
            if (multigrib) { navigator().showHowProsthesisWorksScreen() }
            else { navigator().showHowProsthesisWorksMonoScreen() }
        }
        how_to_put_on_a_prosthesis_socket_btn.setOnClickListener { navigator().showHowPutOnTheProthesisSocketScreen() }
        complectation_btn.setOnClickListener { navigator().showCompleteSetScreen() }
        prosthesis_charge_btn.setOnClickListener { navigator().showChargingTheProthesisScreen() }
        prosthesis_care_btn.setOnClickListener { navigator().showProsthesisCareScreen() }
        service_and_warranty_btn.setOnClickListener { navigator().showServiceAndWarrantyScreen() }

        contact_support_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:88007077197"))
            if (intent.resolveActivity( main!!.packageManager ) != null) {
                startActivity(intent)
            }
        }

        vk_btn.setOnClickListener {
            val url = "https://vk.com/motorica"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
        telegramm_btn.setOnClickListener {
            val url = "https://t.me/motoricans"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
    }
}