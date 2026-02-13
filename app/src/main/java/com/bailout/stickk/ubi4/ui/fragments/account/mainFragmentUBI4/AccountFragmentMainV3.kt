package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

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
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class AccountFragmentMainV3 : BaseWidgetsFragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var mSettings: SharedPreferences? = null

    private lateinit var accountAdapter: AccountMainAdapterUBI4
    private lateinit var bootloaderAdapter: BootloaderAdapterUBI4
    private lateinit var concatAdapter: ConcatAdapter
    
    private lateinit var binding: Ubi4FragmentPersonalAccountMainBinding
    private val fwVersions = mutableMapOf<Int, String>()
    private val bootloaderBoardsList = mutableListOf<BootloaderBoardItemUBI4>()
    private val boardNameByAddr = mutableMapOf<Int, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentPersonalAccountMainBinding.inflate(inflater, container, false)
        main = activity as? MainActivityUBI4
        mContext = context
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = mContext?.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        
        initializeUI()
        
        // 1. Показываем заглушку профиля
        updateProfilePlaceholder()
        
        // 2. Инициализируем список плат
        refreshBoards()

        // Подписка на реальные данные прошивок
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1) Обновления списка плат (после парса)
                launch {
                    UiState.updateFlow.collect {
                        refreshBoards()
                    }
                }

                // 2) Приход версий прошивок
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

        // Подписка на статус бутлоадера
        viewLifecycleOwner.lifecycleScope.launch {
            runProgramTypeFlow.collect { (addr, runType) ->
                val idx = bootloaderBoardsList.indexOfFirst { it.deviceAddress == addr }
                if (idx != -1) {
                    bootloaderBoardsList[idx].isInBootLoader = runType == PreferenceKeysUbi4.RunProgramType.BOOTLOADER
                    bootloaderAdapter.notifyItemChanged(idx)
                }
            }
        }
    }

    private fun initializeUI() {
        initAdapter()
        binding.backBtn.setOnClickListener { handleBackPress() }
        binding.preloaderLav.visibility = View.GONE
        binding.refreshLayout.setOnRefreshListener { binding.refreshLayout.setRefreshing(false) }
    }

    private fun initAdapter() {
        val accountClickListener = object : OnAccountMainUBI4ClickListener {
            override fun onCustomerServiceClicked() { navigator().showAccountCustomerServiceScreen() }
            override fun onProsthesisInformationClicked() { navigator().showAccountProsthesisInformationScreen() }
        }

        val bootloaderClickListener = object : BootloaderAdapterUBI4.OnBootloaderClickListener {
            override fun onUpdateClick(item: BootloaderBoardItemUBI4) {
                showFirmwareFilesDialog(item)
            }
        }

        accountAdapter = AccountMainAdapterUBI4(accountClickListener)
        bootloaderAdapter = BootloaderAdapterUBI4(bootloaderClickListener)
        val bootloaderCardAdapter = BootloaderCardAdapter(bootloaderAdapter)
        concatAdapter = ConcatAdapter(accountAdapter, bootloaderCardAdapter)

        binding.accountRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            itemAnimator = null
        }
    }

    private fun updateProfilePlaceholder() {
        val item = AccountMainUBI4Item(
            avatarUrl = "",
            name = "V3 Mode",
            surname = "Active",
            patronymic = "",
            versionDriver = "1.0",
            versionBms = "1.0",
            versionSensors = "1.0"
        )
        binding.accountRv.post { accountAdapter.submitProfile(item) }
    }

    private fun rebuildBoardNameCache() {
        boardNameByAddr.clear()
        // В V3 используем enum SubDeviceBoard для маппинга адреса в человекочитаемое имя
        baseSubDevicesInfoStructSet.forEach { sub ->
            val board = PreferenceKeysUbi4.SubDeviceBoard.from(sub.deviceAddress)
            boardNameByAddr[sub.deviceAddress] = board.title.removeSuffix(" board")
        }
    }

    private fun refreshBoards() {
        Log.d("refreshBoardsV3", ">>> called, subsSize=${baseSubDevicesInfoStructSet.size}, fwVersions=$fwVersions")
        rebuildBoardNameCache()

        val builtBoards = baseSubDevicesInfoStructSet.map { sub ->
            val addr = sub.deviceAddress
            val name = boardNameByAddr[addr] ?: "Unknown $addr"
            val version = fwVersions.getOrDefault(addr, "—")
            
            BootloaderBoardItemUBI4(
                boardName = name,
                deviceCode = sub.deviceCode,
                deviceAddress = addr,
                canUpdate = true,
                version = version,
                isInBootLoader = false
            )
        }.distinctBy { it.deviceAddress }
        .sortedBy { it.deviceAddress } // Сортируем для стабильности списка

        bootloaderBoardsList.clear()
        bootloaderBoardsList.addAll(builtBoards)
        updateBootloaderSafe(builtBoards)
    }

    private fun handleBackPress() {
        main?.showBottomNavigation()
        parentFragmentManager.popBackStack()
    }

    private fun showFirmwareFilesDialog(boardItem: BootloaderBoardItemUBI4) {
        val fromAssets = FirmwareAssets.collectAssetZips(requireContext(), "")
            .map { (name, path) -> FirmwareFileItem(name, FirmwareAssets.copyToCache(requireContext(), path)) }
        
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

    private fun updateBootloaderSafe(list: List<BootloaderBoardItemUBI4>) =
        binding.accountRv.post { bootloaderAdapter.submitBoards(list) }

    companion object {
        var accountMainList by Delegates.notNull<ArrayList<AccountMainUBI4Item>>()
    }
}
