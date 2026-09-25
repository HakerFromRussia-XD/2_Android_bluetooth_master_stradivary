package com.bailout.stickk.ubi4.ui.main

import com.bailout.stickk.BuildConfig

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.ubi4.versions.v3.di.V3SyncViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.di.V3MainViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.main.V3MainAction
import com.bailout.stickk.ubi4.versions.v3.presentation.main.V3MainUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.main.V3MainViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.sync.V3SyncAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sync.V3SyncViewModel
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.MainPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.MainActivityView
import com.bailout.stickk.scan.view.ScanActivity
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.contract.TransmitterUBI4
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.data.network.SettingsProfileUploadWorkScheduler
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceAddress
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceName
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.batteryPercentFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.BLEState.bleParserV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.bottom.BottomNavigationController
import com.bailout.stickk.ubi4.ui.dialog.DialogManager
import com.bailout.stickk.ubi4.ui.dialog.SyncProgressDialog
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.BleLogFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.MotionTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.ServiceFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.account.customerServiceFragmentUBI4.AccountFragmentCustomerServiceUBI4
import com.bailout.stickk.ubi4.ui.fragments.account.games.AccountGamesFragment
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.AccountFragmentMainUBI4
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentV3.AccountFragmentMainV3
import com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4.AccountFragmentProsthesisInformationUBI4
import com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics.AccountFragmentStatisticsV3
import com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics.withAccountStatisticsCompatibility
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsFragment
import com.bailout.stickk.ubi4.ui.fragments.dashboard.DashboardSlotContentFragment
import com.bailout.stickk.ubi4.ui.fragments.dashboard.DashboardSlotsFragment
import com.bailout.stickk.ubi4.ui.fragments.help.HelpFragmentUBI4
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.REQUEST_ENABLE_BT
import com.bailout.stickk.ubi4.utility.ControllerBleStatusConnection
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import com.bailout.stickk.ubi4.ble.BleCommandQueue
import com.bailout.stickk.ubi4.di.BleDependencies
import kotlin.jvm.java


