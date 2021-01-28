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
import me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main.CustomDialogFragment
import me.romans.motorica.new_electronic_by_Rodeon.utils.NavigationUtils
import me.romans.motorica.new_electronic_by_Rodeon.viewTypes.MainActivityView
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

//import com.an
//import com.androidexception.andexalertdialog.AndExAlertDialog
//import com.androidexception.andexalertdialog.AndExAlertDialogListener


@RequirePresenter(MainPresenter::class)
class MainActivity() : BaseActivity<MainPresenter, MainActivityView>(), MainActivityView, Parcelable {

  private var sensorsDataThreadFlag: Boolean = false
  private var nAdapter: NfcAdapter? = null
  private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter
  private var mDeviceName: String? = null
  private var mDeviceAddress: String? = null
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
  private var state = 0
  private var subscribeThread: Thread? = null
  private var moveThread: Thread? = null
  private var mNumberGesture = 0


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
            displayData(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA)) //вывод на график данных из характеристики показаний пульса
            displayDataWriteOpen(intent.getByteArrayExtra(BluetoothLeService.OPEN_MOTOR_DATA))
            displayDataWriteOpen(intent.getByteArrayExtra(BluetoothLeService.CLOSE_MOTOR_DATA))
            setSensorsDataThreadFlag(intent.getBooleanExtra(BluetoothLeService.SENSORS_DATA_THREAD_FLAG, true))
          }
      }
    }
  }

  constructor(parcel: Parcel) : this() {
    sensorsDataThreadFlag = parcel.readByte() != 0.toByte()
    mDeviceName = parcel.readString()
    mDeviceAddress = parcel.readString()
    mConnected = parcel.readByte() != 0.toByte()
    mNotifyCharacteristic = parcel.readParcelable(BluetoothGattCharacteristic::class.java.classLoader)
    mCharacteristic = parcel.readParcelable(BluetoothGattCharacteristic::class.java.classLoader)
    dataSens1 = parcel.readInt()
    dataSens2 = parcel.readInt()
    state = parcel.readInt()
  }

  private fun displayData(data: ByteArray?) {
    if (data != null) {
      dataSens1 = castUnsignedCharToInt(data[1])
      dataSens2 = castUnsignedCharToInt(data[2])
    }
  }
  private fun displayDataWriteOpen(data: ByteArray?) {
    if (data != null) {
      if (data[0].toInt() == 1){ state = 1 }
      if (data[0].toInt() == 0){ state = 2 }
      System.err.println("open data[0]=" + data[0] + " data[1]=" + data[1])
    }
  }

  private fun castUnsignedCharToInt(Ubyte: Byte): Int {
    var cast = Ubyte.toInt()
    if (cast < 0) {
      cast += 256
    }
    return cast
  }

  private fun clearUI() {
    mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
    enableInterface(false)
  }

//  /**
//   * show badge with delay
//   * @param position
//   */
//  private fun showBadge(position: Int) {
//    mainactivity_navi.postDelayed({
//      val model = mainactivity_navi.models[position]
//      mainactivity_navi.postDelayed({ model.showBadge() }, 100)
//    }, 200)
//  }

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

    // Sets up UI references.
    mGattServicesList = findViewById(R.id.gatt_services_list)
    mConnectView = findViewById(R.id.connect_view)
    mDisconnectView = findViewById(R.id.disconnect_view)

    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

    RxUpdateMainEvent.getInstance().observable
        .compose(bindToLifecycle())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
//          if (!flag) showBadge(0)
          mSectionsPagerAdapter.notifyDataSetChanged()
        }
  }
  override fun initializeUI() {
    mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
    mainactivity_viewpager.adapter = mSectionsPagerAdapter
    mainactivity_viewpager.offscreenPageLimit = 4
//    NavigationUtils.setComponents(baseContext, mainactivity_viewpager, mainactivity_navi)
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
    startSubscribeSensorsDataThread()
//    startChangeStateThread()
  }

  fun bleCommand(byteArray: ByteArray?, Command: String, typeCommand: String){
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
    System.err.println("bleCommand")
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
    parcel.writeByte(if (mConnected) 1 else 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      parcel.writeParcelable(mNotifyCharacteristic, flags)
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

}
