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
import androidx.fragment.app.Fragment
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
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.*
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountProfileViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileHeader
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountBoard
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileAction
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3ServiceFirmwareMessage
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation

class AccountFragmentMainV3 : Fragment() {
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
    private var systemBackCallback: OnBackPressedCallback? = null
    private var renderedFirmwareCatalogRevision = -1L
    private var nextFirmwareRequestId = 0L
    private val pendingFirmwareBoards = mutableMapOf<Long, BootloaderBoardItemUBI4>()

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
        profileViewModel.onAction(V3AccountProfileAction.ViewAttached(
            BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND,
        ))
        renderedHeaderRevision = -1L
        refreshCompletionId = 0L
        renderedFirmwareCatalogRevision = -1L

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

        profileViewModel.onAction(V3AccountProfileAction.CachedBoardsApplied)
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.uiState.collect(::renderAccountProfile)
        }

        val transitionDurationMs = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val createdBinding = binding
        binding.root.postDelayed({
            if (!isAdded || _binding !== createdBinding) return@postDelayed
            profileViewModel.onAction(V3AccountProfileAction.BoardRenderingReady)
            profileViewModel.onAction(V3AccountProfileAction.LoadRequested)
        }, transitionDurationMs + if (BuildConfig.ACCOUNT_LOAD_PROFILE_IN_BACKGROUND) 80L else 0L)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    profileViewModel.onAction(V3AccountProfileAction.BoardUpdatesStarted)
                    try { awaitCancellation() }
                    finally { profileViewModel.onAction(V3AccountProfileAction.BoardUpdatesStopped) }
                }
                launch {
                    profileViewModel.uiState.map { it.areBoardServiceActionsVisible }
                        .distinctUntilChanged()
                        .collect { refreshServiceRoleUi() }
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
            showAchievementsItem = true
        )
        bootloaderAdapter = BootloaderAdapterUBI4(
            listener = bootloaderClickListener,
            showSettingsButtonProvider = ::areBoardServiceActionsVisible,
            showUpdateButtonProvider = ::areBoardServiceActionsVisible,
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
        profileViewModel.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested)
    }

    private fun showFirmwareFilesDialog(boardItem: BootloaderBoardItemUBI4) {
        val requestId = ++nextFirmwareRequestId
        pendingFirmwareBoards[requestId] = boardItem
        profileViewModel.onAction(V3AccountProfileAction.FirmwareFilesRequested(
            boardItem.deviceAddress, requestId,
        ))
        // A rejected click must not replace the row captured by the active download.
        if (profileViewModel.uiState.value.firmwareDownloadRequestId != requestId) pendingFirmwareBoards.remove(requestId)
    }

    private fun renderFirmwareCatalog(state: V3AccountProfileUiState) {
        if (renderedFirmwareCatalogRevision != state.firmwareCatalogRevision) {
            renderedFirmwareCatalogRevision = state.firmwareCatalogRevision
            state.firmwareCatalogSize?.let {
                Log.d(FIRMWARE_LOG_TAG, "Loaded $it firmware files from Yandex Disk")
            }
            state.firmwareCatalogError?.let {
                Log.w(FIRMWARE_LOG_TAG, "Yandex firmware catalog is unavailable", it)
            }
        }
        state.firmwareMessages.forEach { message ->
            profileViewModel.onAction(V3AccountProfileAction.FirmwareMessageShown(message.id))
            when (message) {
                is V3ServiceFirmwareMessage.CatalogUnavailable -> showFirmwareCatalogUnavailableToast()
                is V3ServiceFirmwareMessage.NotFound -> Toast.makeText(requireContext(), R.string.firmware_not_found_for_board, Toast.LENGTH_SHORT).show()
                is V3ServiceFirmwareMessage.DownloadStarted -> Toast.makeText(requireContext(), R.string.firmware_downloading, Toast.LENGTH_SHORT).show()
                is V3ServiceFirmwareMessage.FilesDownloaded -> {
                    val boardItem = pendingFirmwareBoards.remove(message.requestId)
                    if (boardItem != null) {
                        if (isAdded && _binding != null) showDownloadedFirmwareDialog(boardItem,
                            message.files.map { FirmwareFileItem(it.name, java.io.File(it.path)) })
                    }
                }
                is V3ServiceFirmwareMessage.DownloadFailed -> {
                    Log.w(FIRMWARE_LOG_TAG, "Cannot download firmware files", message.error)
                    pendingFirmwareBoards.remove(message.requestId)
                    if (isAdded && _binding != null) showFirmwareCatalogUnavailableToast()
                }
            }
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
        renderFirmwareCatalog(state)
        binding.preloaderLav.visibility = if (state.isContentVisible) View.GONE else View.VISIBLE
        binding.accountRv.visibility = if (state.isContentVisible) View.VISIBLE else View.INVISIBLE
        renderBoards(state)
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

    private fun renderBoards(state: V3AccountProfileUiState) {
        state.boardSubmissions.forEach { submission ->
            profileViewModel.onAction(V3AccountProfileAction.BoardSubmissionRendered(submission.id))
            val snapshot = submission.boards.map { it.toBoardItem() }
            if (submission.submitImmediately) {
                bootloaderAdapter.submitBoards(snapshot)
            } else {
                _binding?.accountRv?.post {
                    bootloaderAdapter.submitBoards(snapshot)
                    scrollAccountListToTop()
                }
                if (state.isTokenLoaded) {
                    binding.accountRv.visibility = View.VISIBLE
                    binding.preloaderLav.visibility = View.GONE
                }
            }
        }
    }

    private fun V3AccountBoard.toBoardItem() = BootloaderBoardItemUBI4(
        boardName = name ?: getString(SharedRes.strings.unknown_board.resourceId),
        deviceCode = deviceCode, deviceAddress = deviceAddress, canUpdate = canUpdate,
        version = version, isInBootLoader = isInBootloader, isUpdateAvailable = isUpdateAvailable,
    )

    private fun areBoardServiceActionsVisible(): Boolean =
        profileViewModel.uiState.value.areBoardServiceActionsVisible

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshServiceRoleUi() {
        if (!profileViewModel.uiState.value.isBoardRenderingReady) return
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
        pendingFirmwareBoards.clear()
        resumeDisposables.clear()
        _binding?.accountRv?.adapter = null
        systemBackCallback = null
        mContext = null
        main = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val FIRMWARE_LOG_TAG = "FirmwareCatalogV3"
    }
}
