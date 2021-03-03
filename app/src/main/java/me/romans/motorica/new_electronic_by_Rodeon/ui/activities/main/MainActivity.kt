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

package me.romans.motorica.new_electronic_by_Rodeon.ui.activities.main

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
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_chart.*
import kotlinx.android.synthetic.main.layout_sens_settings.*
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.ble.BluetoothLeService
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.romans.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.romans.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.romans.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.romans.motorica.new_electronic_by_Rodeon.presenters.MainPresenter
import me.romans.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapter
import me.romans.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterMonograb
import me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main.CustomDialogFragment
import me.romans.motorica.new_electronic_by_Rodeon.utils.NavigationUtils
import me.romans.motorica.new_electronic_by_Rodeon.viewTypes.MainActivityView
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.experimental.xor

//import com.an
//import com.androidexception.andexalertdialog.AndExAlertDialog


@RequirePresenter(MainPresenter::class)
open class MainActivity() : BaseActivity<MainPresenter, MainActivityView>(), MainActivityView, Parcelable {

  private var sensorsDataThreadFlag: Boolean = false
  private var nAdapter: NfcAdapter? = null
  private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter
  private lateinit var mSectionsPagerAdapter2: SectionsPagerAdapterMonograb
  private var mDeviceName: String? = null
  private var mDeviceAddress: String? = null
  private var mDeviceType: String? = null
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
  private var gcVer     = 0x00
  private var bmsVer    = 0x00
  private var sensVer   = 0x00

