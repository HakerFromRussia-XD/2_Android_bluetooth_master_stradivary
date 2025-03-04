package com.bailout.stickk.ubi4.ui.fragments.account

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
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.connection.Requests
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class AccountFragmentMainUBI4(private val reactivatedInterface: ReactivatedChart? = null) : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountMainAdapterUBI4? = null
    private var mSettings: SharedPreferences? = null

    private var token = ""
    private var clientId = 0
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    // Используйте реальный серийный номер, если он доступен
    private var serialNumber = "FEST-F-06879"
    private var myRequests: Requests? = null
    private var fname: String = ""
    private var sname: String = ""
    private var locate: String = "en"
    private var attemptedRequest: Int = 1

    private var driverVersion = "0.01"
    private var bmsVersion = "0.01"
    private var sensorsVersion = "0.01"

    private lateinit var binding: Ubi4FragmentPersonalAccountMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        // Если используется DI, можно вызвать WDApplication.component.inject(this)
        if (activity != null) {
            main = activity as? MainActivityUBI4
        }
        mContext = context
        // Передаем серийный номер из активности, если нужно
        serialNumber = main?.mDeviceName ?: serialNumber
        System.err.println("TEST SERIAL NUMBER $serialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = mContext?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        gson = Gson()
        myRequests = Requests()
        encryptionManager = EncryptionManagerUtils.instance
        attemptedRequest = 1
        if (main?.locate?.contains("ru") == true) { locate = "ru" }

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            System.err.println("Refreshing... requestToken()")
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
            accountMainList.clear()
            accountMainList.add(
                AccountMainUBI4Item(
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

    private fun requestToken() {
        CoroutineScope(Dispatchers.Main).launch {
            encryptionResult = encryptionManager?.encrypt(serialNumber)
            System.err.println("encryptionResult = ${encryptionResult} requestToken")
            myRequests!!.getRequestToken(
                { token ->
                    this@AccountFragmentMainUBI4.token = token
                    binding.preloaderLav.visibility = View.GONE
                    requestUserData()
                    System.err.println("requestToken processed")
                },
                { error ->
                    System.err.println("requestToken error: $error")
                    main?.runOnUiThread { binding.refreshLayout.setRefreshing(false) }
                    when (error) {
                        "500" -> {
                            if (attemptedRequest != 4) {
                                attemptedRequest++
                                requestToken()
                            } else {
                                main?.runOnUiThread {
                                    showInfoWithoutConnection()
                                    Toast.makeText(mContext, "No user data on server", Toast.LENGTH_LONG).show()
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
                "Aesserial $encryptionResult"
            )
        }
    }

    private fun requestUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestUserV2(
                { user ->
                    fname = user.userInfo?.fname ?: ""
                    sname = user.userInfo?.sname ?: ""
                    binding.apply {
                        accountMainList.clear()
                        accountMainList.add(
                            AccountMainUBI4Item(
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
                        binding.refreshLayout.setRefreshing(false)
                    }
                    clientId = user.userInfo?.clientId ?: 0
                    System.err.println("clientId: ${clientId}")
                    System.err.println("Manager name: ${user.userInfo?.manager?.fio}")
                    System.err.println("Manager phone: ${user.userInfo?.manager?.phone}")
                    main?.saveString(PreferenceKeys.ACCOUNT_MANAGER_FIO, user.userInfo?.manager?.fio ?: "")
                    main?.saveString(PreferenceKeys.ACCOUNT_MANAGER_PHONE, user.userInfo?.manager?.phone ?: "")
                    requestDeviceList()
                },
                { error ->
                    binding.refreshLayout.setRefreshing(false)
                    main?.runOnUiThread { Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show() }
                },
                token = this@AccountFragmentMainUBI4.token,
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
                        System.err.println("Device id = ${device.id} serialNumber = ${device.serialNumber}")
                        if (device.serialNumber == serialNumber) {
                            System.err.println("Found target device: ${device.id}")
                            device.id?.let { requestDeviceInfo(deviceId = it.toInt()) }
                        }
                    }
                },
                { error ->
                    binding.refreshLayout.setRefreshing(false)
                    main?.runOnUiThread { Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show() }
                },
                token = this@AccountFragmentMainUBI4.token,
                lang = locate
            )
        }
    }

    private fun requestDeviceInfo(deviceId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            myRequests!!.getRequestDeviceInfo(
                { deviceInfo ->
                    main?.saveString(PreferenceKeys.ACCOUNT_MODEL_PROSTHESIS, simplificationName(deviceInfo.model?.name ?: ""))
                    main?.saveString(PreferenceKeys.ACCOUNT_SIZE_PROSTHESIS, deviceInfo.size?.name ?: "")
                    main?.saveString(PreferenceKeys.ACCOUNT_SIDE_PROSTHESIS, deviceInfo.side?.name ?: "")
                    main?.saveString(PreferenceKeys.ACCOUNT_STATUS_PROSTHESIS, deviceInfo.status?.name ?: "")
                    main?.saveString(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS, deviceInfo.dateTransfer ?: "")
                    main?.saveString(PreferenceKeys.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, deviceInfo.guaranteePeriod ?: "")
                    System.err.println("Device Info model: ${deviceInfo.model?.name}")
                    // ... (другие логи)
                },
                { error ->
                    binding.refreshLayout.setRefreshing(false)
                    main?.runOnUiThread { Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show() }
                },
                token = this@AccountFragmentMainUBI4.token,
                deviceId = deviceId,
                lang = locate
            )
        }
    }

    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(mContext)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter = AccountMainAdapterUBI4(object : OnAccountMainUBI4ClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onCustomerServiceClicked() {
                navigator().showAccountCustomerServiceScreen()
            }

            override fun onProsthesisInformationClicked() {
//                navigator().showAccountProsthesisInformationScreen()
            }
        })
        accountRv.adapter = adapter
    }

    // Восстанавливаем обработку кнопки "назад" как в рабочем фрагменте
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener { }
        initAdapter(binding.accountRv)
        //TODO когда придет Рома, исправить закрытие стека над SensorsFragment
        binding.backBtn.setOnClickListener {
            // Проверяем, что фрагмент всё ещё прикреплён
            if (isAdded) {
                parentFragmentManager.popBackStack()  // закрывает только верхний фрагмент
            } else {
                main?.finish()

            }
        }

        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                System.err.println("AccountFragmentMainUBI4 back key pressed")
                main?.saveInt(PreferenceKeys.FIRST_LOAD_ACCOUNT_INFO, 0)
                navigator().goingBackUbi4()
                Handler().postDelayed({
                    reactivatedInterface?.reactivatedChart()
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
            main?.driverVersionS ?: "0.01"
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

    private fun checkMultigrib(): Boolean {
        return main?.mDeviceType?.contains(ConstantManager.DEVICE_TYPE_FEST_X) == true ||
                main?.mDeviceType?.contains(ConstantManager.DEVICE_TYPE_FEST_H) == true ||
                main?.mDeviceType?.contains(ConstantManager.DEVICE_TYPE_FEST_A) == true
    }

    private fun simplificationName(name: String): String {
        return name.substringFrom("ПР", name.lastIndex)
    }
    private fun String.substringFrom(char: String, maxLen: Int) =
        indexOf(char).let {
            if (it >= 0) substring(it, min(it + maxLen, length)) else this
        }

    private fun showInfoWithoutConnection() {
        binding.preloaderLav.visibility = View.GONE
        binding.apply {
            accountMainList.clear()
            accountMainList.add(
                AccountMainUBI4Item(
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
        main?.saveString(PreferenceKeys.ACCOUNT_MANAGER_FIO, "")
        main?.saveString(PreferenceKeys.ACCOUNT_MANAGER_PHONE, "")
        main?.saveString(PreferenceKeys.ACCOUNT_MODEL_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_SIZE_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_SIDE_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_STATUS_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_DATE_TRANSFER_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_ROTATOR_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_ACCUMULATOR_PROSTHESIS, "")
        main?.saveString(PreferenceKeys.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS, "")
    }

    companion object {
        var accountMainList by Delegates.notNull<ArrayList<AccountMainUBI4Item>>()
    }
}