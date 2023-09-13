package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.start.motorica.databinding.FragmentHowProsthesisWorksBinding
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class HowProsthesesWorksFragment: Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null

    private lateinit var binding: FragmentHowProsthesisWorksBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHowProsthesisWorksBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
    }


    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }

        binding.howProsthesesWorksBtn.setOnClickListener {
            if (main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
                || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
                || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X))
            {
                navigator().showHowProsthesesWorksScreen()
            } else {
                navigator().showHowProsthesesWorksMonoScreen()
            }
        }
        binding.howToPutOnAProsthesesSocketBtn.setOnClickListener { navigator().showHowPutOnTheProsthesesSocketScreen() }
        binding.complectationBtn.setOnClickListener { navigator().showCompleteSetScreen() }
        binding.prosthesesChargeBtn.setOnClickListener { navigator().showChargingTheProsthesesScreen() }
        binding.prosthesesCareBtn.setOnClickListener { navigator().showProsthesesCareScreen() }
        binding.serviceAndWarrantyBtn.setOnClickListener { navigator().showServiceAndWarrantyScreen() }

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }
}