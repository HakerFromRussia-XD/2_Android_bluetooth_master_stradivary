package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountMainBinding
import com.bailout.stickk.ubi4.adapters.dialog.FirmwareFilesAdapter
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.data.state.ConnectionState.fullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.user.Manager
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.properties.Delegates


//TODO изменить импорты из UBI3 для фрагмента "help"
@Suppress("DEPRECATION")
class AccountFragmentMainUBI4: BaseWidgetsFragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var mSettings: SharedPreferences? = null

    private var token = ""
    private var clientId = 0
    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtilsUbi4? = null
    private var encryptionResult: String? = null
    // Используйте реальный серийный номер, если он доступен
    private var serialNumber = "FEST-F-05670"
//    private var serialNumber = "FEST-H-04921"
    private var myRequests: Ubi4RequestsApi? = null
    private var fname: String = ""
    private var sname: String = ""
    private var locate: String = "en"
    private var attemptedRequest: Int = 1

    private val api = Ubi4RequestsApi()

    private var driverVersion = "0.01"
    private var bmsVersion = "0.01"
    private var sensorsVersion = "0.01"

    private lateinit var accountAdapter: AccountMainAdapterUBI4
    private lateinit var bootloaderAdapter: BootloaderAdapterUBI4
    private lateinit var concatAdapter: ConcatAdapter
    private val boardNameByAddr = mutableMapOf<Int, String>()
    private val bootloaderBoardsList = mutableListOf<BootloaderBoardItemUBI4>()


    private var _binding: Ubi4FragmentPersonalAccountMainBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val resumeDisposables = CompositeDisposable()

    private val fwVersions = mutableMapOf<Int, String>()
    private var canRenderBoards = false
    private var systemBackCallback: OnBackPressedCallback? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        System.err.println("AccountFragmentMainUBI4: onCreateView")
        _binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        // Если используется DI, можно вызвать WDApplication.component.inject(this)
        if (activity != null) {
            main = activity as? MainActivityUBI4
        }
        mContext = context
        // Передаем серийный номер из активности, если нужно
        val deviceName = main?.mDeviceName // <- раскоменитировать после теста
        //TODO после теста убрать фильтр по названию серийного номера
        serialNumber = deviceName
            .takeIf { !it.isNullOrBlank() && it.startsWith("FEST-") }
            ?: serialNumber

        serialNumber = main?.mDeviceName ?: serialNumber
        serialNumber = deviceName ?: serialNumber
