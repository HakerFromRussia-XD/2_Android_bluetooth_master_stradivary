package com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4FragmentPersonalAccountProsthesisInformationBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.utils.EncryptionManagerUtils
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.data.network.RequestsUBI4
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.di.V3ProsthesisInformationViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation.V3ProsthesisInformation
import com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation.V3ProsthesisInformationAction
import com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation.V3ProsthesisInformationUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation.V3ProsthesisInformationViewModel
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class AccountFragmentProsthesisInformationUBI4 : Fragment() {
    private var mContext: Context? = null
    private var main: MainActivityUBI4? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: AccountProsthesisInformationAdapterUBI4? = null
    private var v3ViewModel: V3ProsthesisInformationViewModel? = null
    private var renderedV3Information: V3ProsthesisInformation? = null

    private var gson: Gson? = null
    private var encryptionManager: EncryptionManagerUtils? = null
    private var encryptionResult: String? = null
    private var testSerialNumber = "FEST-F-05670"
    private var myRequests: RequestsUBI4? = null

    private var _binding: Ubi4FragmentPersonalAccountProsthesisInformationBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentPersonalAccountProsthesisInformationBinding.inflate(inflater, container, false)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivityUBI4? }
        this.mContext = context
//        testSerialNumber = main?.mDeviceName.toString()

        val deviceName = main?.mDeviceName
        testSerialNumber = deviceName
            .takeIf { !it.isNullOrBlank() && it.startsWith("FEST-") }
            ?: testSerialNumber

        System.err.println("TEST SERIAL NUMBER $testSerialNumber")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (UiState.isInterfaceV3Activated) {
            bindV3ProsthesisInformation()
        } else {
            initializeUbi4Data()
        }
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.setRefreshing(false)
        }
        initializeUI()
        v3ViewModel?.let { viewModel ->
            renderV3ProsthesisInformation(viewModel.uiState.value)
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.uiState.collect(::renderV3ProsthesisInformation)
                }
            }
        }
    }

    private fun bindV3ProsthesisInformation() {
        v3ViewModel = ViewModelProvider(
            this, V3ProsthesisInformationViewModelFactory.from(requireContext()),
        )[V3ProsthesisInformationViewModel::class.java].also {
            it.onAction(V3ProsthesisInformationAction.ViewAttached)
        }
    }

    private fun renderV3ProsthesisInformation(state: V3ProsthesisInformationUiState) {
        val information = state.information ?: return
        if (information != renderedV3Information) {
            renderedV3Information = information
            adapter?.submitItems(listOf(information.toAccountItem()))
        }
    }

    private fun initializeUbi4Data() {
        gson = Gson()
        myRequests = RequestsUBI4()
        encryptionManager = EncryptionManagerUtils.instance
        encryptionResult = encryptionManager?.encrypt(testSerialNumber)

        accountProsthesisInformationList = ArrayList()
        accountProsthesisInformationList.clear()
        accountProsthesisInformationList.add(
            AccountProsthesisInformationItemUBI4(
                prosthesisModel = main?.loadText(PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS).toString(),
                prosthesisSize = main?.loadText(PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS).toString(),
                handSide = main?.loadText(PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS).toString(),
                rotatorType = main?.loadText(PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS).orDash(),
                touchscreenFingerPads = main?.loadText(PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS).toString(),
                batteryType = main?.loadText(PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS).toString())
        )
    }

    private fun initAdapter(accountRv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(mContext)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        accountRv.layoutManager = linearLayoutManager
        adapter = AccountProsthesisInformationAdapterUBI4(items = if (v3ViewModel != null) emptyList() else null)
        accountRv.adapter = adapter
    }
    private fun initializeUI() {
        binding.titleClickBlockBtn.setOnClickListener {  }
        initAdapter(binding.accountProsthesisInformationRv)

        binding.backBtn.setOnClickListener {
            (activity as? NavigatorUBI4)?.goingBackUbi4() ?:
            println("Activity не реализует NavigatorUBI4")
        }
    }

    override fun onDestroyView() {
        v3ViewModel?.onAction(V3ProsthesisInformationAction.ViewDetached)
        v3ViewModel = null
        renderedV3Information = null
        binding.accountProsthesisInformationRv.adapter = null
        adapter = null
        linearLayoutManager = null
        myRequests = null
        gson = null
        encryptionManager = null
        encryptionResult = null
        main = null
        mContext = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        var accountProsthesisInformationList by Delegates.notNull<ArrayList<AccountProsthesisInformationItemUBI4>>()
    }
}
