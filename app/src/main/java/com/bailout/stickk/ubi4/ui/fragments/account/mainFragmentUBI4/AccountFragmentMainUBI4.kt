package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.network.RequestsUBI4
import com.bailout.stickk.ubi4.data.state.ConnectionState.fullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.properties.Delegates

import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.BootloaderCardAdapter


//TODO изменить импорты из UBI3 для фрагмента "help"
@Suppress("DEPRECATION")
class AccountFragmentMainUBI4: BaseWidgetsFragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var mSettings: SharedPreferences? = null

    private var token = ""
    private var clientId = 0
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    // Используйте реальный серийный номер, если он доступен
    private var serialNumber = "FEST-F-05670"
    private var myRequests: RequestsUBI4? = null
    private var fname: String = ""
    private var sname: String = ""
    private var locate: String = "en"
    private var attemptedRequest: Int = 1

    private var driverVersion = "0.01"
    private var bmsVersion = "0.01"
    private var sensorsVersion = "0.01"

    private lateinit var accountAdapter: AccountMainAdapterUBI4
    private lateinit var bootloaderAdapter: BootloaderAdapterUBI4
    private lateinit var concatAdapter: ConcatAdapter

    private lateinit var binding: Ubi4FragmentPersonalAccountMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        System.err.println("AccountFragmentMainUBI4: onCreateView")
        binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        // Если используется DI, можно вызвать WDApplication.component.inject(this)
        if (activity != null) {
            main = activity as? MainActivityUBI4
        }
        mContext = context
        // Передаем серийный номер из активности, если нужно
        val deviceName = main?.mDeviceName
        //TODO после теста убрать фильтр по названию серийного номера
        serialNumber = deviceName
            .takeIf { !it.isNullOrBlank() && it.startsWith("FEST-") }
            ?: serialNumber

