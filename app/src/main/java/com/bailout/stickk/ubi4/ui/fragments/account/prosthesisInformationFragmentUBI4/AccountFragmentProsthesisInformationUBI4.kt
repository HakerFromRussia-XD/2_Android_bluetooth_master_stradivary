package com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountProsthesisInformationBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.data.network.RequestsUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
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
    private var myRequests: RequestsUBI4? = null

    private var _binding: Ubi4FragmentPersonalAccountProsthesisInformationBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentPersonalAccountProsthesisInformationBinding.inflate(inflater, container, false)
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
        myRequests = RequestsUBI4()
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

        accountProsthesisInformationList.clear()
        accountProsthesisInformationList.add(
            AccountProsthesisInformationItemUBI4(
                prosthesisModel = main?.loadText(PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS).toString(),
                prosthesisSize = main?.loadText(PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS).toString(),
                handSide = main?.loadText(PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS).toString(),
                rotatorType = main?.loadText(PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS).toString(),
                touchscreenFingerPads = main?.loadText(PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS).toString(),
                batteryType = main?.loadText(PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS).toString())
        )

        initializeUI()
    }

    private fun requestToken() {
        viewLifecycleOwner.lifecycleScope.launch {
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

    override fun onDestroyView() {
        binding.accountProsthesisInformationRv.adapter = null
        adapter = null
        linearLayoutManager = null
        myRequests = null
        gson = null
        encryptionManager = null
        encryptionResult = null
        main = null
        mContext = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        var accountProsthesisInformationList by Delegates.notNull<ArrayList<AccountProsthesisInformationItemUBI4>>()
    }
}
