package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentV3

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountMainBinding
import com.bailout.stickk.ubi4.adapters.dialog.FirmwareFilesAdapter
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.data.network.RemoteFirmwareFile
import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.firmware.FirmwareCompatibility
import com.bailout.stickk.ubi4.firmware.FirmwareVersionCatalog
import com.bailout.stickk.ubi4.models.FirmwareFileItem
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
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountProfileViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileHeader
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileAction
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileViewModel
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountFragmentMainV3 : BaseWidgetsFragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null

    private val profileViewModel: V3AccountProfileViewModel by viewModels {
        V3AccountProfileViewModelFactory.from(requireContext())
    }
    private var renderedHeaderRevision = -1L
    private var refreshCompletionId = 0L

    private lateinit var accountAdapter: AccountMainAdapterUBI4
    private lateinit var bootloaderAdapter: BootloaderAdapterUBI4

    private var _binding: Ubi4FragmentPersonalAccountMainBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val resumeDisposables = CompositeDisposable()
    private val bootloaderBoardsList = mutableListOf<BootloaderBoardItemUBI4>()
    private val boardNameByCode = mutableMapOf<Int, String>()
    private var canRenderBoards = false
    private var isBoardsRendered = false
    private var systemBackCallback: OnBackPressedCallback? = null
    private val firmwareRepository = YandexDiskFirmwareRepository()
    private var remoteFirmwareCatalog: List<RemoteFirmwareFile>? = null
    private var firmwareCatalogJob: Job? = null
    private var firmwareDownloadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        main = activity as? MainActivityUBI4
        mContext = context

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = mContext?.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        UiState.isServiceEngineerRole.value =
            settings?.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, ROLE_DEFAULT_INDEX) ==
                ROLE_SERVICE_ENGINEER_INDEX

        profileViewModel.onAction(V3AccountProfileAction.ViewAttached(
            BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND, !cachedBootloaderBoards.isNullOrEmpty(),
        ))
        renderedHeaderRevision = -1L
        refreshCompletionId = 0L

        if (BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND) {
            binding.preloaderLav.cancelAnimation()
            binding.refreshLayout.setOnRefreshListener {
                profileViewModel.onAction(V3AccountProfileAction.LoadRequested)
                refreshFirmwareCatalog()
            }
        } else {
            setupRefreshLayout()
        }

        initAdapter()
        binding.backBtn.setOnClickListener { handleBackPress() }
        refreshFirmwareCatalog()

        val hasCachedContent = applyCachedContentIfAvailable()
        canRenderBoards = hasCachedContent && !BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.uiState.collect(::renderAccountProfile)
        }

        val transitionDurationMs = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val createdBinding = binding
        binding.root.postDelayed({
            if (!isAdded || _binding !== createdBinding) return@postDelayed
            canRenderBoards = true
            refreshBoards()
            profileViewModel.onAction(V3AccountProfileAction.LoadRequested)
        }, transitionDurationMs + if (BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND) 80L else 0L)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    UiState.updateFlow.collect {
                        if (canRenderBoards) refreshBoards()
                    }
                }
                launch {
                    UiState.isServiceEngineerRole.collect {
                        refreshServiceRoleUi()
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
                        profileViewModel.onAction(V3AccountProfileAction.HeaderRefreshRequested)
                    }
                }
        )
    }

    private fun setupRefreshLayout() {
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            profileViewModel.onAction(V3AccountProfileAction.LoadRequested)
        }
    }

    private fun initAdapter() {
        val accountClickListener = object : OnAccountMainUBI4ClickListener {
            override fun onCustomerServiceClicked() { navigator().showAccountCustomerServiceScreen() }
            override fun onProsthesisInformationClicked() { navigator().showAccountProsthesisInformationScreen() }
            override fun onGamesClicked() { navigator().showGamesScreen() }
            override fun onStatisticsClicked() { navigator().showAccountStatisticsScreen() }
            override fun onAchievementsClicked() { navigator().showAchievementsScreen() }
        }
        val bootloaderClickListener = object : BootloaderAdapterUBI4.OnBootloaderClickListener {
            override fun onUpdateClick(item: BootloaderBoardItemUBI4) { showFirmwareFilesDialog(item) }
            override fun onSettingsClick(item: BootloaderBoardItemUBI4) { navigator().showDashboardSlotsScreen(item.deviceAddress) }
        }
        accountAdapter = AccountMainAdapterUBI4(
            onAccountClickListener = accountClickListener,
            showStatisticsItem = true,
            showAchievementsItem = false
        )
        bootloaderAdapter = BootloaderAdapterUBI4(
            listener = bootloaderClickListener,
            showSettingsButtonProvider = ::isServiceFragmentVisibleInBottomNavigation,
            showUpdateButtonProvider = ::isServiceFragmentVisibleInBottomNavigation,
            loadLocalVersionsOnBind = !BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND
        )
        if (BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND) {
            profileViewModel.uiState.value.let { state ->
                state.header?.let { accountAdapter.submitProfile(it.toAccountItem()) }
                renderedHeaderRevision = state.headerRevision
            }
            viewLifecycleOwner.lifecycleScope.launch {
                bootloaderAdapter.preloadLocalVersions(requireContext())
            }
        }
        val concatAdapter = ConcatAdapter(accountAdapter, BootloaderCardAdapter(bootloaderAdapter))
        binding.accountRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            itemAnimator = null
        }
    }

    private fun applyCachedContentIfAvailable(): Boolean {
        val hasProfile = profileViewModel.uiState.value.hasCachedProfile
        val boards = cachedBootloaderBoards

        if (!hasProfile && boards.isNullOrEmpty()) return false

        if (!boards.isNullOrEmpty()) {
            val snapshot = boards
                .filterNot { FirmwareVersionCatalog.isZeroVersion(it.version) }
                .map { it.copy(isUpdateAvailable = false) }
            bootloaderBoardsList.clear()
            bootloaderBoardsList.addAll(snapshot)
            if (!BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND) updateBootloaderSafe(snapshot)
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
            val unknownName = getString(SharedRes.strings.unknown_board.resourceId)
            val family = FirmwareBoardFamily.fromDeviceAddress(sub.deviceAddress)
            val nameByCode = boardNameByCode[sub.deviceCode]
            val name = nameByCode
                ?.takeUnless { it.equals("Unknown", ignoreCase = true) }
                ?: family.takeUnless { it == FirmwareBoardFamily.UNKNOWN }?.name
                ?: unknownName
            val fw = sub.fwVersion.takeIf { it.isNotBlank() }
                ?: "—"
            val familyFileNames = remoteFirmwareCatalog
                ?.asSequence()
                ?.filter { it.family == family }
                ?.map { it.name }
                ?.toList()
                .orEmpty()
            val isUpdateAvailable = remoteFirmwareCatalog != null &&
                FirmwareCompatibility.isUpdateAvailable(
                    deviceAddress = sub.deviceAddress,
                    installedVersion = fw,
                    fileNames = familyFileNames
                )
            if (name == unknownName) {
                Log.w(
                    BOARD_LOG_TAG,
                    "Unknown board resolved: addr=${sub.deviceAddress}, code=${sub.deviceCode}, fw=$fw, nameByDataCode=${
                        PreferenceKeysUbi4.DeviceCodeV3.fromCode(sub.deviceCode).title.removeSuffix(" board")
                    }"
                )
            }
            BootloaderBoardItemUBI4(
                boardName = name,
                deviceCode = sub.deviceCode,
                deviceAddress = sub.deviceAddress,
                canUpdate = true,
                version = fw,
                isInBootLoader = false,
                isUpdateAvailable = isUpdateAvailable
            )
        }.distinctBy { it.deviceAddress }.sortedBy { it.deviceAddress }
        val builtBoards = allBoards.filter {
            !FirmwareVersionCatalog.isZeroVersion(it.version) &&
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
        if (!profileViewModel.uiState.value.isTokenLoaded || !isBoardsRendered) return
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

    private fun refreshFirmwareCatalog() {
        firmwareCatalogJob?.cancel()
        remoteFirmwareCatalog = null
        if (canRenderBoards) refreshBoards()
        firmwareCatalogJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val catalog = firmwareRepository.loadCatalog()
                remoteFirmwareCatalog = catalog
                if (canRenderBoards) refreshBoards()
                Log.d(FIRMWARE_LOG_TAG, "Loaded ${catalog.size} firmware files from Yandex Disk")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                remoteFirmwareCatalog = null
                if (canRenderBoards) refreshBoards()
                Log.w(FIRMWARE_LOG_TAG, "Yandex firmware catalog is unavailable", error)
            }
        }
    }

    private fun showFirmwareFilesDialog(boardItem: BootloaderBoardItemUBI4) {
        val catalog = remoteFirmwareCatalog
        if (catalog == null) {
            showFirmwareCatalogUnavailableToast()
            return
        }

        val family = FirmwareBoardFamily.fromDeviceAddress(boardItem.deviceAddress)
        val candidates = catalog
            .filter { it.family == family }
            .filter { FirmwareCompatibility.isCompatible(boardItem.deviceAddress, it.name) }
            .sortedWith { left, right -> compareFirmwareForBoard(boardItem.deviceAddress, left, right) }

        if (family == FirmwareBoardFamily.UNKNOWN || candidates.isEmpty()) {
            Toast.makeText(requireContext(), R.string.firmware_not_found_for_board, Toast.LENGTH_SHORT).show()
            return
        }
        if (firmwareDownloadJob?.isActive == true) return

        Toast.makeText(requireContext(), R.string.firmware_downloading, Toast.LENGTH_SHORT).show()
        firmwareDownloadJob = viewLifecycleOwner.lifecycleScope.launch {
            val cacheDirectory = sharedFile(requireContext().cacheDir.absolutePath)
            try {
                val items = candidates.map { remote ->
                    FirmwareFileItem(
                        name = remote.name,
                        file = java.io.File(firmwareRepository.download(remote, cacheDirectory).path)
                    )
                }
                if (isAdded && _binding != null) showDownloadedFirmwareDialog(boardItem, items)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(FIRMWARE_LOG_TAG, "Cannot download firmware files", error)
                if (isAdded && _binding != null) showFirmwareCatalogUnavailableToast()
            }
        }
    }

    private fun compareFirmwareForBoard(
        deviceAddress: Int,
        left: RemoteFirmwareFile,
        right: RemoteFirmwareFile
    ): Int {
        val leftVersion = FirmwareCompatibility.versionForDevice(deviceAddress, left.name)
        val rightVersion = FirmwareCompatibility.versionForDevice(deviceAddress, right.name)
        return when {
            FirmwareVersionCatalog.isLocalVersionNewer(rightVersion, leftVersion) -> -1
            FirmwareVersionCatalog.isLocalVersionNewer(leftVersion, rightVersion) -> 1
            else -> left.name.compareTo(right.name, ignoreCase = true)
        }
    }

    private fun showDownloadedFirmwareDialog(
        boardItem: BootloaderBoardItemUBI4,
        downloadedItems: List<FirmwareFileItem>
    ) {
        val items = downloadedItems.toMutableList()

        val view = layoutInflater.inflate(R.layout.ubi4_dialog_firmware_files, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        val rv = view.findViewById<RecyclerView>(R.id.dialogFirmwareFileRv)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = FirmwareFilesAdapter(items, object : FirmwareFilesAdapter.OnFileActionListener {
            override fun onDelete(position: Int, fileItem: FirmwareFileItem) = Unit

            override fun onSelect(position: Int, fileItem: FirmwareFileItem, onComplete: () -> Unit) {
                main?.dialogManager?.showConfirmSendFirmwareFileDialog(boardItem, fileItem) {}
                dialog.dismiss()
            }
        }, showDeleteButton = false)
        view.findViewById<View>(R.id.dialogFirmwareFileCancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showFirmwareCatalogUnavailableToast() {
        Toast.makeText(requireContext(), R.string.firmware_catalog_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun renderAccountProfile(state: V3AccountProfileUiState) {
        binding.preloaderLav.visibility = if (state.isContentVisible) View.GONE else View.VISIBLE
        binding.accountRv.visibility = if (state.isContentVisible) View.VISIBLE else View.INVISIBLE
        if (state.header != null && renderedHeaderRevision != state.headerRevision) {
            renderedHeaderRevision = state.headerRevision
            val currentBinding = binding
            currentBinding.accountRv.post {
                if (_binding !== currentBinding || profileViewModel.uiState.value.headerRevision != state.headerRevision) return@post
                profileViewModel.onAction(V3AccountProfileAction.HeaderRendered(state.headerRevision))
                accountAdapter.submitProfile(state.header.toAccountItem())
                scrollAccountListToTop()
            }
        }
        if (refreshCompletionId != state.refreshCompletionId) {
            refreshCompletionId = state.refreshCompletionId
            binding.refreshLayout.setRefreshing(false)
        }
        state.messages.forEach { message ->
            if (message.serverMessage == null) {
                Toast.makeText(context, getString(SharedRes.strings.no_user_data_on_server.resourceId), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, message.serverMessage, Toast.LENGTH_SHORT).show()
            }
            profileViewModel.onAction(V3AccountProfileAction.MessageShown(message.id))
        }
    }

    private fun V3AccountProfileHeader.toAccountItem() = AccountMainUBI4Item(
        avatarUrl = "avatarUrl", name = firstName, surname = lastName, patronymic = "Ivanovich",
        versionDriver = versions.driver, versionBms = versions.bms, versionSensors = versions.sensors,
    )

    private fun updateBootloaderSafe(list: List<BootloaderBoardItemUBI4>) {
        val snapshot = list.map { it.copy() }
        cachedBootloaderBoards = snapshot
        _binding?.accountRv?.post {
            bootloaderAdapter.submitBoards(snapshot)
            scrollAccountListToTop()
        }
    }

    private fun isServiceFragmentVisibleInBottomNavigation(): Boolean =
        UiState.isServiceEngineerRole.value

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshServiceRoleUi() {
        if (!canRenderBoards) return
        bootloaderAdapter.notifyDataSetChanged()
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
        if (!hidden && _binding != null) refreshFirmwareCatalog()
    }

    override fun onDestroyView() {
        profileViewModel.onAction(V3AccountProfileAction.ViewDetached)
        firmwareCatalogJob?.cancel()
        firmwareDownloadJob?.cancel()
        resumeDisposables.clear()
        _binding?.accountRv?.adapter = null
        canRenderBoards = false
        isBoardsRendered = false
        systemBackCallback = null
        mContext = null
        main = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val BOARD_LOG_TAG = "AccountBoardsV3"
        private const val FIRMWARE_LOG_TAG = "FirmwareCatalogV3"
        private const val ROLE_SERVICE_ENGINEER_INDEX = 1
        private const val ROLE_DEFAULT_INDEX = 2
        private var cachedBootloaderBoards: List<BootloaderBoardItemUBI4>? = null
    }
}
