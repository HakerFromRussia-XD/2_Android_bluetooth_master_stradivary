package com.bailout.stickk.ubi4.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.MainPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.MainActivityView
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.contract.TransmitterUBI4
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.MyViewModel
import com.bailout.stickk.ubi4.models.ParameterRef
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.ui.bottom.BottomNavigationController
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.REQUEST_ENABLE_BT
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates


@RequirePresenter(MainPresenter::class)
class MainActivityUBI4 : BaseActivity<MainPresenter, MainActivityView>(), NavigatorUBI4, TransmitterUBI4 {
    private lateinit var viewModel: MyViewModel
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private lateinit var mBLEController: BLEController
    // Очередь для задачь работы с BLE
    private val queue = BlockingQueueUbi4()


    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        viewModel = ViewModelProvider(this)[MyViewModel::class.java]
        val view = binding.root
        main = this
        val window = this.window
        window.statusBarColor = ContextCompat.getColor(this, R.color.ubi4_back)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.ubi4_dark_back)
        setContentView(view)
        initAllVariables()
        BottomNavigationController(bottomNavigation = binding.bottomNavigation)

        // инициализация блютуз
        mBLEController = BLEController(viewModel,this)
        mBLEController.initBLEStructure()
        mBLEController.scanLeDevice(true)
        startQueue()

        showSensorsScreen()
        binding.addCommandBtn.setOnClickListener {
            val commandName = "Команда ${System.currentTimeMillis()}"
            runWriteDataTest(byteArrayOf(0x00,0x01,0x02), MAIN_CHANNEL, WRITE)
        }
//        binding.runCommandBtn.setOnClickListener {
//            if (queue.size() != 0) triggerSendFlag()
//        }

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
            connectedDeviceAddress =  getString(CONNECTED_DEVICE_ADDRESS)
            System.err.println("onResume ${getString(CONNECTED_DEVICE_ADDRESS)}")
        }
        if (!mBLEController.getStatusConnected()) {
            mBLEController.setReconnectThreadFlag(true)
            mBLEController.reconnectThread()
        }
    }


    override fun showGesturesScreen() { launchFragmentWithoutStack(GesturesFragment()) }
    override fun showSensorsScreen() { launchFragmentWithoutStack(SensorsFragment()) }
    override fun showAdvancedScreen() { launchFragmentWithoutStack(AdvancedFragment()) }
    override fun showToast(massage: String) {
        Toast.makeText(this,massage,Toast.LENGTH_SHORT).show()
    }
    override fun getBackStackEntryCount(): Int { return supportFragmentManager.backStackEntryCount }
    override fun goingBack() { onBackPressed() }
    override fun goToMenu() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
    private fun launchFragmentWithoutStack(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        if (!supportFragmentManager.isDestroyed) transaction.commit()
    }

    private fun initAllVariables() {
        connectedDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME).orEmpty()
        connectedDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS).orEmpty()
        setStaticVariables()
        //settings
    }
    internal fun sendWidgetsArray() {
        //событие эммитится только в случае если size отличается от предыдущего
        updateFlow.value += 1
    }
    private fun setStaticVariables() {
        listWidgets = mutableSetOf()
        updateFlow = MutableStateFlow(0)
        plotArrayFlow = MutableStateFlow(arrayListOf())
        rotationGroupFlow = MutableStateFlow(0)
        slidersFlow = MutableStateFlow(ParameterRef(0, 0))
        switcherFlow = MutableStateFlow(0)
        baseSubDevicesInfoStructSet = mutableSetOf()
        baseParametrInfoStructArray = arrayListOf()
        plot = MutableStateFlow(0)
        plotArray = arrayListOf()
        rotationGroupGestures = arrayListOf()
        countBinding = 0
        graphThreadFlag = true
        canSendFlag = false
    }

    // сохранение и загрузка данных
    override fun saveString(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    override fun getString(key: String) :String {
//        System.err.println("getString test key: $key  value: ${mSettings!!.getString(key, "NOT SET!").toString()}")
        return mSettings!!.getString(key, "NOT SET!").toString()
    }
    internal fun saveInt(key: String, variable: Int) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putInt(key, variable)
        editor.apply()
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
//    fun runWriteData(byteArray: ByteArray?, Command: String, typeCommand: String) { getWriteData(byteArray, Command, typeCommand).let { queue.put(it) } }
//    private fun getWriteData(byteArray: ByteArray?, Command: String, typeCommand: String): Runnable { return Runnable { writeData(byteArray, Command, typeCommand) } }
//    private fun writeData(byteArray: ByteArray?, Command: String, typeCommand: String) {
//        //TODO дописать
//        while () {
//            bleCommand(byteArray, Command, typeCommand)
//        }

    fun runWriteDataTest(byteArray: ByteArray?, Command: String, typeCommand: String) { queue.put(getWriteDataTest(byteArray, Command, typeCommand)) }
    private fun getWriteDataTest(byteArray: ByteArray?, Command: String, typeCommand: String): Runnable { return Runnable { writeDataTest(byteArray, Command, typeCommand) } }
    private fun writeDataTest(byteArray: ByteArray?, Command: String, typeCommand: String) {
        synchronized(this) {
            Log.d("TestSendByteArray","Command is sending...")
            canSendFlag = false
            bleCommand(byteArray, Command, typeCommand)
            while (!canSendFlag) {
                Thread.sleep(1)
            }
            Log.d("TestSendByteArray","CallBack is BLEService was complete")
        }
    }


    override fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String) {
        System.err.println("BLE debug bleCommand")
        mBLEController.bleCommand( byteArray, uuid, typeCommand )
    }

    companion object {
        var main by Delegates.notNull<MainActivityUBI4>()

        var updateFlow by Delegates.notNull<MutableStateFlow<Int>>()
        var listWidgets by Delegates.notNull<MutableSet<Any>>()

        var plotArrayFlow by Delegates.notNull<MutableStateFlow<ArrayList<Int>>>()
        var rotationGroupFlow by Delegates.notNull<MutableSharedFlow<Int>>()//MutableStateFlow
        var slidersFlow by Delegates.notNull<MutableSharedFlow<ParameterRef>>()//MutableStateFlow
        var switcherFlow by Delegates.notNull<MutableSharedFlow<Int>>()
        var plotArray by Delegates.notNull<ArrayList<Int>>()
        var plot by Delegates.notNull<MutableStateFlow<Int>>()


        var fullInicializeConnectionStruct by Delegates.notNull<FullInicializeConnectionStruct>()
        var baseParametrInfoStructArray by Delegates.notNull<ArrayList<BaseParameterInfoStruct>>()
        var baseSubDevicesInfoStructSet by Delegates.notNull<MutableSet<BaseSubDeviceInfoStruct>>()

        var rotationGroupGestures by Delegates.notNull<ArrayList<Gesture>>()


        var connectedDeviceName by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()

        var countBinding by Delegates.notNull<Int>()
        var graphThreadFlag by Delegates.notNull<Boolean>()

        var canSendFlag by Delegates.notNull<Boolean>()

        var inScanFragmentFlag by Delegates.notNull<Boolean>()
    }
}
