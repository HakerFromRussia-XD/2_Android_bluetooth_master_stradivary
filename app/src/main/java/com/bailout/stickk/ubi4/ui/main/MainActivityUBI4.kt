package com.bailout.stickk.ubi4.ui.main

import SprGestureFragment
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.MainPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.MainActivityView
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.contract.TransmitterUBI4
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.OpticTrainingStruct
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.ui.bottom.BottomNavigationController
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.MotionTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment
import com.bailout.stickk.ubi4.ui.fragments.account.AccountFragmentMainUBI4
import com.bailout.stickk.ubi4.ui.fragments.customerServiceFragmentUBI4.AccountFragmentCustomerServiceUBI4
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.REQUEST_ENABLE_BT
import com.bailout.stickk.ubi4.utility.TrainingModelHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@RequirePresenter(MainPresenter::class)
class MainActivityUBI4 : BaseActivity<MainPresenter, MainActivityView>(), NavigatorUBI4,
    TransmitterUBI4 {
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private lateinit var mBLEController: BLEController
    private lateinit var trainingModelHandler: TrainingModelHandler
    private var activeFragment: Fragment? = null
    private lateinit var activeGestureNameCl: ConstraintLayout

    internal var locate = ""
    var mDeviceName: String? = null
    var mDeviceAddress: String? = null
    var mDeviceType: String? = null
    var driverVersionS: String? = null
    var driverVersionINDY: Int? = null

    var lastButtonPressTime: Long = 0L

    // Очередь для задачь работы с BLE
    val queue = BlockingQueueUbi4()
    private lateinit var bottomNavigationController: BottomNavigationController


    @SuppressLint("CommitTransaction", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        val view = binding.root
        main = this
        val window = this.window
        window.statusBarColor = ContextCompat.getColor(this, R.color.ubi4_back)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.ubi4_dark_back)
        setContentView(view)
        initAllVariables()
        bottomNavigationController =
            BottomNavigationController(bottomNavigation = binding.bottomNavigation)

        trainingModelHandler = TrainingModelHandler(this)
        trainingModelHandler.initialize()

        // инициализация блютуз
        mBLEController = BLEController()
        mBLEController.initBLEStructure()
        mBLEController.scanLeDevice(true)
        startQueue()

        showSensorsScreen()
        if (savedInstanceState == null) {
//            showOpticGesturesScreen()
        }
        //после того как фрагмент будет удалён из back stack, activeFragment обновится
        supportFragmentManager.addOnBackStackChangedListener {
            activeFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        }
        //получение серийного номера
        main.bleCommandWithQueue(BLECommands.requestProductInfoType(), MAIN_CHANNEL, WRITE){}

        binding.accountBtn.setOnClickListener {
            showAccountScreen()
        }

        binding.runCommandBtn.setOnClickListener {
            Log.d("RunCommand", "Кнопка нажата!")
            main.bleCommandWithQueue(BLECommands.requestProductInfoType(), MAIN_CHANNEL, WRITE){}
        }
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
        bleCommandWithQueue(
            BLECommands.requestProductInfoType(),
            MAIN_CHANNEL,
            WRITE
        ) {}
    }


    override fun showGesturesScreen() { launchFragmentWithoutStack(GesturesFragment()) }
    override fun showOpticGesturesScreen() { launchFragmentWithoutStack(SprGestureFragment()) }
    override fun showSensorsScreen() { launchFragmentWithoutStack(SensorsFragment()) }
    override fun showAdvancedScreen() { launchFragmentWithoutStack(AdvancedFragment()) }
    override fun showOpticTrainingGesturesScreen() { launchFragmentWithoutStack(SprTrainingFragment()) }
    override fun showAccountScreen() {
        if (activeFragment is AccountFragmentMainUBI4)
            return
        launchFragmentWithStack(AccountFragmentMainUBI4())
    }
    override fun showAccountCustomerServiceScreen() { launchFragmentWithStack(AccountFragmentCustomerServiceUBI4()) }


    override fun showMotionTrainingScreen(onFinishTraining: () -> Unit) {
        val fragment = MotionTrainingFragment(onFinishTraining)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        activeFragment = fragment
        Log.d("StateCallBack", "showMotionTrainingScreen called, new MotionTrainingFragment created")
    }

    override fun manageTrainingLifecycle() {
        Log.d("StateCallBack", "manageTrainingLifecycle called")
        trainingModelHandler.runModel()
    }

    override fun getPercentProgressLearningModel(): Int {
        return trainingModelHandler.getPercentProgressLearningModel()
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
    private fun launchFragmentWithoutStack(fragment: Fragment) {
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


    private fun initAllVariables() {
        connectedDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME).orEmpty()
        connectedDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS).orEmpty()
        setStaticVariables()

        saveString(PreferenceKeysUBI4.LAST_CONNECTION_MAC_UBI4, connectedDeviceAddress)
        Log.d("initAllVariables","connectedDeviceAddress $connectedDeviceAddress" )

        //settings
    }
    internal fun sendWidgetsArray() { CoroutineScope(Dispatchers.IO).launch { updateFlow.emit(1) } }
    private fun setStaticVariables() {
//        listWidgets = mutableSetOf()
        canSendNextChunkFlagFlow = MutableSharedFlow()
        updateFlow = MutableSharedFlow()
        plotArrayFlow = MutableStateFlow(PlotParameterRef(0,0, arrayListOf()))
        rotationGroupFlow = MutableSharedFlow()
        slidersFlow = MutableSharedFlow()
        switcherFlow = MutableSharedFlow()
        bindingGroupFlow = MutableSharedFlow()
        activeGestureFlow = MutableSharedFlow()
        selectGestureModeFlow = MutableSharedFlow()
        stateOpticTrainingFlow = MutableStateFlow(PreferenceKeysUBI4.TrainingModelState.BASE)
        thresholdFlow = MutableSharedFlow()
//        baseSubDevicesInfoStructSet = mutableSetOf()
//        baseParametrInfoStructArray = arrayListOf()
        plotArray = arrayListOf()
        rotationGroupGestures = arrayListOf()
        countBinding = 0
        graphThreadFlag = true
        canSendFlag = false
        bindingGroupGestures = arrayListOf()
        activeGestureFragmentFilterFlow = MutableStateFlow(1)
        activeSettingsFragmentFilterFlow = MutableStateFlow(4)
        spinnerFlow = MutableSharedFlow()
        bleParser = BLEParser()
        runCommandFlow = MutableStateFlow(0)
        isMobileSettings = false
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
            }
        }
        worker.start()
    }
    fun getQueueUBI4() : BlockingQueueUbi4 { return queue }
    override fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit) {
        if (byteArray != null) {
            queue.put(getBleCommandWithQueue(byteArray, command, typeCommand, onChunkSent), byteArray)
        }
    }
    private fun getBleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit): Runnable {
        return Runnable {
            writeData(byteArray, command, typeCommand)
            onChunkSent() } }
    private fun writeData(byteArray: ByteArray?, command: String, typeCommand: String) {
        synchronized(this) {
            canSendFlag = false
            bleCommand(byteArray, command, typeCommand)
            Log.d("TestSendByteArray","send!!!!")
            while (!canSendFlag) {
                Thread.sleep(1)
            }
            Log.d("TestSendByteArray","CallBack is BLEService was complete")
        }
    }

    fun loadText(key: String): String { return mSettings!!.getString(key, "null").toString() }


    //не нарушая инкапсуляцию
    fun getBLEController(): BLEController {
        return mBLEController
    }

    fun getBottomNavigationController(): BottomNavigationController {
        return bottomNavigationController
    }

    fun updateSerialNumber(deviceInfo: DeviceInfoStructs) {
        val serialNumber = "${deviceInfo.deviceUUIDPrefix}${deviceInfo.formattedDeviceUUID}"
        mDeviceName = serialNumber
        runOnUiThread {
            binding.nameTv.text = serialNumber
        }
    }



    override fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String) {
        System.err.println("BLE debug bleCommand")
        mBLEController.bleCommand( byteArray, uuid, typeCommand )
    }

    companion object {
        var main by Delegates.notNull<MainActivityUBI4>()
        var runCommandFlow by Delegates.notNull<MutableStateFlow<Int>>()

        var bleParser by Delegates.notNull<BLEParser>()
        var canSendNextChunkFlagFlow by Delegates.notNull<MutableSharedFlow<Int>>()
        var updateFlow by Delegates.notNull<MutableSharedFlow<Int>>()
//        var listWidgets by Delegates.notNull<MutableSet<Any>>()

        var plotArrayFlow by Delegates.notNull<MutableStateFlow<PlotParameterRef>>()
        var plotArray by Delegates.notNull<ArrayList<Int>>()
        var rotationGroupFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
        var bindingGroupFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
        var stateOpticTrainingFlow by Delegates.notNull<MutableStateFlow<PreferenceKeysUBI4.TrainingModelState>>()
        var slidersFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()//MutableStateFlow
        var switcherFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
        var thresholdFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
        var activeGestureFlow  by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
        var selectGestureModeFlow  by Delegates.notNull<MutableSharedFlow<ParameterRef>>()
        var spinnerFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()


        var activeGestureFragmentFilterFlow by Delegates.notNull<MutableStateFlow<Int>>()
        var activeSettingsFragmentFilterFlow by Delegates.notNull<MutableStateFlow<Int>>()



        var fullInicializeConnectionStruct by Delegates.notNull<FullInicializeConnectionStruct>()
//        var baseParametrInfoStructArray by Delegates.notNull<ArrayList<BaseParameterInfoStruct>>()
        var opticTrainingStructArray by Delegates.notNull<ArrayList<OpticTrainingStruct>>()

//        var baseSubDevicesInfoStructSet by Delegates.notNull<MutableSet<BaseSubDeviceInfoStruct>>()

        var rotationGroupGestures by Delegates.notNull<ArrayList<Gesture>>()

        var bindingGroupGestures by Delegates.notNull<ArrayList<Pair<Int, Int>>>()

        var isMobileSettings by Delegates.notNull<Boolean>()

        var connectedDeviceName by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()

        var countBinding by Delegates.notNull<Int>()
        var graphThreadFlag by Delegates.notNull<Boolean>()

        var canSendFlag by Delegates.notNull<Boolean>()

        var inScanFragmentFlag by Delegates.notNull<Boolean>()
    }
}