@RequirePresenter(MainPresenter::class)
class MainActivityUBI4 : BaseActivity<MainPresenter, MainActivityView>(), NavigatorUBI4,
    TransmitterUBI4, BleCommandExecutor {
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private lateinit var mBLEController: BLEController
    private var activeFragment: Fragment? = null
    var dialogManager: DialogManager? = null
    private var ubi4Serial: String? = null
    private var syncShownOnce = false
    private var syncObservation: Job? = null
    private val v3SyncViewModel by lazy {
        ViewModelProvider(this, V3SyncViewModelFactory)[V3SyncViewModel::class.java]
    }
    private val v3MainViewModel by lazy {
        ViewModelProvider(this, V3MainViewModelFactory(this, this))[V3MainViewModel::class.java]
    }
    private var renderedNavigationRevision: Long? = null
    private var renderedBatteryPercent: Int? = null
    private val transitionPauseHandler = Handler(Looper.getMainLooper())
    private var resumePlotPointsRunnable: Runnable? = null
    private var isImeVisible = false
    private var bottomNavHiddenByIme = false
    private var openingScanAfterDisconnect = false
    private var ubi4AppCloseUploadRequested = false

    private val percentProgressLearningModel = MutableStateFlow(0)

    internal var locate = ""
    private var ubi4DeviceName: String? = null
    val mDeviceName: String?
        get() = if (UiState.isInterfaceV3Activated) v3MainViewModel.uiState.value.deviceIdentity?.deviceName else ubi4DeviceName
    var mDeviceAddress: String? = null
    var mDeviceType: String? = null
    var driverVersionS: String? = null

    private val bleManager = BleManagerKmm()

    private lateinit var syncDialog: SyncProgressDialog

    private var job: Job? = null

    // Очередь для задачь работы с BLE
    private val commandQueue = BleCommandQueue()
    private val bleCommandWriter by lazy {
        BleDependencies.createCommandWriter { packet, command, type ->
            mBLEController.bleCommand(packet, command, type)
        }
    }
    private lateinit var bottomNavigationController: BottomNavigationController
    private var isV3BleEmulatorMode = false


    @SuppressLint("CommitTransaction", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = supportFragmentManager.fragmentFactory.withAccountStatisticsCompatibility()
        super.onCreate(savedInstanceState)
        Log.i(DFU_TRACE_TAG, "diagnostic_build=v2-link-params-control-20260904-1 entry_probe_only=${BuildConfig.DFU_BOOT_ENTRY_PROBE_ONLY} version=${BuildConfig.VERSION_NAME} type=${BuildConfig.BUILD_TYPE} package=$packageName")
        syncDialog = SyncProgressDialog(this, layoutInflater, this)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
//        applyDfuDiagnostics(intent)
        mSettings = this.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        val view = binding.root
        main = this
        val window = this.window
        window.statusBarColor = ContextCompat.getColor(this, R.color.ubi4_back)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.ubi4_dark_back)
        window.setBackgroundDrawableResource(R.color.ubi4_back)
        isV3BleEmulatorMode = intent.getBooleanExtra(
            V3BleEmulatorTestHooks.EXTRA_ENABLED,
            false
        ) || V3BleEmulatorTestHooks.isEnabled()
        if (isV3BleEmulatorMode) {
            V3BleEmulatorTestHooks.enable()
            UiState.isInterfaceV3Activated = true
            UiState.startupInProgress.value = false
            UiState.v3WidgetsInteractionEnabled.value = true
        }
        //TODO проверить
//        setContentView(view)
        initializeDeviceSession()
        if (UiState.isInterfaceV3Activated) v3SyncViewModel.onAction(V3SyncAction.ViewCreated)
        if (!isV3BleEmulatorMode) {
            showStartupLoaderIfNeeded()
        }
        if (UiState.isInterfaceV3Activated) v3MainViewModel.onAction(V3MainAction.WidgetStorageInitializationRequested)
        else WidgetRepoProvider.setCurrentMac(connectedDeviceAddress)


        bottomNavigationController = BottomNavigationController(bottomNavigation = binding.bottomNavigation)
        setupImeBottomNavBehavior()
        if (UiState.isInterfaceV3Activated) {
            bindV3MainState()
        } else {
            refreshBottomNavVisibility()
            lifecycleScope.launch {
                updateFlow.collect { refreshBottomNavVisibility() }
            }
            observeBattery()
        }
        // инициализация блютуз

        //это для того что бы сразу показывать диалог лоудер и не отображать боттом навигацию
        mBLEController = BLEController(bleManager).also { controller ->
            controller.setOnNeedFullInitListener {
                // этот колбэк всегда будет на main-потоке (мы так сделали в smartInitWithCrc)
                ensureSyncDialogShown()
            }
        }
        if (UiState.isInterfaceV3Activated) BleDependencies.bindSensorsRefresh(this, mBLEController, ::observeSyncProgress)
        BleDependencies.bindTelemetry(this, lifecycleScope, mSettings!!, mBLEController, ::showToast)
        if (!isV3BleEmulatorMode) {
            mBLEController.initBLEStructure()
            mBLEController.connectToSavedDeviceNow()
        } else {
            lifecycleScope.launch {
                bleParserV3.generatedHardcodeWidgets()
                UiState.v3WidgetsInteractionEnabled.value = true
                V3BleEmulatorTestHooks.injectInitialResponses(bleParserV3)
            }
        }
        commandQueue.start()


        if (savedInstanceState == null) {
            if (isV3BleEmulatorMode &&
                intent.getBooleanExtra(V3BleEmulatorTestHooks.EXTRA_OPEN_GESTURES, false)
            ) {
                binding.bottomNavigation.selectedItemId = R.id.page_1
                showOpticGesturesScreen()
            } else {
                binding.bottomNavigation.selectedItemId = R.id.page_2
                showSensorsScreen()
            }
        }


        //после того как фрагмент будет удалён из back stack, activeFragment обновится
        supportFragmentManager.addOnBackStackChangedListener {
            activeFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        }

        dialogManager = DialogManager(this, layoutInflater, viewLifecycleOwner = this) {
            mBLEController.disconnect()
        }
//        maybeStartDebugFirmwareUpdate()
        binding.nameTv.setOnClickListener {
            dialogManager?.showDisconnectDialog()
        }


        binding.helpView.setOnClickListener {
            showHelpScreen()
        }


        binding.accountBtn.setOnClickListener {
            showAccountScreen()

        }
        binding.statusBackBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val accountPb = binding.accountPb.apply {
            max = 100
            visibility = View.GONE
        }
        binding.accountBtn.setOnLongClickListener {
            // запустить анимацию заполнения
            accountPb.visibility = View.VISIBLE
            ObjectAnimator.ofInt(accountPb, "progress", 0, 100).apply {
                duration = 800L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        accountPb.visibility = View.GONE
                    }
                })
            }.start()
            // показать или скрыть секретный пункт
            bottomNavigationController.toggleSecretItem()
            refreshBottomNavVisibility()
            true
        }



        val bleStatusController = ControllerBleStatusConnection(
            context = this,
            indicator = binding.bleIndicator,
            isBleConnected = { mBLEController.getStatusConnected() }
        )
        lifecycle.addObserver(bleStatusController)
    }

