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

@file:Suppress("SameParameterValue")

package me.start.motorica.new_electronic_by_Rodeon.ui.activities.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.*
import android.nfc.NfcAdapter
import android.os.*
import android.view.View
import android.view.WindowManager
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
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
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.presenters.MainPresenter
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapter
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterMonograb
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterMonograbWithAdvancedSettings
import me.start.motorica.new_electronic_by_Rodeon.ui.adapters.SectionsPagerAdapterWithAdvancedSettings
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.*
import me.start.motorica.new_electronic_by_Rodeon.utils.NavigationUtils
import me.start.motorica.new_electronic_by_Rodeon.viewTypes.MainActivityView
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.experimental.xor

@Suppress("SameParameterValue", "SameParameterValue", "DEPRECATION")
@RequirePresenter(MainPresenter::class)
open class MainActivity() : BaseActivity<MainPresenter, MainActivityView>(), MainActivityView, Parcelable {

  private var sensorsDataThreadFlag: Boolean = true
  var reconnectThreadFlag: Boolean = false
  private var reconnectThread: Thread? = null
  private var mScanning = false
  private var mBluetoothAdapter: BluetoothAdapter? = null

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
  private var askAboutUpdate: Boolean = true
  private var progressUpdate: Int = 0

  private var state = 0
  private var subscribeThread: Thread? = null
  private var mNumberGesture = 0
  // 3D
  var firstRead = true
  private var speedFinger = 0
  // Очередь для задачь работы с BLE
  private val queue = me.start.motorica.new_electronic_by_Rodeon.services.receivers.BlockingQueue()
  private var readDataFlag = true
  private var globalSemaphore = false // флаг, который преостанавливает отправку новой команды, пока ответ на предыдущую не пришёл
  private var endFlag = false
  //  private var showAdvancedSettings = false
  private var swapOpenCloseButton = false
  var setReverseNum = 0
  var setOneChannelNum = 0
  var calibrationDialogOpen: Boolean = false
  var percentSynchronize = 0