  private var state = 0
  private var subscribeThread: Thread? = null
  private var moveThread: Thread? = null
  private var mNumberGesture = 0
  // Очередь для задачь работы с BLE
  private val queue = me.romans.motorica.new_electronic_by_Rodeon.services.receivers.BlockingQueue()
  var readDataFlag = true
  internal var globalSemaphore = true // флаг, который преостанавливает отправку новой команды, пока ответ на предыдущую не пришёл


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
      if (!mDeviceType.equals("FESTO_A"))
      {
        mainactivity_navi.visibility = View.GONE
        //TODO Здесь можно выставлять флаг, меняющий логику в зависимости от имени
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
            if (mDeviceType.equals("FESTO_A")) { // новая схема обработки данных
              displayData(intent.getByteArrayExtra(BluetoothLeService.FESTO_A_DATA))
            } else {
              displayData(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA))
            }
             //вывод на график данных из характеристики показаний пульса
            displayDataWriteOpen(intent.getByteArrayExtra(BluetoothLeService.OPEN_MOTOR_DATA))
            displayDataWriteOpen(intent.getByteArrayExtra(BluetoothLeService.CLOSE_MOTOR_DATA))
            setSensorsDataThreadFlag(intent.getBooleanExtra(BluetoothLeService.SENSORS_DATA_THREAD_FLAG, true))
          }
      }
    }
  }
  private fun displayData(data: ByteArray?) {
    if (data != null){
      if (castUnsignedCharToInt(data[0]) != 0x01) {
        if (data.size == 3) {
          dataSens1 = castUnsignedCharToInt(data[1])
          dataSens2 = castUnsignedCharToInt(data[2])
        } else if (data.size == 6) {
          dataSens1 = castUnsignedCharToInt(data[1])
          dataSens2 = castUnsignedCharToInt(data[2])
          gcVer     = castUnsignedCharToInt(data[3])
          bmsVer    = castUnsignedCharToInt(data[4])
          sensVer   = castUnsignedCharToInt(data[5])
        }
      } else {
        globalSemaphore = false
      }
    }
  }
  private fun displayDataWriteOpen(data: ByteArray?) {
    if (data != null) {
      if (data[0].toInt() == 1){ state = 1 }
      if (data[0].toInt() == 0){ state = 2 }
    }
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

  /**
   * show badge with delay
   * @param position
   */
  private fun showBadge(position: Int) {
    mainactivity_navi.postDelayed({
      val model = mainactivity_navi.models[position]
      mainactivity_navi.postDelayed({ model.showBadge() }, 100)
    }, 200)
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

    val intent = intent
    mDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME)
    mDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS)
    mDeviceType = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_TYPE)
    initUI()

    // Sets up UI references.
    mGattServicesList = findViewById(R.id.gatt_services_list)
    mConnectView = findViewById(R.id.connect_view)
    mDisconnectView = findViewById(R.id.disconnect_view)

    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

    RxUpdateMainEvent.getInstance().observable
        .compose(bindToLifecycle())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { flag ->
          if (!flag) showBadge(0)
          mSectionsPagerAdapter.notifyDataSetChanged()
        }

    val worker = Thread {
      while (true) {
        val task: Runnable = queue.get()
        task.run()
      }
    }
    worker.start()
  }
  private fun initUI() {
    if ( mDeviceType.equals("FESTO_A") ) {
      mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
      mainactivity_viewpager.adapter = mSectionsPagerAdapter
      mainactivity_navi.setViewPager(mainactivity_viewpager, 1)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
    } else {
      mSectionsPagerAdapter2 = SectionsPagerAdapterMonograb(supportFragmentManager)
      mainactivity_viewpager.adapter = mSectionsPagerAdapter2
      mainactivity_navi.setViewPager(mainactivity_viewpager, 0)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
    }
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
    shutdown_current_sb.isEnabled = enabled
    start_up_step_sb.isEnabled = enabled
    dead_zone_sb.isEnabled = enabled
    brake_motor_sb.isEnabled = enabled
    correlator_noise_threshold_1_sb.isEnabled = enabled
    correlator_noise_threshold_2_sb.isEnabled = enabled
    sensorsDataThreadFlag = enabled
    if ( mDeviceType.equals("FESTO_A") ) {
      runReadData()
//      TODO включить после теста записи
    } else {
      startSubscribeSensorsDataThread()
    }
//    startChangeStateThread()
  }

  fun bleCommandConnector(byteArray: ByteArray?, Command: String, typeCommand: String, register: Int) {
    if ( mDeviceType.equals("FESTO_A") ) {
      val length = byteArray!!.size + 2
      val sendByteMassive = ByteArray(length + 2)
      sendByteMassive[0] = 0x01
      sendByteMassive[1] = length.toByte()
      when (register) {
        0 -> {
          sendByteMassive[2] = 0x00
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
        1 -> {
          sendByteMassive[2] = 0x01
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
        3 -> {
          sendByteMassive[2] = 0x03
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
        4 -> {
          sendByteMassive[2] = 0x04
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
        5 -> {
          sendByteMassive[2] = 0x05
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
        6 -> {
          sendByteMassive[2] = 0x06
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = byteArray[1]
          sendByteMassive[5] = crcCalc(sendByteMassive, 4)
        }
        7 -> {
          sendByteMassive[2] = 0x07
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = byteArray[1]
          sendByteMassive[5] = crcCalc(sendByteMassive, 4)
        }
        10 -> {
          sendByteMassive[2] = 10.toByte()
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
        11 -> {
          sendByteMassive[2] = 11.toByte()
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = byteArray[1]
          sendByteMassive[5] = byteArray[2]
          sendByteMassive[6] = crcCalc(sendByteMassive, 4)
        }
        12 -> {
          sendByteMassive[2] = 12.toByte()
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = byteArray[1]
          sendByteMassive[5] = byteArray[2]
          sendByteMassive[6] = byteArray[3]
          sendByteMassive[7] = crcCalc(sendByteMassive, 4)
        }
        13 -> {
          sendByteMassive[2] = 13.toByte()
          sendByteMassive[3] = byteArray[0]
          sendByteMassive[4] = crcCalc(sendByteMassive, 4)
        }
      }
      bleCommand(sendByteMassive, FESTO_A_CHARACTERISTIC, WRITE_WR)
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
          Thread.sleep(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) {
        }
      }
    }
    subscribeThread?.start()
  }


  fun runWriteData() { getWriteData().let { queue.put(it) } }
  open fun getWriteData(): Runnable { return Runnable { writeData() } }
  private fun writeData() {
    var i = 10

    while (i != 0) {
      if (globalSemaphore) {
        bleCommand(byteArrayOf(0x01, 0x02, 0x03), FESTO_A_CHARACTERISTIC, WRITE_WR)
        i--
      }
      try {
        Thread.sleep(50)
      } catch (ignored: Exception) {}
    }
  }

  private fun runReadData() { getReadData().let { queue.put(it) } }
  open fun getReadData(): Runnable { return Runnable { readData() } }
  private fun readData() {
    while (readDataFlag) {
        bleCommand(null, FESTO_A_CHARACTERISTIC, READ)
      try {
        Thread.sleep(1)
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

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MainActivity> {
    override fun createFromParcel(parcel: Parcel): MainActivity {
      return MainActivity(parcel)
    }

    override fun newArray(size: Int): Array<MainActivity?> {
      return arrayOfNulls(size)
    }
  }

  fun openFragment(numberGesture: Int) {
    val dialog = CustomDialogFragment()
    mNumberGesture = numberGesture
    dialog.show(supportFragmentManager, "custom dialog")
  }
  override fun initializeUI() {}

  open fun crcCalc(data: ByteArray, count: Int): Byte {
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
}