//    private fun maybeStartDebugFirmwareUpdate() {
//        if (!BuildConfig.DEBUG || !intent.getBooleanExtra("DFU_TEST_AUTO_UPDATE", false)) return
//        UiState.startupInProgress.value = false
//        UiState.fullInitInProgress.value = false
//        syncDialog.dismiss()
//        lifecycleScope.launch {
//            repeat(2_400) {
//                if (mBLEController.getStatusConnected()) {
//                    delay(750)
//                    if (mBLEController.getStatusConnected()) {
//                        if (!mBLEController.prepareFirmwareSessionNotifications()) {
//                            Log.w("DFU_V2_TRACE", "debug_autorun serial notify unavailable; waiting for reconnect")
//                            delay(500)
//                            return@repeat
//                        }
//                        val firmwareFile = File(cacheDir, "FH_FAM_v0.1.29_.zip")
//                        assets.open("Firmware/FH_FAM_v0.1.29_.zip").use { input ->
//                            firmwareFile.outputStream().use(input::copyTo)
//                        }
//                        Log.i("DFU_V2_TRACE", "debug_autorun connected; dispatching firmware update")
//                        dialogManager?.runV3FirmwareUpdateForDebug(firmwareFile)
//                        return@launch
//                    }
//                }
//                delay(250)
//            }
//            Log.e("DFU_V2_TRACE", "debug_autorun connection timeout")
//        }
//    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        BleDependencies.updateLaunchIntent(this, intent)
//        applyDfuDiagnostics(intent)
    }

