package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account.mainFragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.FragmentPersonalAccountMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.connection.Requests
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.runOnUiThread
import kotlin.properties.Delegates

class AccountFragmentMain : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountMainAdapter? = null

    private var token = ""
    private var clientId = 0
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    private var testSerialNumber = "FEST-H-04921"//"FEST-EP-05674"//"FEST-F-06879"//
    private var myRequests: Requests? = null

    private lateinit var binding: FragmentPersonalAccountMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPersonalAccountMainBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gson = Gson()
        myRequests = Requests()
        encryptionManager = EncryptionManagerUtils.instance
        encryptionResult = encryptionManager?.encrypt(testSerialNumber)
        System.err.println("encryptionResult = ${encryptionManager?.encrypt(testSerialNumber)}")
//        System.err.println("encryption Decription = ${encryptionManager?.decrypt(encryptionResult)}")

        accountMainList = ArrayList()
        requestToken()
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { requestToken() }

        initializeUI()
    }

    private fun requestToken() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestToken(
                { token ->
                    this@AccountFragmentMain.token = token
                    binding.preloaderLav.visibility = View.GONE
                    requestUserData()
                },
                { error ->
                    main?.runOnUiThread {Toast.makeText(context, error, Toast.LENGTH_SHORT).show()}
                },
                "Aesserial $encryptionResult")
        }
    }
    private fun requestUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestUserV2(
                { user ->
                    binding.apply {
                        accountMainList.clear()
                        accountMainList.add(
                            AccountMainItem(
                            avatarUrl = "avatarUrl",
                            name = user.userInfo?.fname.toString(),
                            surname = user.userInfo?.sname.toString(),
                            patronymic = "Ivanovich",
                            versionDriver = "111111111.111",
                            versionBms = "222222.22222",
                            versionSensors = "33333.33333")
                        )
                        initAdapter(binding.accountRv)
                        binding.refreshLayout.setRefreshing(false)
                    }
                    clientId = user.userInfo?.clientId ?: 0
                    System.err.println("TEST  clientId: ${user.userInfo?.clientId}")
                    System.err.println("Custom service  Manager name: ${user.userInfo?.manager?.fio}")
                    System.err.println("Custom service  Manager phone: ${user.userInfo?.manager?.phone}")
                    requestDeviceList()
                },
                { error ->
                    main?.runOnUiThread {Toast.makeText(context, error, Toast.LENGTH_SHORT).show()}
                },
                token = this@AccountFragmentMain.token
            )
        }
    }
    private fun requestDeviceList() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestUser(
                { user ->
                    System.err.println("Device list size: ${user.devices.size}")
                    for (device in user.devices) {
                        if (device.serialNumber == testSerialNumber) {
                            System.err.println("Device list искомый девайс: ${device.id}")
                            device.id?.let { requestDeviceInfo(deviceId = it.toInt()) }
                        }
                    }
                },
                { error ->
                    main?.runOnUiThread {Toast.makeText(context, error, Toast.LENGTH_SHORT).show()}
                },
                token = this@AccountFragmentMain.token
            )
        }
    }
    private fun requestDeviceInfo(deviceId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestDeviceInfo(
                { deviceInfo ->
                    System.err.println("Device Info model: ${deviceInfo.model?.name}")
                    System.err.println("Device Info size: ${deviceInfo.size?.name}")
                    System.err.println("Device Info side: ${deviceInfo.side?.name}")
                    System.err.println("Device Info status: ${deviceInfo.status?.name}")
                    System.err.println("Device Info date transfer: ${deviceInfo.dateTransfer}")
                    System.err.println("Device Info guarantee period: ${deviceInfo.guaranteePeriod}")
                    System.err.println("Device Info options: ${deviceInfo.options.size}")
                    var rotatorSet = false
                    var accumulatorSet = false
                    var touchscreenFingersSet = false
                    for (option in deviceInfo.options) {
                        if (option.id == 3) {
                            System.err.println("Device Info rotator: ${option.value?.name}")
                            rotatorSet = true
                        }
                        if (option.id == 15) {
                            System.err.println("Device Info accumulator: ${option.value?.name}")
                            accumulatorSet = true
                        }
                        if (option.id == 5) {
                            System.err.println("Device Info Touchscreen fingers: ${option.value?.name}")
                            touchscreenFingersSet = true
                        }
                    }
                    if (!rotatorSet) { System.err.println("Device Info rotator NOT SET")}
                    if (!accumulatorSet) { System.err.println("Device Info accumulator NOT SET") }
                    if (!touchscreenFingersSet) { System.err.println("Device Info Touchscreen fingers NOT SET") }
                },
                { error ->
                    main?.runOnUiThread {Toast.makeText(context, error, Toast.LENGTH_SHORT).show()}
                },
                token = this@AccountFragmentMain.token,
                deviceId = deviceId
            )
        }
    }
    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter = AccountMainAdapter(object : OnAccountMainClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onCustomerServiceClicked() {
                navigator().showAccountCustomerServiceScreen()

                main?.showToast("onCustomerServiceClicked")
//                CoroutineScope(Dispatchers.Main).launch {
//                    myRequests?.postRequestSettings(
//                        { error -> Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show() },
//                        token = token,
//                        prosthesisId = address,
//                        gson = this@MainActivity.gson!!
//                    )
//                }
            }

            override fun onProsthesisInformationClicked() {
                navigator().showAccountProsthesisInformationScreen()

                main?.showToast("onProsthesisInformationClicked")
//                CoroutineScope(Dispatchers.Main).launch {
//                    myRequests?.getRequestProthesisSettings(
//                        { allOptions ->
//                            binding.apply {
//                                tvUserId.visibility = View.VISIBLE
//                                progressBar.visibility = View.GONE
//                                tvUserId.text = allOptions.toString()
//                            }
//                        },
//                        { error -> Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show() },
//                        token = token,
//                        prosthesisId = id
//                    )
//                }
            }
        })
        accountRv.adapter = adapter
    }
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }
        initAdapter(binding.accountRv)

        binding.backBtn.setOnClickListener { navigator().goingBack() }
    }

    companion object {
        var accountMainList by Delegates.notNull<ArrayList<AccountMainItem>>()
    }
}