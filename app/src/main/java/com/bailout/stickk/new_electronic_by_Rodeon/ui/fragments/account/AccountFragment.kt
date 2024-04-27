package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bailout.stickk.databinding.FragmentPersonalAccountBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import kotlin.properties.Delegates

class AccountFragment : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null

    private lateinit var binding: FragmentPersonalAccountBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPersonalAccountBinding.inflate(layoutInflater)
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

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }

    companion object {
        var accountList by Delegates.notNull<ArrayList<AccountItem>>()
    }
}