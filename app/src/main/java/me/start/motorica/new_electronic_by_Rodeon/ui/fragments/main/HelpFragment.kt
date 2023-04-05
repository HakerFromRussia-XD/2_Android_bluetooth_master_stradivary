package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_help.*
import kotlinx.android.synthetic.main.layout_advanced_settings.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps.navigator
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class HelpFragment : Fragment() {
    private var rootView: View? = null
    private var mContext: Context? = null
    private var main: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_help, container, false)
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
        back_btn.setOnClickListener { navigator().goingBack() }
        settings_reset_cannule_timer_btn.setOnClickListener { navigator().showTestScreen() }

        contact_support_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:88007077197"))
            if (intent.resolveActivity( main!!.packageManager ) != null) {
                startActivity(intent)
            }
        }

        vk_btn.setOnClickListener {
            val url = "http://www.example.com"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
        telegramm_btn.setOnClickListener {
            val url = "http://www.example.com"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity( main!!.packageManager ) != null) {}
            startActivity(intent)
        }
    }
}