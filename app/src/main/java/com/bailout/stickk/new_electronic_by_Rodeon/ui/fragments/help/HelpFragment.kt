package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.help

import android.annotation.SuppressLint
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
import com.bailout.stickk.databinding.FragmentHelpBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

@Suppress("DEPRECATION")
class HelpFragment(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private val reactivatedInterface: ReactivatedChart = chartFragmentClass
    private var mSettings: SharedPreferences? = null
    private var multigrib = false

    private lateinit var binding: FragmentHelpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHelpBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }

        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
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
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        initializeUI()
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }

        binding.backBtn.setOnClickListener {
            navigator().goingBack()
            reactivatedInterface.reactivatedChart()
        }

        binding.sensorsSettingsBtn.setOnClickListener { navigator().showSensorsHelpScreen(chartFragmentClass) }
        binding.settingsGestureBtn.setOnClickListener { navigator().showGesturesHelpScreen(chartFragmentClass) }
        binding.advancedSettingsBtn.setOnClickListener {
            if (multigrib) { navigator().showHelpMultyAdvancedSettingsScreen(chartFragmentClass) }
            else { navigator().showHelpMonoAdvancedSettingsScreen(chartFragmentClass) }
        }
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
            binding.advancedSettingsRl.visibility = View.VISIBLE
        } else {
            binding.advancedSettingsRl.visibility = View.GONE
        }
        if (multigrib) { binding.gestureCustomizationRl.visibility =View.VISIBLE }
        else { binding.gestureCustomizationRl.visibility =View.GONE }


        binding.howProsthesesWorksBtn.setOnClickListener {
            if (multigrib) { navigator().showHowProsthesesWorksScreen() }
            else { navigator().showHowProsthesesWorksMonoScreen() }
        }
        binding.howToPutOnAProsthesesSocketBtn.setOnClickListener { navigator().showHowPutOnTheProsthesesSocketScreen() }
        binding.complectationBtn.setOnClickListener { navigator().showCompleteSetScreen() }
        binding.prosthesesChargeBtn.setOnClickListener { navigator().showChargingTheProsthesesScreen() }
        binding.prosthesesCareBtn.setOnClickListener { navigator().showProsthesesCareScreen() }
        binding.serviceAndWarrantyBtn.setOnClickListener { navigator().showServiceAndWarrantyScreen() }

        binding.contactSupportBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:88007077197"))
            if (intent.resolveActivity( main!!.packageManager ) != null) {
                startActivity(intent)
            }
        }

        binding.vkBtn.setOnClickListener {
            val url = "https://vk.com/motorica"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
        binding.telegrammBtn.setOnClickListener {
            val url = "https://t.me/motoricans"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
    }
}