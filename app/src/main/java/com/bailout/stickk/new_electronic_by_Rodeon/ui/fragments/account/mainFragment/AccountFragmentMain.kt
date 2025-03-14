package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account.mainFragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.FragmentPersonalAccountMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.connection.Requests
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class AccountFragmentMain(private val chartFragmentClass: ChartFragment) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountMainAdapter? = null
    private var mSettings: SharedPreferences? = null

    private var token = ""
    private var clientId = 0
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private val reactivatedInterface: ReactivatedChart = chartFragmentClass
    private var encryptionResult: String? = null
    private var serialNumber = "FEST-F-06879"//"FEST-H-04921"//"FEST-F-06879"//FEST-EP-05674//FEST-H-02211
    private var myRequests: Requests? = null
//    private var myUser: UserV2? = null
    private var fname: String = ""
    private var sname: String = ""
    private var locate: String = "en"
    private var attemptedRequest: Int = 1

    private var driverVersion = "0.01"
    private var bmsVersion = "0.01"
    private var sensorsVersion = "0.01"


    private lateinit var binding: FragmentPersonalAccountMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPersonalAccountMainBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        //передача реального серийного номера
        serialNumber = main?.mDeviceName.toString()
//        if (testSerialNumber == "INDY") { testSer ialNumber = "INDY-H-05668" } //"INDY-H-02453" }
//        if (serialNumber == "FEST-X") { serialNumber = "INDY-H-05668" } //"INDY-H-02453" }
        System.err.println("TEST SERIAL NUMBER $serialNumber")
