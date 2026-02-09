package com.bailout.stickk.ubi4.ui.main

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.MainPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.MainActivityView
import com.bailout.stickk.scan.view.ScanActivity
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.contract.TransmitterUBI4
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.data.state.BLEState.bleParser
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceAddress
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceName
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.batteryPercentFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.data.state.BLEState.bleParserV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendFlag
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendNextChunkFlagFlow
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.ui.bottom.BottomNavigationController
import com.bailout.stickk.ubi4.ui.dialog.DialogManager
import com.bailout.stickk.ubi4.ui.dialog.SyncProgressDialog
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.MotionTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.ServiceFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.account.customerServiceFragmentUBI4.AccountFragmentCustomerServiceUBI4
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.AccountFragmentMainUBI4
import com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4.AccountFragmentProsthesisInformationUBI4
import com.bailout.stickk.ubi4.ui.fragments.help.HelpFragmentUBI4
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.REQUEST_ENABLE_BT
import com.bailout.stickk.ubi4.utility.ControllerBleStatusConnection
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates


@RequirePresenter(MainPresenter::class)
class MainActivityUBI4 : BaseActivity<MainPresenter, MainActivityView>(), NavigatorUBI4,
    TransmitterUBI4, BleCommandExecutor {
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private lateinit var mBLEController: BLEController
    private var activeFragment: Fragment? = null
    var dialogManager: DialogManager? = null
    private var currentSerial: String? = null
    private var syncShownOnce = false
    private var bluetoothLeService: BluetoothLeService? = null
    private lateinit var mServiceConnection: ServiceConnection
    private val remainingTasks = AtomicInteger(0) // Счётчик оставшихся задач

    private val percentProgressLearningModel = MutableStateFlow(0)

    internal var locate = ""
    var mDeviceName: String? = null
    var mDeviceAddress: String? = null
    var mDeviceType: String? = null
    var driverVersionS: String? = null

    private val bleManager = BleManagerKmm()

    private lateinit var syncDialog: SyncProgressDialog
    private var chromeHidden = false

    private var job: Job? = null

    // Очередь для задачь работы с BLE
    val queue = BlockingQueueUbi4()
    private lateinit var bottomNavigationController: BottomNavigationController



    @SuppressLint("CommitTransaction", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncDialog = SyncProgressDialog(this, layoutInflater, this)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        val view = binding.root
        main = this
        val window = this.window
        window.statusBarColor = ContextCompat.getColor(this, R.color.ubi4_back)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.ubi4_dark_back)
        //TODO проверить
//        setContentView(view)
        initAllVariables()
        showStartupLoaderIfNeeded()
        WidgetRepoProvider.setCurrentMac(connectedDeviceAddress)


        bottomNavigationController = BottomNavigationController(bottomNavigation = binding.bottomNavigation)
        bottomNavigationController.applyVisibility(computeVisibleDisplays())
        refreshBottomNavVisibility()
        observeBattery()
        // инициализация блютуз

        //это для того что бы сразу показывать диалог лоудер и не отображать боттом навигацию
        mBLEController = BLEController().also { controller ->
            controller.setOnNeedFullInitListener {
                // этот колбэк всегда будет на main-потоке (мы так сделали в smartInitWithCrc)
                ensureSyncDialogShown()
            }
        }
        mBLEController.initBLEStructure()
        mBLEController.connectToSavedDeviceNow()
        bluetoothLeService = BluetoothLeService()
        startQueue()

        bluetoothLeService = BluetoothLeService()
        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
                System.err.println("Check ServiceConnection onServiceConnected()")
                bluetoothLeService = (service as BluetoothLeService.LocalBinder).service
                bluetoothLeService?.let { service ->
                    if (!service.initialize()) {
                        Timber.e("Unable to initialize Bluetooth")
                        finish()
                    }
                } ?: run {
                    Timber.e("BluetoothLeService is null")
                    finish()
                }
            }
            override fun onServiceDisconnected(componentName: ComponentName) {
                System.err.println("Service disconnected")
                bluetoothLeService = null
            }
        }


        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.page_2
            showSensorsScreen()
        }


        //после того как фрагмент будет удалён из back stack, activeFragment обновится
        supportFragmentManager.addOnBackStackChangedListener {
            activeFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        }
        //получение серийного номера
