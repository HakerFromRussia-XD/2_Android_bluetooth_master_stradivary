package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import javax.inject.Inject

class KibiFragment: Fragment() {
    @Inject
    lateinit var sqliteManager: SqliteManager
    @Inject
    lateinit var preferenceManager: PreferenceManager

    private var rootView: View? = null
    private var main: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.layout_kibi, container, false)
        WDApplication.component.inject(this)
        this.rootView = rootView
        if (activity != null) {
            main = activity as MainActivity?
//            main?.startService(main!!.intent.putExtra("time", 3).putExtra("service--> label", "Call 1") )
//            main?.startService(Intent(activity, MyService::class.java))

//            val toast = Toast.makeText(
//                requireContext(),
//                "$activity", Toast.LENGTH_LONG
//            )
//            toast.show()
        }
        return rootView
    }

}