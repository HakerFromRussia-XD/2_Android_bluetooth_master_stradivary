package com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.FragmentPersonalAccountProsthesisInformationBinding
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountProsthesisInformationBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.connection.Requests
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class AccountFragmentProsthesisInformationUBI4 : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountProsthesisInformationAdapterUBI4? = null

    private var token = ""
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    private var testSerialNumber = "FEST-F-05670"
    private var myRequests: Requests? = null

    private lateinit var binding: Ubi4FragmentPersonalAccountProsthesisInformationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentPersonalAccountProsthesisInformationBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivityUBI4? }
        this.mContext = context
//        testSerialNumber = main?.mDeviceName.toString()

        val deviceName = main?.mDeviceName
        testSerialNumber = deviceName
            .takeIf { !it.isNullOrBlank() && it.startsWith("FEST-") }
            ?: testSerialNumber

        System.err.println("TEST SERIAL NUMBER $testSerialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gson = Gson()
        myRequests = Requests()
        encryptionManager = EncryptionManagerUtils.instance
        encryptionResult = encryptionManager?.encrypt(testSerialNumber)

        accountProsthesisInformationList = ArrayList()
//        requestToken()
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
//            requestToken()
            binding.refreshLayout.setRefreshing(false)
        }


        initializeUI()

        accountProsthesisInformationList.clear()
        accountProsthesisInformationList.add(
            AccountProsthesisInformationItemUBI4(
                prosthesisModel = main?.loadText(PreferenceKeysUBI4.ACCOUNT_MODEL_PROSTHESIS).toString(),
                prosthesisSize = main?.loadText(PreferenceKeysUBI4.ACCOUNT_SIZE_PROSTHESIS).toString(),
                handSide = main?.loadText(PreferenceKeysUBI4.ACCOUNT_SIDE_PROSTHESIS).toString(),
                rotatorType = main?.loadText(PreferenceKeysUBI4.ACCOUNT_ROTATOR_PROSTHESIS).toString(),
                touchscreenFingerPads = main?.loadText(PreferenceKeysUBI4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS).toString(),
                batteryType = main?.loadText(PreferenceKeysUBI4.ACCOUNT_ACCUMULATOR_PROSTHESIS).toString())
        )
        initAdapter(binding.accountProsthesisInformationRv)
    }

    private fun requestToken() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestToken(
                { token ->
                    this@AccountFragmentProsthesisInformationUBI4.token = token
//                    requestUserData()
                },
                { error -> main?.runOnUiThread {Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()}},
                "Aesserial $encryptionResult")
        }
    }
    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(mContext)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter = AccountProsthesisInformationAdapterUBI4()
        accountRv.adapter = adapter
    }
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }
        initAdapter(binding.accountProsthesisInformationRv)

        binding.backBtn.setOnClickListener {
            (activity as? NavigatorUBI4)?.goingBackUbi4() ?:
            println("Activity не реализует NavigatorUBI4")
        }
    }

    companion object {
        var accountProsthesisInformationList by Delegates.notNull<ArrayList<AccountProsthesisInformationItemUBI4>>()
    }
}