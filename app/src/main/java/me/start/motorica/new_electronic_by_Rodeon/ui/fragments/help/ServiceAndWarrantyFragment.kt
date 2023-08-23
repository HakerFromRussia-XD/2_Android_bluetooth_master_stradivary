package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class ServiceAndWarrantyFragment: Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_service_and_warranty, container, false)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.rootView = rootView
        this.mContext = context
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
    }


    private fun initializeUI() {
        rootView?.findViewById<Button>(R.id.title_click_block_btn)?.setOnClickListener {  }

        rootView?.findViewById<Button>(R.id.how_prostheses_works_btn)?.setOnClickListener {
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
        rootView?.findViewById<Button>(R.id.how_to_put_on_a_prostheses_socket_btn)?.setOnClickListener { navigator().showHowPutOnTheProsthesesSocketScreen() }
        rootView?.findViewById<Button>(R.id.complectation_btn)?.setOnClickListener { navigator().showCompleteSetScreen() }
        rootView?.findViewById<Button>(R.id.prostheses_charge_btn)?.setOnClickListener { navigator().showChargingTheProsthesesScreen() }
        rootView?.findViewById<Button>(R.id.prostheses_care_btn)?.setOnClickListener { navigator().showProsthesesCareScreen() }
        rootView?.findViewById<Button>(R.id.service_and_warranty_btn)?.setOnClickListener { navigator().showServiceAndWarrantyScreen() }


        rootView?.findViewById<Button>(R.id.back_btn)?.setOnClickListener { navigator().goingBack() }
    }
}