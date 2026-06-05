package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentV3

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
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
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.user.Manager
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.*
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.properties.Delegates

class AccountFragmentMainV3 : BaseWidgetsFragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var mSettings: SharedPreferences? = null

    private var token = ""
    private var clientId = 0
    private var encryptionManager: EncryptionManagerUtilsUbi4? = null
    private var encryptionResult: String? = null
    private var serialNumber = "FEST-F-06879"
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

    private var _binding: Ubi4FragmentPersonalAccountMainBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val resumeDisposables = CompositeDisposable()
    private val bootloaderBoardsList = mutableListOf<BootloaderBoardItemUBI4>()
    private val boardNameByCode = mutableMapOf<Int, String>()
    private var canRenderBoards = false
    private var isTokenLoaded = false
    private var isBoardsRendered = false
    private var systemBackCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        main = activity as? MainActivityUBI4
        mContext = context

        serialNumber = main?.mDeviceName
            ?.takeIf { it.startsWith("FEST-") }
            ?: serialNumber

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = mContext?.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)

        encryptionManager = EncryptionManagerUtilsUbi4.instance
        attemptedRequest = 1
        if (main?.locate?.contains("ru") == true) { locate = "ru" }

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
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
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    UiState.updateFlow.collect {
                        if (canRenderBoards) refreshBoards()
                    }
                }
            }
        }

        systemBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            requireNotNull(systemBackCallback)
        )
        systemBackCallback?.isEnabled = !isHidden

        viewLifecycleOwner.lifecycleScope.launch {
            FirmwareInfoState.runProgramTypeFlow.collect { (addr, runType) ->
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

    private fun initializeUI() {
        initAdapter()
        binding.backBtn.setOnClickListener { handleBackPress() }

        driverVersion = if (!checkMultigrib()) {
            ((mSettings?.getInt(main?.mDeviceAddress + PreferenceKeysUbi4.DRIVER_NUM, 1) ?: 1) / 100f).toString()
        } else {
            main?.driverVersionS ?: "0.01"
        }
        bmsVersion = ((mSettings?.getInt(main?.mDeviceAddress + PreferenceKeysUbi4.BMS_NUM, 1) ?: 1) / 100f).toString()
        sensorsVersion = ((mSettings?.getInt(main?.mDeviceAddress + PreferenceKeysUbi4.SENS_NUM, 1) ?: 1) / 100f).toString()
    }

    private fun requestToken() {
        viewLifecycleOwner.lifecycleScope.launch {
            encryptionResult = withContext(Dispatchers.Default) {
                encryptionManager?.encrypt(serialNumber)
            }
            when (val res = api.getToken("Aesserial $encryptionResult")) {
                is NetworkResult.Success -> {
                    token = res.value.token
                    isTokenLoaded = true
                    binding.preloaderLav.visibility = View.GONE
                    binding.accountRv.visibility = View.VISIBLE
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

    @SuppressLint("NotifyDataSetChanged")
    private fun showInfoWithoutConnection() {
        binding.preloaderLav.visibility = View.GONE
        binding.accountRv.visibility = View.VISIBLE
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

    private fun checkMultigrib(): Boolean = main?.mDeviceType?.contains(ConstantManagerUBI4.DEVICE_TYPE_FEST_X) == true

    private fun simplificationName(name: String): String = name.substringFrom("ПР", name.lastIndex)

    private fun String.substringFrom(char: String, maxLen: Int) =
        indexOf(char).let { if (it >= 0) substring(it, min(it + maxLen, length)) else this }

    private fun initAdapter() {
        val accountClickListener = object : OnAccountMainUBI4ClickListener {
            override fun onCustomerServiceClicked() { navigator().showAccountCustomerServiceScreen() }
            override fun onProsthesisInformationClicked() { navigator().showAccountProsthesisInformationScreen() }
        }
        val bootloaderClickListener = object : BootloaderAdapterUBI4.OnBootloaderClickListener {
            override fun onUpdateClick(item: BootloaderBoardItemUBI4) { showFirmwareFilesDialog(item) }
        }
        accountAdapter = AccountMainAdapterUBI4(accountClickListener)
        bootloaderAdapter = BootloaderAdapterUBI4(bootloaderClickListener)
        concatAdapter = ConcatAdapter(accountAdapter, BootloaderCardAdapter(bootloaderAdapter))
        binding.accountRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            itemAnimator = null
        }
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
            isBoardsRendered = true
        }
        return true
    }

    private fun rebuildBoardNameCache() {
        boardNameByCode.clear()
        GlobalParameters.baseSubDevicesInfoStructSet.forEach { sub ->
            val resolvedName = PreferenceKeysUbi4.DeviceCodeV3
                .fromCode(sub.deviceCode)
                .title
                .removeSuffix(" board")
            boardNameByCode[sub.deviceCode] = resolvedName
            Log.d(
                BOARD_LOG_TAG,
                "rebuildBoardNameCache: addr=${sub.deviceAddress}, code=${sub.deviceCode}, nameByDataCode=$resolvedName"
            )
        }
        Log.d(BOARD_LOG_TAG, "rebuildBoardNameCache result: $boardNameByCode")
    }

    private fun refreshBoards() {
        rebuildBoardNameCache()
        Log.d(
            BOARD_LOG_TAG,
            "refreshBoards start: subDevices=${
                GlobalParameters.baseSubDevicesInfoStructSet.joinToString(prefix = "[", postfix = "]") {
                    "{addr=${it.deviceAddress}, code=${it.deviceCode}, fw=${it.fwVersion}}"
                }
            }"
        )
        val allBoards = GlobalParameters.baseSubDevicesInfoStructSet.map { sub ->
            val name = boardNameByCode[sub.deviceCode] ?: getString(SharedRes.strings.unknown_board.resourceId)
            val fw = sub.fwVersion.takeIf { it.isNotBlank() }
                ?: "—"
            if (name == getString(SharedRes.strings.unknown_board.resourceId)) {
                Log.w(
                    BOARD_LOG_TAG,
                    "Unknown board resolved: addr=${sub.deviceAddress}, code=${sub.deviceCode}, fw=$fw, nameByDataCode=${
                        PreferenceKeysUbi4.DeviceCodeV3.fromCode(sub.deviceCode).title.removeSuffix(" board")
                    }"
                )
            }
            BootloaderBoardItemUBI4(name, sub.deviceCode, sub.deviceAddress, true, fw, false)
        }.distinctBy { it.deviceAddress }.sortedBy { it.deviceAddress }
        val builtBoards = allBoards.filter {
            it.boardName != getString(SharedRes.strings.unknown_board.resourceId) &&
                !it.boardName.equals("Unknown", ignoreCase = true)
        }
        if (allBoards.isEmpty() && bootloaderBoardsList.isNotEmpty()) return
        bootloaderBoardsList.clear()
        bootloaderBoardsList.addAll(builtBoards)
        updateBootloaderSafe(builtBoards)
        Log.d(
            BOARD_LOG_TAG,
            "refreshBoards done: built=${
                builtBoards.joinToString(prefix = "[", postfix = "]") {
                    "{addr=${it.deviceAddress}, code=${it.deviceCode}, name=${it.boardName}, fw=${it.version}}"
                }
            }"
        )
        isBoardsRendered = true
        revealVersionsWhenReady()
    }

    private fun revealVersionsWhenReady() {
        if (!isTokenLoaded || !isBoardsRendered) return
        binding.accountRv.visibility = View.VISIBLE
        binding.preloaderLav.visibility = View.GONE
    }

    private fun handleBackPress() {
        val mainActivity = activity as? MainActivityUBI4
        val source = arguments?.getString("sourceFragmentClass")

        mainActivity?.showTopStatusBar()
        mainActivity?.setStatusBarBackMode(false)
        mainActivity?.showBottomNavigation()
        if (parentFragmentManager.backStackEntryCount > 0) {
            if (source == SensorsFragment::class.java.name) {
                mainActivity?.pausePlotPointsForTransition()
            }
            parentFragmentManager.popBackStack()
            return
        }
        when (source) {
            SprTrainingFragment::class.java.name -> main?.showOpticTrainingGesturesScreen()
            SprGestureFragment::class.java.name -> main?.showOpticGesturesScreen()
            SensorsFragment::class.java.name -> main?.showSensorsScreen()
            SpecialSettingsFragment::class.java.name -> main?.showSpecialScreen()
            else -> parentFragmentManager.popBackStack()
        }
    }

    private fun showFirmwareFilesDialog(boardItem: BootloaderBoardItemUBI4) {
        val fromDir: List<FirmwareFileItem> = requireActivity()
            .getExternalFilesDir(null)
            ?.listFiles { f -> f.extension.equals("zip", ignoreCase = true) }
            ?.map { f -> FirmwareFileItem(name = f.name, file = f) }
            ?: emptyList()

        val fromAssets: List<FirmwareFileItem> =
            FirmwareAssets.collectAssetZips(requireContext(), dir = "")
                .map { (displayName, assetPath) ->
                    val file = FirmwareAssets.copyToCache(requireContext(), assetPath)
                    FirmwareFileItem(name = displayName, file = file)
                }

        val items: MutableList<FirmwareFileItem> = (fromDir + fromAssets)
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name.lowercase() }
            .toMutableList()

        val view = layoutInflater.inflate(R.layout.ubi4_dialog_firmware_files, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        val rv = view.findViewById<RecyclerView>(R.id.dialogFirmwareFileRv)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = FirmwareFilesAdapter(items, object : FirmwareFilesAdapter.OnFileActionListener {
            override fun onDelete(position: Int, fileItem: FirmwareFileItem) {
                items.removeAt(position)
                rv.adapter?.notifyItemRemoved(position)
            }

            override fun onSelect(position: Int, fileItem: FirmwareFileItem, onComplete: () -> Unit) {
                main?.dialogManager?.showConfirmSendFirmwareFileDialog(boardItem, fileItem) {}
                dialog.dismiss()
            }
        })
        view.findViewById<View>(R.id.dialogFirmwareFileCancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun updateAccountSafe(item: AccountMainUBI4Item) {
        _binding?.accountRv?.post {
            cachedProfileItem = item
            accountAdapter.submitProfile(item)
            scrollAccountListToTop()
        }
    }

    private fun updateBootloaderSafe(list: List<BootloaderBoardItemUBI4>) {
        val snapshot = list.map { it.copy() }
        cachedBootloaderBoards = snapshot
        _binding?.accountRv?.post {
            bootloaderAdapter.submitBoards(snapshot)
            scrollAccountListToTop()
        }
    }

    private fun scrollAccountListToTop() {
        val rv = _binding?.accountRv ?: return
        if ((rv.adapter?.itemCount ?: 0) > 0) {
            rv.scrollToPosition(0)
        }
    }

    override fun onPause() {
        resumeDisposables.clear()
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        systemBackCallback?.isEnabled = !hidden
    }

    override fun onDestroyView() {
        resumeDisposables.clear()
        _binding?.accountRv?.adapter = null
        canRenderBoards = false
        isTokenLoaded = false
        isBoardsRendered = false
        systemBackCallback = null
        mContext = null
        mSettings = null
        main = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val BOARD_LOG_TAG = "AccountBoardsV3"
        private var cachedProfileItem: AccountMainUBI4Item? = null
        private var cachedBootloaderBoards: List<BootloaderBoardItemUBI4>? = null
        var accountMainList by Delegates.notNull<ArrayList<AccountMainUBI4Item>>()
    }
}