//        serialNumber = main?.mDeviceName ?: serialNumber
        System.err.println("TEST SERIAL NUMBER $serialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        System.err.println("AccountFragmentMainUBI4: onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        mSettings = mContext?.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        gson = Gson()
        myRequests = RequestsUBI4()
        encryptionManager = EncryptionManagerUtils.instance
        attemptedRequest = 1
        if (main?.locate?.contains("ru") == true) { locate = "ru" }


        //кнопка назад самого андроида - дублируем код из backBtn
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdded) {
                    handleBackPress()
                } else {
                    main?.finish()
                }
            }
        })


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            System.err.println("Refreshing... requestToken()")
            requestToken()
        }

        accountMainList = ArrayList()
        binding.preloaderLav.visibility = View.VISIBLE
        requestToken()
        initializeUI()
        showBoardsVersion()
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        System.err.println("AccountFragmentMainUBI4: onResume")
        //TODO временно RxUpdateMainEvent от UBI3
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
        val item = AccountMainUBI4Item(
            avatarUrl      = "avatarUrl",
            name           = fname,
            surname        = sname,
            patronymic     = "Ivanovich",
            versionDriver  = driverVersion,
            versionBms     = bmsVersion,
            versionSensors = sensorsVersion
        )
        updateAccountSafe(item)
    }

    private val cleanSerialNumber = serialNumber.replace("\u0000", "").trim()


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
//                cleanSerialNumber
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
                        val item = AccountMainUBI4Item(
                            avatarUrl      = "avatarUrl",
                            name           = fname,
                            surname        = sname,
                            patronymic     = "Ivanovich",
                            versionDriver  = driverVersion,
                            versionBms     = bmsVersion,
                            versionSensors = sensorsVersion
                        )
                        updateAccountSafe(item)
                        binding.refreshLayout.setRefreshing(false)
                    }
                    clientId = user.userInfo?.clientId ?: 0
                    System.err.println("clientId: ${clientId}")
                    System.err.println("Manager name: ${user.userInfo?.manager?.fio}")
                    System.err.println("Manager phone: ${user.userInfo?.manager?.phone}")
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_MANAGER_FIO, user.userInfo?.manager?.fio ?: "")
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_MANAGER_PHONE, user.userInfo?.manager?.phone ?: "")
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
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_MODEL_PROSTHESIS, simplificationName(deviceInfo.model?.name ?: ""))
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_SIZE_PROSTHESIS, deviceInfo.size?.name ?: "")
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_SIDE_PROSTHESIS, deviceInfo.side?.name ?: "")
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_STATUS_PROSTHESIS, deviceInfo.status?.name ?: "")
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_DATE_TRANSFER_PROSTHESIS, deviceInfo.dateTransfer ?: "")
                    main?.saveString(PreferenceKeysUBI4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, deviceInfo.guaranteePeriod ?: "")
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


    private fun initAdapter() {

        // ==== 1. Листенеры ===========================================
        val accountClickListener = object : OnAccountMainUBI4ClickListener {
            override fun onCustomerServiceClicked() { navigator().showAccountCustomerServiceScreen() }
            override fun onProsthesisInformationClicked() { navigator().showAccountProsthesisInformationScreen() }
        }

        val bootloaderClickListener = object : BootloaderAdapterUBI4.OnBootloaderClickListener {
            override fun onUpdateClick(item: BootloaderBoardItemUBI4) {
                Toast.makeText(requireContext(), "Update ${item.boardName}", Toast.LENGTH_SHORT).show()
            }
        }

        // ==== 2. Адаптеры секций =====================================
        accountAdapter    = AccountMainAdapterUBI4(accountClickListener)     // профиль и софт

        // строки‑платы
        bootloaderAdapter = BootloaderAdapterUBI4(bootloaderClickListener)

        // карточка‑обёртка, которая содержит вложенный RecyclerView
        val bootloaderCardAdapter = BootloaderCardAdapter(bootloaderAdapter)

        // ==== 3. ConcatAdapter – порядок: профиль → карточка плат =====
        concatAdapter = ConcatAdapter(accountAdapter, bootloaderCardAdapter)

        // ==== 4. RecyclerView — общий ================================
        binding.accountRv.apply {
            layoutManager  = LinearLayoutManager(requireContext())
            adapter        = concatAdapter
            setHasFixedSize(true)
            itemAnimator   = null
        }
    }

    // Восстанавливаем обработку кнопки "назад" как в рабочем фрагменте
    private fun initializeUI() {
        // Заглушка на клик заголовка (оставь, если нужен)
        binding.titleClickBlockBtn.setOnClickListener { }

        // Подключаем ConcatAdapter с двумя секциями
        initAdapter()

        // Кнопка «назад» в шапке
        binding.backBtn.setOnClickListener {
            if (isAdded) handleBackPress() else main?.finish()
        }

        // Чтобы «Back» отрабатывал даже при фокусе внутри списка
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()

        // Локальные версии прошивок
        driverVersion = if (!checkMultigrib()) {
            ((mSettings?.getInt(
                main?.mDeviceAddress + PreferenceKeysUBI4.DRIVER_NUM, 1
            ) ?: 1) / 100f).toString()
        } else {
            main?.driverVersionS ?: "0.01"
        }

        bmsVersion = ((mSettings?.getInt(
            main?.mDeviceAddress + PreferenceKeysUBI4.BMS_NUM, 1
        ) ?: 1) / 100f).toString()

        sensorsVersion = ((mSettings?.getInt(
            main?.mDeviceAddress + PreferenceKeysUBI4.SENS_NUM, 1
        ) ?: 1) / 100f).toString()
    }

    private fun handleBackPress() {
        // Получаем имя исходного фрагмента из аргументов
        val sourceFragmentClassName = arguments?.getString("sourceFragmentClass")
        if (sourceFragmentClassName != null) {
            when (sourceFragmentClassName) {
                SensorsFragment::class.java.name -> { main?.showSensorsScreen() }
                SpecialSettingsFragment::class.java.name -> { main?.showSpecialScreen() }
                SprTrainingFragment::class.java.name -> { main?.showOpticTrainingGesturesScreen() }
                SprGestureFragment::class.java.name -> { main?.showOpticGesturesScreen() }
                // Если будут ещё варианты, их можно добавить здесь
                else -> {
                    // Если имя фрагмента неизвестно – возвращаемся в back stack
                    parentFragmentManager.popBackStack()
                }
            }
        } else {
            // Если аргумента нет – возвращаемся в back stack по умолчанию
            parentFragmentManager.popBackStack()
        }
    }

    private fun checkMultigrib(): Boolean {
        return main?.mDeviceType?.contains(ConstantManagerUBI4.DEVICE_TYPE_FEST_X) == true
//                main?.mDeviceType?.contains(ConstantManager.DEVICE_TYPE_FEST_H) == true ||
//                main?.mDeviceType?.contains(ConstantManager.DEVICE_TYPE_FEST_A) == true
    }

    private fun simplificationName(name: String): String {
        return name.substringFrom("ПР", name.lastIndex)
    }
    private fun String.substringFrom(char: String, maxLen: Int) =
        indexOf(char).let {
            if (it >= 0) substring(it, min(it + maxLen, length)) else this
        }


    override fun onPause() {
        super.onPause()
        System.err.println("AccountFragmentMainUBI4: onPause")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        System.err.println("AccountFragmentMainUBI4: onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        System.err.println("AccountFragmentMainUBI4: onDestroy")
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun showInfoWithoutConnection() {
        binding.preloaderLav.visibility = View.GONE
        binding.apply {
            val item = AccountMainUBI4Item(
                avatarUrl      = "avatarUrl",
                name           = fname,
                surname        = sname,
                patronymic     = "Ivanovich",
                versionDriver  = driverVersion,
                versionBms     = bmsVersion,
                versionSensors = sensorsVersion
            )
            updateAccountSafe(item)

        }
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_MANAGER_FIO, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_MANAGER_PHONE, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_MODEL_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_SIZE_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_SIDE_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_STATUS_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_DATE_TRANSFER_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_ROTATOR_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_ACCUMULATOR_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUBI4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS, "")
    }

//    private fun addFakeBoards() {
//        val fake = listOf(
//            BootloaderBoardItemUBI4("Board 01", 0x01, true),
//            BootloaderBoardItemUBI4("Board 02", 0x02, false),
//            BootloaderBoardItemUBI4("Board 03", 0x03, true)
//        )
//        updateBootloaderSafe(fake)
//    }

    private fun showBoardsVersion() {
        val boards = buildList {
            fullInicializeConnectionStruct?.let { add(it.toBoardItem()) }

            baseSubDevicesInfoStructSet.forEach { sub ->
                add(sub.toBoardItem())
            }
        }
            .distinctBy { it.deviceCode } // вдруг дубликаты
            .map { it.copy(boardName = it.boardName.removeSuffix(" version").trimEnd()) }
        updateBootloaderSafe(boards)
    }

    private fun FullInicializeConnectionStruct.toBoardItem() =
        BootloaderBoardItemUBI4(
            boardName = PreferenceKeysUBI4.DeviceCode.from(deviceCode).title,
            deviceCode = deviceCode,
            deviceAddress = 0,
            canUpdate = true
        )


    private fun BaseSubDeviceInfoStruct.toBoardItem() =
        BootloaderBoardItemUBI4(
            boardName = PreferenceKeysUBI4.DeviceCode.from(deviceCode).title,
            deviceCode = deviceCode,
            deviceAddress = deviceAddress,
            canUpdate = true
        )


    fun updateBoardVersion(deviceAddress: Int, version: String) {
        val fresh = bootloaderAdapter.currentList.map { item ->
            if (item.deviceAddress == deviceAddress)          // ← ищем по адресу
                item.copy(boardName = "${item.boardName}  $version")
            else item
        }
        updateBootloaderSafe(fresh)
    }

    private fun updateAccountSafe(item: AccountMainUBI4Item) =
        binding.accountRv.post { accountAdapter.submitProfile(item) }

    private fun updateBootloaderSafe(list: List<BootloaderBoardItemUBI4>) =
        binding.accountRv.post { bootloaderAdapter.submitBoards(list) }
    companion object {
        var accountMainList by Delegates.notNull<ArrayList<AccountMainUBI4Item>>()
    }
}