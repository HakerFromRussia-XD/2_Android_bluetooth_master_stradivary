/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.start.motorica.new_electronic_by_Rodeon.ui.activities.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.nfc.NfcAdapter
import android.os.*
import android.view.View
import android.view.WindowManager
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_advanced_settings.*
import kotlinx.android.synthetic.main.layout_chart.*
import kotlinx.android.synthetic.main.layout_gestures.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ble.BluetoothLeService
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager.*
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.presenters.MainPresenter
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapter
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterMonograb
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterMonograbWithAdvancedSettings
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterWithAdvancedSettings
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.CustomDialogFragment
import me.start.motorica.new_electronic_by_Rodeon.utils.NavigationUtils
import me.start.motorica.new_electronic_by_Rodeon.viewTypes.MainActivityView
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.experimental.xor


@RequirePresenter(MainPresenter::class)
open class MainActivity() : BaseActivity<MainPresenter, MainActivityView>(), MainActivityView, Parcelable {

  @Inject
  lateinit var preferenceManager: PreferenceManager

  private var sensorsDataThreadFlag: Boolean = false
  private var mDeviceName: String? = null
  var mDeviceAddress: String? = null
  var mDeviceType: String? = null
  private var mBluetoothLeService: BluetoothLeService? = null
  private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
  private var mGattServicesList: ExpandableListView? = null
  private var mConnectView: View? = null
  private var mDisconnectView: View? = null
  private var mConnected = false
  private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
  private var mCharacteristic: BluetoothGattCharacteristic? = null
  private var dataSens1 = 0x00
  private var dataSens2 = 0x00
  private var mSettings: SharedPreferences? = null

  private var state = 0
  private var subscribeThread: Thread? = null
  private var mNumberGesture = 0
  // Очередь для задачь работы с BLE
  private val queue = me.start.motorica.new_electronic_by_Rodeon.services.receivers.BlockingQueue()
  private var readDataFlag = true
  private var globalSemaphore = true // флаг, который преостанавливает отправку новой команды, пока ответ на предыдущую не пришёл
//  private var showAdvancedSettings = false
  private var swapOpenCloseButton = false
  private var countCommand = 0
  private var actionState = READ
  var savingSettingsWhenModified = false
  var lockWriteBeforeFirstRead = true //переменная, необходимая для ожидания первого пришедшего ответа от устройства на
  // отправленный запрос чтения. Если не ожидать её, то поток чтения не перезамускается
  internal var locate = ""

  private val listName = "NAME"
  private val listUUID = "UUID"

  // Code to manage Service lifecycle.
  private val mServiceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
      mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
      if (!mBluetoothLeService?.initialize()!!) {
        Timber.e("Unable to initialize Bluetooth")
        finish()
      }
      // Automatically connects to the device upon successful start-up initialization.
      mBluetoothLeService?.connect(mDeviceAddress)
      if (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3))
      {} else {
        mainactivity_navi.visibility = View.GONE
      }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
      mBluetoothLeService = null
    }
  }

  // Handles various events fired by the Service.
  // ACTION_GATT_CONNECTED: connected to a GATT server.
  // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
  // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
  // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
  //                        or notification operations.
  private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      when {
          BluetoothLeService.ACTION_GATT_CONNECTED == action -> {
            //connected state
            mConnected = true
            mConnectView!!.visibility = View.VISIBLE
            mDisconnectView!!.visibility = View.GONE
            System.err.println("DeviceControlActivity-------> момент индикации коннекта")
            invalidateOptionsMenu()
          }
          BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
            //disconnected state
            mConnected = false
            mConnectView!!.visibility = View.GONE
            mDisconnectView!!.visibility = View.VISIBLE
            System.err.println("DeviceControlActivity-------> момент индикации коннекта")
            invalidateOptionsMenu()
            clearUI()
          }
          BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
            // Show all the supported services and characteristics on the user interface.
            displayGattServices(mBluetoothLeService!!.supportedGattServices)
          }
          BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
            if ((mDeviceType!!.contains(EXTRAS_DEVICE_TYPE)) || (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2)) || (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3))) { // новая схема обработки данных
              displayData(intent.getByteArrayExtra(BluetoothLeService.FESTO_A_DATA))
              intent.getStringExtra(BluetoothLeService.ACTION_STATE)?.let { setActionState(it) }
//              System.err.println("попадаем сюда")
            } else {
              displayData(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA))