//        System.err.println("AccountFragmentMain onCreateView()")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = mContext?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        gson = Gson()
        myRequests = Requests()
        encryptionManager = EncryptionManagerUtils.instance
        attemptedRequest = 1
        if (main?.locate?.contains("ru")!!) { locate = "ru" }


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            System.err.println("TEST SERIAL NUMBER $serialNumber  requestToken()")
            requestToken()
        }

        accountMainList = ArrayList()
        if (mSettings!!.getInt(PreferenceKeys.FIRST_LOAD_ACCOUNT_INFO, 0) == 0) {
            main?.saveInt(PreferenceKeys.FIRST_LOAD_ACCOUNT_INFO, 1)
            requestToken()
        } else {
            binding.preloaderLav.visibility = View.GONE
            updateAllParameters()
        }

        initializeUI()
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()

        RxUpdateMainEvent.getInstance().uiAccountMain
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (mContext != null) {
                    updateAllParameters()
                }
            }
    }
    private fun updateAllParameters() {
        activity?.runOnUiThread {
            initializeUI()

            accountMainList.clear()
            accountMainList.add(
                AccountMainItem(
                    avatarUrl = "avatarUrl",
                    name = fname,
                    surname = sname,
                    patronymic = "Ivanovich",
                    versionDriver = driverVersion,
                    versionBms = bmsVersion,
                    versionSensors = sensorsVersion
                )
            )
            initAdapter(binding.accountRv)
        }
    }
    private fun checkMultigrib(): Boolean {
        return main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X) ||
                main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) ||
                main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_A)
    }

    private fun requestToken() {
        CoroutineScope(Dispatchers.Main).launch {
            encryptionResult = encryptionManager?.encrypt(serialNumber)
            System.err.println("encryptionResult = ${encryptionManager?.encrypt(serialNumber)}  requestToken")
            myRequests!!.getRequestToken(
                { token ->
                    this@AccountFragmentMain.token = token
                    binding.preloaderLav.visibility = View.GONE
                    requestUserData()
                    System.err.println("requestToken запрос обработан")
                },
                { error ->
                    System.err.println("requestToken error: $error")
                    main?.runOnUiThread { binding.refreshLayout.setRefreshing(false) }

                    when (error) {
                        "500" -> {
                            if (attemptedRequest != 4) {
                                //для того чтобы по другому зашифровать серийник
                                attemptedRequest ++
                                requestToken()
                            } else {
                                main?.runOnUiThread {
                                    showInfoWithoutConnection()
                                    Toast.makeText(mContext, "На сервере нет данных пользователя", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        else -> {
                            main?.runOnUiThread {
                                showInfoWithoutConnection()
                                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                "Aesserial $encryptionResult")
        }
    }
    private fun requestUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestUserV2(
                { user ->
                    fname = user.userInfo?.fname.toString()
                    sname = user.userInfo?.sname.toString()
                    binding.apply {
                        accountMainList.clear()
                        accountMainList.add(
                            AccountMainItem(
                            avatarUrl = "avatarUrl",
                            name = fname,
                            surname = sname,
                            patronymic = "Ivanovich",
                            versionDriver = driverVersion,
                            versionBms = bmsVersion,
                            versionSensors = sensorsVersion)
                        )
                        initAdapter(binding.accountRv)
                        binding.refreshLayout.setRefreshing(false)
                    }
                    clientId = user.userInfo?.clientId ?: 0
                    System.err.println("TEST  clientId: ${user.userInfo?.clientId}")
                    System.err.println("Custom service  Manager name: ${user.userInfo?.manager?.fio}")
                    System.err.println("Custom service  Manager phone: ${user.userInfo?.manager?.phone}")

                    main?.saveText(PreferenceKeys.ACCOUNT_MANAGER_FIO, user.userInfo?.manager?.fio)
                    main?.saveText(PreferenceKeys.ACCOUNT_MANAGER_PHONE, user.userInfo?.manager?.phone)
                    requestDeviceList()
                },
                { error ->
                    binding.refreshLayout.setRefreshing(false)
                    main?.runOnUiThread {Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()}
                },
                token = this@AccountFragmentMain.token,
                lang = locate
            )
        }
    }
    private fun requestDeviceList() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestUser(
                { user ->
                    System.err.println("Device list size: ${user.devices.size}")
                    for (device in user.devices) {
                        System.err.println("Device list id = ${device.id}    serialNumber = ${device.serialNumber}")
                        if (device.serialNumber == serialNumber) {
                            System.err.println("Device list искомый девайс: ${device.id}")
                            device.id?.let { requestDeviceInfo(deviceId = it.toInt()) }
                        }
                    }
                },
                { error ->
                    binding.refreshLayout.setRefreshing(false)
                    main?.runOnUiThread {Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()}
                },
                token = this@AccountFragmentMain.token,
                lang = locate
            )
        }
    }
    private fun requestDeviceInfo(deviceId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestDeviceInfo(
                { deviceInfo ->
                    main?.saveText(PreferenceKeys.ACCOUNT_MODEL_PROSTHESIS, simplificationName(deviceInfo.model?.name.toString()))
                    main?.saveText(PreferenceKeys.ACCOUNT_SIZE_PROSTHESIS, deviceInfo.size?.name)
                    main?.saveText(PreferenceKeys.ACCOUNT_SIDE_PROSTHESIS, deviceInfo.side?.name)
                    main?.saveText(PreferenceKeys.ACCOUNT_STATUS_PROSTHESIS, deviceInfo.status?.name)
                    main?.saveText(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS, deviceInfo.dateTransfer)
                    main?.saveText(PreferenceKeys.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, deviceInfo.guaranteePeriod)


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
                            main?.saveText(PreferenceKeys.ACCOUNT_ROTATOR_PROSTHESIS, option.value?.name)
                            System.err.println("Device Info rotator: ${option.value?.name}")
                            rotatorSet = true
                        }
                        if (option.id == 15) {
                            main?.saveText(PreferenceKeys.ACCOUNT_ACCUMULATOR_PROSTHESIS, option.value?.name)
                            System.err.println("Device Info accumulator: ${option.value?.name}")
                            accumulatorSet = true
                        }
                        if (option.id == 5) {
                            main?.saveText(PreferenceKeys.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS, option.value?.name)
                            System.err.println("Device Info Touchscreen fingers: ${option.value?.name}")
                            touchscreenFingersSet = true
                        }
                    }
                    if (!rotatorSet) { System.err.println("Device Info rotator NOT SET")}
                    if (!accumulatorSet) { System.err.println("Device Info accumulator NOT SET") }
                    if (!touchscreenFingersSet) { System.err.println("Device Info Touchscreen fingers NOT SET") }
                },
                { error ->
                    binding.refreshLayout.setRefreshing(false)
                    main?.runOnUiThread {Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()}
                },
                token = this@AccountFragmentMain.token,
                deviceId = deviceId,
                lang = locate
            )
        }
    }
    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(mContext)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter = AccountMainAdapter(object : OnAccountMainClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onCustomerServiceClicked() {
                navigator().showAccountCustomerServiceScreen()

//                main?.showToast("onCustomerServiceClicked")
//                CoroutineScope(Dispatchers.Main).launch {
//                    myRequests?.postRequestSettings(
//                        { error -> Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show() },
//                        token = token,
//                        prosthesisId = address,
//                        gson = this@MainActivity.gson!!
//                    )
//                }
            }

            override fun onProsthesisInformationClicked() {
                navigator().showAccountProsthesisInformationScreen()

//                main?.showToast("onProsthesisInformationClicked")
//                CoroutineScope(Dispatchers.Main).launch {
//                    myRequests?.getRequestProthesisSettings(
//                        { allOptions ->
//                            binding.apply {
//                                tvUserId.visibility = View.VISIBLE
//                                progressBar.visibility = View.GONE
//                                tvUserId.text = allOptions.toString()
//                            }
//                        },
//                        { error -> Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show() },
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

        binding.backBtn.setOnClickListener {
            System.err.println("AccountFragmentMain backBtn")
            main?.saveInt(PreferenceKeys.FIRST_LOAD_ACCOUNT_INFO, 0)
            navigator().goingBack()
            Handler().postDelayed({
                reactivatedInterface.reactivatedChart()
            }, 300)

        }

        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                System.err.println("AccountFragmentMain backBtn")
                main?.saveInt(PreferenceKeys.FIRST_LOAD_ACCOUNT_INFO, 0)
                navigator().goingBack()
                Handler().postDelayed({
                    reactivatedInterface.reactivatedChart()
                }, 300)
                requireFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                return@OnKeyListener true
            }
            false
        })

        driverVersion = if (!checkMultigrib()) {
            ((mSettings!!.getInt(
                main?.mDeviceAddress + PreferenceKeys.DRIVER_NUM,
                1
            )).toFloat() / 100).toString()
        } else {
            main?.driverVersionS.toString()
        }
        bmsVersion = ((mSettings!!.getInt(
            main?.mDeviceAddress + PreferenceKeys.BMS_NUM,
            1
        )).toFloat() / 100).toString()
        sensorsVersion = ((mSettings!!.getInt(
            main?.mDeviceAddress + PreferenceKeys.SENS_NUM,
            1
        )).toFloat() / 100).toString()
    }
    private fun simplificationName(name: String): String {
        return name.substringFrom("ПР", name.lastIndex)
    }
    private fun String.substringFrom(char: String, maxLen: Int)
            = indexOf(char).let {
        if (it >= 0)
            substring(it, min(it + maxLen, length))
        else
            this
    }
    private fun showInfoWithoutConnection() {
        binding.preloaderLav.visibility = View.GONE
        binding.apply {
            accountMainList.clear()
            accountMainList.add(
                AccountMainItem(
                    avatarUrl = "avatarUrl",
                    name = fname,
                    surname = sname,
                    patronymic = "Ivanovich",
                    versionDriver = driverVersion,
                    versionBms = bmsVersion,
                    versionSensors = sensorsVersion
                )
            )
            initAdapter(binding.accountRv)
        }
        main?.saveText(PreferenceKeys.ACCOUNT_MANAGER_FIO, "")
        main?.saveText(PreferenceKeys.ACCOUNT_MANAGER_PHONE, "")
        main?.saveText(PreferenceKeys.ACCOUNT_MODEL_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_SIZE_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_SIDE_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_STATUS_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_ROTATOR_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_ACCUMULATOR_PROSTHESIS, "")
        main?.saveText(PreferenceKeys.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS, "")
    }

    companion object {
        var accountMainList by Delegates.notNull<ArrayList<AccountMainItem>>()
    }
}