//        serialNumber = "FEST-F-06879"
        System.err.println("TEST SERIAL NUMBER $serialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        System.err.println("AccountFragmentMainUBI4: onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        mSettings = mContext?.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        gson = Gson()
        myRequests = Ubi4RequestsApi()
        encryptionManager = EncryptionManagerUtilsUbi4.instance
        attemptedRequest = 1
        if (main?.locate?.contains("ru") == true) { locate = "ru" }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FirmwareInfoState.firmwareInfoFlow.collect { fw ->
                    // апдейтим конкретную плату
                    fwVersions[fw.deviceAddress] = fw.fwVersion
                    if (canRenderBoards) refreshBoards()
                }
            }
        }



        //кнопка назад самого андроида - дублируем код из backBtn
        systemBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdded) {
                    handleBackPress()
                } else {
                    main?.finish()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            requireNotNull(systemBackCallback)
        )
        systemBackCallback?.isEnabled = !isHidden


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            System.err.println("Refreshing... requestToken()")
            requestToken()
        }

        accountMainList = ArrayList()
        initializeUI()

        val hasCachedContent = applyCachedContentIfAvailable()
        if (hasCachedContent) {
            canRenderBoards = true
            binding.preloaderLav.visibility = View.GONE
            binding.accountRv.visibility = View.VISIBLE
        } else {
            binding.preloaderLav.visibility = View.VISIBLE
            binding.accountRv.visibility = View.INVISIBLE
        }

        val transitionDurationMs = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        binding.root.postDelayed({
            if (!isAdded || _binding == null) return@postDelayed
            canRenderBoards = true
            refreshBoards()
            requestToken()
        }, transitionDurationMs)

        viewLifecycleOwner.lifecycleScope.launch {
             runProgramTypeFlow.collect { (addr, runType) ->
                if (!canRenderBoards) return@collect
                val idx = bootloaderBoardsList.indexOfFirst { it.deviceAddress == addr }
                if (idx != -1) {
                    bootloaderBoardsList[idx].isInBootLoader = runType == PreferenceKeysUbi4.RunProgramType.BOOTLOADER
                    bootloaderAdapter.notifyItemChanged(idx)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        systemBackCallback?.isEnabled = !isHidden
        System.err.println("AccountFragmentMainUBI4: onResume")
        //TODO временно RxUpdateMainEvent от UBI3
        resumeDisposables.add(
            RxUpdateMainEventUbi4.getInstance().uiAccountMain
                .compose(main?.bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (mContext != null) {
                        updateAllParameters()
                    }
                }
        )
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
        viewLifecycleOwner.lifecycleScope.launch {
            encryptionResult = withContext(Dispatchers.Default) {
                encryptionManager?.encrypt(serialNumber)
            }
            when (val res = api.getToken("Aesserial $encryptionResult")) {
                is NetworkResult.Success -> {
                    token = res.value.token
                    binding.preloaderLav.visibility = View.GONE
                    requestUserData()
                }
                is NetworkResult.Error -> {
                    binding.refreshLayout.setRefreshing(false)
                    handleTokenError(res)
                }
            }
        }
    }

    private fun handleTokenError(err: NetworkResult.Error) {
        if (err.isCancelledByLifecycle()) return
        if (err.code == 500) retryOrShowNoData()
        else {
            showInfoWithoutConnection()
            Toast.makeText(mContext, err.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun retryOrShowNoData() {
        if (attemptedRequest++ < 4) requestToken()
        else {
            showInfoWithoutConnection()
            Toast.makeText(
                mContext,
                getString(SharedRes.strings.no_user_data_on_server.resourceId),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    private fun requestUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val res = api.getUserInfoV2(token, locate)) {
                is NetworkResult.Success -> {
                    val info = res.value.userInfo
                    fname = info?.fname.orEmpty()
                    sname = info?.sname.orEmpty()
                    updateProfileUI()
                    clientId = info?.clientId ?: 0
                    saveManagerInfo(info?.manager)
                    requestDeviceList()
                }
                is NetworkResult.Error -> {
                    binding.refreshLayout.setRefreshing(false)
                    if (res.isCancelledByLifecycle()) return@launch
                    Toast.makeText(mContext, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun saveManagerInfo(manager: Manager?) {
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_MANAGER_FIO, manager?.fio.orEmpty())
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_MANAGER_PHONE, manager?.phone.orEmpty())
    }

    private fun requestDeviceList() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val res = api.getDevicesList(clientId, token, locate)) {
                is NetworkResult.Success -> {
                    val devices: List<DeviceInList_DEV> = res.value
                    devices
                        .firstOrNull { it.serialNumber == serialNumber }
                        ?.id
                        ?.let { requestDeviceInfo(it) }
                }
                is NetworkResult.Error -> {
                    binding.refreshLayout.setRefreshing(false)
                    if (res.isCancelledByLifecycle()) return@launch
                    Toast.makeText(mContext, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateProfileUI() {
        binding.apply {
            updateAccountSafe(
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
            refreshLayout.setRefreshing(false)
        }
    }

    private fun requestDeviceInfo(deviceId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val res = api.getDeviceInfo(deviceId, token, locate)) {
                is NetworkResult.Success -> saveDeviceInfo(res.value)
                is NetworkResult.Error -> {
                    binding.refreshLayout.setRefreshing(false)
                    if (res.isCancelledByLifecycle()) return@launch
                    Toast.makeText(mContext, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun NetworkResult.Error.isCancelledByLifecycle(): Boolean {
        val msg = message
        return msg.contains("Job was cancelled", ignoreCase = true) ||
                msg.contains("CancellationException", ignoreCase = true) ||
                msg.contains("cancelled", ignoreCase = true)
    }

    private fun saveDeviceInfo(info: DeviceInfo) {
        main?.saveString(
            PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS,
            simplificationName(info.model?.name.orEmpty())
        )
        main?.saveString(
            PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS,
            info.size?.name.orEmpty()
        )
        main?.saveString(
            PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS,
            info.side?.name.orEmpty()
        )
        main?.saveString(
            PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS,
            info.status?.name.orEmpty()
        )
        main?.saveString(
            PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS,
            info.dateTransfer.orEmpty()
        )
        main?.saveString(
            PreferenceKeysUbi4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS,
            info.guaranteePeriod.orEmpty()
        )

        System.err.println("Device Info model: ${info.model?.name}")
        System.err.println("Device Info size: ${info.size?.name}")
        System.err.println("Device Info side: ${info.side?.name}")
        System.err.println("Device Info status: ${info.status?.name}")
        System.err.println("Device Info date transfer: ${info.dateTransfer}")
        System.err.println("Device Info guarantee period: ${info.guaranteePeriod}")
        System.err.println("Device Info options: ${info.options.size}")
        var rotatorSet = false
        var accumulatorSet = false
        var touchscreenFingersSet = false
        for (option in info.options) {
            if (option.id == 3) {
                main?.saveString(PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS, option.value?.name.orDash())
                System.err.println("Device Info rotator: ${option.value?.name}")
                rotatorSet = true
            }
            if (option.id == 15) {
                main?.saveString(PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS, option.value?.name.orEmpty())
                System.err.println("Device Info accumulator: ${option.value?.name}")
                accumulatorSet = true
            }
            if (option.id == 5) {
                main?.saveString(PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS, option.value?.name.orEmpty())
                System.err.println("Device Info Touchscreen fingers: ${option.value?.name}")
                touchscreenFingersSet = true
            }
        }
        if (!rotatorSet) {
            main?.saveString(PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS, "-")
            System.err.println("Device Info rotator NOT SET")
        }
        if (!accumulatorSet) { System.err.println("Device Info accumulator NOT SET") }
        if (!touchscreenFingersSet) { System.err.println("Device Info Touchscreen fingers NOT SET") }
    }

    private fun String?.orDash(): String =
        takeIf { !it.isNullOrBlank() && it != "null" } ?: "-"


    private fun initAdapter() {

        // ==== 1. Листенеры ===========================================
        val accountClickListener = object : OnAccountMainUBI4ClickListener {
            override fun onCustomerServiceClicked() { navigator().showAccountCustomerServiceScreen() }
            override fun onProsthesisInformationClicked() { navigator().showAccountProsthesisInformationScreen() }
            override fun onGamesClicked() { navigator().showGamesScreen() }
        }

        val bootloaderClickListener = object : BootloaderAdapterUBI4.OnBootloaderClickListener {
            override fun onUpdateClick(item: BootloaderBoardItemUBI4) {
                showFirmwareFilesDialog(item)
                Toast.makeText(
                    requireContext(),
                    getString(SharedRes.strings.update_board.resourceId, item.boardName),
                    Toast.LENGTH_SHORT
                ).show()
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
                main?.mDeviceAddress + PreferenceKeysUbi4.DRIVER_NUM, 1
            ) ?: 1) / 100f).toString()
        } else {
            main?.driverVersionS ?: "0.01"
        }

        bmsVersion = ((mSettings?.getInt(
            main?.mDeviceAddress + PreferenceKeysUbi4.BMS_NUM, 1
        ) ?: 1) / 100f).toString()

        sensorsVersion = ((mSettings?.getInt(
            main?.mDeviceAddress + PreferenceKeysUbi4.SENS_NUM, 1
        ) ?: 1) / 100f).toString()
    }

    private fun applyCachedContentIfAvailable(): Boolean {
        val profile = cachedProfileItem
        val boards = cachedBootloaderBoards

        if (profile == null && boards.isNullOrEmpty()) return false

        profile?.let { updateAccountSafe(it) }
        if (!boards.isNullOrEmpty()) {
            val snapshot = boards.map { it.copy() }
            bootloaderBoardsList.clear()
            bootloaderBoardsList.addAll(snapshot)
            updateBootloaderSafe(snapshot)
        }
        return true
    }

    private fun handleBackPress() {
        val sourceFragmentClassName = arguments?.getString("sourceFragmentClass")
        // Получаем имя исходного фрагмента из аргументов
        val mainActivity = activity as? MainActivityUBI4
        mainActivity?.showTopStatusBar()
        mainActivity?.setStatusBarBackMode(false)
        mainActivity?.showBottomNavigation()
        if (parentFragmentManager.backStackEntryCount > 0) {
            if (sourceFragmentClassName == SensorsFragment::class.java.name) {
                mainActivity?.pausePlotPointsForTransition()
            }
            parentFragmentManager.popBackStack()
            return
        }
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
        resumeDisposables.clear()
        super.onPause()
        System.err.println("AccountFragmentMainUBI4: onPause")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        systemBackCallback?.isEnabled = !hidden
    }


    override fun onDestroyView() {
        resumeDisposables.clear()
        _binding?.accountRv?.adapter = null
        canRenderBoards = false
        systemBackCallback = null
        main?.showBottomNavigation()
        mContext = null
        mSettings = null
        main = null
        _binding = null
        System.err.println("AccountFragmentMainUBI4: onDestroyView")
        super.onDestroyView()
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
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_MANAGER_FIO, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_MANAGER_PHONE, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS, "")
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS, "")
    }




    private fun rebuildBoardNameCache() {
        boardNameByAddr.clear()
        // CPU (addr = 0)
        fullInicializeConnectionStruct?.let {
            boardNameByAddr[0] =
                PreferenceKeysUbi4.DeviceCode
                    .from(it.deviceCode)
                    .title.removeSuffix(" version")      // ← важно!
        }

        // Sub-devices
        baseSubDevicesInfoStructSet.forEach { sub ->
            boardNameByAddr[sub.deviceAddress] =
                PreferenceKeysUbi4.DeviceCode
                    .from(sub.deviceCode)
                    .title.removeSuffix(" version")      // ← то же
        }
    }


    private fun refreshBoards() {
        // 1) Перекешируем имена
        rebuildBoardNameCache()

        // 2) Строим список
        val allBoards = buildList {
            fullInicializeConnectionStruct?.let { cpu ->
                val versionCpu = fwVersions[0] ?: "${cpu.deviceVersion}.${cpu.deviceSubVersion}"
                val nameCpu = boardNameByAddr[0] ?: getString(SharedRes.strings.unknown_board.resourceId)
                add(
                    BootloaderBoardItemUBI4(
                        boardName     = nameCpu,
                        deviceCode    = cpu.deviceCode,
                        deviceAddress = 0,
                        canUpdate     = true,
                        version       = versionCpu,
                        isInBootLoader = false
                    )
                )
            }

            baseSubDevicesInfoStructSet.forEach { sub ->
                val addr = sub.deviceAddress
                val versionSub = fwVersions.getOrDefault(addr, "—")
                val nameSub = boardNameByAddr[addr] ?: getString(SharedRes.strings.unknown_board.resourceId)
                add(
                    BootloaderBoardItemUBI4(
                        boardName     = nameSub,
                        deviceCode    = sub.deviceCode,
                        deviceAddress = addr,
                        canUpdate     = true,
                        version       = versionSub,
                        isInBootLoader = false
                    )
                )
            }
        }
            .distinctBy { it.deviceAddress }
        val builtBoards = allBoards.filter {
            it.boardName != getString(SharedRes.strings.unknown_board.resourceId) &&
                !it.boardName.equals("Unknown", ignoreCase = true)
        }

        if (allBoards.isEmpty() && bootloaderBoardsList.isNotEmpty()) return

        bootloaderBoardsList.clear()
        bootloaderBoardsList.addAll(builtBoards)
        updateBootloaderSafe(builtBoards)
    }

    private fun showFirmwareFilesDialog(boardItem: BootloaderBoardItemUBI4) {

        // 1) Берём ZIP’ы из приватной директории приложения
        val fromDir: List<FirmwareFileItem> = requireActivity()
            .getExternalFilesDir(null)
            ?.listFiles { f -> f.extension.equals("zip", ignoreCase = true) }
            ?.map { f -> FirmwareFileItem(name = f.name, file = f) }
            ?: emptyList()
//
//        // 2) Берём ZIP’ы из ассетов (копируем во временный файл, чтобы был реальный File)
        val fromAssets: List<FirmwareFileItem> =
            FirmwareAssets.collectAssetZips(requireContext(), dir = "")
                .map { (displayName, assetPath) ->
                    val file = FirmwareAssets.copyToCache(requireContext(), assetPath)
                    FirmwareFileItem(name = displayName, file = file)
                }

        // 3) Объединяем, убираем дубли по имени (или по абсолютному пути), сортируем
        val items: MutableList<FirmwareFileItem> = (fromDir + fromAssets)
            .distinctBy { it.name.lowercase() }          // можно сменить ключ на it.file.length() или CRC
            .sortedBy { it.name.lowercase() }
            .toMutableList()

        // 4) Inflate диалога и RecyclerView
        val view = layoutInflater.inflate(R.layout.ubi4_dialog_firmware_files, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        val rv = view.findViewById<RecyclerView>(R.id.dialogFirmwareFileRv)
        val adapter = FirmwareFilesAdapter(items, object : FirmwareFilesAdapter.OnFileActionListener {
            override fun onDelete(position: Int, fileItem: FirmwareFileItem) {
                items.removeAt(position)
                rv.adapter?.notifyItemRemoved(position)
            }
                override fun onSelect(position: Int, fileItem: FirmwareFileItem, onComplete: () -> Unit) {

                        main?.dialogManager?.showConfirmSendFirmwareFileDialog(boardItem, fileItem) {  }
            }
        })


        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        view.findViewById<View>(R.id.dialogFirmwareFileCancelBtn)
            .setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }




    private fun updateAccountSafe(item: AccountMainUBI4Item) {
        _binding?.accountRv?.post {
            cachedProfileItem = item
            accountAdapter.submitProfile(item)
        }
    }

    private fun updateBootloaderSafe(list: List<BootloaderBoardItemUBI4>) {
        val snapshot = list.map { it.copy() }
        cachedBootloaderBoards = snapshot
        _binding?.accountRv?.post { bootloaderAdapter.submitBoards(snapshot) }
    }
    companion object {
        private var cachedProfileItem: AccountMainUBI4Item? = null
        private var cachedBootloaderBoards: List<BootloaderBoardItemUBI4>? = null
        var accountMainList by Delegates.notNull<ArrayList<AccountMainUBI4Item>>()
    }
}