//              System.err.println("попадаем не сюда")
            }
             //вывод на график данных из характеристики показаний пульса
            displayDataWriteOpen(intent.getByteArrayExtra(BluetoothLeService.OPEN_MOTOR_DATA))
            displayDataWriteOpen(intent.getByteArrayExtra(BluetoothLeService.CLOSE_MOTOR_DATA))
            setSensorsDataThreadFlag(intent.getBooleanExtra(BluetoothLeService.SENSORS_DATA_THREAD_FLAG, true))
          }
      }
    }
  }
  @SuppressLint("SetTextI18n")
  private fun displayData(data: ByteArray?) {
    if (data != null){
//      System.err.println("BluetoothLeService-------------> прошли первый иф")
      if (castUnsignedCharToInt(data[0]) != 0xAA) {
//        System.err.println("BluetoothLeService-------------> прошли второй иф")
//        System.err.println("data.size: " + data.size)
        if (data.size == 3) {
//          System.err.println("mDeviceAddress-------------> прошли третий иф. Распарсили нотификацию")
          dataSens1 = castUnsignedCharToInt(data[1])
          dataSens2 = castUnsignedCharToInt(data[2])
          savingSettingsWhenModified = true
        } else if (data.size == 10) {
          dataSens1 = castUnsignedCharToInt(data[1])
          dataSens2 = castUnsignedCharToInt(data[2])
          if (castUnsignedCharToInt(data[3]) != mSettings!!.getInt(PreferenceKeys.DRIVER_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.DRIVER_NUM, castUnsignedCharToInt(data[3]))
          }
          if (castUnsignedCharToInt(data[4]) != mSettings!!.getInt(PreferenceKeys.BMS_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.BMS_NUM, castUnsignedCharToInt(data[4]))
          }
          if (castUnsignedCharToInt(data[5]) != mSettings!!.getInt(PreferenceKeys.SENS_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.SENS_NUM, castUnsignedCharToInt(data[5]))
          }
          if (castUnsignedCharToInt(data[6]) != mSettings!!.getInt(PreferenceKeys.OPEN_CH_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, castUnsignedCharToInt(data[6]))
          }
          if (castUnsignedCharToInt(data[7]) != mSettings!!.getInt(PreferenceKeys.CLOSE_CH_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, castUnsignedCharToInt(data[7]))
          }
          if (castUnsignedCharToInt(data[8]) != mSettings!!.getInt(PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, castUnsignedCharToInt(data[8]))
          }
          if (castUnsignedCharToInt(data[9]) != mSettings!!.getInt(PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, castUnsignedCharToInt(data[9]))
          }
        }
        lockWriteBeforeFirstRead = false
      } else {
        countCommand--
        System.err.println("Count command before start read: $countCommand")
        if (countCommand == 0) {
          globalSemaphore = false
          readDataFlag = true
          runReadData()
        }
      }
    }
  }
  private fun displayDataWriteOpen(data: ByteArray?) {
    if (data != null) {
      if (data[0].toInt() == 1){ state = 1 }
      if (data[0].toInt() == 0){ state = 2 }
    }
  }
  fun setActionState(value: String) {
    actionState = value
  }

  constructor(parcel: Parcel) : this() {
    sensorsDataThreadFlag = parcel.readByte() != 0.toByte()
    mDeviceName = parcel.readString()
    mDeviceAddress = parcel.readString()
    mDeviceType = parcel.readString()
    mConnected = parcel.readByte() != 0.toByte()
    mNotifyCharacteristic = parcel.readParcelable(BluetoothGattCharacteristic::class.java.classLoader)
    mCharacteristic = parcel.readParcelable(BluetoothGattCharacteristic::class.java.classLoader)
    dataSens1 = parcel.readInt()
    dataSens2 = parcel.readInt()
    state = parcel.readInt()
  }

  private fun clearUI() {
    mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
    enableInterface(false)
  }

  @SuppressLint("CheckResult", "NewApi")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    initBaseView(this)
    //changing statusbar
    val window = this.window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.statusBarColor = this.resources.getColor(R.color.blueStatusBar, theme)

    locate = Locale.getDefault().toString()

    mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

    val intent = intent
    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
    mDeviceType = intent.getStringExtra(EXTRAS_DEVICE_TYPE)
    System.err.println("mDeviceType: $mDeviceType")


    // Sets up UI references.
    mGattServicesList = findViewById(R.id.gatt_services_list)
    mConnectView = findViewById(R.id.connect_view)
    mDisconnectView = findViewById(R.id.disconnect_view)

    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

    val worker = Thread {
      while (true) {
        val task: Runnable = queue.get()
        task.run()
      }
    }
    worker.start()

    initUI()
  }

  private fun initUI() {
    if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapterWithAdvancedSettings(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 1)
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograbWithAdvancedSettings(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 0)
      }
      NavigationUtils.showAdvancedSettings = true
    } else {
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapter(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 1)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograb(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 0)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
      }
    }

    mainactivity_viewpager.offscreenPageLimit = 3
    NavigationUtils.setComponents(baseContext, mainactivity_navi)
  }

  fun showAdvancedSettings(showAdvancedSettings: Boolean) {
    NavigationUtils.showAdvancedSettings = showAdvancedSettings
    if (showAdvancedSettings) {
      saveInt(PreferenceKeys.ADVANCED_SETTINGS, 1)
    }  else {
      saveInt(PreferenceKeys.ADVANCED_SETTINGS, 0)
    }


    mainactivity_viewpager.isSaveFromParentEnabled = false
    if (showAdvancedSettings) {
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapterWithAdvancedSettings(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 1)
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograbWithAdvancedSettings(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 0)
      }
    } else {
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapter(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 1)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograb(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 0)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
      }
    }

    Toast.makeText(this, "Advanced settings: $showAdvancedSettings", Toast.LENGTH_SHORT).show()

    mainactivity_viewpager.offscreenPageLimit = 3
    NavigationUtils.setComponents(baseContext, mainactivity_navi)
  }


  override fun onResume() {
    super.onResume()
    val filter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
    try {
      filter.addDataType("waterdays_nfc/*")
    } catch (e: Exception) {
      e.printStackTrace()
    }

    val i = Intent(this, javaClass)
    i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

    readDataFlag = true
    //BLE
    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    if (mBluetoothLeService != null) {
      mBluetoothLeService!!.connect(mDeviceAddress)
    }
  }
  override fun onPause() {
    super.onPause()
    unregisterReceiver(mGattUpdateReceiver)
  }
  override fun onDestroy() {
    super.onDestroy()
    unbindService(mServiceConnection)
    mBluetoothLeService = null
    readDataFlag = false
  }
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }


  // Demonstrates how to iterate through the supported GATT Services/Characteristics.
  // In this sample, we populate the data structure that is bound to the ExpandableListView
  // on the UI.
  private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
    System.err.println("DeviceControlActivity-------> момент начала выстраивания списка параметров")
    if (gattServices == null) return
    var uuid: String?
    val unknownServiceString = ("unknown_service")
    val unknownCharaString =("unknown_characteristic")
    val gattServiceData = ArrayList<HashMap<String, String?>>()
    val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
    mGattCharacteristics = java.util.ArrayList()


    // Loops through available GATT Services.
    for (gattService in gattServices) {
      val currentServiceData = HashMap<String, String?>()
      uuid = gattService.uuid.toString()
      currentServiceData[listName] = lookup(uuid, unknownServiceString)
      currentServiceData[listUUID] = uuid
      gattServiceData.add(currentServiceData)
      val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
      val gattCharacteristics = gattService.characteristics
      val charas = ArrayList<BluetoothGattCharacteristic>()

      // Loops through available Characteristics.
      for (gattCharacteristic in gattCharacteristics) {
        charas.add(gattCharacteristic)
        val currentCharaData = HashMap<String, String?>()
        uuid = gattCharacteristic.uuid.toString()
        currentCharaData[listName] = lookup(uuid, unknownCharaString)
        currentCharaData[listUUID] = uuid
        gattCharacteristicGroupData.add(currentCharaData)
      }
      mGattCharacteristics.add(charas)
      gattCharacteristicData.add(gattCharacteristicGroupData)
    }
    val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(listName, listUUID), intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(listName, listUUID), intArrayOf(android.R.id.text1, android.R.id.text2))
    mGattServicesList!!.setAdapter(gattServiceAdapter)
    enableInterface(true)
  }
  private fun enableInterface(enabled: Boolean) {
    close_btn.isEnabled = enabled
    open_btn.isEnabled = enabled
    thresholds_blocking_sw.isEnabled = enabled
    correlator_noise_threshold_1_sb.isEnabled = enabled
    correlator_noise_threshold_2_sb.isEnabled = enabled
    if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
      gesture_1_btn.isEnabled = enabled
      gesture_2_btn.isEnabled = enabled
      gesture_3_btn.isEnabled = enabled
      gesture_4_btn.isEnabled = enabled
      gesture_5_btn.isEnabled = enabled
      gesture_6_btn.isEnabled = enabled
      gesture_7_btn.isEnabled = enabled
      gesture_8_btn.isEnabled = enabled
      gesture_settings_2_btn.isEnabled = enabled
      gesture_settings_3_btn.isEnabled = enabled
      gesture_settings_4_btn.isEnabled = enabled
      gesture_settings_5_btn.isEnabled = enabled
      gesture_settings_6_btn.isEnabled = enabled
      gesture_settings_7_btn.isEnabled = enabled
      gesture_settings_8_btn.isEnabled = enabled
      if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
        swap_sensors_sw.isEnabled = enabled
        swap_open_close_sw.isEnabled = enabled
        single_channel_control_sw.isEnabled = enabled
        reset_to_factory_settings_btn.isEnabled = enabled
      }
    }

    sensorsDataThreadFlag = enabled
    if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
      runReadData()
    } else {
      startSubscribeSensorsDataThread()
    }
  }

  fun bleCommandConnector(byteArray: ByteArray?, Command: String, typeCommand: String, register: Int) {
    if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2)  || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3))  {
      val length = byteArray!!.size + 2
      val sendByteMassive = ByteArray(length + 3)
      sendByteMassive[0] = 0xAA.toByte()
      sendByteMassive[1] = 0xAA.toByte()
      sendByteMassive[2] = length.toByte()
      when (register) {
        0 -> {
          sendByteMassive[3] = 0x00
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        1 -> {
          sendByteMassive[3] = 0x01
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        3 -> {
          sendByteMassive[3] = 0x03
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        4 -> {
          sendByteMassive[3] = 0x04
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        5 -> {
          sendByteMassive[3] = 0x05
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        6 -> {
          sendByteMassive[3] = 0x06
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = byteArray[1]
          sendByteMassive[6] = crcCalc(sendByteMassive)
        }
        7 -> {
          sendByteMassive[3] = 0x07
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = byteArray[1]
          sendByteMassive[6] = crcCalc(sendByteMassive)
        }
        10 -> {
          sendByteMassive[3] = 10.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        11 -> {
          sendByteMassive[3] = 11.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = byteArray[1]
          sendByteMassive[6] = byteArray[2]
          sendByteMassive[7] = crcCalc(sendByteMassive)
        }
        12 -> {
          sendByteMassive[3] = 12.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = byteArray[1]
          sendByteMassive[6] = byteArray[2]
          sendByteMassive[7] = byteArray[3]
          sendByteMassive[8] = crcCalc(sendByteMassive)
        }
        13 -> {
          sendByteMassive[3] = 13.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        14 -> {
          sendByteMassive[3] = 14.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        15 -> {
          sendByteMassive[3] = 15.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
        16 -> {
          sendByteMassive[3] = 16.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
      }
      readDataFlag = false
      globalSemaphore = true
      runWriteData(sendByteMassive, FESTO_A_CHARACTERISTIC, WRITE_WR)
    } else {
      bleCommand(byteArray, Command, typeCommand)
    }
  }
  private fun bleCommand(byteArray: ByteArray?, Command: String, typeCommand: String){
    for (i in mGattCharacteristics.indices) {
      for (j in mGattCharacteristics[i].indices) {
        if (mGattCharacteristics[i][j].uuid.toString() == Command) {
          mCharacteristic = mGattCharacteristics[i][j]
          if (typeCommand == WRITE){
            if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
              mCharacteristic?.value = byteArray
              mBluetoothLeService?.writeCharacteristic(mCharacteristic)
            }
          }

          if (typeCommand == WRITE_WR){
            if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
              mCharacteristic?.value = byteArray
              mBluetoothLeService?.writeCharacteristic(mCharacteristic)
            }
          }

          if (typeCommand == READ){
            if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
              mBluetoothLeService?.readCharacteristic(mCharacteristic)
            }
          }

          if (typeCommand == NOTIFY){
            if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
              mNotifyCharacteristic = mCharacteristic
              mBluetoothLeService!!.setCharacteristicNotification(
                      mCharacteristic, true)
            }
          }
        }
      }
    }
//    System.err.println("bleCommand")
  }

  private fun startSubscribeSensorsDataThread() {
    subscribeThread = Thread {
      while (sensorsDataThreadFlag) {
        runOnUiThread {
          bleCommand(null, MIO_MEASUREMENT, NOTIFY)
          System.err.println("startSubscribeSensorsDataThread попытка подписки")
        }
        try {
          Thread.sleep(GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) { }
      }
    }
    subscribeThread?.start()
  }

  private fun runWriteData(byteArray: ByteArray?, Command: String, typeCommand: String) { getWriteData(byteArray, Command, typeCommand).let { queue.put(it) } }
  open fun getWriteData(byteArray: ByteArray?, Command: String, typeCommand: String): Runnable { return Runnable { writeData(byteArray, Command, typeCommand) } }
  private fun writeData(byteArray: ByteArray?, Command: String, typeCommand: String) {
    try {
      Thread.sleep(200)
    } catch (ignored: Exception) {}
    bleCommand(byteArray, Command, typeCommand)
    try {
      Thread.sleep(100)
    } catch (ignored: Exception) {}
  }

  private fun runReadData() {
    getReadData().let { queue.put(it) }
  }
  open fun getReadData(): Runnable { return Runnable { readData() } }
  private fun readData() {
    while (readDataFlag) {
      bleCommand(null, FESTO_A_CHARACTERISTIC, READ)
      try {
        Thread.sleep(10)
      } catch (ignored: Exception) {}
    }
  }


  private fun makeGattUpdateIntentFilter(): IntentFilter {
    val intentFilter = IntentFilter()
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
    return intentFilter
  }

  fun getDataSens1(): Int { return dataSens1 }
  fun getDataSens2(): Int { return dataSens2 }
  fun getMNumberGesture(): Int { return mNumberGesture }
  fun setSensorsDataThreadFlag(value: Boolean){ sensorsDataThreadFlag = value }
  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeByte(if (sensorsDataThreadFlag) 1 else 0)
    parcel.writeString(mDeviceName)
    parcel.writeString(mDeviceAddress)
    parcel.writeString(mDeviceType)
    parcel.writeByte(if (mConnected) 1 else 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      parcel.writeParcelable(mNotifyCharacteristic, flags)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      parcel.writeParcelable(mCharacteristic, flags)
    }
    parcel.writeInt(dataSens1)
    parcel.writeInt(dataSens2)
    parcel.writeInt(state)
  }

  override fun describeContents(): Int { return 0 }

  companion object CREATOR : Parcelable.Creator<MainActivity> {
    override fun createFromParcel(parcel: Parcel): MainActivity { return MainActivity(parcel) }
    override fun newArray(size: Int): Array<MainActivity?> { return arrayOfNulls(size) }
  }

  fun openFragment(numberGesture: Int) {
    val dialog = CustomDialogFragment()
    mNumberGesture = numberGesture
    dialog.show(supportFragmentManager, "custom dialog")
  }
  override fun initializeUI() {}

  open fun crcCalc(data: ByteArray): Byte {
    var countLocal = data.size - 1
    val crcTable = byteArrayOf(
            0, 94, 188.toByte(), 226.toByte(), 97, 63, 221.toByte(), 131.toByte(), 194.toByte(), 156.toByte(), 126, 32, 163.toByte(), 253.toByte(), 31, 65,
            157.toByte(), 195.toByte(), 33, 127, 252.toByte(), 162.toByte(), 64, 30, 95, 1, 227.toByte(), 189.toByte(), 62, 96, 130.toByte(), 220.toByte(),
            35, 125, 159.toByte(), 193.toByte(), 66, 28, 254.toByte(), 160.toByte(), 225.toByte(), 191.toByte(), 93, 3, 128.toByte(), 222.toByte(), 60, 98,
            190.toByte(), 224.toByte(), 2, 92, 223.toByte(), 129.toByte(), 99, 61, 124, 34, 192.toByte(), 158.toByte(), 29, 67, 161.toByte(), 255.toByte(),
            70, 24, 250.toByte(), 164.toByte(), 39, 121, 155.toByte(), 197.toByte(), 132.toByte(), 218.toByte(), 56, 102, 229.toByte(), 187.toByte(), 89, 7,
            219.toByte(), 133.toByte(), 103, 57, 186.toByte(), 228.toByte(), 6, 88, 25, 71, 165.toByte(), 251.toByte(), 120, 38, 196.toByte(), 154.toByte(),
            101, 59, 217.toByte(), 135.toByte(), 4, 90, 184.toByte(), 230.toByte(), 167.toByte(), 249.toByte(), 27, 69, 198.toByte(), 152.toByte(), 122, 36,
            248.toByte(), 166.toByte(), 68, 26, 153.toByte(), 199.toByte(), 37, 123, 58, 100, 134.toByte(), 216.toByte(), 91, 5, 231.toByte(), 185.toByte(),
            140.toByte(), 210.toByte(), 48, 110, 237.toByte(), 179.toByte(), 81, 15, 78, 16, 242.toByte(), 172.toByte(), 47, 113, 147.toByte(), 205.toByte(),
            17, 79, 173.toByte(), 243.toByte(), 112, 46, 204.toByte(), 146.toByte(), 211.toByte(), 141.toByte(), 111, 49, 178.toByte(), 236.toByte(), 14, 80,
            175.toByte(), 241.toByte(), 19, 77, 206.toByte(), 144.toByte(), 114, 44, 109, 51, 209.toByte(), 143.toByte(), 12, 82, 176.toByte(), 238.toByte(),
            50, 108, 142.toByte(), 208.toByte(), 83, 13, 239.toByte(), 177.toByte(), 240.toByte(), 174.toByte(), 76, 18, 145.toByte(), 207.toByte(), 45, 115,
            202.toByte(), 148.toByte(), 118, 40, 171.toByte(), 245.toByte(), 23, 73, 8, 86, 180.toByte(), 234.toByte(), 105, 55, 213.toByte(), 139.toByte(),
            87, 9, 235.toByte(), 181.toByte(), 54, 104, 138.toByte(), 212.toByte(), 149.toByte(), 203.toByte(), 41, 119, 244.toByte(), 170.toByte(), 72, 22,
            233.toByte(), 183.toByte(), 85, 11, 136.toByte(), 214.toByte(), 52, 106, 43, 117, 151.toByte(), 201.toByte(), 74, 20, 246.toByte(), 168.toByte(),
            116, 42, 200.toByte(), 150.toByte(), 21, 75, 169.toByte(), 247.toByte(), 182.toByte(), 232.toByte(), 10, 84, 215.toByte(), 137.toByte(), 107, 53
    )
    var result: Byte = 0
    var i = 0
    while (countLocal != 0 ) {
      result = crcTable[castUnsignedCharToInt(result xor data[i])]
      i++
      countLocal--
    }
    return result
  }

  private fun castUnsignedCharToInt(Ubyte: Byte): Int {
    var cast = Ubyte.toInt()
    if (cast < 0) {
      cast += 256
    }
    return cast
  }

  internal fun saveInt (key: String, variable: Int) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putInt(key, variable)
    editor.apply()
  }

  fun setSwapOpenCloseButton(swap: Boolean) {
    swapOpenCloseButton = swap
    if (swap) {
      saveInt(mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, 1)
    } else {
      saveInt(mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, 0)
    }

  }
  fun getSwapOpenCloseButton() : Boolean {
    return swapOpenCloseButton
  }
  fun incrementCountCommand() {
    countCommand++
  }
}