//        val requestData = BLECommands.requestProductInfoType()
//        Log.d("MainActivity", "Отправка команды запроса серийного номера: ${EncodeByteToHex.bytesToHexString(requestData)}")
//        main.bleCommandWithQueue(BLECommands.requestProductInfoType(0x00.toByte()), MAIN_CHANNEL_CHARACTERISTIC, WRITE){}

        dialogManager = DialogManager(this, layoutInflater, viewLifecycleOwner = this) {
            mBLEController.disconnect()
        }
        binding.nameTv.setOnClickListener {
            dialogManager?.showDisconnectDialog()
        }


        binding.helpView.setOnClickListener {
            showHelpScreen()
            binding.bottomNavigation.visibility = View.INVISIBLE
        }


        binding.accountBtn.setOnClickListener {
            sendFwInfoRequests()
            sendRunProgramTypeRequests()
            showAccountScreen()
            binding.bottomNavigation.visibility = View.INVISIBLE

        }

//        binding.runCommandBtn.setOnClickListener {
//
//        }
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
            true
        }

        val bleStatusController = ControllerBleStatusConnection(this, binding.bleIndicator)
        lifecycle.addObserver(bleStatusController)
        ControllerBleStatusConnection.UiBridges.bleStatusController = bleStatusController
    }


    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (!mBLEController.getBluetoothAdapter()?.isEnabled!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBLEController.getBluetoothLeService() != null) {
            connectedDeviceName = getString(CONNECTED_DEVICE)
            connectedDeviceAddress = getString(CONNECTED_DEVICE_ADDRESS)
            System.err.println("onResume ${getString(CONNECTED_DEVICE_ADDRESS)}")
        }
        if (!mBLEController.getStatusConnected()) {
            mBLEController.setReconnectThreadFlag(true)
            mBLEController.reconnectThread()
        }
        lifecycleScope.launch {
            val c = WidgetRepoProvider.get().count()
            platformLog("ROOM_CHECK", "Widget rows = $c")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        if (this::syncDialog.isInitialized) syncDialog.dismiss()
        mBLEController.cleanup()
    }

    override fun showGesturesScreen() { launchFragmentWithoutStack(GesturesFragment()) }
    override fun showOpticGesturesScreen() { launchFragmentWithoutStack(SprGestureFragment()) }
    override fun showSensorsScreen() { launchFragmentWithoutStack(SensorsFragment()) }
    override fun showAdvancedScreen() { launchFragmentWithoutStack(AdvancedFragment()) }
    override fun showOpticTrainingGesturesScreen() { launchFragmentWithoutStack(SprTrainingFragment()) }
    override fun showAccountScreen() {
        if (activeFragment is AccountFragmentMainUBI4)
            return
        val sourceFragment = activeFragment?.javaClass?.name ?: ""
        val accountFragment = AccountFragmentMainUBI4().apply {
            arguments = Bundle().apply {
                putString("sourceFragmentClass", sourceFragment)
            }
        }
        launchFragmentWithStack(accountFragment)
//        launchFragmentWithStack(AccountFragmentMainUBI4())
    }
    override fun showAccountCustomerServiceScreen() { launchFragmentWithStack(
        AccountFragmentCustomerServiceUBI4()
    ) }
    override fun showAccountProsthesisInformationScreen() { launchFragmentWithStack(
        AccountFragmentProsthesisInformationUBI4()
    ) }

    override fun showSecretScreen() {
        launchFragmentWithStack(ServiceFragment())
    }

    override fun showHelpScreen() {
        if (activeFragment is HelpFragmentUBI4) return

        val sourceFragment = activeFragment?.javaClass?.name.orEmpty()

        val helpFragment = HelpFragmentUBI4().apply {
            arguments = Bundle().apply {
                putString("sourceFragmentClass", sourceFragment)
            }
        }

        launchFragmentWithStack(helpFragment)
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
    fun launchFragmentWithoutStack(fragment: Fragment) {
        // Проверяем, отличается ли класс нового фрагмента от текущего активного
        if (activeFragment?.javaClass != fragment.javaClass) {
            activeFragment = fragment
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            if (!supportFragmentManager.isDestroyed) transaction.commit()
        }
    }

    private fun launchFragmentWithStack(fragment: Fragment) {
        activeFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    fun openScanActivity() {
        System.err.println("Check openScanActivity()")
        resetLastMAC()
        val intent = Intent(this@MainActivityUBI4, ScanActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun resetLastMAC() {
        saveString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "null")
    }

    fun setPercentProgressLearningModel(p: Int) {
        percentProgressLearningModel.value = p.coerceIn(0, 100)
    }

    private fun initAllVariables() {
        connectedDeviceName = intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME).orEmpty()
        connectedDeviceAddress = intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS).orEmpty()
        setStaticVariables()

        saveString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, connectedDeviceAddress)
        Log.d("initAllVariables","connectedDeviceAddress $connectedDeviceAddress" )
    }
    override fun sendWidgetsArray() { CoroutineScope(Dispatchers.IO).launch { updateFlow.emit(1) } }
    private fun setStaticVariables() {
        canSendNextChunkFlagFlow = MutableSharedFlow()
        synchronized(writeLock) {
            canSendFlag = false
            writeLock.notifyAll()
        }
        bleManager.setBleCommandExecutor(this)
        bleParser = BLEParser(lifecycleScope, bleCommandExecutor = this, bleManager = bleManager)
        bleParserV3 = BLEParserV3(lifecycleScope, bleCommandExecutor = this, bleManager = bleManager)
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

    private fun startQueue() {
        val worker = Thread {
            while (true) {
                val task: Runnable = queue.get()
                task.run()
                remainingTasks.decrementAndGet()
            }
        }
        worker.start()
    }
    override fun getQueueUBI4() : BlockingQueueUbi4 { return queue }
    override fun getRemainingTasksCount(): Int = remainingTasks.get()
    override fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit) {
        if (byteArray != null) {
            queue.put(getBleCommandWithQueue(byteArray, command, typeCommand, onChunkSent), byteArray)
            remainingTasks.incrementAndGet()
        }
    }
    private fun getBleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit): Runnable {
        return Runnable {
            writeData(byteArray, command, typeCommand)
            onChunkSent() } }
    val writeLock = Any()
    private fun writeData(byteArray: ByteArray?, command: String, typeCommand: String) {
        synchronized(writeLock) {
            canSendFlag = false
            bleCommand(byteArray, command, typeCommand)
            Log.d("TestSendByteArray","send!!!!")
            while (!canSendFlag) {
                writeLock.wait()    // ждём, пока кто-то вызовет notify()
            }
            Log.d("TestSendByteArray","CallBack is BLEService was complete")
        }
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
        mDeviceName = serial
        currentSerial = mDeviceName
        runOnUiThread { binding.nameTv.text = serial }
    }

    fun getCurrentSerial(): String? = currentSerial


    private fun sendFwInfoRequests() {
        bleParser.sendFwInfoRequestsWithRetry()
//        // CPU
//        bleCommandWithQueue(BLECommands.requestProductFWInfoType(0), MAIN_CHANNEL_CHARACTERISTIC, WRITE) {}
//        // Sub-devices (если уже известны)
//        baseSubDevicesInfoStructSet.forEach { sub ->
//            bleCommandWithQueue(
//                BLECommands.requestProductFWInfoType(sub.deviceAddress),
//                MAIN_CHANNEL_CHARACTERISTIC, WRITE
//            ) {}
//        }
    }

    private fun sendRunProgramTypeRequests() {
        baseSubDevicesInfoStructSet.forEach { sub ->
            bleCommandWithQueue(
                BLECommands.requestRunProgramType(sub.deviceAddress.toByte()),
                MAIN_CHANNEL_CHARACTERISTIC, WRITE
            ) {}
        }
    }

    fun observeBattery(){
        val layer = binding.batteryProgressBar.progressDrawable as LayerDrawable
        val rotate = layer.findDrawableByLayerId(android.R.id.progress) as RotateDrawable
        val shapeDrawable = rotate.drawable as GradientDrawable
        lifecycleScope.launch {
            batteryPercentFlow.collect{ percent ->
                binding.batteryProgressBar.progress = percent
                if (percent < 20){
                    shapeDrawable.setColor(ContextCompat.getColor(this@MainActivityUBI4, R.color.red))
                }
                else{
                    (percent >= 22)
                    shapeDrawable.setColor(ContextCompat.getColor(this@MainActivityUBI4, R.color.ubi4_active))
                }
            }

        }
    }


    fun observeSyncProgress() {
        platformLog("SyncProgressDialog","Main observeSyncProgress run ")
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

    override fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String) {
        System.err.println("BLE debug bleCommand")
        mBLEController.bleCommand( byteArray, uuid, typeCommand )
    }

    private fun computeVisibleDisplays(): Set<Int> {
        val factory = DataFactory()
        return (0..4)
            .filter { display -> factory.prepareData(display).isNotEmpty() }
            .toSet()
    }

    fun refreshBottomNavVisibility() {
        bottomNavigationController.applyVisibility(computeVisibleDisplays())
    }

    fun showBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }

    private fun showStartupLoaderIfNeeded() {
        val hasMac = connectedDeviceAddress.isNotBlank() && connectedDeviceAddress != "null"
        if (!hasMac) return

        UiState.startupInProgress.value = true
        // прячем chrome сразу, без ожидания flow-коллекторов
        setChromeVisible(false)
        // показываем диалог сразу, чтобы не было фликера
        syncDialog.show()
        // подписка на состояние (дальше он сам закроется)
        ensureSyncDialogShown()
    }

    private fun setChromeVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.INVISIBLE
        binding.statusBar.visibility = v
        binding.bottomNavigation.visibility = v
        binding.dividerV.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        var main by Delegates.notNull<MainActivityUBI4>()
    }
}