  private  var countCommand: AtomicInteger = AtomicInteger()
  private var actionState = READ
  var savingSettingsWhenModified = true//продакшн false
  var lockWriteBeforeFirstRead = false //продакшн true    переменная, необходимая для ожидания первого пришедшего ответа от устройства на
  var lockChangeTelemetryNumber = true //продакшн true    переменная, для разового изменения серийника телеметрии
  private var enableInterfaceStatus: Boolean = false
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
      if (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)
              || mDeviceType!!.contains(DEVICE_TYPE_4))
      {} else {
        mainactivity_navi.visibility = View.GONE
      }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
      mBluetoothLeService = null
    }
  }

  private var gestureTable: Array<Array<Array<Int>>> = Array(7) { Array(2) { Array(6) { 0 } } }
  private var byteEnabledGesture: Byte = 1 // байт по маске показывающий единицами, какие из жестов сконфигурированы и доступны для использования
  private var byteActiveGesture: Byte = 0 // номер активного в данный момент жеста 0-7
  private var byteSideHand: Byte = 0 //  0-left 1-right
  var calibrationStage: Int = 0 // состояния калибровки протеза 0-не откалиброван  1-калибруется  2-откалиброван  |  для запуска калибровки пишем !0
  var telemetryNumber: String = "" // состояния калибровки протеза 0-не откалиброван  1-калибруется  2-откалиброван  |  для запуска калибровки пишем !0
  private var firstShowPreloaderCalibration: Boolean = true // нужна для одиночного показа уведомления о начале калибровки
  private var firstHidePreloaderCalibration: Boolean = true // нужна для скрытия уведомления о начале калибровки
  private lateinit var dialog: DialogFragment

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
//          mConnected = true
//          mConnectView!!.visibility = View.VISIBLE
//          mDisconnectView!!.visibility = View.GONE
          System.err.println("DeviceControlActivity------->   момент индикации коннекта")
          Toast.makeText(context, "подключение установлено к $mDeviceAddress", Toast.LENGTH_SHORT).show()
          reconnectThreadFlag = false
          invalidateOptionsMenu()
        }
        BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
          //disconnected state
          mConnected = false
          endFlag = true
          mConnectView!!.visibility = View.GONE
          mDisconnectView!!.visibility = View.VISIBLE
          System.err.println("DeviceControlActivity------->   момент индикации дисконнекта")
          invalidateOptionsMenu()
          clearUI()
          percentSynchronize = 0

          if(!reconnectThreadFlag && !mScanning){
            reconnectThreadFlag = true
            reconnectThread()
            System.err.println("scanLeDevice------->  запуск сканирования из ACTION_GATT_DISCONNECTED")
          }
        }
        BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
          System.err.println("DeviceControlActivity------->   ACTION_GATT_SERVICES_DISCOVERED")
          mConnected = true
          mConnectView!!.visibility = View.VISIBLE
          mDisconnectView!!.visibility = View.GONE
          if (mBluetoothLeService != null) {
            displayGattServices(mBluetoothLeService!!.supportedGattServices)
          }
        }

        BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
          if ((mDeviceType!!.contains(EXTRAS_DEVICE_TYPE)) || (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2)) || (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3))) { // новая схема обработки данных
            displayData(intent.getByteArrayExtra(BluetoothLeService.FESTO_A_DATA))
            intent.getStringExtra(BluetoothLeService.ACTION_STATE)?.let { setActionState(it) }
//              System.err.println("попадаем сюда")
          } else {
            if (mDeviceType!!.contains(DEVICE_TYPE_4)) {
              //TODO прописать новую функцию обработки пришедших данных
              if(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA_NEW) != null) displayDataNew(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA_NEW))
              if(intent.getByteArrayExtra(BluetoothLeService.SENS_VERSION_NEW_DATA) != null) displayDataSensAndBMSVersionNew(intent.getByteArrayExtra(BluetoothLeService.SENS_VERSION_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.OPEN_THRESHOLD_NEW_DATA) != null) displayDataOpenThresholdNew(intent.getByteArrayExtra(BluetoothLeService.OPEN_THRESHOLD_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.CLOSE_THRESHOLD_NEW_DATA) != null) displayDataCloseThresholdNew(intent.getByteArrayExtra(BluetoothLeService.CLOSE_THRESHOLD_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SENS_OPTIONS_NEW_DATA) != null) displayDataSensOptionsNew(intent.getByteArrayExtra(BluetoothLeService.SENS_OPTIONS_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SET_GESTURE_NEW_DATA) != null) displayDataSetGestureNew(intent.getByteArrayExtra(BluetoothLeService.SET_GESTURE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SET_REVERSE_NEW_DATA) != null) displayDataSetReverseNew(intent.getByteArrayExtra(BluetoothLeService.SET_REVERSE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.ADD_GESTURE_NEW_DATA) != null) displayDataAddGestureNew(intent.getByteArrayExtra(BluetoothLeService.ADD_GESTURE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.TELEMETRY_NUMBER_NEW_DATA) != null) displayDataTelemetryNumberNew(intent.getByteArrayExtra(BluetoothLeService.TELEMETRY_NUMBER_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.CALIBRATION_NEW_DATA) != null) {
                intent.getStringExtra(BluetoothLeService.ACTION_STATE)?.let { setActionState(it) }
                displayDataCalibrationNew(intent.getByteArrayExtra(BluetoothLeService.CALIBRATION_NEW_DATA))
              }
              if(intent.getByteArrayExtra(BluetoothLeService.SET_ONE_CHANNEL_NEW_DATA) != null) displayDataSetOneChannelNew(intent.getByteArrayExtra(BluetoothLeService.SET_ONE_CHANNEL_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.STATUS_CALIBRATION_NEW_DATA) != null) displayDataStatusCalibrationNew(intent.getByteArrayExtra(BluetoothLeService.STATUS_CALIBRATION_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SHUTDOWN_CURRENT_NEW_DATA) != null) displayDataShutdownCurrentNew(intent.getByteArrayExtra(BluetoothLeService.SHUTDOWN_CURRENT_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.DRIVER_VERSION_NEW_DATA) != null) displayDataDriverVersionNew(intent.getByteArrayExtra(BluetoothLeService.DRIVER_VERSION_NEW_DATA))
            } else {
              displayData(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA))
            }
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
//      System.err.println("BluetoothLeService-------------> прошли первый иф ")
//      System.err.println("============================================")
//      for (bite in data) {
//        System.err.println("BluetoothLeService-------------> байт: $bite  size: ${data.size}")
//      }
      if (castUnsignedCharToInt(data[0]) != 0xAA) {
//        System.err.println("BluetoothLeService-------------> прошли второй иф")
//        System.err.println("data.size: " + data.size)
        if (data.size == 3) {
//          System.err.println("mDeviceAddress-------------> прошли третий иф. Распарсили нотификацию")
          if (castUnsignedCharToInt(data[0]) == 1) {
//            System.err.println("mDeviceAddress-------------> штатный режим работы")
          } else if (castUnsignedCharToInt(data[0]) == 2) {
            if (askAboutUpdate) {
              openFragmentQuestion()
              askAboutUpdate = false
            }
//            System.err.println("mDeviceAddress-------------> вывести сообщение о готовности обновления")
          } else if (castUnsignedCharToInt(data[0]) in 3..102) {
            progressUpdate = (castUnsignedCharToInt(data[0]) - 2)
//            System.err.println("mDeviceAddress-------------> процент обновления  " + (castUnsignedCharToInt(data[0])-2)  + "%")
          }
          dataSens1 = castUnsignedCharToInt(data[1])
          dataSens2 = castUnsignedCharToInt(data[2])
          savingSettingsWhenModified = true
        } else if (data.size == 10) {
          if (castUnsignedCharToInt(data[0]) == 1) {
//            System.err.println("mDeviceAddress-------------> штатный режим работы")
          } else if (castUnsignedCharToInt(data[0]) == 2) {
            if (askAboutUpdate) {
              openFragmentQuestion()
              askAboutUpdate = false
            }
//            System.err.println("mDeviceAddress-------------> вывести сообщение о готовности обновления")
          } else if (castUnsignedCharToInt(data[0]) in 3..102) {
            progressUpdate = (castUnsignedCharToInt(data[0]) - 2)
//            System.err.println("mDeviceAddress-------------> процент обновления  " + (castUnsignedCharToInt(data[0])-2) + "%")
          }
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
        if(countCommand.get() > 0) {
          countCommand.get().dec()
          System.err.println("Decrement counter: ${countCommand.get()}")
        }
        if (countCommand.get() == 0) {
          globalSemaphore = false
          readDataFlag = true
          runReadData()
        }
      }
    }
  }
  private fun displayDataNew(data: ByteArray?) {
    if (data != null) {
        if (data.size == 2) {
          dataSens1 = castUnsignedCharToInt(data[0])
          dataSens2 = castUnsignedCharToInt(data[1])
          calibrationDialogOpen = false
          savingSettingsWhenModified = true
        }
      lockWriteBeforeFirstRead = false
    }
  }
  private fun displayDataSensAndBMSVersionNew(data: ByteArray?) {
    if (data != null) {
      saveInt(mDeviceAddress + PreferenceKeys.SENS_NUM, castUnsignedCharToInt(data[0]))
      saveInt(mDeviceAddress + PreferenceKeys.BMS_NUM, 100)
      globalSemaphore = true
    }
  }
  private fun displayDataOpenThresholdNew(data: ByteArray?) {
    if (data != null) {
      saveInt(mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, castUnsignedCharToInt(data[0]))
      globalSemaphore = true
      System.err.println("---> Принятые данные порога: " + data[0])
    }
  }
  private fun displayDataCloseThresholdNew(data: ByteArray?) {
    if (data != null) {
      saveInt(mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, castUnsignedCharToInt(data[0]))
      globalSemaphore = true
    }
  }
  private fun displayDataSensOptionsNew(data: ByteArray?) {
    if (data != null) {
      if (data.size > 13) {
        saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, castUnsignedCharToInt(data[0]))
        saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, castUnsignedCharToInt(data[13]))
      }
      globalSemaphore = true
    }
  }
  private fun displayDataSetGestureNew(data: ByteArray?) {
    if (data != null) {
      saveInt(mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, castUnsignedCharToInt(data[0]) + 1)
      System.err.println("---> Принятые данные активного жеста: " + (data[0] + 1))
      globalSemaphore = true
    }
  }
  private fun displayDataSetReverseNew(data: ByteArray?) {
    if (data != null) {
      setReverseNum = castUnsignedCharToInt(data[0])
      globalSemaphore = true
    }
  }
  private fun displayDataAddGestureNew(data: ByteArray?) {
    if (data != null) {
      System.err.println("Данные data.size = " + data.size)
      if (data.size == 87) {
        for (i in 0 until 7) {
          for (j in 0 until 2) {
            for (k in 0 until 6) {
              gestureTable[i][j][k] = castUnsignedCharToInt(data[i * 12 + j * 6 + k])
              if(k == 4) { gestureTable[i][j][k] = ((88 - castUnsignedCharToInt(data[i * 12 + j * 6 + k])).toFloat()/100*91).toInt()-52 }
              if(k == 5) { gestureTable[i][j][k] = (( castUnsignedCharToInt(data[i * 12 + j * 6 + k])).toFloat()/100*90).toInt() }
            }
          }
        }
        byteEnabledGesture = castUnsignedCharToInt(data[84]).toByte()
        byteActiveGesture = castUnsignedCharToInt(data[85]).toByte()

        saveGestureState()
      }

      for (i in 0 until 7) {
        System.err.println("Данные жеста №$i")
        for (j in 0 until 2) {
          System.err.println("Данные схвата №$j")
          for (k in 0 until 6) {
            System.err.println("Данные пальца №$k   Данные:" + gestureTable[i][j][k])
          }
        }
      }
      System.err.println("Данные byteEnabledGesture   Данные:$byteEnabledGesture")
      System.err.println("Данные byteActiveGesture   Данные:$byteActiveGesture")
      globalSemaphore = true
    }
  }
  private fun displayDataTelemetryNumberNew(data: ByteArray?) {
    if (data != null) {
      telemetryNumber = ""
      for (i in data.indices) {
        telemetryNumber += data[i].toChar()
      }
      System.err.println("Принятые данные телеметрии: $telemetryNumber")
      globalSemaphore = true
    }
  }
  private fun displayDataSetOneChannelNew(data: ByteArray?) {
    if (data != null) {
      setOneChannelNum = castUnsignedCharToInt(data[0])
      globalSemaphore = true
    }
  }
  private fun displayDataCalibrationNew(data: ByteArray?) {
    if (data != null) {
      if (actionState.equals(READ)) {
          calibrationStage = castUnsignedCharToInt(data[0])
          System.err.println("---> чтение глобальной калибровки: $calibrationStage")
      }
      if (actionState.equals(WRITE)) {
        calibrationStage = castUnsignedCharToInt(data[0])
        if (calibrationStage == 9 || calibrationStage == 10) {// 9 и 10 - это числа отправляемые для калибровки правой и левой руки соответственно
//          openFragmentInfoCalibration()
          RxUpdateMainEvent.getInstance().updateCalibrationStatus(true)
        }
        System.err.println("---> запись глобальной калибровки: $calibrationStage")
      }
      globalSemaphore = true
    }
  }
  private fun displayDataStatusCalibrationNew(data: ByteArray?) {
    if (data != null) {
      var statusCalibration = ""
      for (i in data.indices) {
        statusCalibration += "  "+data[i]

//        if (data[i].toInt() == 6) Toast.makeText(this, "Протез откалиброван!", Toast.LENGTH_LONG).show() //если палец слишком сильно затянут
//        if (data[i].toInt() == 5) Toast.makeText(this, "Палец №$i слишком сильно затянут", Toast.LENGTH_LONG).show() //если палец слишком сильно затянут
//        if (data[i].toInt() == 4) Toast.makeText(this, "Палец №$i прокручивается", Toast.LENGTH_LONG).show() //если палец прокручивается
//        if (data[i].toInt() == 3) Toast.makeText(this, "На пальце №$i отключен энкодер", Toast.LENGTH_LONG).show() //если на пальце отключен энкодер
//        if (data[i].toInt() == 2) Toast.makeText(this, "На пальце №$i отключен мотор", Toast.LENGTH_LONG).show() //если на пальце отключен мотор
      }
      Toast.makeText(this, "Статус калибровки: $statusCalibration", Toast.LENGTH_LONG).show()
      saveInt(mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 0)
      globalSemaphore = true
    }
  }
  private fun displayDataShutdownCurrentNew(data: ByteArray?) {
    if (data != null) {
      for (i in data.indices) {
        System.err.println("Принятые данные состояния токов: " + data[i] + "  " + mDeviceAddress + "SHUTDOWN_CURRENT_NUM_$i")
        saveInt(mDeviceAddress + "SHUTDOWN_CURRENT_NUM_" + (i + 1), castUnsignedCharToInt(data[i]))
      }
      globalSemaphore = true
    }
  }
  @SuppressLint("SetTextI18n")
  private fun displayDataDriverVersionNew(data: ByteArray?) {
    if (data != null) {
      var driverVersion = ""
      for (i in data.indices) {
        if (i > 0) {
          driverVersion += data[i].toChar()
        }
      }
      //TODO
      driver_tv?.text = resources.getString(R.string.driver) +driverVersion + "v"
      System.err.println("Принятые данные версии прошивки: $driverVersion ${data.size}")
      globalSemaphore = true
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

    // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
    // BluetoothAdapter through BluetoothManager.
    val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    mBluetoothAdapter = bluetoothManager.adapter
    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())


    locate = Locale.getDefault().toString()
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    getWindow().navigationBarColor = resources.getColor(R.color.colorPrimary)
    mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

    val intent = intent
    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
    presenter.preferenceManager.putString(PreferenceKeys.DEVICE_NAME, mDeviceName.toString())
    presenter.preferenceManager.putString(PreferenceKeys.DEVICE_ADDR, mDeviceAddress.toString())
    saveText(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, mDeviceAddress.toString())
    mDeviceType = intent.getStringExtra(EXTRAS_DEVICE_TYPE)
    System.err.println("mDeviceAddress: $mDeviceAddress")

    // Sets up UI references.
    mGattServicesList = findViewById(R.id.gatt_services_list)
    mConnectView = findViewById(R.id.connect_view)
    mDisconnectView = findViewById(R.id.disconnect_view)

    RxUpdateMainEvent.getInstance().gestureStateObservable
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameters ->
              bleCommandConnector(byteArrayOf((parameters.gestureNumber).toByte(), parameters.openStage.toByte(), parameters.closeStage.toByte(), parameters.state.toByte()), ADD_GESTURE, WRITE, 12)
            }
    RxUpdateMainEvent.getInstance().gestureStateWithEncodersObservable
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameters ->
              System.err.println("Prishedshie parametri: ${parameters.openStage1.toByte()}")
              if (parameters.state == 1) {
                runWriteData(byteArrayOf(parameters.openStage1.toByte(), parameters.openStage2.toByte(), parameters.openStage3.toByte(),
                        parameters.openStage4.toByte(), parameters.openStage5.toByte(), parameters.openStage6.toByte()), MOVE_ALL_FINGERS_NEW, WRITE)
              } else {
                runWriteData(byteArrayOf(parameters.closeStage1.toByte(), parameters.closeStage2.toByte(), parameters.closeStage3.toByte(),
                        parameters.closeStage4.toByte(), parameters.closeStage5.toByte(), parameters.closeStage6.toByte()), MOVE_ALL_FINGERS_NEW, WRITE)
              }
              if (parameters.withChangeGesture) {
                System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage1: ${parameters.openStage1}    closeStage1: ${parameters.closeStage1}")
                System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage2: ${parameters.openStage2}    closeStage2: ${parameters.closeStage2}")
                System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage3: ${parameters.openStage3}    closeStage3: ${parameters.closeStage3}")
                System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage4: ${parameters.openStage4}    closeStage4: ${parameters.closeStage4}")
                System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage5: ${parameters.openStage5}    closeStage5: ${parameters.closeStage5}")
                System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage6: ${parameters.openStage6}    closeStage6: ${parameters.closeStage6}")

                runWriteData(byteArrayOf((parameters.gestureNumber).toByte(),
                        parameters.openStage1.toByte(), parameters.openStage2.toByte(), parameters.openStage3.toByte(),
                        parameters.openStage4.toByte(), parameters.openStage5.toByte(), parameters.openStage6.toByte(),
                        parameters.closeStage1.toByte(), parameters.closeStage2.toByte(), parameters.closeStage3.toByte(),
                        parameters.closeStage4.toByte(), parameters.closeStage5.toByte(), parameters.closeStage6.toByte()), CHANGE_GESTURE_NEW, WRITE)
              }
            }
    RxUpdateMainEvent.getInstance().fingerSpeedObservable
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { speed ->
              System.err.println(" MainActivity -----> change gripper. fingerSpeed = $speed")
              speedFinger = speed
            }
    RxUpdateMainEvent.getInstance().calibratingStatusObservable
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
              openFragmentInfoCalibration()
            }

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
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)
              || mDeviceType!!.contains(DEVICE_TYPE_4)) {
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
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)
              || mDeviceType!!.contains(DEVICE_TYPE_4)) {
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
      if (mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)
              || mDeviceType!!.contains(DEVICE_TYPE_4)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapterWithAdvancedSettings(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 1)
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograbWithAdvancedSettings(supportFragmentManager)
        mainactivity_viewpager.adapter = mSectionsPagerAdapter
        mainactivity_navi.setViewPager(mainactivity_viewpager, 0)
      }
    } else {
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)
              || mDeviceType!!.contains(DEVICE_TYPE_4)) {
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
    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    if (!mBluetoothAdapter!!.isEnabled) {
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

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
    if (mBluetoothLeService != null) {
      mDeviceName = presenter.preferenceManager.getString(PreferenceKeys.DEVICE_NAME, DEVICE_NAME)
      mDeviceAddress = presenter.preferenceManager.getString(PreferenceKeys.DEVICE_ADDR, "7F:D6:3D:68:62:28")
    }
  }
  override fun onPause() {
    super.onPause()
    endFlag = true

  }
  override fun onDestroy() {
    super.onDestroy()
    if (mBluetoothLeService != null) {
      unbindService(mServiceConnection)
      mBluetoothLeService = null
    }
    readDataFlag = false
//    sensorsDataThreadFlag = false
    endFlag = true
    if (mScanning) { mBluetoothAdapter!!.stopLeScan(mLeScanCallback) }
  }
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  // Demonstrates how to iterate through the supported GATT Services/Characteristics.
  // In this sample, we populate the data structure that is bound to the ExpandableListView
  // on the UI.
  private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
    System.err.println("DeviceControlActivity------->   момент начала выстраивания списка параметров")
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
        System.err.println("------->   ХАРАКТЕРИСТИКА: $uuid")
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
    if (mScanning) { scanLeDevice(false) }
    enableInterface(true)
  }
  private fun enableInterface(enabled: Boolean) {
    enableInterfaceStatus = enabled
    close_btn.isEnabled = enabled
    open_btn.isEnabled = enabled
    calibration_btn.isEnabled = enabled
    thresholds_blocking_sw.isEnabled = enabled
    correlator_noise_threshold_1_sb.isEnabled = enabled
    correlator_noise_threshold_2_sb.isEnabled = enabled
    if(enabled) {
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3) || mDeviceType!!.contains(DEVICE_TYPE_4)) {
        gesture_1_btn?.isEnabled = enabled
        gesture_2_btn?.isEnabled = enabled
        gesture_3_btn?.isEnabled = enabled
        gesture_4_btn?.isEnabled = enabled
        gesture_5_btn?.isEnabled = enabled
        gesture_6_btn?.isEnabled = enabled
        gesture_7_btn?.isEnabled = enabled
        gesture_8_btn?.isEnabled = enabled
        gesture_settings_2_btn?.isEnabled = enabled
        gesture_settings_3_btn?.isEnabled = enabled
        gesture_settings_4_btn?.isEnabled = enabled
        gesture_settings_5_btn?.isEnabled = enabled
        gesture_settings_6_btn?.isEnabled = enabled
        gesture_settings_7_btn?.isEnabled = enabled
        gesture_settings_8_btn?.isEnabled = enabled
        if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
          swap_sensors_sw?.isEnabled = enabled
          swap_open_close_sw?.isEnabled = enabled
          single_channel_control_sw?.isEnabled = enabled
          reset_to_factory_settings_btn?.isEnabled = enabled
//          calibration_btn?.isEnabled = enabled
          get_setup_btn?.isEnabled = enabled
          set_setup_btn?.isEnabled = enabled
          shutdown_current_sb?.isEnabled = enabled
        }
      }
      sensorsDataThreadFlag = enabled
      if ( mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
        runReadData()
      } else {
        if (mDeviceType!!.contains(DEVICE_TYPE_4)) {
          runStart()
        } else {
          startSubscribeSensorsDataThread()
        }
      }
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
        17 -> { //настройки переключения жестов
          sendByteMassive[3] = 17.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = byteArray[1]
          sendByteMassive[6] = byteArray[2]
          sendByteMassive[7] = byteArray[3]
          sendByteMassive[8] = byteArray[4]
          sendByteMassive[9] = crcCalc(sendByteMassive)
        }
        18 -> { //подтверждение перепрошивки
          sendByteMassive[3] = 18.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
      }
      readDataFlag = false
      runWriteData(sendByteMassive, FESTO_A_CHARACTERISTIC, WRITE_WR)
    } else {
        bleCommand(byteArray, Command, typeCommand)
        System.err.println("Отправили команду! Чтение")
    }
  }
  fun bleCommand(byteArray: ByteArray?, Command: String, typeCommand: String){
    if (mBluetoothLeService != null) {
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
                System.err.println("bleCommand Write Characteristic")
              }
            }

            if (typeCommand == READ){
              if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                mBluetoothLeService?.readCharacteristic(mCharacteristic)
                System.err.println("------->   bleCommand Read Characteristic:  $Command")
              }
            }

            if (typeCommand == NOTIFY){
              if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                mNotifyCharacteristic = mCharacteristic
                mBluetoothLeService!!.setCharacteristicNotification(
                        mCharacteristic, true)
                System.err.println("bleCommand Notify Characteristic")
              }
            }
          }
        }
      }
    }

  }
  private fun reconnectThread() {
    System.err.println("reconnectThread started")
    var j = 1
    reconnectThread = Thread {
      while (reconnectThreadFlag) {
        runOnUiThread {
          if(j % 5 == 0) {
            reconnectThreadFlag = false
            scanLeDevice(true)
            System.err.println("DeviceControlActivity------->   Переподключение cо сканированием №$j")
          } else {
            reconnect()
            System.err.println("DeviceControlActivity------->   Переподключение без сканирования №$j")
          }
          j++
        }
        try {
          Thread.sleep(RECONNECT_BLE_PERIOD.toLong())
        } catch (ignored: Exception) { }
      }
    }
    reconnectThread?.start()
  }

  private fun startSubscribeSensorsDataThread() {
    subscribeThread = Thread {
      while (sensorsDataThreadFlag) {
        runOnUiThread {
          bleCommand(null, MIO_MEASUREMENT, NOTIFY)
          System.err.println("startSubscribeSensorsDataThread попытка подписки")
        }
        percentSynchronize = 100
        try {
          Thread.sleep(GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) { }
      }
    }
    subscribeThread?.start()
  }
  private fun startSubscribeSensorsNewDataThread() {
    subscribeThread = Thread {
      while (sensorsDataThreadFlag) {
        try {
          Thread.sleep(500)
        } catch (ignored: Exception) {}
        runOnUiThread {
          bleCommand(null, MIO_MEASUREMENT_NEW, NOTIFY)
//          System.err.println("---> startSubscribeSensorsNewDataThread попытка подписки")
        }
        try {
          Thread.sleep(GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) { }
      }
    }
    subscribeThread?.start()
  }

  /**
   * Запуск задачи чтения параметров экрана графиков
   */
  private fun runStart() { getStart()?.let { queue.put(it) } }
  open fun getStart(): Runnable? { return Runnable { readStart() } }
  private fun readStart() {
    val info = "------->   Чтение порогов и версий"
    var count = 0
    var state = 0 // переключается здесь в потоке
    endFlag = false // меняется на последней стадии машины состояний, служит для немедленного прекращния операции
    globalSemaphore = true // меняется по приходу ответа от подключаемого уст-ва

    while (!endFlag) {
      if (globalSemaphore) {
        when (state) {
          // ПРАВИЛЬНАЯ ЦЕПЬ ЗАПРОСОВ
          0 -> {
            showToast("Старт потока запросов начальных параметров")
            System.err.println("$info = 0")
            bleCommand(READ_REGISTER, SENS_VERSION_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 5
            state = 1
          }
          1 -> {
            System.err.println("$info = 1")
            bleCommand(READ_REGISTER, OPEN_THRESHOLD_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 15
            state = 2
          }
          2 -> {
            System.err.println("$info = 2")
            bleCommand(READ_REGISTER, CLOSE_THRESHOLD_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 25
            state = 3
          }
          3 -> {
            System.err.println("$info = 3")
            bleCommand(READ_REGISTER, SENS_OPTIONS_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 35
            state = 4
          }
          4 -> {
            System.err.println("$info = 4")
            bleCommand(READ_REGISTER, SET_REVERSE_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 45
            state = 5
          }
          5 -> {
            System.err.println("$info = 5")
            bleCommand(READ_REGISTER, SET_ONE_CHANNEL_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 55
            state = 6
          }
          6 -> {
            System.err.println("$info = 6")
            bleCommand(READ_REGISTER, ADD_GESTURE_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 65
            state = 7  //11 пропустить калибровку //7 - выполнить
          }

          7 -> {
            System.err.println("$info = 7")
            bleCommand(READ_REGISTER, CALIBRATION_NEW, READ) //TODO тут
            globalSemaphore = false
            percentSynchronize = 75
            state = 8
          }
          8 -> {
            System.err.println("$info = 8")
            if (calibrationStage == 0) {
              state = 9 //9   //TODO вернуть калибровку
            } else {
              if (calibrationStage == 6) {
                state = 14
              } else {
                if (calibrationStage == 2) {
                  state = 10
                } else {
                  if (calibrationStage == 3) {
                    state = 11
                  } else {
                    if (calibrationStage == 4) {
                      state = 12
                    } else {
                      if (calibrationStage == 5) {
                        state = 13
                      }
                    }
                  }
                }
              }
            }
          }

          9 -> {
            System.err.println("$info = 9")
            openFragmentInfoNotCalibration()
            state = 14
          }
          10 -> {
            System.err.println("$info = 10")
            showToast("В протезе отключён двигатель одной или нескольких степеней свободы!")
            state = 14
          }
          11 -> {
            System.err.println("$info = 11")
            showToast("В протезе отключён энкодер одной или нескольких степеней свободы!")
            state = 14
          }
          12 -> {
            System.err.println("$info = 12")
            showToast("В протезе нет энкодеров одного или нескольких степеней свободы!")
            state = 14
          }
          13 -> {
            System.err.println("$info = 13")
            showToast("В протезе сильно заьянута одна или несколько степеней свободы!")
            state = 14
          }
          14 -> {
            System.err.println("$info = 14")
            bleCommand(READ_REGISTER, SHUTDOWN_CURRENT_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 85
            state = 15
          }
          15 -> {
            System.err.println("$info = 15")
            bleCommand(READ_REGISTER, SET_GESTURE_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 95
            state = 16
          }
          16 -> {
            System.err.println("$info = 16")
            bleCommand(READ_REGISTER, DRIVER_VERSION_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 100
            state = 0
            endFlag = true
            startSubscribeSensorsNewDataThread()
          }

        }
        count = 0
      } else {
        count++
        if (count == 100000) {
          endFlag = mConnected
          state = 0
          count = 0
        }
      }
      try {
        Thread.sleep(10)
      } catch (ignored: Exception) {
      }
    }
  }

  fun runWriteData(byteArray: ByteArray?, Command: String, typeCommand: String) { getWriteData(byteArray, Command, typeCommand).let { queue.put(it) } }
  open fun getWriteData(byteArray: ByteArray?, Command: String, typeCommand: String): Runnable { return Runnable { writeData(byteArray, Command, typeCommand) } }
  private fun writeData(byteArray: ByteArray?, Command: String, typeCommand: String) {
    try {
      Thread.sleep(200) // меньше нельзя ставить для работоспособности xiaomi 6 | samsung работает на значении 200
    } catch (ignored: Exception) {}
//    if (countCommand == 1) countCommand = 0
    bleCommand(byteArray, Command, typeCommand)
    incrementCountCommand()
    System.err.println("write counter: ${countCommand.get()}")
    try {
      Thread.sleep(100)
    } catch (ignored: Exception) {}
  }

  fun runReadDataAllCharacteristics(Command: String) {
    getReadDataAllCharacteristics(Command).let { queue.put(it) }
  }
  open fun getReadDataAllCharacteristics(Command: String): Runnable { return Runnable { readDataAllCharacteristics(Command) } }
  private fun readDataAllCharacteristics(Command: String) {
      System.err.println("тык")
      bleCommand(null, Command, READ)
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
      System.err.println("read counter: ${countCommand.get()}")
      bleCommand(null, FESTO_A_CHARACTERISTIC, READ)
      percentSynchronize = 100
      try {
        Thread.sleep(100)
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
    dialog = CustomDialogFragment()
    mNumberGesture = numberGesture
    dialog.show(supportFragmentManager, "custom dialog")
  }
  private fun openFragmentQuestion() {
    dialog = CustomUpdateDialogFragment()
    dialog.show(supportFragmentManager, "custom update dialog")
  }
  fun openFragmentInfoUpdate() {
    dialog = CustomInfoUpdateDialogFragment()
    dialog.show(supportFragmentManager, "update dialog")
  }
  private fun openFragmentInfoCalibration() {
    dialog = CustomInfoCalibrationDialogFragment()
    calibrationDialogOpen = true
    dialog.show(supportFragmentManager, "calibration dialog")
  }
  private fun openFragmentInfoNotCalibration() {
    val dialog = CustomInfoNotCalibratedDialogFragment()
    dialog.show(supportFragmentManager, "update dialog")
  }
  fun getProgressUpdate(): Int {
    return progressUpdate
  }
  fun showToast(massage: String) {
    runOnUiThread {
      Toast.makeText(this, massage, Toast.LENGTH_SHORT).show()
    }
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

  private fun saveGestureState() {
    for (i in 0 until 7) {
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + (i + 2), gestureTable[i][0][0])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + (i + 2), gestureTable[i][0][1])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + (i + 2), gestureTable[i][0][2])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + (i + 2), gestureTable[i][0][3])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + (i + 2), gestureTable[i][0][4])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + (i + 2), gestureTable[i][0][5])

      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + (i + 2), gestureTable[i][1][0])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + (i + 2), gestureTable[i][1][1])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + (i + 2), gestureTable[i][1][2])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + (i + 2), gestureTable[i][1][3])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + (i + 2), gestureTable[i][1][4])
      saveInt(mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + (i + 2), gestureTable[i][1][5])
    }
  }
  internal fun saveInt(key: String, variable: Int) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putInt(key, variable)
    editor.apply()
  }
  private fun saveText(key: String, text: String) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putString(key, text)
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
  private fun incrementCountCommand() {
    countCommand.get().inc() //++
    System.err.println("countCommand.get().inc() counter: ${countCommand.get()}")
  }
  fun offGesturesUIBeforeConnection () {
    gesture_1_btn?.isEnabled = false
    gesture_2_btn?.isEnabled = false
    gesture_3_btn?.isEnabled = false
    gesture_4_btn?.isEnabled = false
    gesture_5_btn?.isEnabled = false
    gesture_6_btn?.isEnabled = false
    gesture_7_btn?.isEnabled = false
    gesture_8_btn?.isEnabled = false
    gesture_settings_2_btn?.isEnabled = false
    gesture_settings_3_btn?.isEnabled = false
    gesture_settings_4_btn?.isEnabled = false
    gesture_settings_5_btn?.isEnabled = false
    gesture_settings_6_btn?.isEnabled = false
    gesture_settings_7_btn?.isEnabled = false
    gesture_settings_8_btn?.isEnabled = false
  }
  fun offSensorsUIBeforeConnection () {
    close_btn?.isEnabled = false
    open_btn?.isEnabled = false
    thresholds_blocking_sw?.isEnabled = false
    correlator_noise_threshold_1_sb?.isEnabled = false
    correlator_noise_threshold_2_sb?.isEnabled = false
  }


  private fun reconnect () {
    //полное завершение сеанса связи и создание нового в onResume
    if (mBluetoothLeService != null) {
      unbindService(mServiceConnection)
      mBluetoothLeService = null
    }

    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

    //BLE
    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    if (mBluetoothLeService != null) {
      mBluetoothLeService!!.connect(mDeviceAddress)
    }
  }
  /**
   * Запуск/остановка сканирования эфира на наличие BLE устройств
   * @param enable - true запуск | false остановка
   */
  private fun scanLeDevice(enable: Boolean) {
    if (enable) {
      mScanning = true
      mBluetoothAdapter!!.startLeScan(mLeScanCallback)
      System.err.println("DeviceControlActivity------->   startLeScan")
    } else {
      mScanning = false
      mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
      System.err.println("DeviceControlActivity------->   stopLeScan")
    }
  }
  // Device scan callback.
  private val mLeScanCallback = LeScanCallback { device, _, _ ->
    runOnUiThread {
      if (device.name != null) {
        System.err.println("------->   ===============найден девайс: " + device.address + "==============")
        System.err.println("------->   preferenceManager подключаемся к DEVICE_NAME = $mDeviceAddress")
        if (device.address == mDeviceAddress) {
          System.err.println("------->   ==========это нужный нам девайс $device==============")
          mDeviceAddress = device.toString()
          scanLeDevice(false)
          reconnect()
        }
      }
    }
  }
}