//    private fun applyDfuDiagnostics(intent: Intent) {
//        DfuDiagnostics.forceLegacy = BuildConfig.DFU_DIAGNOSTIC_FORCE_LEGACY
//        // Production keeps the proven v1-compatible boot-entry behavior.
//        // Requiring a main-app start remains an explicit diagnostics option.
//        DfuDiagnostics.requireMainStart = false
//        if (intent.hasExtra(EXTRA_DFU_FORCE_LEGACY)) {
//            DfuDiagnostics.forceLegacy = intent.getBooleanExtra(EXTRA_DFU_FORCE_LEGACY, false)
//            Log.i("DFU_METRIC", "diagnostic_force_legacy=${DfuDiagnostics.forceLegacy}")
//        }
//        if (intent.hasExtra(EXTRA_DFU_REQUIRE_MAIN_START)) {
//            DfuDiagnostics.requireMainStart =
//                intent.getBooleanExtra(EXTRA_DFU_REQUIRE_MAIN_START, false)
//            Log.i("DFU_METRIC", "diagnostic_require_main_start=${DfuDiagnostics.requireMainStart}")
//        }
//        Log.i(
//            DFU_TRACE_TAG,
//            "diagnostics forceLegacy=${DfuDiagnostics.forceLegacy} " +
//                "requireMainStart=${DfuDiagnostics.requireMainStart}"
//        )
//    }


    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (UiState.isInterfaceV3Activated) v3MainViewModel.onAction(V3MainAction.ViewResumed)
        else {
            ubi4AppCloseUploadRequested = false
            SettingsProfileUploadWorkScheduler.cancelAppCloseUpload(this)
        }
        if (isV3BleEmulatorMode) {
            UiState.v3WidgetsInteractionEnabled.value = true
            return
        }
        if (!mBLEController.getBluetoothAdapter()?.isEnabled!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBLEController.getBluetoothLeService() != null) {
            if (UiState.isInterfaceV3Activated) v3MainViewModel.onAction(V3MainAction.SavedConnectionRestoreRequested)
            else {
                connectedDeviceName = getString(CONNECTED_DEVICE)
                connectedDeviceAddress = getString(CONNECTED_DEVICE_ADDRESS)
            }
            System.err.println("onResume $connectedDeviceAddress")
        }
        if (!mBLEController.getStatusConnected()) {
            mBLEController.setReconnectThreadFlag(true)
            mBLEController.reconnectThread()
        }
        if (!UiState.isInterfaceV3Activated) lifecycleScope.launch {
            val c = WidgetRepoProvider.get().count()
            platformLog("ROOM_CHECK", "Widget rows = $c")
        }
    }

    override fun onStop() {
        enqueueAppCloseUploadIfNeeded()
        super.onStop()
    }

    override fun onDestroy() {
        if (UiState.isInterfaceV3Activated) v3MainViewModel.onAction(V3MainAction.ViewDestroyed)
        enqueueAppCloseUploadIfNeeded()
        commandQueue.stop()
        job?.cancel()
        resumePlotPointsRunnable?.let(transitionPauseHandler::removeCallbacks)
        resumePlotPointsRunnable = null
        WidgetState.pausePlotPointsDuringTransition = false
        dialogManager?.onDestroy()
        dialogManager = null
        if (this::syncDialog.isInitialized) syncDialog.dismiss()
        mBLEController.cleanup()
        BleDependencies.unbindCommandExecutor(this)
        clearMainIfSame(this)
        super.onDestroy()
    }

    private fun enqueueAppCloseUploadIfNeeded() {
        if (UiState.isInterfaceV3Activated) {
            v3MainViewModel.onAction(V3MainAction.ViewStopped(openingScanAfterDisconnect,
                this::mBLEController.isInitialized && mBLEController.getStatusConnected(), locate))
            return
        }
        if (openingScanAfterDisconnect || ubi4AppCloseUploadRequested) return
        if (!this::mBLEController.isInitialized || !mBLEController.getStatusConnected()) {
            platformLog("SettingsProfileUploadWork", "skip app close enqueue: device is not connected")
            return
        }

        ubi4AppCloseUploadRequested = true
        SettingsProfileUploadWorkScheduler.enqueueAppCloseUpload(
            context = this,
            lang = locate.takeIf { it.isNotBlank() } ?: "en"
        )
    }

    override fun showGesturesScreen() { launchFragmentWithoutStack(GesturesFragment()) }
    override fun showOpticGesturesScreen() { launchFragmentWithoutStack(SprGestureFragment()) }
    override fun showSensorsScreen() { launchFragmentWithoutStack(SensorsFragment()) }
    override fun showAdvancedScreen() { launchFragmentWithoutStack(AdvancedFragment()) }
    override fun showOpticTrainingGesturesScreen() { launchFragmentWithoutStack(SprTrainingFragment()) }
    override fun showBleLogScreen() { launchFragmentWithoutStack(BleLogFragment()) }
    override fun showAccountScreen() {
        if (activeFragment is AccountFragmentMainUBI4 || activeFragment is AccountFragmentMainV3)
            return
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()
            
        val sourceFragment = activeFragment?.javaClass?.name ?: ""
        if (sourceFragment == SensorsFragment::class.java.name) {
            pausePlotPointsForTransition()
        }
        
        val fragment: Fragment = if (UiState.isInterfaceV3Activated) {
            AccountFragmentMainV3()
        } else {
            AccountFragmentMainUBI4()
        }.apply {
            arguments = Bundle().apply {
                putString("sourceFragmentClass", sourceFragment)
            }
        }
        
        launchFragmentWithStack(fragment, withSlideAnimation = true)
    }
    override fun showAccountCustomerServiceScreen() {
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        val preserveCurrentFragmentView =
            activeFragment is AccountFragmentMainUBI4 || activeFragment is AccountFragmentMainV3
        launchFragmentWithStack(
            fragment = AccountFragmentCustomerServiceUBI4(),
            withSlideAnimation = true,
            preserveCurrentFragmentView = preserveCurrentFragmentView
        )
    }

    override fun showAccountProsthesisInformationScreen() {
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        val preserveCurrentFragmentView =
            activeFragment is AccountFragmentMainUBI4 || activeFragment is AccountFragmentMainV3
        launchFragmentWithStack(
            fragment = AccountFragmentProsthesisInformationUBI4(),
            withSlideAnimation = true,
            preserveCurrentFragmentView = preserveCurrentFragmentView
        )
    }

    override fun showAccountStatisticsScreen() {
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        launchFragmentWithStack(
            fragment = AccountFragmentStatisticsV3(),
            withSlideAnimation = true,
            preserveCurrentFragmentView = activeFragment is AccountFragmentMainV3
        )
    }

    override fun showAchievementsScreen() {
        hideTopStatusBar()
        hideBottomNavigationAnimated()

        launchFragmentWithStack(
            fragment = AchievementsFragment(),
            withSlideAnimation = true,
            preserveCurrentFragmentView = activeFragment is AccountFragmentMainV3
        )
    }

    override fun showDashboardSlotsScreen(deviceAddress: Int) {
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        val preserveCurrentFragmentView =
            activeFragment is AccountFragmentMainUBI4 || activeFragment is AccountFragmentMainV3
        launchFragmentWithStack(
            fragment = DashboardSlotsFragment.newInstance(deviceAddress),
            withSlideAnimation = true,
            preserveCurrentFragmentView = preserveCurrentFragmentView
        )
    }

    override fun showDashboardSlotContentScreen(
        deviceAddress: Int,
        dataCode: Int,
        title: String,
        version: Int,
        subVersion: Int,
        declaredSize: Int
    ) {
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        launchFragmentWithStack(
            fragment = DashboardSlotContentFragment.newInstance(
                deviceAddress = deviceAddress,
                dataCode = dataCode,
                title = title,
                version = version,
                subVersion = subVersion,
                declaredSize = declaredSize
            ),
            withSlideAnimation = true,
            preserveCurrentFragmentView = true
        )
    }

    override fun showGamesScreen() {
        if (activeFragment is AccountGamesFragment) return
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        val preserveCurrentFragmentView =
            activeFragment is AccountFragmentMainUBI4 || activeFragment is AccountFragmentMainV3
        launchFragmentWithStack(
            fragment = AccountGamesFragment(),
            withSlideAnimation = true,
            preserveCurrentFragmentView = preserveCurrentFragmentView
        )
    }

    override fun showSecretScreen() {
        launchFragmentWithStack(ServiceFragment())
    }

    override fun showHelpScreen() {
        if (activeFragment is HelpFragmentUBI4) return
        showTopStatusBar()
        setStatusBarBackMode(enabled = true)
        hideBottomNavigationAnimated()

        val sourceFragment = activeFragment?.javaClass?.name.orEmpty()
        if (sourceFragment == SensorsFragment::class.java.name) {
            pausePlotPointsForTransition()
        }

        val helpFragment = HelpFragmentUBI4().apply {
            arguments = Bundle().apply {
                putString("sourceFragmentClass", sourceFragment)
            }
        }

        launchFragmentWithStack(helpFragment, withSlideAnimation = true)
    }

    override fun showMotionTrainingScreen(onFinishTraining: () -> Unit) {
        val fragment = MotionTrainingFragment(onFinishTraining)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        activeFragment = fragment
        Log.d("StateCallBack", "showMotionTrainingScreen called, new MotionTrainingFragment created")
    }
    override fun showSpecialScreen() { launchFragmentWithoutStack(SpecialSettingsFragment()) }
    override fun showToast(massage: String) {
        Toast.makeText(this,massage,Toast.LENGTH_SHORT).show()
    }
    override fun getBackStackEntryCount(): Int { return supportFragmentManager.backStackEntryCount }
    override fun goingBackUbi4() { onBackPressed()}
    override fun goToMenu() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun pausePlotPointsForTransition(durationMs: Long = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()) {
        WidgetState.pausePlotPointsDuringTransition = true
        resumePlotPointsRunnable?.let(transitionPauseHandler::removeCallbacks)

        val resumeRunnable = Runnable {
            WidgetState.pausePlotPointsDuringTransition = false
        }
        resumePlotPointsRunnable = resumeRunnable
        transitionPauseHandler.postDelayed(resumeRunnable, durationMs + 80L)
    }

    private fun launchFragmentWithoutStack(fragment: Fragment) {
        // Проверяем, отличается ли класс нового фрагмента от текущего активного
        if (activeFragment?.javaClass != fragment.javaClass) {
            showTopStatusBar()
            setStatusBarBackMode(enabled = false)
            activeFragment = fragment
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            if (!supportFragmentManager.isDestroyed) transaction.commit()
        }
    }

    private fun launchFragmentWithStack(
        fragment: Fragment,
        withSlideAnimation: Boolean = false,
        preserveCurrentFragmentView: Boolean = false
    ) {
        activeFragment = fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        if (withSlideAnimation) {
            transaction.setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out_next,
                R.anim.slide_in_next,
                R.anim.slide_out
            )
        }

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (preserveCurrentFragmentView &&
            currentFragment != null &&
            currentFragment !== fragment &&
            currentFragment.isAdded
        ) {
            transaction
                .hide(currentFragment)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            return
        }

        transaction
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    fun openScanActivity() {
        System.err.println("Check openScanActivity()")
        openingScanAfterDisconnect = true
        resetLastMAC()
        val intent = Intent(this@MainActivityUBI4, ScanActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun resetLastMAC() {
        if (UiState.isInterfaceV3Activated) v3MainViewModel.onAction(V3MainAction.LastConnectionResetRequested)
        else saveString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "null")
    }

    fun setPercentProgressLearningModel(p: Int) {
        percentProgressLearningModel.value = p.coerceIn(0, 100)
    }

    private fun initializeDeviceSession() {
        BleDependencies.initializeSession(intent, lifecycleScope, bleManager, this, bleCommandWriter, this, applicationContext)
        if (UiState.isInterfaceV3Activated) v3MainViewModel.onAction(V3MainAction.DeviceConnected)
        else saveString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, connectedDeviceAddress)
        Log.d("initAllVariables","connectedDeviceAddress $connectedDeviceAddress" )
    }
    override fun sendWidgetsArray() {
        lifecycleScope.launch(Dispatchers.IO) {
            updateFlow.emit(1)
        }
    }

    // сохранение и загрузка данных
    override fun saveString(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    override fun getString(key: String) :String {
        return mSettings!!.getString(key, "NOT SET!").toString()
    }
    fun loadText(key: String): String { return mSettings!!.getString(key, "null").toString() }
    internal fun saveInt(key: String, variable: Int) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putInt(key, variable)
        editor.apply()
    }
    internal fun getInt(key: String, default: Int): Int {
        return mSettings?.getInt(key, default) ?: default
    }
    internal fun saveBoolean(key: String, variable: Boolean) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putBoolean(key, variable)
        editor.apply()
    }
    internal fun getBoolean(key: String, default: Boolean): Boolean {
        return mSettings?.getBoolean(key, default) ?: default
    }

    override fun getQueueUBI4(): BlockingQueueUbi4 = commandQueue.queue
    override fun getRemainingTasksCount(): Int = commandQueue.pendingCount
    override fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit) {
        if (byteArray != null) {
            if (V3BleEmulatorTestHooks.tryHandleOutgoing(byteArray, bleParserV3, onChunkSent)) {
                return
            }
            commandQueue.enqueue(getBleCommandWithQueue(byteArray, command, typeCommand, onChunkSent), byteArray)
        }
    }

    override suspend fun firmwareJumpToBootloader(packet: ByteArray) {
        val startedAt = System.currentTimeMillis()
        val resetAlreadyObserved = mBLEController.prepareFirmwareBootloaderJump()
        if (resetAlreadyObserved) {
            Log.i(
                DFU_TRACE_TAG,
                "legacy jump skipped because pending boot transition reset during flash quiet window"
            )
            mBLEController.firmwareReconnectAfterBootloaderJump()
            return
        }
        Log.i(
            DFU_TRACE_TAG,
            "legacy jump enqueue bytes=${packet.size} remaining_before=${commandQueue.pendingCount}"
        )
        val sent = CompletableDeferred<Unit>()
        bleCommandWithQueue(packet, SERIALPORTCHAR_UUID, WRITE) {
            sent.complete(Unit)
        }
        val completed = withTimeoutOrNull(DFU_CONTROL_WRITE_TIMEOUT_MS) {
            sent.await()
        } != null
        Log.i(
            DFU_TRACE_TAG,
            "legacy jump write_complete=$completed remaining=${commandQueue.pendingCount} " +
                "elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        check(completed) { "Legacy JUMP_TO_BOOTLOADER write timeout" }
        mBLEController.firmwareReconnectAfterBootloaderJump()
    }

    override suspend fun dfuMaximumWriteWithoutResponseSize(): Int =
        mBLEController.dfuMaximumWriteWithoutResponseSize()

    override suspend fun dfuSupportsWriteWithoutResponse(): Boolean =
        mBLEController.dfuSupportsWriteWithoutResponse()

    override suspend fun dfuSetHighPerformanceMode() {
        val startedAt = System.currentTimeMillis()
        Log.i(DFU_TRACE_TAG, "queue drain_before_high_performance start remaining=${commandQueue.pendingCount}")
        while (commandQueue.pendingCount > 0) delay(5L)
        Log.i(
            DFU_TRACE_TAG,
            "queue drain_before_high_performance complete elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        mBLEController.dfuSetHighPerformanceMode()
    }

    override suspend fun dfuWriteControl(packet: ByteArray) {
        val startedAt = System.currentTimeMillis()
        Log.d(
            DFU_TRACE_TAG,
            "direct control start bytes=${packet.size} remaining_before=${commandQueue.pendingCount}"
        )
        while (commandQueue.pendingCount > 0) delay(5L)
        val completed = mBLEController.dfuWriteControlAndAwait(
            packet = packet,
            timeoutMs = DFU_CONTROL_WRITE_TIMEOUT_MS
        )
        Log.d(
            DFU_TRACE_TAG,
            "direct control result completed=$completed remaining=${commandQueue.pendingCount} " +
                "elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        check(completed) {
            "DFU control write was not completed in ${DFU_CONTROL_WRITE_TIMEOUT_MS}ms"
        }
    }

    override suspend fun dfuWriteControlExpectDisconnect(packet: ByteArray) {
        /* JUMP_TO_BOOTLOADER is acknowledged by the ensuing disconnect.  FAM
         * resets quickly enough that Android often never receives
         * onCharacteristicWrite, so this one command must not enter the
         * callback-gated stop-and-wait queue. */
        val startedAt = System.currentTimeMillis()
        Log.i(DFU_TRACE_TAG, "queue reset_write drain start remaining=${commandQueue.pendingCount}")
        while (commandQueue.pendingCount > 0) delay(5L)
        val accepted = mBLEController.dfuWriteControlExpectDisconnect(packet)
        Log.i(
            DFU_TRACE_TAG,
            "queue reset_write accepted=$accepted elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        check(accepted) {
            "DFU reset control write was not accepted by Android GATT"
        }
    }

    override suspend fun dfuWriteWithoutResponse(packet: ByteArray): Boolean =
        mBLEController.dfuWriteWithoutResponse(packet)

    override suspend fun dfuAwaitWritable() {
        // Android exposes immediate GATT busy as a rejected write. The common
        // uploader retries it with a short delay and device credit window.
    }

    override suspend fun dfuReconnect() {
        mBLEController.dfuReconnect()
    }

    override suspend fun dfuAwaitReconnect() {
        mBLEController.dfuAwaitReconnect()
    }
    private fun getBleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit): Runnable {
        return Runnable {
            if (UiState.isInterfaceV3Activated) {
                bleCommandWriter.writeAsync(lifecycleScope, byteArray, command, typeCommand, onSent = onChunkSent)
            } else {
                bleCommandWriter.write(byteArray, command, typeCommand)
                onChunkSent()
            }
        }
    }
    internal fun onBleWriteCompleted() {
        bleCommandWriter.onWriteCompleted()
    }

    //не нарушая инкапсуляцию
    fun getBLEController(): BLEController {
        return mBLEController
    }

    fun getBottomNavigationController(): BottomNavigationController {
        return bottomNavigationController
    }

    override fun updateSerialNumber(info: DeviceInfoStructs) {
        val isCpu = info.deviceType == 1 || info.deviceCode == 1 || info.deviceAddress == 0
        val uuidOk = info.deviceUUID != 0
        if (!isCpu || !uuidOk) return          // игнорируем саб-модули
        val serial = "${info.deviceUUIDPrefix}${'-'}${'0'}${info.formattedDeviceUUID}"
        if (UiState.isInterfaceV3Activated) {
            v3MainViewModel.onAction(V3MainAction.SerialNumberReceived(serial))
            return
        }
        ubi4DeviceName = serial
        ubi4Serial = ubi4DeviceName
        SettingsProfileManager.setCurrentSerial(ubi4Serial)
        val displayName = DeviceNameBridgeV3.displayName(serial)
        runOnUiThread { binding.nameTv.text = displayName }
    }

    fun getCurrentSerial(): String? =
        if (UiState.isInterfaceV3Activated) v3MainViewModel.uiState.value.deviceIdentity?.serial else ubi4Serial


    private fun observeBattery(){
        lifecycleScope.launch {
            batteryPercentFlow.collect{ percent ->
                renderBattery(percent)
            }
        }
    }

    private fun renderBattery(percent: Int) {
        val layer = binding.batteryProgressBar.progressDrawable as LayerDrawable
        val rotate = layer.findDrawableByLayerId(android.R.id.progress) as RotateDrawable
        val shapeDrawable = rotate.drawable as GradientDrawable
        binding.batteryProgressBar.progress = percent
        val colorRes = when {
            percent < 20 -> R.color.ubi4_no_system_red
            percent <= 40 -> R.color.ubi4_no_system_yellow
            else -> R.color.ubi4_active
        }
        shapeDrawable.setColor(ContextCompat.getColor(this, colorRes))
    }

    private fun bindV3MainState() {
        v3MainViewModel.onAction(V3MainAction.ViewCreated)
        renderV3MainState(v3MainViewModel.uiState.value)
        lifecycleScope.launch {
            v3MainViewModel.uiState.collect(::renderV3MainState)
        }
    }

    private fun renderV3MainState(state: V3MainUiState) {
        state.deviceIdentity?.let { identity ->
            if (binding.nameTv.text.toString() != identity.displayName) binding.nameTv.text = identity.displayName
        }
        if (renderedNavigationRevision != state.navigationRevision) {
            renderedNavigationRevision = state.navigationRevision
            bottomNavigationController.applyVisibility(state.visibleDisplays)
            syncBottomNavigationContainerVisibility()
        }
        state.batteryPercent?.let { percent ->
            if (renderedBatteryPercent != percent) {
                renderedBatteryPercent = percent
                renderBattery(percent)
            }
        }
    }

    fun observeSyncProgress() {
        platformLog("SyncProgressDialog","Main observeSyncProgress run ")
        if (UiState.isInterfaceV3Activated) {
            val viewModel = v3SyncViewModel
            if (syncObservation == null) {
                syncObservation = lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.onAction(V3SyncAction.ViewAttached)
                        try {
                            viewModel.uiState.collect { syncDialog.renderV3(it, ::setChromeVisible) }
                        } finally {
                            viewModel.onAction(V3SyncAction.ViewDetached)
                        }
                    }
                }
            } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                viewModel.onAction(V3SyncAction.ViewAttached)
            }
            return
        }
        syncDialog.observeSyncProgress { visible ->
            setChromeVisible(visible)
        }
    }

    fun ensureSyncDialogShown() {
        // уже показывали — больше не трогаем
        if (syncShownOnce) return

        // если активити уже закрывается / закрыта — просто выходим
        if (isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)) {
            Log.w("SyncProgressDialog", "ensureSyncDialogShown: activity is finishing/destroyed, skip")
            return
        }

        syncShownOnce = true
        observeSyncProgress()
    }

    private fun computeVisibleDisplays(): Set<Int> {
        val factory = DataFactory()
        return (0..4)
            .filter { display -> factory.prepareData(display).isNotEmpty() }
            .toSet()
    }

    fun refreshBottomNavVisibility() {
        if (UiState.isInterfaceV3Activated) {
            v3MainViewModel.onAction(V3MainAction.NavigationRefreshRequested)
            // Existing callers expect navigation to be refreshed before returning.
            renderV3MainState(v3MainViewModel.uiState.value)
            return
        }
        bottomNavigationController.applyVisibility(computeVisibleDisplays())
        syncBottomNavigationContainerVisibility()
    }

    private fun syncBottomNavigationContainerVisibility() {
        if (!bottomNavigationController.hasVisibleItems()) {
            binding.bottomNavigation.visibility = View.GONE
            return
        }
        if (canRestoreBottomNavigationAfterIme() && !isImeVisible) {
            showBottomNavigationAnimated()
        }
    }

    fun showBottomNavigation() {
        if (!bottomNavigationController.hasVisibleItems()) {
            binding.bottomNavigation.visibility = View.GONE
            return
        }
        if (isImeVisible) {
            bottomNavHiddenByIme = true
            binding.bottomNavigation.visibility = View.GONE
            return
        }
        showBottomNavigationAnimated()
    }

    fun showTopStatusBar() {
        binding.statusBar.visibility = View.VISIBLE
        binding.dividerV.visibility = View.VISIBLE
    }

    fun hideTopStatusBar() {
        binding.statusBar.visibility = View.GONE
        binding.dividerV.visibility = View.GONE
    }

    fun setStatusBarBackMode(enabled: Boolean) {
        binding.statusBackContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.accountContainer.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.helpView.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun showStartupLoaderIfNeeded() {
        val hasMac = connectedDeviceAddress.isNotBlank() && connectedDeviceAddress != "null"
        if (!hasMac) return

        UiState.startupInProgress.value = true
        // прячем chrome сразу, без ожидания flow-коллекторов
        setChromeVisible(false)
        // показываем диалог сразу, чтобы не было фликера
        syncDialog.show()
        if (UiState.isInterfaceV3Activated) v3SyncViewModel.onAction(V3SyncAction.StartupShown)
        // подписка на состояние (дальше он сам закроется)
        ensureSyncDialogShown()
    }

    private fun setChromeVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.INVISIBLE
        binding.statusBar.visibility = v
        binding.bottomNavigation.visibility = if (
            visible && !isImeVisible && bottomNavigationController.hasVisibleItems()
        ) View.VISIBLE else View.INVISIBLE
        binding.dividerV.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    private fun setupImeBottomNavBehavior() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.myMainLl) { _, insets ->
            val imeVisibleNow = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (imeVisibleNow != isImeVisible) {
                isImeVisible = imeVisibleNow
                if (imeVisibleNow) {
                    hideBottomNavigationForIme()
                } else {
                    restoreBottomNavigationAfterIme()
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.myMainLl)
    }

    private fun hideBottomNavigationForIme() {
        val nav = binding.bottomNavigation
        if (nav.visibility != View.VISIBLE) return

        bottomNavHiddenByIme = true
        hideBottomNavigationAnimated()
    }

    private fun hideBottomNavigationAnimated() {
        val nav = binding.bottomNavigation
        if (nav.visibility != View.VISIBLE) {
            nav.visibility = View.GONE
            nav.translationY = 0f
            nav.alpha = 1f
            return
        }

        nav.animate().cancel()
        val translateDistance = nav.height.takeIf { it > 0 }?.toFloat()
            ?: (56f * resources.displayMetrics.density)
        nav.animate()
            .translationY(translateDistance)
            .alpha(0f)
            .setDuration(180L)
            .withEndAction {
                nav.visibility = View.GONE
                nav.translationY = 0f
                nav.alpha = 1f
            }
            .start()
    }

    private fun restoreBottomNavigationAfterIme() {
        if (!bottomNavHiddenByIme) return
        bottomNavHiddenByIme = false
        if (!canRestoreBottomNavigationAfterIme()) return

        val nav = binding.bottomNavigation
        showBottomNavigationAnimated(nav)
    }

    private fun canRestoreBottomNavigationAfterIme(): Boolean {
        return binding.statusBar.visibility == View.VISIBLE &&
            binding.statusBackContainer.visibility != View.VISIBLE &&
            !UiState.startupInProgress.value &&
            bottomNavigationController.hasVisibleItems()
    }

    private fun showBottomNavigationAnimated(nav: View = binding.bottomNavigation) {
        if (isImeVisible) return
        if (nav.visibility == View.VISIBLE && nav.alpha == 1f && nav.translationY == 0f) return

        nav.animate().cancel()
        val translateDistance = nav.height.takeIf { it > 0 }?.toFloat()
            ?: (56f * resources.displayMetrics.density)
        nav.translationY = translateDistance
        nav.alpha = 0f
        nav.visibility = View.VISIBLE
        nav.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220L)
            .start()
    }

    companion object {
        private const val DFU_TRACE_TAG = "DFU_V2_DIAG"
        private const val DFU_CONTROL_WRITE_TIMEOUT_MS = 3_000L
        const val EXTRA_DFU_FORCE_LEGACY = "com.bailout.stickk.extra.DFU_FORCE_LEGACY"
        const val EXTRA_DFU_REQUIRE_MAIN_START =
            "com.bailout.stickk.extra.DFU_REQUIRE_MAIN_START"
        private var mainRef: WeakReference<MainActivityUBI4>? = null

        val mainOrNull: MainActivityUBI4?
            get() = mainRef?.get()

        var main: MainActivityUBI4
            get() = mainRef?.get()
                ?: error("MainActivityUBI4 reference is not available")
            set(value) {
                mainRef = WeakReference(value)
            }

        private fun clearMainIfSame(owner: MainActivityUBI4) {
            val current = mainRef?.get()
            if (current === owner) {
                mainRef?.clear()
                mainRef = null
            }
        }
    }
}
