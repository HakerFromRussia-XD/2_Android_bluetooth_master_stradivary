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
import kotlinx.coroutines.launch
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
    private var serialNumber = "FEST-F-05670"
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
    private val fwVersions = mutableMapOf<Int, String>()
    private val bootloaderBoardsList = mutableListOf<BootloaderBoardItemUBI4>()
    private val boardNameByAddr = mutableMapOf<Int, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        main = activity as? MainActivityUBI4
        mContext = context

        val deviceName = main?.mDeviceName
        serialNumber = deviceName
            .takeIf { !it.isNullOrBlank() && it.startsWith("FEST-") }
            ?: serialNumber

        serialNumber = main?.mDeviceName ?: serialNumber
        serialNumber = deviceName ?: serialNumber

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

        binding.preloaderLav.visibility = View.VISIBLE
        requestToken()
        refreshBoards()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    UiState.updateFlow.collect { refreshBoards() }
                }
                launch {
                    FirmwareInfoState.firmwareInfoFlowV3.collect { versions ->
                        fwVersions.clear()
                        fwVersions.putAll(versions)
                        refreshBoards()
                    }
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            FirmwareInfoState.runProgramTypeFlow.collect { (addr, runType) ->
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
            encryptionResult = encryptionManager?.encrypt(serialNumber)
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
            Toast.makeText(mContext, "No user data on server", Toast.LENGTH_LONG).show()
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
                    devices.firstOrNull { it.serialNumber == serialNumber }?.id?.let { requestDeviceInfo(it) }
                }
                is NetworkResult.Error -> {
                    binding.refreshLayout.setRefreshing(false)
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
                    Toast.makeText(mContext, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveDeviceInfo(info: DeviceInfo) {
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS, simplificationName(info.model?.name.orEmpty()))
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS, info.size?.name.orEmpty())
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS, info.side?.name.orEmpty())
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS, info.status?.name.orEmpty())
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS, info.dateTransfer.orEmpty())
        main?.saveString(PreferenceKeysUbi4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS, info.guaranteePeriod.orEmpty())
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showInfoWithoutConnection() {
        binding.preloaderLav.visibility = View.GONE
        updateAccountSafe(AccountMainUBI4Item("avatarUrl", fname, sname, "Ivanovich", driverVersion, bmsVersion, sensorsVersion))

        val keys = listOf(
            PreferenceKeysUbi4.ACCOUNT_MANAGER_FIO, PreferenceKeysUbi4.ACCOUNT_MANAGER_PHONE,
            PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS
        )
        keys.forEach { main?.saveString(it, "") }
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

    private fun rebuildBoardNameCache() {
        boardNameByAddr.clear()
        GlobalParameters.baseSubDevicesInfoStructSet.forEach { sub ->
            boardNameByAddr[sub.deviceAddress] = PreferenceKeysUbi4.SubDeviceBoard.from(sub.deviceAddress).title.removeSuffix(" board")
        }
    }

    private fun refreshBoards() {
        rebuildBoardNameCache()
        val builtBoards = GlobalParameters.baseSubDevicesInfoStructSet.map { sub ->
            BootloaderBoardItemUBI4(boardNameByAddr[sub.deviceAddress] ?: "Unknown", sub.deviceCode, sub.deviceAddress, true, fwVersions.getOrDefault(sub.deviceAddress, "—"), false)
        }.distinctBy { it.deviceAddress }.sortedBy { it.deviceAddress }
        bootloaderBoardsList.clear()
        bootloaderBoardsList.addAll(builtBoards)
        updateBootloaderSafe(builtBoards)
    }

    private fun handleBackPress() {
        (activity as? MainActivityUBI4)?.showBottomNavigation()
        val source = arguments?.getString("sourceFragmentClass")
        when (source) {
            SprTrainingFragment::class.java.name -> main?.showOpticTrainingGesturesScreen()
            SprGestureFragment::class.java.name -> main?.showOpticGesturesScreen()
            SensorsFragment::class.java.name -> main?.showSensorsScreen()
            SpecialSettingsFragment::class.java.name -> main?.showSpecialScreen()
            else -> parentFragmentManager.popBackStack()
        }
    }

    private fun showFirmwareFilesDialog(boardItem: BootloaderBoardItemUBI4) {
        val fromAssets = FirmwareAssets.collectAssetZips(requireContext(), "").map { (name, path) ->
            FirmwareFileItem(name, FirmwareAssets.copyToCache(requireContext(), path))
        }
        val view = layoutInflater.inflate(R.layout.ubi4_dialog_firmware_files, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        val rv = view.findViewById<RecyclerView>(R.id.dialogFirmwareFileRv)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = FirmwareFilesAdapter(fromAssets.toMutableList(), object : FirmwareFilesAdapter.OnFileActionListener {
            override fun onDelete(p: Int, f: FirmwareFileItem) {}
            override fun onSelect(p: Int, f: FirmwareFileItem, done: () -> Unit) {
                main?.dialogManager?.showConfirmSendFirmwareFileDialog(boardItem, f) {}
                dialog.dismiss()
            }
        })
        view.findViewById<View>(R.id.dialogFirmwareFileCancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun updateAccountSafe(item: AccountMainUBI4Item) {
        _binding?.accountRv?.post { accountAdapter.submitProfile(item) }
    }

    private fun updateBootloaderSafe(list: List<BootloaderBoardItemUBI4>) {
        _binding?.accountRv?.post { bootloaderAdapter.submitBoards(list) }
    }

    override fun onPause() {
        resumeDisposables.clear()
        super.onPause()
    }

    override fun onDestroyView() {
        resumeDisposables.clear()
        _binding?.accountRv?.adapter = null
        mContext = null
        mSettings = null
        main = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        var accountMainList by Delegates.notNull<ArrayList<AccountMainUBI4Item>>()
    }
}
