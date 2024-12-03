@file:Suppress("SameParameterValue")
package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.nfc.NfcAdapter
import android.os.*
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.ActivityMainBinding
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.*
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingersEncoderValue
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys.GESTURE_OPEN_DELAY_FINGER
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.MainPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.services.DataTransferToService
import com.bailout.stickk.new_electronic_by_Rodeon.services.MyService
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.Decorator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.Navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.TypeGuides
import com.bailout.stickk.new_electronic_by_Rodeon.ui.adapters.*
import com.bailout.stickk.new_electronic_by_Rodeon.ui.dialogs.*
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account.customerServiceFragment.AccountFragmentCustomerService
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account.mainFragment.AccountFragmentMain
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account.prosthesisInformationFragment.AccountFragmentProsthesisInformation
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.help.*
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.ArcanoidFragment
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.NeuralFragment
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main.SecretSettingsFragment
import com.bailout.stickk.new_electronic_by_Rodeon.utils.BlockingQueue
import com.bailout.stickk.new_electronic_by_Rodeon.utils.NameUtil
import com.bailout.stickk.new_electronic_by_Rodeon.utils.NavigationUtils
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.MainActivityView
import com.bailout.stickk.scan.view.ScanActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import lib.kingja.switchbutton.SwitchMultiButton
import online.devliving.passcodeview.PasscodeView
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.xor


@SuppressLint("MissingPermission")
@Suppress("SameParameterValue", "SameParameterValue", "DEPRECATION")
@RequirePresenter(MainPresenter::class)
class MainActivity() : BaseActivity<MainPresenter, MainActivityView>(), MainActivityView, Parcelable,
  Navigator {

  var startScrollForGesturesFragment: (()->Unit)? = null
  var startScrollForChartFragment: ((enabledGraph: Boolean)->Unit)? = null
  var startScrollForAdvancedSettingsFragment: (()->Unit)? = null
  private var sensorsDataThreadFlag: Boolean = true
  var reconnectThreadFlag: Boolean = false
  private var reconnectThread: Thread? = null
  private var mScanning = false
  private var mDisconnected = false
  private var mBluetoothAdapter: BluetoothAdapter? = null

  var mDeviceName: String? = null
  var mDeviceAddress: String? = null
  var mDeviceType: String? = null
  var driverVersionS: String? = null
  var driverVersionINDY: Int? = null
  private var mBluetoothLeService: BluetoothLeService? = null
  private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
  private var mGattServicesList: ExpandableListView? = null
  private var mConnectView: View? = null
  private var mDisconnectView: View? = null
  private var mConnected = false
  private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
  private var mCharacteristic: BluetoothGattCharacteristic? = null
  private var openChNum = 0x00
  private var closeChNum = 0x00
  private var dataSens1 = 0x00
  private var dataSensPrevious1 = 0x00
  private var dataSens2 = 0x00
  private var dataSensPrevious2 = 0x00
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
  private val queue = BlockingQueue()
  private var readDataFlag = true
  private var globalSemaphore = false // флаг, который преостанавливает отправку новой команды, пока ответ на предыдущую не пришёл
  private var endFlag = false
  //  private var showAdvancedSettings = false
  private var swapOpenCloseButton = false
  var setOneChannelNum = 0
  var calibrationDialogOpen: Boolean = false
  var percentSynchronize = 0
  private var timer: CountDownTimer? = null

  private  var countCommand: AtomicInteger = AtomicInteger()
  private var actionState = READ
  var savingSettingsWhenModified = false//продакшн false
  var lockWriteBeforeFirstRead = true //продакшн true    переменная, необходимая для ожидания первого пришедшего ответа от устройства на
  var lockChangeSerialNumber = true //продакшн true    переменная, для разового изменения серийника телеметрии
  private var enableInterfaceStatus: Boolean = false
  // отправленный запрос чтения. Если не ожидать её, то поток чтения не перезамускается
  internal var locate = ""

  private val listName = "NAME"
  private val listUUID = "UUID"
  var firstActivateSetScaleDialog = false
  private var scaleProstheses = 5
  private var oldNumGesture = 0
  @Volatile
  private var expectedIdCommand = "not set"
  private var expectedReceiveConfirmation = 0
  private var timerResendCommandDLE: CountDownTimer? = null
  var stage = "not set"
  var testingConnection = false
  private var countdownToUpdate = COUNT_ATTEMPTS_TO_UPDATE
  private var debugScreenIsOpen = false
  private var decorator: Decorator? = null

  private lateinit var binding: ActivityMainBinding

  // Code to manage Service lifecycle.
  private var mServiceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
      System.err.println("Check ServiceConnection onServiceConnected()")
      mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
      if (!mBluetoothLeService?.initialize()!!) {
        Timber.e("Unable to initialize Bluetooth")
        finish()
      }

      // Automatically connects to the device upon successful start-up initialization.
      //TODO закомментить быстрый вход после завершения экспериментов
      System.err.println("connectedDeviceAddress $mDeviceAddress")
      mBluetoothLeService?.connect(mDeviceAddress)
      if (mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
        || mDeviceType!!.contains(DEVICE_TYPE_BT05)
        || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_H)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_X))
      {} else {
        binding.mainactivityNavi.visibility = View.GONE
      }
    }


    override fun onServiceDisconnected(componentName: ComponentName) {
      System.err.println("Check ServiceConnection onServiceDisconnected()")
      mBluetoothLeService = null
    }
  }

  private var gestureTable: Array<Array<Array<Int>>> = Array(7) { Array(2) { Array(6) { 0 } } }
  private var gestureTableBig: Array<Array<Array<Int>>> = Array(13) { Array(2) { Array(6) { 0 } } }
  private var byteEnabledGesture: Byte = 1 // байт по маске показывающий единицами, какие из жестов сконфигурированы и доступны для использования
  var calibrationStage: Int = 0 // состояния калибровки протеза 0-не откалиброван  1-калибруется  2-откалиброван  |  для запуска калибровки пишем !0
  var serialNumber: String = ""
  private lateinit var dialog: DialogFragment
  private var mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      when {
        BluetoothLeService.ACTION_GATT_CONNECTED == action -> {
          System.err.println("Check BroadcastReceiver() ACTION_GATT_CONNECTED")
          System.err.println("DeviceControlActivity-------> $mDeviceAddress   момент индикации коннекта ${this.hashCode()}")
          reconnectThreadFlag = false
          invalidateOptionsMenu()
        }
        BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
          System.err.println("Check BroadcastReceiver() ACTION_GATT_DISCONNECTED")
          //disconnected state
          mConnected = false
          endFlag = true
          mConnectView!!.visibility = View.GONE
          mDisconnectView!!.visibility = View.VISIBLE
          System.err.println("DeviceControlActivity-------> $mDeviceAddress  момент индикации дисконнекта ${this.hashCode()}")
          invalidateOptionsMenu()
          clearUI()
          percentSynchronize = 0
          updateUIChart(1)

          if(!reconnectThreadFlag && !mScanning && !mDisconnected){
            reconnectThreadFlag = true
            reconnectThread()
            System.err.println("DeviceControlActivity------->  запуск сканирования из ACTION_GATT_DISCONNECTED")
          }
        }
        BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
          System.err.println("Check BroadcastReceiver() ACTION_GATT_SERVICES_DISCOVERED")
          Toast.makeText(context, "подключение установлено к $mDeviceName", Toast.LENGTH_SHORT).show()
          System.err.println("DeviceControlActivity-------> $mDeviceAddress  ACTION_GATT_SERVICES_DISCOVERED ${this.hashCode()}")
          mConnected = true
          mConnectView!!.visibility = View.VISIBLE
          mDisconnectView!!.visibility = View.GONE
          if (mBluetoothLeService != null) {
            displayGattServices(mBluetoothLeService!!.supportedGattServices)
          }
        }
        BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {

          if ((mDeviceType!!.contains(DEVICE_TYPE_FEST_A))
            || (mDeviceType!!.contains(DEVICE_TYPE_BT05))
            || (mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE))) { // новая схема обработки данных
            System.err.println("mDeviceType ACTION_DATA_AVAILABLE A:$mDeviceType.")
            displayData(intent.getByteArrayExtra(BluetoothLeService.FESTO_A_DATA))
            intent.getStringExtra(BluetoothLeService.ACTION_STATE)?.let { setActionState(it) }
          } else {
            if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H) || mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
//              if(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA_NEW) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. MIO_DATA_NEW")
//              if(intent.getByteArrayExtra(BluetoothLeService.SENS_VERSION_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SENS_VERSION_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.OPEN_THRESHOLD_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. OPEN_THRESHOLD_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.CLOSE_THRESHOLD_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. CLOSE_THRESHOLD_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.SENS_OPTIONS_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SENS_OPTIONS_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.SET_GESTURE_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SET_GESTURE_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.SET_REVERSE_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SET_REVERSE_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.ADD_GESTURE_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. ADD_GESTURE_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.SERIAL_NUMBER_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SERIAL_NUMBER_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.CALIBRATION_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. CALIBRATION_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.SET_ONE_CHANNEL_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SET_ONE_CHANNEL_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.STATUS_CALIBRATION_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. STATUS_CALIBRATION_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.CHANGE_GESTURE_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. CHANGE_GESTURE_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.SHUTDOWN_CURRENT_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. SHUTDOWN_CURRENT_NEW_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.ROTATION_GESTURE_NEW_VM_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. ROTATION_GESTURE_NEW_VM_DATA")
//              if(intent.getByteArrayExtra(BluetoothLeService.DRIVER_VERSION_NEW_DATA) != null) System.err.println("mDeviceType ACTION_DATA_AVAILABLE X:$mDeviceType. DRIVER_VERSION_NEW_DATA")


              if(intent.getByteArrayExtra(BluetoothLeService.MIO_DATA_NEW) != null) displayDataNew(intent.getByteArrayExtra(
                BluetoothLeService.MIO_DATA_NEW))
              if(intent.getByteArrayExtra(BluetoothLeService.SENS_VERSION_NEW_DATA) != null) displayDataSensAndBMSVersionNew(intent.getByteArrayExtra(
                BluetoothLeService.SENS_VERSION_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.OPEN_THRESHOLD_NEW_DATA) != null) displayDataOpenThresholdNew(intent.getByteArrayExtra(
                BluetoothLeService.OPEN_THRESHOLD_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.CLOSE_THRESHOLD_NEW_DATA) != null) displayDataCloseThresholdNew(intent.getByteArrayExtra(
                BluetoothLeService.CLOSE_THRESHOLD_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SENS_OPTIONS_NEW_DATA) != null) displayDataSensOptionsNew(intent.getByteArrayExtra(
                BluetoothLeService.SENS_OPTIONS_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SET_GESTURE_NEW_DATA) != null) displayDataSetGestureNew(intent.getByteArrayExtra(
                BluetoothLeService.SET_GESTURE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SET_REVERSE_NEW_DATA) != null) displayDataSetReverseNew(intent.getByteArrayExtra(
                BluetoothLeService.SET_REVERSE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.ADD_GESTURE_NEW_DATA) != null) displayDataAddGestureNew(intent.getByteArrayExtra(
                BluetoothLeService.ADD_GESTURE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SERIAL_NUMBER_NEW_DATA) != null) displayDataSerialNumberNew(intent.getByteArrayExtra(
                BluetoothLeService.SERIAL_NUMBER_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.CALIBRATION_NEW_DATA) != null) {
                intent.getStringExtra(BluetoothLeService.ACTION_STATE)?.let { setActionState(it) }
                displayDataCalibrationNew(intent.getByteArrayExtra(BluetoothLeService.CALIBRATION_NEW_DATA))
              }
              if(intent.getByteArrayExtra(BluetoothLeService.SET_ONE_CHANNEL_NEW_DATA) != null) displayDataSetOneChannelNew(intent.getByteArrayExtra(
                BluetoothLeService.SET_ONE_CHANNEL_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.STATUS_CALIBRATION_NEW_DATA) != null) displayDataStatusCalibrationNew(intent.getByteArrayExtra(
                BluetoothLeService.STATUS_CALIBRATION_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.CHANGE_GESTURE_NEW_DATA) != null) displayDataChangeGestureNew(intent.getByteArrayExtra(
                BluetoothLeService.CHANGE_GESTURE_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.SHUTDOWN_CURRENT_NEW_DATA) != null) displayDataShutdownCurrentNew(intent.getByteArrayExtra(
                BluetoothLeService.SHUTDOWN_CURRENT_NEW_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.ROTATION_GESTURE_NEW_VM_DATA) != null) displayDataRotationGesture(intent.getByteArrayExtra(
                BluetoothLeService.ROTATION_GESTURE_NEW_VM_DATA))
              if(intent.getByteArrayExtra(BluetoothLeService.DRIVER_VERSION_NEW_DATA) != null) displayDataDriverVersionNew(intent.getByteArrayExtra(
                BluetoothLeService.DRIVER_VERSION_NEW_DATA))
            } else {
              System.err.println("mDeviceType ACTION_DATA_AVAILABLE I:$mDeviceType.")
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
      System.err.println("============================================ notify")
      for (i in data.indices) {
        System.err.println("BluetoothLeService------------->  size: ${data.size}    $i - ${data[i]} notify")
      }


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
        } else if (data.size >= 10) {
//          System.err.println("BluetoothLeService------------->  data.size >= 10")
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
          driverVersionINDY = castUnsignedCharToInt(data[3])
          if (castUnsignedCharToInt(data[3]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.DRIVER_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.DRIVER_NUM, castUnsignedCharToInt(data[3]))
            RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
            updateUIChart(2)
            updateUIAccountMain()
          }
          if (castUnsignedCharToInt(data[4]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.BMS_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.BMS_NUM, castUnsignedCharToInt(data[4]))
            updateUIChart(3)
            updateUIAccountMain()
          }
          if (castUnsignedCharToInt(data[5]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.SENS_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.SENS_NUM, castUnsignedCharToInt(data[5]))
            updateUIChart(4)
            updateUIAccountMain()
          }
          if (castUnsignedCharToInt(data[6]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.OPEN_CH_NUM, 0)) {
            openChNum = castUnsignedCharToInt(data[6])
            saveInt(mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, castUnsignedCharToInt(data[6]))
            updateUIChart(5)
          }
          if (castUnsignedCharToInt(data[7]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.CLOSE_CH_NUM, 0)) {
            closeChNum = castUnsignedCharToInt(data[7])
            saveInt(mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, castUnsignedCharToInt(data[7]))
            updateUIChart(6)
          }
          if (castUnsignedCharToInt(data[8]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, castUnsignedCharToInt(data[8]))
            updateUIChart(7)
          }
          if (castUnsignedCharToInt(data[9]) != mSettings!!.getInt(mDeviceAddress +PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, 0)) {
            saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, castUnsignedCharToInt(data[9]))
            updateUIChart(8)
          }


          if (data.size >= 12) {
            // оптимизация для уменьшения лагов на маломощных телефонах
            // (проводим сложные расчёты в n раз реже)
            countdownToUpdate -= 1
            if (countdownToUpdate == 0) {
              if (castUnsignedCharToInt(data[10]) != mSettings!!.getInt(
                  mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM,
                  80
                )
              ) {
                saveInt(
                  mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM,
                  castUnsignedCharToInt(data[10])
                )
                System.err.println("SHUTDOWN_CURRENT_NUM приём = ${castUnsignedCharToInt(data[10])}")
                RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
                RxUpdateMainEvent.getInstance().updateUIChart(enableInterfaceStatus)
              }

              if (((castUnsignedCharToInt(data[11]) shr 0 and 0b00000001) == 1) != mSettings!!.getBoolean(
                  mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM,
                  false
                )
              ) {
                saveBool(
                  mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM,
                  ((castUnsignedCharToInt(data[11]) shr 0 and 0b00000001) == 1)
                )
                updateUIChart(9)
              }

              if (((castUnsignedCharToInt(data[11]) shr 1 and 0b00000001) == 1) != mSettings!!.getBoolean(
                  mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM,
                  false
                )
              ) {
                saveBool(
                  mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM,
                  ((castUnsignedCharToInt(data[11]) shr 1 and 0b00000001) == 1)
                )
                RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
              }

              if ((castUnsignedCharToInt(data[11]) shr 7 and 0b00000001) != 1) {
                if (!firstActivateSetScaleDialog) {
//                  System.err.println("BluetoothLeService-------------> ПОКАЗЫВАЕМ ДИАЛОГ ВЫБОРА РАЗМЕРА ПРОТЕЗА")
                  if (mDeviceName != DEVICE_TYPE_BT05) {
                    showChangeSizeDialog()
                    firstActivateSetScaleDialog = true
                  }
                }
              }

              if ((castUnsignedCharToInt(data[11]) shr 2 and 0b00000011) != mSettings!!.getInt(
                  mDeviceAddress + PreferenceKeys.SET_SCALE,
                  0
                )
              ) {
                saveInt(
                  mDeviceAddress + PreferenceKeys.SET_SCALE,
                  (castUnsignedCharToInt(data[11]) shr 2 and 0b00000011)
                )
                updateUIChart(9)
              }
              countdownToUpdate = COUNT_ATTEMPTS_TO_UPDATE
            }
          }

          if (data.size >= 14) {
            if (castUnsignedCharToInt(data[12]) != mSettings!!.getInt(
                mDeviceAddress + PreferenceKeys.AUTOCALIBRATION_MODE, 0)) {
              saveInt(
                mDeviceAddress + PreferenceKeys.AUTOCALIBRATION_MODE,
                (castUnsignedCharToInt(data[12]))
              )
              RxUpdateMainEvent.getInstance().updateUISecretSettings(true)
            }
            if (castUnsignedCharToInt(data[13]) != mSettings!!.getInt(
                mDeviceAddress + PreferenceKeys.GESTURE_TYPE, 0)) {
              saveInt(
                mDeviceAddress + PreferenceKeys.GESTURE_TYPE,
                (castUnsignedCharToInt(data[13]))
              )
              System.err.println("GESTURE_TYPE ${castUnsignedCharToInt(data[13])}  key= ${mDeviceAddress + PreferenceKeys.GESTURE_TYPE}")
              RxUpdateMainEvent.getInstance().updateUISecretSettings(true)
            }
          }

          System.err.println("data.size было ${data.size}")
          if (data.size >= 15) {
            System.err.println("SET_MODE_EMG_SENSORS было = ${mSettings!!.getInt(mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 0)}")
            if (castUnsignedCharToInt(data[14]) != mSettings!!.getInt(
                mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 0)) {
              saveInt(
                mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,
                (castUnsignedCharToInt(data[14]))
              )
              RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
              System.err.println("SET_MODE_EMG_SENSORS приём = ${castUnsignedCharToInt(data[14])}")
            }
          }
          if (data.size >= 18) {
            if (castUnsignedCharToInt(data[17]) != mSettings!!.getInt(
                mDeviceAddress + PreferenceKeys.MIN_SHUTDOWN_CURRENT_NUM, 0)) {
              saveInt(
                mDeviceAddress + PreferenceKeys.MIN_SHUTDOWN_CURRENT_NUM,
                (castUnsignedCharToInt(data[17]))
              )
              RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
              System.err.println("MIN_SHUTDOWN_CURRENT_NUM приём = ${castUnsignedCharToInt(data[17])}")
            }
          }
        }

//        включить для трансляции данных в игру
//        if (dataSensPrevious1 != dataSens1 || dataSensPrevious2 != dataSens2) {
//          dataSensPrevious1 = dataSens1
//          dataSensPrevious2 = dataSens2
//
//          val transferIntent = Intent(this, DataTransferToService::class.java)
//          transferIntent.putExtra("sensor_level_1", dataSens1)
//          transferIntent.putExtra("sensor_level_2", dataSens2)
//          transferIntent.putExtra("open_ch_num", openChNum)
//          transferIntent.putExtra("close_ch_num", closeChNum)
//          startService(transferIntent)
//        }

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
      } else  {
        if (data.size >= 3) {
          dataSens1 = castUnsignedCharToInt(data[0])
          dataSens2 = castUnsignedCharToInt(data[1])
          if (oldNumGesture != castUnsignedCharToInt(data[2])+1) {
            RxUpdateMainEvent.getInstance().updateUIGestures(castUnsignedCharToInt(data[2])+1)
            saveInt(mDeviceAddress+PreferenceKeys.ACTIVE_GESTURE_NUM, castUnsignedCharToInt(data[2])+1)
//            System.err.println("gonka main displayDataNew $enableInterfaceStatus")
            updateUIChart(100)
            oldNumGesture = castUnsignedCharToInt(data[2])+1
            System.err.println("displayDataNew номер жеста ${castUnsignedCharToInt(data[2])+1}")
          }

          // оптимизация для уменьшения лагов на маломощных телефонах
          // (просчёт этой части только при открытом отладочном окне)
          if (debugScreenIsOpen) {
            if (data.size >= 10) {
              val fingersEncoderValue = FingersEncoderValue(
                castUnsignedCharToInt(data[3]),
                castUnsignedCharToInt(data[4]),
                castUnsignedCharToInt(data[5]),
                castUnsignedCharToInt(data[6]),
                (((88 - castUnsignedCharToInt(data[7])).toFloat() / 100 * 91).toInt() - 52),
                castUnsignedCharToInt(data[8])
              )
              RxUpdateMainEvent.getInstance().updateEncoders(fingersEncoderValue)
              RxUpdateMainEvent.getInstance().updateEncodersError(castUnsignedCharToInt(data[9]))
            }
          }


          if (data.size >= 12) {
              val receiveIdCommand = "%02x".format(castUnsignedCharToInt(data[11])) + "%02x".format(castUnsignedCharToInt(data[10]))
//              System.err.println("данные IdCommand: $receiveIdCommand")
              if (expectedIdCommand == receiveIdCommand) {
//                System.err.println("startSendCommand id $receiveIdCommand  receive")
                expectedReceiveConfirmation = 2
                expectedIdCommand = "not set"
              }
            }
        }
      }

//      включить для трансляции данных в игру
//      if (dataSensPrevious1 != dataSens1 || dataSensPrevious2 != dataSens2) {
//        dataSensPrevious1 = dataSens1
//        dataSensPrevious2 = dataSens2
//
//        val transferIntent = Intent(this, DataTransferToService::class.java)
//        transferIntent.putExtra("sensor_level_1", dataSens1)
//        transferIntent.putExtra("sensor_level_2", dataSens2)
//        transferIntent.putExtra("open_ch_num", openChNum)
//        transferIntent.putExtra("close_ch_num", closeChNum)
//        startService(transferIntent)
//      }

      calibrationDialogOpen = false
      savingSettingsWhenModified = true
      lockWriteBeforeFirstRead = false
    }
  }
  private fun displayDataSensAndBMSVersionNew(data: ByteArray?) {
    if (data != null) {
      saveInt(mDeviceAddress + PreferenceKeys.SENS_NUM, castUnsignedCharToInt(data[0]))
      saveInt(mDeviceAddress + PreferenceKeys.BMS_NUM, 100)
      globalSemaphore = true
      System.err.println("---> Принятые данные SensAndBMSVersion" + data[0])
    }
  }
  private fun displayDataOpenThresholdNew(data: ByteArray?) {
    if (data != null) {
      openChNum = castUnsignedCharToInt(data[0])
      saveInt(mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, castUnsignedCharToInt(data[0]))
      globalSemaphore = true
      updateUIChart(10)
      System.err.println("---> Принятые данные порога: " + data[0])
    }
  }
  private fun displayDataCloseThresholdNew(data: ByteArray?) {
    if (data != null) {
      closeChNum = castUnsignedCharToInt(data[0])
      saveInt(mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, castUnsignedCharToInt(data[0]))
      globalSemaphore = true
      updateUIChart(11)
    }
  }
  private fun displayDataSensOptionsNew(data: ByteArray?) {
    if (data != null) {
      if (data.size > 13) {
        saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, castUnsignedCharToInt(data[0]))
        saveInt(mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, castUnsignedCharToInt(data[13]))
      }
      globalSemaphore = true
      updateUIChart(11)
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
      saveBool(mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, ((castUnsignedCharToInt(data[0]) and 0b00000001) ==  1))
      if (data.size >= 5 ) {
        System.err.println("Принятые данные состояния переключения жестов      data.size=${data.size}   NUM_ACTIVE_GESTURES=${castUnsignedCharToInt(data[1])}")
        System.err.println("Принятые данные состояния переключения жестов      data.size=${data.size}   SET_MODE_PROSTHESIS=${castUnsignedCharToInt(data[2])}")
        System.err.println("Принятые данные состояния переключения жестов      data.size=${data.size}   MAX_STAND_CYCLES=${(castUnsignedCharToInt(data[3]) + 256*castUnsignedCharToInt(data[4]))}")
        saveInt(mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, castUnsignedCharToInt(data[1]))
        saveInt(mDeviceAddress + PreferenceKeys.SET_MODE_PROSTHESIS, castUnsignedCharToInt(data[2]))
        saveInt(mDeviceAddress + PreferenceKeys.MAX_STAND_CYCLES, (castUnsignedCharToInt(data[3]) + 256*castUnsignedCharToInt(data[4])))
      }


      RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
      globalSemaphore = true
      updateUIChart(12)
    }
  }
  private fun displayDataAddGestureNew(data: ByteArray?) {
    if (data != null) {
      System.err.println("Данные data.size = " + data.size)
      //обработка для 8 жестов
      if (data.size == 87) {
        for (i in 0 until 7) {
          for (j in 0 until 2) {
            for (k in 0 until 6) {
              if (data[i * 12 + j * 6 + k] < 0) { data[i * 12 + j * 6 + k] = 0}
              if (data[i * 12 + j * 6 + k] > 100) { data[i * 12 + j * 6 + k] = 100}
              gestureTable[i][j][k] = castUnsignedCharToInt(data[i * 12 + j * 6 + k])
              if(k == 4) {
                if (data[i * 12 + j * 6 + k] < 5) { data[i * 12 + j * 6 + k] = 5}
                gestureTable[i][j][k] = ((88 - castUnsignedCharToInt(data[i * 12 + j * 6 + k])).toFloat()/100*91).toInt()-52
              }
              if(k == 5) { gestureTable[i][j][k] = (( castUnsignedCharToInt(data[i * 12 + j * 6 + k])).toFloat()/100*90).toInt() }
            }
          }
        }
        byteEnabledGesture = castUnsignedCharToInt(data[84]).toByte()

        saveGestureState()
      }

      //обработка для 14 жестов
      if (data.size == 159) {
        for (i in 0 until 13) {
          for (j in 0 until 2) {
            for (k in 0 until 6) {
              if (data[i * 12 + j * 6 + k] < 0) { data[i * 12 + j * 6 + k] = 0}
              if (data[i * 12 + j * 6 + k] > 100) { data[i * 12 + j * 6 + k] = 100}
              gestureTableBig[i][j][k] = castUnsignedCharToInt(data[i * 12 + j * 6 + k])
              if(k == 4) {
                if (data[i * 12 + j * 6 + k] < 5) { data[i * 12 + j * 6 + k] = 5}
                gestureTableBig[i][j][k] = ((88 - castUnsignedCharToInt(data[i * 12 + j * 6 + k])).toFloat()/100*91).toInt()-52
              }
              if(k == 5) { gestureTableBig[i][j][k] = (( castUnsignedCharToInt(data[i * 12 + j * 6 + k])).toFloat()/100*90).toInt() }
            }
          }
        }

        saveBigGestureState()
      }


      for (i in 0 until 13) {
        System.err.println("Данные жеста №$i  данные жестов")
        for (j in 0 until 2) {
          System.err.println("Данные схвата №$j данные жестов")
          for (k in 0 until 6) {
            System.err.println("Данные пальца №$k   Данные:" + gestureTableBig[i][j][k] + " данные жестов ")
          }
        }
      }
      System.err.println("Данные byteEnabledGesture   Данные:$byteEnabledGesture")
      globalSemaphore = true
    }
  }
  private fun displayDataSerialNumberNew(data: ByteArray?) {
    if (data != null) {
      serialNumber = ""
      for (i in data.indices) {
        serialNumber += data[i].toChar()
      }
      System.err.println("Принятые данные телеметрии: $serialNumber")
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
          RxUpdateMainEvent.getInstance().updateCalibrationStatus(true)
          saveInt(mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 1)
        }
        System.err.println("---> запись глобальной калибровки: $calibrationStage")
      }
      globalSemaphore = true
    }
  }
  private fun displayDataStatusCalibrationNew(data: ByteArray?) {
    if (data != null) {
      if (data.size == 42) {
        val statusFingers = mutableListOf<String>()
        val statusEncoderFingers = mutableListOf<Int>()
        val statusCurrentFingers = mutableListOf<Int>()
        for (i in 0..5) {
          println("------> statusCalibration: statusFingers   i : $i")
          if (data[36+i].toInt() == 6) statusFingers.add(getString(R.string.pre_status_num) + " 6")
          if (data[36+i].toInt() == 5) statusFingers.add(getString(R.string.pre_status_num) + " 5")
          if (data[36+i].toInt() == 4) statusFingers.add(getString(R.string.pre_status_num) + " 4")
          if (data[36+i].toInt() == 3) statusFingers.add(getString(R.string.pre_status_num) + " 3")
          if (data[36+i].toInt() == 2) statusFingers.add(getString(R.string.pre_status_num) + " 2")
          if (data[36+i].toInt() == 1) statusFingers.add(getString(R.string.pre_status_num) + " 1")
          if (data[36+i].toInt() == 0) statusFingers.add(getString(R.string.pre_status_num) + " 0")
        }

        for (i in 0..5) {
          println("------> statusCalibration: statusCalibration   i : $i")
          println("------> statusCalibration: statusCalibration   ++++++++++++++++++++")
          val temp: Int = (castUnsignedCharToInt(data[(i*4)])) +
                          (castUnsignedCharToInt(data[1+(i*4)]) shl 8) +
                          (castUnsignedCharToInt(data[2+(i*4)]) shl 16) +
                          (castUnsignedCharToInt(data[3+(i*4)]) shl 24)
          statusEncoderFingers.add(temp)
        }

        for (i in 0..5) {
          println("------> statusCalibration: statusCurrentFingers   i : $i")
          val temp: Int = (castUnsignedCharToInt(data[24+(i*2)])) +
                          (castUnsignedCharToInt(data[25+(i*2)])  shl 8)
          statusCurrentFingers.add(temp)
        }
        showCalibrationInfoDialog(statusFingers, statusEncoderFingers, statusCurrentFingers)
      }
      if (data.size == 6) {
        var statusCalibration = ""
        for (j in data.indices) {
          statusCalibration += data[j]
          statusCalibration += " "
        }
        Toast.makeText(this, "Статус калибровки: $statusCalibration", Toast.LENGTH_LONG).show()
      }
      saveInt(mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 0)
      globalSemaphore = true
    }
  }
  private fun displayDataChangeGestureNew(data: ByteArray?) {
    if (data != null) {
      if (data.size >= 25) {
        for (i in 13..18) {
          saveInt(GESTURE_OPEN_DELAY_FINGER+"${i-12}", castUnsignedCharToInt(data[i]))
          System.err.println("Принятые данные состояния задержек открытого состояния: " + data[i])
        }
        for (i in 19..24) {
          saveInt(GESTURE_CLOSE_DELAY_FINGER+"${i-18}", castUnsignedCharToInt(data[i]))
          System.err.println("Принятые данные состояния задержек закрытого состояния: " + data[i])
        }
      }
      saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, true)
    }
  }
  private fun displayDataShutdownCurrentNew(data: ByteArray?) {
    if (data != null) {
      for (i in data.indices) {
        System.err.println("Принятые данные состояния токов: " + data[i] + "  " + mDeviceAddress + "SHUTDOWN_CURRENT_NUM_${i+1}")
        saveInt(mDeviceAddress + "SHUTDOWN_CURRENT_NUM_" + (i + 1), castUnsignedCharToInt(data[i]))
      }
      RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
      globalSemaphore = true
    }
  }
  private fun displayDataRotationGesture(data: ByteArray?) {
    if (data != null) {
      System.err.println("Принятые данные состояния переключения жестов==================================")
//      for (i in data.indices) {
//        System.err.println("Принятые данные состояния переключения жестов: " + data[i])
//      }
      if (data.size >= 8) {
        System.err.println("Принятые данные состояния переключения жестов активация: " + data[0])
        System.err.println("Принятые данные состояния переключения жестов время: " + data[2])
        System.err.println("Принятые данные состояния переключения жестов стартовй жест: " + data[6])
        System.err.println("Принятые данные состояния переключения жестов конечный жест: " + data[7])
      }

      if (data.size >= 4) {
        if (castUnsignedCharToInt(data[0]) == 1) {
          saveBool(mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, true)
        } else {
          saveBool(mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
        }
        saveInt(mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, castUnsignedCharToInt(data[2]))

        if (data.size >= 6) {
          if (castUnsignedCharToInt(data[4]) == 1) {
            saveBool(mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, true)
          } else {
            saveBool(mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)
          }
          saveInt(mDeviceAddress + PreferenceKeys.HOLD_TO_LOCK_TIME_NUM, castUnsignedCharToInt(data[5]))
        }

        if (data.size >= 8) {
          if (castUnsignedCharToInt(data[6]) >= 14) { showToast("ошибка протеза start gesture "+castUnsignedCharToInt(data[6])) }
          else { saveInt(mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, castUnsignedCharToInt(data[6])) }
          if (castUnsignedCharToInt(data[7]) >= 14) { showToast("ошибка протеза end gesture "+castUnsignedCharToInt(data[6])) }
          else { saveInt(mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, castUnsignedCharToInt(data[7])) }
        }
      }

      RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enableInterfaceStatus)
      if (enableInterfaceStatus) {
        RxUpdateMainEvent.getInstance().updateUIGestures(100)
      } else {
        RxUpdateMainEvent.getInstance().updateUIGestures(101)
      }

      globalSemaphore = true
    }
  }
  @SuppressLint("CheckResult", "NewApi", "UnspecifiedRegisterReceiverFlag")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)
    initBaseView(this)
    //changing statusbar
    val window = this.window
    decorator = Decorator( this, window, this, binding.myMainLl)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.navigationBarColor = resources.getColor(R.color.color_primary)
    window.statusBarColor = this.resources.getColor(R.color.blue_status_bar, theme)
    mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)


    // Initializes a Bluetooth adapter.
    // For API level 18 and above, get a reference to
    // BluetoothAdapter through BluetoothManager.
    val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    mBluetoothAdapter = bluetoothManager.adapter
    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())


    locate = Locale.getDefault().toString()

    val intent = intent
    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
    presenter.preferenceManager.putString(PreferenceKeys.DEVICE_NAME, mDeviceName.toString())
    presenter.preferenceManager.putString(PreferenceKeys.DEVICE_ADDR, mDeviceAddress.toString())
    mDeviceType = intent.getStringExtra(EXTRAS_DEVICE_TYPE)
    saveText(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, mDeviceAddress.toString())
    saveText(EXTRAS_DEVICE_TYPE, mDeviceType)
    System.err.println("mDeviceAddress: $mDeviceAddress")
    saveText(PreferenceKeys.LAST_CONNECTION_MAC, mDeviceAddress)



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
        System.err.println("GripperSettingsRender--------> gestureStateWithEncodersObservable "+
                "withChangeGesture = " +parameters.withChangeGesture + "    state = " + parameters.state)
        System.err.println("Prishedshie parametri: ${parameters.openStage1.toByte()}")
        if (parameters.onlyNumberGesture) {
          if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            stage = "main activity"
            runSendCommand(
              byteArrayOf((parameters.gestureNumber).toByte()),
              CHANGE_GESTURE_NEW_VM, 50)
          } else {
            runWriteData(
              byteArrayOf((parameters.gestureNumber).toByte()),
              CHANGE_GESTURE_NEW_VM, WRITE)
          }
        } else {
          if (parameters.state == 1) {
            if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              stage = "main activity"
              runSendCommand(byteArrayOf(
                parameters.openStage1.toByte(),
                parameters.openStage2.toByte(),
                parameters.openStage3.toByte(),
                parameters.openStage4.toByte(),
                parameters.openStage5.toByte(),
                parameters.openStage6.toByte()
              ), MOVE_ALL_FINGERS_NEW_VM, 50)
            } else {
              runWriteData(
                byteArrayOf(
                  parameters.openStage1.toByte(),
                  parameters.openStage2.toByte(),
                  parameters.openStage3.toByte(),
                  parameters.openStage4.toByte(),
                  parameters.openStage5.toByte(),
                  parameters.openStage6.toByte()
                ), MOVE_ALL_FINGERS_NEW, WRITE
              )
            }
          } else {
            if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              stage = "main activity"
              runSendCommand(byteArrayOf(
                parameters.closeStage1.toByte(),
                parameters.closeStage2.toByte(),
                parameters.closeStage3.toByte(),
                parameters.closeStage4.toByte(),
                parameters.closeStage5.toByte(),
                parameters.closeStage6.toByte()
              ), MOVE_ALL_FINGERS_NEW_VM, 50)
            } else {
              runWriteData(
                byteArrayOf(
                  parameters.closeStage1.toByte(),
                  parameters.closeStage2.toByte(),
                  parameters.closeStage3.toByte(),
                  parameters.closeStage4.toByte(),
                  parameters.closeStage5.toByte(),
                  parameters.closeStage6.toByte()
                ), MOVE_ALL_FINGERS_NEW, WRITE
              )
            }
          }
          if (parameters.withChangeGesture) {
            System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage1: ${parameters.openStage1}    closeStage1: ${parameters.closeStage1}")
            System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage2: ${parameters.openStage2}    closeStage2: ${parameters.closeStage2}")
            System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage3: ${parameters.openStage3}    closeStage3: ${parameters.closeStage3}")
            System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage4: ${parameters.openStage4}    closeStage4: ${parameters.closeStage4}")
            System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage5: ${parameters.openStage5}    closeStage5: ${parameters.closeStage5}")
            System.err.println("Prishedshie s izmeneniem gesta v pamiati openStage6: ${parameters.openStage6}    closeStage6: ${parameters.closeStage6}")


            if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              System.err.println("Prishedshie s izmeneniem gesta v pamiati ++========")
              stage = "main activity"
              runSendCommand(byteArrayOf(
                (parameters.gestureNumber).toByte(),
                parameters.openStage1.toByte(),
                parameters.openStage2.toByte(),
                parameters.openStage3.toByte(),
                parameters.openStage4.toByte(),
                parameters.openStage5.toByte(),
                parameters.openStage6.toByte(),
                parameters.closeStage1.toByte(),
                parameters.closeStage2.toByte(),
                parameters.closeStage3.toByte(),
                parameters.closeStage4.toByte(),
                parameters.closeStage5.toByte(),
                parameters.closeStage6.toByte(),
                parameters.openStageDelay1.toByte(),
                parameters.openStageDelay2.toByte(),
                parameters.openStageDelay3.toByte(),
                parameters.openStageDelay4.toByte(),
                parameters.openStageDelay5.toByte(),
                parameters.openStageDelay6.toByte(),
                parameters.closeStageDelay1.toByte(),
                parameters.closeStageDelay2.toByte(),
                parameters.closeStageDelay3.toByte(),
                parameters.closeStageDelay4.toByte(),
                parameters.closeStageDelay5.toByte(),
                parameters.closeStageDelay6.toByte()
              ), CHANGE_GESTURE_NEW_VM, 50)
            } else {
              runWriteData(
                byteArrayOf(
                  (parameters.gestureNumber).toByte(),
                  parameters.openStage1.toByte(),
                  parameters.openStage2.toByte(),
                  parameters.openStage3.toByte(),
                  parameters.openStage4.toByte(),
                  parameters.openStage5.toByte(),
                  parameters.openStage6.toByte(),
                  parameters.closeStage1.toByte(),
                  parameters.closeStage2.toByte(),
                  parameters.closeStage3.toByte(),
                  parameters.closeStage4.toByte(),
                  parameters.closeStage5.toByte(),
                  parameters.closeStage6.toByte()
                ), CHANGE_GESTURE_NEW, WRITE
              )
            }
          }
        }
      }

    RxUpdateMainEvent.getInstance().readCharacteristicBLEObservable
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        runReadDataAllCharacteristics(it)
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
      .subscribe {
        openFragmentInfoCalibration()
      }
    RxUpdateMainEvent.getInstance().openSecretSettings
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        showSecretSettingsScreen()
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
  @SuppressLint("SetTextI18n")
  private fun displayDataDriverVersionNew(data: ByteArray?) {
    if (data != null) {
      var driverVersion = ""
      for (i in data.indices) {
        if (i > 0) {
          driverVersion += data[i].toChar()
        }
      }

      driverVersionS = driverVersion
      saveText(mDeviceAddress + PreferenceKeys.DRIVER_VERSION_STRING, driverVersionS)
//      System.err.println("gonka main displayDataDriverVersionNew $enableInterfaceStatus")
      updateUIChart(100)
      updateUIAccountMain()
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
  private fun updateUIChart(source: Int) {
    System.err.println("updateAllParameters updateUIChart($source) 1")
    try {
      System.err.println("updateAllParameters updateUIChart($source) 2")
      runOnUiThread{
        System.err.println("updateAllParameters updateUIChart($source) 3")
        timer?.cancel()
        timer = object : CountDownTimer(300, 1) {
          override fun onTick(millisUntilFinished: Long) {}

          override fun onFinish() {
            System.err.println("gonka main updateUIChart $enableInterfaceStatus  source:$source  $mDeviceType")
            RxUpdateMainEvent.getInstance().updateUIChart(enableInterfaceStatus)

            System.err.println("updateAllParameters updateUIChart($source) 4")
          }
        }.start()
      }
    } catch (err : Exception)  {
      System.err.println("Exception: $err")
      return
    }
  }
  private fun updateUIAccountMain() {
    try {
      runOnUiThread{
        timer?.cancel()
        timer = object : CountDownTimer(300, 1) {
          override fun onTick(millisUntilFinished: Long) {}

          override fun onFinish() {
            RxUpdateMainEvent.getInstance().updateUIAccountMain(true)
          }
        }.start()
      }
    } catch (err : Exception)  {
      System.err.println("Exception: $err")
      return
    }
  }


  override fun onResume() {
    super.onResume()
    System.err.println("Check life cycle onResume()")
    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    if (!mBluetoothAdapter!!.isEnabled) {
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      if (ActivityCompat.checkSelfPermission(
          this,
          Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
      ) { return } else {
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
      }
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
    System.err.println("Check life cycle onPause()")
    endFlag = true
  }
  override fun onDestroy() {
    super.onDestroy()
    System.err.println("Check life cycle onDestroy()")
    if (mBluetoothLeService != null) {
      unbindService(mServiceConnection)
      mBluetoothLeService = null
    }
    try {
      System.err.println("DeviceControlActivity-------> onDestroy() unregisterReceiver")
      unregisterReceiver(mGattUpdateReceiver)
    } catch (e: IllegalArgumentException) {
      showToast("Receiver не был зарегистрирован" )
    }
    readDataFlag = false
//    sensorsDataThreadFlag = false
    endFlag = true
    if (mScanning) { mBluetoothAdapter!!.stopLeScan(mLeScanCallback) }
  }
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    System.err.println("Check life cycle onNewIntent()")
    setIntent(intent)
  }


  override fun showWhiteStatusBar(show: Boolean) {
    if (show) {
      window.statusBarColor = resources.getColor(R.color.back_help_menu, theme)
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    else {
      window.statusBarColor = resources.getColor(R.color.blue_status_bar, theme)
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE
    }
  }
  override fun showGrayStatusBar(show: Boolean) {
    if (show) {
      window.statusBarColor = resources.getColor(R.color.color_primary, theme)
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    else {
      window.statusBarColor = resources.getColor(R.color.blue_status_bar, theme)
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE
    }
  }
  override fun showSecretSettingsScreen() { launchFragment(SecretSettingsFragment()) }
  override fun showHelpScreen(chartFragmentClass: ChartFragment) { launchFragment(HelpFragment(chartFragmentClass)) }
  override fun showSensorsHelpScreen(chartFragmentClass: ChartFragment) { launchFragment(SensorsFragmentHelp(chartFragmentClass)) }
  override fun showGesturesHelpScreen(chartFragmentClass: ChartFragment) { launchFragment(GestureCustomizationFragment(chartFragmentClass)) }
  override fun showHelpMonoAdvancedSettingsScreen(chartFragmentClass: ChartFragment) { launchFragment(AdvancedSettingsFragmentMono(chartFragmentClass)) }
  override fun showHelpMultyAdvancedSettingsScreen(chartFragmentClass: ChartFragment) { launchFragment(AdvancedSettingsFragmentMulty(chartFragmentClass)) }
  override fun showHowProsthesesWorksScreen() { launchFragment(HowProsthesesWorksFragment()) }
  override fun showHowProsthesesWorksMonoScreen() { launchFragment(HowProsthesesWorksMonoFragment()) }
  override fun showHowPutOnTheProsthesesSocketScreen() { launchFragment(HowToPutOnProsthesesSocketFragment()) }
  override fun showCompleteSetScreen() { launchFragment(CompleteSetFragment()) }
  override fun showChargingTheProsthesesScreen() { launchFragment(ChargingTheProsthesesFragment()) }
  override fun showProsthesesCareScreen() { launchFragment(ProsthesesCareFragment()) }
  override fun showServiceAndWarrantyScreen() { launchFragment(ServiceAndWarrantyFragment()) }
  override fun showNeuralScreen() { launchFragment(NeuralFragment()) }
  override fun showArcanoidScreen(chartFragmentClass: ChartFragment) { launchFragment(ArcanoidFragment(chartFragmentClass)) }
  override fun showAccountScreen(chartFragmentClass: ChartFragment) { launchFragment(AccountFragmentMain(chartFragmentClass)) }
  override fun showAccountCustomerServiceScreen() { launchFragment(AccountFragmentCustomerService()) }
  override fun showAccountProsthesisInformationScreen() { launchFragment(AccountFragmentProsthesisInformation()) }
  override fun getBackStackEntryCount():Int { return supportFragmentManager.backStackEntryCount }
  override fun goingBack() { onBackPressed() }
  @SuppressLint("MissingSuperCall")
//  override fun onBackPressed() {
//    System.err.println("backStackEntryCount: ${supportFragmentManager.backStackEntryCount}")
//    //эта хитрая конструкция отключает системную кнопку "назад", когда мы НЕ в меню помощи
//    if (supportFragmentManager.backStackEntryCount != 0) {
//      super.onBackPressed()
//    }
//    if (supportFragmentManager.backStackEntryCount == 0) {
//      showWhiteStatusBar(false)
//      showGrayStatusBar(false)
//    }
//  }


  private fun launchFragment(fragment: Fragment) {
    supportFragmentManager
      .beginTransaction()
      .setCustomAnimations(
        R.anim.slide_in,
        R.anim.slide_out_next,
        R.anim.slide_in_next,
        R.anim.slide_out
      )
      .addToBackStack(null)
      .replace(R.id.mainactivity_help_fcv, fragment)
      .commit()
  }

  private fun initUI() {
    if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
      if ( mDeviceType!!.contains(DEVICE_TYPE_FEST_TEST)) {
        val mSectionsPagerAdapter =  SelectionsPagerAdapterKibi(supportFragmentManager)
        binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
        binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 0)

        val myIntent = Intent(this, MyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(this, myIntent)
        } else {
          startService(myIntent)
        }
      } else {
        if (mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
          || mDeviceType!!.contains(DEVICE_TYPE_BT05)
          || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE)
          || mDeviceType!!.contains(DEVICE_TYPE_FEST_H)
          || mDeviceType!!.contains(DEVICE_TYPE_FEST_X)
        ) {
          val mSectionsPagerAdapter = SectionsPagerAdapterWithAdvancedSettings(supportFragmentManager)
          binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
          binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 1)//1//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
        } else {
          val mSectionsPagerAdapter =
            SectionsPagerAdapterMonograbWithAdvancedSettings(supportFragmentManager)
          binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
          binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 0)
        }
      }
      NavigationUtils.showAdvancedSettings = true
    } else {
      if (mDeviceType?.contains(DEVICE_TYPE_FEST_TEST) == true) {
        val mSectionsPagerAdapter =  SelectionsPagerAdapterKibi(supportFragmentManager)
        binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
        binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 0)

        val myIntent = Intent(this, MyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(this, myIntent)
        } else {
          startService(myIntent)
        }
      } else {
        if (mDeviceType?.contains(DEVICE_TYPE_FEST_A) == true
          || mDeviceType?.contains(DEVICE_TYPE_BT05) == true
          || mDeviceType?.contains(DEVICE_TYPE_MY_IPHONE) == true
          || mDeviceType?.contains(DEVICE_TYPE_FEST_H) == true
          || mDeviceType?.contains(DEVICE_TYPE_FEST_X) == true
        ) {
          val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
          binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
          binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 1)//1
        } else {
          val mSectionsPagerAdapter = SectionsPagerAdapterMonograb(supportFragmentManager)
          binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
          //здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
          binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 0)
        }
      }
    }


    binding.mainactivityViewpager.offscreenPageLimit = 3
    NavigationUtils.setComponents(baseContext, binding.mainactivityNavi)
    optimizationNavi()
  }

  private fun optimizationNavi() {
    var increment = 0
    binding.mainactivityNavi.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (increment != 0) {
          startScrollForGesturesFragment?.invoke()
          startScrollForChartFragment?.invoke(false)
          startScrollForAdvancedSettingsFragment?.invoke()
        }
        increment = 1
      }

      override fun onPageSelected(position: Int) {
        startScrollForChartFragment?.invoke(true)
      }

      override fun onPageScrollStateChanged(state: Int) {}
    })
  }


  @SuppressLint("ClickableViewAccessibility")
  fun setDecorator(guide: TypeGuides, targetView: View, rootClass: Any) {
    binding.cancelableTouchBtn.visibility = View.VISIBLE
    decorator?.showGuide(guide, targetView, rootClass)
  }
  @SuppressLint("ClickableViewAccessibility")
  fun hideDecorator() {
    decorator?.hideDecorator()
    binding.cancelableTouchBtn.visibility = View.GONE
  }

  // Demonstrates how to iterate through the supported GATT Services/Characteristics.
  // In this sample, we populate the data structure that is bound to the ExpandableListView
  // on the UI.
  private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
    System.err.println("DeviceControlActivity-------> $mDeviceAddress  момент начала выстраивания списка параметров ${mGattUpdateReceiver.hashCode()}")
    if (gattServices == null) return
    var uuid: String?
    val unknownServiceString = ("unknown_service")
    val unknownCharaString =("unknown_characteristic")
    val gattServiceData = ArrayList<HashMap<String, String?>>()
    val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
    mGattCharacteristics = ArrayList()


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
    readStartData(true)
    if (!mDeviceName!!.contains(DEVICE_TYPE_FEST_X)) {
      enableInterface(true)
    }
  }
  private fun enableInterface(enabled: Boolean) {
    if (mDeviceName!!.contains(DEVICE_TYPE_FEST_TEST)) { } else {
      enableInterfaceStatus = enabled
//      System.err.println("gonka main enableInterface $enabled")
      updateUIChart(100)
      if (mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
        || mDeviceType!!.contains(DEVICE_TYPE_BT05)
        || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_H)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_X)
      ) {
        if (enabled) {
          RxUpdateMainEvent.getInstance().updateUIGestures(100)
        } else {
          RxUpdateMainEvent.getInstance().updateUIGestures(101)
        }
      }
      if (mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4) == 1) {
        RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(enabled)
      }
    }
  }
  fun readStartData(enabled: Boolean) {
    sensorsDataThreadFlag = enabled
    if (mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
      || mDeviceType!!.contains(DEVICE_TYPE_BT05)
      || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE)
    ) {
      runReadData()
    } else {
      if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
        runStart()
      } else {
        if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
          runStartVM()
        } else {
          startSubscribeSensorsDataThread()
        }
      }
    }
  }

  fun bleCommandConnector(byteArray: ByteArray?, Command: String, typeCommand: String, register: Int) {
    if ( mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
      || mDeviceType!!.contains(DEVICE_TYPE_BT05)
      || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE))  {
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
        19 -> { //отправка автокалибровки (но мы сюда никогда не попадём, это просто описание)
          sendByteMassive[3] = 19.toByte()
          sendByteMassive[4] = byteArray[0]
          sendByteMassive[5] = crcCalc(sendByteMassive)
        }
      }
      readDataFlag = false
      runWriteData(sendByteMassive, FESTO_A_CHARACTERISTIC, WRITE_WR)
    } else {
        bleCommand(byteArray, Command, typeCommand)
//        System.err.println("Отправили команду! Чтение")
    }
  }
  private fun bleCommand(byteArray: ByteArray?, Command: String, typeCommand: String){
    if (mBluetoothLeService != null) {
      for (i in mGattCharacteristics.indices) {
        for (j in mGattCharacteristics[i].indices) {
          System.err.println("mGattCharacteristics i = $i  j = $j   ${mGattCharacteristics[i][j].uuid}")
          if (mGattCharacteristics[i][j].uuid.toString() == Command) {
            mCharacteristic = mGattCharacteristics[i][j]
            if (typeCommand == WRITE){
              if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
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
    System.err.println("DeviceControlActivity-------> $mDeviceAddress reconnectThread started")
    var j = 1
    reconnectThread = Thread {
      while (reconnectThreadFlag) {
        runOnUiThread {
          if(j % 2 == 0) {
            reconnectThreadFlag = false
            scanLeDevice(true)
            System.err.println("DeviceControlActivity-------> $mDeviceAddress  Переподключение cо сканированием №$j")
          } else {
            reconnect()
            System.err.println("DeviceControlActivity-------> $mDeviceAddress  Переподключение без сканирования №$j")
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
//          bleCommand(null, "ауацуацацацац", NOTIFY)
          System.err.println("startSubscribeSensorsDataThread попытка подписки")
        }
        percentSynchronize = 100
        updateUIChart(13)
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
          if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            bleCommand(null, MIO_MEASUREMENT_NEW_VM, NOTIFY)
          }
          if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            bleCommand(null, MIO_MEASUREMENT_NEW, NOTIFY)
          }
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
  private fun runStart() { getStart().let { queue.put(it) } }
  private fun getStart(): Runnable { return Runnable { readStart() } }
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
//            showToast("Старт потока запросов начальных параметров")
            System.err.println("$info = 0")
            bleCommand(READ_REGISTER, OPEN_THRESHOLD_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 5
            updateUIChart(14)
            state = 1
          }
          1 -> {
            System.err.println("$info = 1")
//            bleCommand(READ_REGISTER, OPEN_THRESHOLD_NEW, READ)
            bleCommand(READ_REGISTER, SENS_VERSION_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 15
            updateUIChart(15)
            updateUIAccountMain()
            state = 2
          }
          2 -> {
            System.err.println("$info = 2")
            bleCommand(READ_REGISTER, CLOSE_THRESHOLD_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 25
            updateUIChart(16)
            state = 3
          }
          3 -> {
            System.err.println("$info = 3")
            bleCommand(READ_REGISTER, SENS_OPTIONS_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 35
            updateUIChart(17)
            state = 4
          }
          4 -> {
            System.err.println("$info = 4")
            bleCommand(READ_REGISTER, SET_REVERSE_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 45
            updateUIChart(18)
            state = 5
          }
          5 -> {
            System.err.println("$info = 5")
            bleCommand(READ_REGISTER, SET_ONE_CHANNEL_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 55
            updateUIChart(19)
            state = 6
          }
          6 -> {
            System.err.println("$info = 6")
            bleCommand(READ_REGISTER, ADD_GESTURE_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 65
            updateUIChart(20)
            state = 7  //11 пропустить калибровку //7 - выполнить
          }

          7 -> {
            System.err.println("$info = 7")
            bleCommand(READ_REGISTER, CALIBRATION_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 75
            updateUIChart(21)
            state = 8
          }
          8 -> {
            System.err.println("$info = 8")
            if (calibrationStage == 0) {
              state = 9 //9
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
            showToast("В протезе сильно затянута одна или несколько степеней свободы!")
            state = 14
          }
          14 -> {
            System.err.println("$info = 14")
            bleCommand(READ_REGISTER, SHUTDOWN_CURRENT_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 85
            updateUIChart(22)
            state = 15
          }
          15 -> {
            System.err.println("$info = 15")
            bleCommand(READ_REGISTER, SET_GESTURE_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 95
            updateUIChart(23)
            state = 16
          }
          16 -> {
            System.err.println("$info = 16")
            bleCommand(READ_REGISTER, DRIVER_VERSION_NEW, READ)
            globalSemaphore = false
            percentSynchronize = 100
            updateUIChart(24)
            updateUIAccountMain()
            state = 0
            endFlag = true
            startSubscribeSensorsNewDataThread()
          }

        }
        count = 0
      } else {
        count++
        if (count == 100) {
//          if (state != 0) { runStart() } //перезапускаем функцию если споткнулись
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

  /**
   * Запуск задачи чтения параметров экрана графиков
   */
  private fun runStartVM() { getStartVM().let { queue.put(it) } }
  private fun getStartVM(): Runnable { return Runnable { readStartVM() } }
  private fun readStartVM() {
    val info = "DeviceControlActivity-------> $mDeviceAddress  Чтение порогов и версий"
    var count = 0
    var state = 0 // переключается здесь в потоке
    endFlag = false // меняется на последней стадии машины состояний, служит для немедленного прекращния операции
    globalSemaphore = true // меняется по приходу ответа от подключаемого уст-ва
    System.err.println()

    while (!endFlag) {
      if (globalSemaphore) {
        when (state) {
          // ПРАВИЛЬНАЯ ЦЕПЬ ЗАПРОСОВ
          0 -> {
//            showToast("Старт потока запросов начальных параметров для FEST-X")
            System.err.println("$info = 0")
            bleCommand(READ_REGISTER, DRIVER_VERSION_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 5
            updateUIChart(25)
            updateUIAccountMain()
            state = 1
          }
          1 -> {
            System.err.println("$info = 1")
            bleCommand(READ_REGISTER, OPEN_THRESHOLD_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 15
            updateUIChart(26)
            state = 2
          }
          2 -> {
            System.err.println("$info = 2")
            bleCommand(READ_REGISTER, CLOSE_THRESHOLD_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 25
            updateUIChart(27)
            state = 3
          }
          3 -> {
            System.err.println("$info = 3")
            bleCommand(READ_REGISTER, SENS_OPTIONS_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 35
            updateUIChart(28)
            state = 4
          }
          4 -> {
            System.err.println("$info = 4")
            bleCommand(READ_REGISTER, SET_REVERSE_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 45
            updateUIChart(29)
            state = 5
          }
          5 -> {
            System.err.println("$info = 5")
            bleCommand(READ_REGISTER, SET_ONE_CHANNEL_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 55
            updateUIChart(30)
            state = 6
          }
          6 -> {
            System.err.println("$info = 6")
            bleCommand(READ_REGISTER, ADD_GESTURE_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 65
            updateUIChart(31)
            state = 7  //11 пропустить калибровку //7 - выполнить
          }

          7 -> {
            System.err.println("$info = 7")
            bleCommand(READ_REGISTER, CALIBRATION_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 75
            updateUIChart(32)
            state = 8
          }
          8 -> {
            System.err.println("$info = 8")
            if (calibrationStage == 0) {
              state = 9 //9
            } else {
              if (calibrationStage == 6 || calibrationStage == 1) {
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
            showToast("В протезе сильно затянута одна или несколько степеней свободы!")
            state = 14
          }
          14 -> {
            System.err.println("$info = 14")
            bleCommand(READ_REGISTER, SHUTDOWN_CURRENT_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 85
            updateUIChart(33)
            state = 15
          }
          15 -> {
            System.err.println("$info = 15")
            bleCommand(READ_REGISTER, ROTATION_GESTURE_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 90
            updateUIChart(34)
            state = 16
          }
          16 -> {
            System.err.println("$info = 16")
            bleCommand(READ_REGISTER, SENS_VERSION_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 95
            updateUIChart(35)
            updateUIAccountMain()
            state = 17
          }
          17 -> {
            System.err.println("$info = 17")
            bleCommand(READ_REGISTER, SET_GESTURE_NEW_VM, READ)
            globalSemaphore = false
            percentSynchronize = 100
            updateUIChart(36)
            state = 0
            endFlag = true
            startSubscribeSensorsNewDataThread()
            runOnUiThread { enableInterface(true) }
          }
        }
        count = 0
      } else {
        count++
        if (count == 100) {

          if (state != 0) { runStartVM() } //перезапускаем функцию если споткнулись, но
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
  private fun getWriteData(byteArray: ByteArray?, Command: String, typeCommand: String): Runnable { return Runnable { writeData(byteArray, Command, typeCommand) } }
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
  private fun getReadDataAllCharacteristics(Command: String): Runnable { return Runnable { readDataAllCharacteristics(Command) } }
  private fun readDataAllCharacteristics(Command: String) {
      System.err.println("тык")
      bleCommand(null, Command, READ)
      try {
        Thread.sleep(1000)
      } catch (ignored: Exception) {}
  }

  private fun runReadData() { getReadData().let { queue.put(it) } }
  private fun getReadData(): Runnable { return Runnable { readData() } }
  private fun readData() {
    while (readDataFlag) {
      System.err.println("read counter: ${countCommand.get()}")
      bleCommand(null, FESTO_A_CHARACTERISTIC, READ)
      percentSynchronize = 100
      updateUIChart(37)
      try {
        Thread.sleep(100)
      } catch (ignored: Exception) {}
    }
  }

  fun runSendCommand(data: ByteArray?, uuidCommand: String, countRestart: Int) { getSendCommand(data, uuidCommand, countRestart).let { queue.put(it) } }
  private fun getSendCommand(data: ByteArray?, uuidCommand: String, countRestart: Int): Runnable { return Runnable { sendCommand(data, uuidCommand, countRestart) } }
  private fun sendCommand(data: ByteArray?, uuidCommand: String, countRestart: Int) {
    val idCommand = uuidCommand.substring(4).substringBefore('-')
    val info = "startSendCommand id $idCommand   state"

    var useNewSystemSendCommand = false
    if (driverVersionS != null) {
      val driverNum = driverVersionS?.substring(0, 1) + driverVersionS?.substring(2, 4)
      useNewSystemSendCommand = driverNum.toInt() > 233
//      System.err.println("$info = $state  driverNum:$driverNum   useNewSystemSendCommand=$useNewSystemSendCommand")
    }

    expectedIdCommand = idCommand
    var countRestartLocal = countRestart
    var countAttempt = 0
    var state = 0 // переключается здесь в потоке
    var endFlag = false // меняется на последней стадии машины состояний, служит для немедленного прекращния операции
    var resendCommandTimer = true
    while (!endFlag) {
        when (state) {
          0 -> {
            System.err.println("$info = $state  countRestart:$countRestart  useNewSystemSendCommand=$useNewSystemSendCommand")
            if (useNewSystemSendCommand) {
              expectedReceiveConfirmation = 1
              bleCommand(data, uuidCommand, WRITE)
              incrementCountCommand()

              state += 1 //если версия используемого протеза (касается только FEST-X) 234 и
                         // выше то ожидаем подтверждения оправки команды и используем повторную отправку
                         // до подтверждения
            } else {
              try {
                Thread.sleep(200)
              } catch (ignored: Exception) {}

              bleCommand(data, uuidCommand, WRITE)
              incrementCountCommand()

              state += 2 //если версия используемого протеза (касается только FEST-X) 233 и
                         // ниже, то просто отправляем команду и завершаем отправку команды
            }
          }
          1 -> {
            System.err.println("$info = $state   testingConnection: $testingConnection")
            if (countRestartLocal > 0) {
              if (resendCommandTimer) {
                runOnUiThread{
                  timerResendCommandDLE = object : CountDownTimer(500, 1) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                      resendCommandTimer = true
                      state -= 1
                      countRestartLocal -= 1
                      countAttempt += 1
                    }
                  }.start()
                }
                resendCommandTimer = false
              }
            } else {
              state += 1
//              showToast(getString(R.string.unstable_connection))
            }

            if (expectedReceiveConfirmation == 2) {
              timerResendCommandDLE?.cancel()
              countAttempt += 1
              state += 1
            }
          }
          2 -> {
            System.err.println("$info = $state   testingConnection: $testingConnection")
            if ( testingConnection ) {
              RxUpdateMainEvent.getInstance().updateCommunicationTestResult(countAttempt)
            }
            if ( !useNewSystemSendCommand) { //добавляем задержку для совместимости с предыдущими версиями
              try {
                Thread.sleep(100)
              } catch (ignored: Exception) {}
            }
            endFlag = true
            state = 0
            expectedReceiveConfirmation = 0
          }
        }
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
  fun setDebugScreenIsOpen(value: Boolean) { debugScreenIsOpen = value }
  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeByte(if (sensorsDataThreadFlag) 1 else 0)
    parcel.writeString(mDeviceName)
    parcel.writeString(mDeviceAddress)
    parcel.writeString(mDeviceType)
    parcel.writeByte(if (mConnected) 1 else 0)
    parcel.writeParcelable(mNotifyCharacteristic, flags)
    parcel.writeParcelable(mCharacteristic, flags)
    parcel.writeInt(dataSens1)
    parcel.writeInt(dataSens2)
    parcel.writeInt(state)
  }
  override fun describeContents(): Int { return 0 }   

  companion object CREATOR : Parcelable.Creator<MainActivity> {
    override fun createFromParcel(parcel: Parcel): MainActivity { return MainActivity(parcel) }
    override fun newArray(size: Int): Array<MainActivity?> { return arrayOfNulls(size) }
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
  fun openValueChangeDialog(keyValue: String, callback: ChartFragmentCallback? = null) {
    val dialog = CustomDialogChangeValue(keyValue = keyValue, callbackChartFragment = callback)
    dialog.show(supportFragmentManager, "update dialog")
  }
  fun openTestConnectionProgressDialog() {
    val dialog = CustomDialogTestCommunication()
    dialog.show(supportFragmentManager, "update dialog")
  }
  fun openTestConnectionResultDialog(percentageCommunicationQuality: Int) {
    val dialog = CustomDialogResultTestCommunication(percentageCommunicationQuality)
    dialog.show(supportFragmentManager, "update dialog")
  }
  fun showAdvancedSettings(showAdvancedSettings: Boolean) {
    NavigationUtils.showAdvancedSettings = showAdvancedSettings
    if (showAdvancedSettings) {
      saveInt(PreferenceKeys.ADVANCED_SETTINGS, 1)
    }  else {
      saveInt(PreferenceKeys.ADVANCED_SETTINGS, 0)
    }


    binding.mainactivityViewpager.isSaveFromParentEnabled = false
    if (showAdvancedSettings) {
      if ( mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
        || mDeviceType!!.contains(DEVICE_TYPE_BT05)
        || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_H)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapterWithAdvancedSettings(supportFragmentManager)
        binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
        binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 1)
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograbWithAdvancedSettings(supportFragmentManager)
        binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
        binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 0)
      }
    } else {
      if ( mDeviceType!!.contains(DEVICE_TYPE_FEST_A)
        || mDeviceType!!.contains(DEVICE_TYPE_BT05)
        || mDeviceType!!.contains(DEVICE_TYPE_MY_IPHONE)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_H)
        || mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        val mSectionsPagerAdapter =  SectionsPagerAdapter(supportFragmentManager)
        binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
        binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 1)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
      } else {
        val mSectionsPagerAdapter =  SectionsPagerAdapterMonograb(supportFragmentManager)
        binding.mainactivityViewpager.adapter = mSectionsPagerAdapter
        binding.mainactivityNavi.setViewPager(binding.mainactivityViewpager, 0)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
      }
    }

    Toast.makeText(this, "Advanced settings: $showAdvancedSettings", Toast.LENGTH_SHORT).show()

    binding.mainactivityViewpager.offscreenPageLimit = 3
    System.err.println("setComponents ${NavigationUtils.setComponents(baseContext, binding.mainactivityNavi)}")
    updateUIChart(40)
  }
  @SuppressLint("InflateParams", "SetTextI18n", "StringFormatInvalid")
  private fun showCalibrationInfoDialog(statusFinger: List<String>, statusEncoderFingers: List<Int>, statusCurrentFingers: List<Int>) {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_info, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()


    myDialog.findViewById<TextView>(R.id.dialog_info_title_tv).text = getString(R.string.calibration_status)


    if (statusFinger.isNotEmpty()) {

      myDialog.findViewById<TextView>(R.id.dialog_first_in_queue_tv).text =
        getString(R.string.pre_status_finger, 1) + " " + statusFinger[0] + getString(
          R.string.status_finger,
          statusEncoderFingers[0],
          statusCurrentFingers[0]
        )
      myDialog.findViewById<TextView>(R.id.dialog_second_in_queue_tv).text =
        getString(R.string.pre_status_finger, 2) + " " + statusFinger[1] + getString(
          R.string.status_finger,
          statusEncoderFingers[1],
          statusCurrentFingers[1]
        )
      myDialog.findViewById<TextView>(R.id.dialog_third_in_queue_tv).text =
        getString(R.string.pre_status_finger, 3) + " " + statusFinger[2] + getString(
          R.string.status_finger,
          statusEncoderFingers[2],
          statusCurrentFingers[2]
        )
      myDialog.findViewById<TextView>(R.id.dialog_fourth_in_queue_tv).text =
        getString(R.string.pre_status_finger, 4) + " " + statusFinger[3] + getString(
          R.string.status_finger,
          statusEncoderFingers[3],
          statusCurrentFingers[3]
        )
      myDialog.findViewById<TextView>(R.id.dialog_fifth_in_queue_tv).text =
        getString(R.string.pre_status_finger, 5) + " " + statusFinger[4] + getString(
          R.string.status_finger,
          statusEncoderFingers[4],
          statusCurrentFingers[4]
        )
      myDialog.findViewById<TextView>(R.id.dialog_sixth_in_queue_tv).text =
        getString(R.string.pre_status_finger, 6) + " " + statusFinger[5] + getString(
          R.string.status_finger,
          statusEncoderFingers[5],
          statusCurrentFingers[5]
        )
    }

    myDialog.findViewById<LottieAnimationView>(R.id.info_animation_view).setAnimation(R.raw.loader_calibrating)

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_info_confirm)
    yesBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams", "SetTextI18n", "StringFormatInvalid")
  private fun showChangeSizeDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_select_scale, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()


    myDialog.findViewById<SwitchMultiButton>(R.id.dialog_select_scale_sw).setOnSwitchListener { position, _ ->
      System.err.println("scaleProstheses = ${myDialog.findViewById<SwitchMultiButton>(R.id.dialog_select_scale_sw).selectedTab}")
      when (position) {
        0 -> { scaleProstheses = 0 }
        1 -> { scaleProstheses = 1 }
        2 -> { scaleProstheses = 2 }
        3 -> { scaleProstheses = 3 }
      }
    }


    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_select_scale_confirm)
    yesBtn.setOnClickListener {
      scaleProstheses = myDialog.findViewById<SwitchMultiButton>(R.id.dialog_select_scale_sw).selectedTab
      System.err.println("scaleProstheses = $scaleProstheses")
      showConfirmChangeSideDialog()
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams", "SetTextI18n", "StringFormatInvalid")
  private fun showConfirmChangeSideDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_confirm_select_scale, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    myDialog.findViewById<TextView>(R.id.dialog_confirm_select_scale_message_tv).text =
      when (scaleProstheses) {
        0 -> getString(R.string.the_size_of_your_prostheses) + " \"S\" ?"
        1 -> getString(R.string.the_size_of_your_prostheses) + " \"M\" ?"
        2 -> getString(R.string.the_size_of_your_prostheses) + " \"L\" ?"
        3 -> getString(R.string.the_size_of_your_prostheses) + " \"XL\" ?"
        else -> {getString(R.string.the_size_of_your_prostheses) +" \"S\" ?"}
      }

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_confirm_select_scale_confirm)
    yesBtn.setOnClickListener {
      bleCommand(byteArrayOf(scaleProstheses.toByte()), SET_SELECT_SCALE, WRITE)
      myDialog.dismiss()
    }

    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_confirm_select_scale_cancel)
    noBtn.setOnClickListener {
      showChangeSizeDialog()
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams")
  fun showDisconnectDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_disconnection, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_disconnection_confirm)
    yesBtn.setOnClickListener {
      disconnect()
      myDialog.dismiss()
    }
    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_disconnection_cancel)
    noBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams", "MissingInflatedId")
  fun showSoftResetDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_confirm_soft_reset, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_reset_soft_confirm)
    yesBtn.setOnClickListener {
      if (!lockWriteBeforeFirstRead) {
        if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
          runSendCommand(byteArrayOf(0x02), RESET_TO_FACTORY_SETTINGS_NEW_VM, 50)
        } else {
          if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            runWriteData(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW, WRITE)
          } else {
            firstActivateSetScaleDialog = false
            bleCommandConnector(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS, WRITE, 15)
          }
        }
      } else {
        showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
      }
      RxUpdateMainEvent.getInstance().updateResetAdvancedSettings(true)

      Handler().postDelayed({
        readStartData(true)
        System.err.println("-------> displayDataNew showCalibrationDialog runStartVM")
      }, 1000)
      myDialog.dismiss()
    }

    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_reset_soft_cancel)
    noBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }

  @SuppressLint("InflateParams", "MissingInflatedId")
  fun showHardResetDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_confirm_hard_reset, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_reset_hard_confirm)
    yesBtn.setOnClickListener {
      if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        runSendCommand(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW_VM, 50)
      } else {
        if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
          runWriteData(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW, WRITE)
        } else {
          firstActivateSetScaleDialog = false
          bleCommandConnector(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS, WRITE, 15)
        }
      }

      RxUpdateMainEvent.getInstance().updateResetAdvancedSettings(true)

      Handler().postDelayed({
        readStartData(true)
        System.err.println("-------> displayDataNew showCalibrationDialog runStartVM")
      }, 1000)
      myDialog.dismiss()
    }

    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_reset_hard_cancel)
    noBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams", "MissingInflatedId")
  fun showGestureResetDialog() {
    System.err.println("showGestureResetDialog")
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_confirm_gesture_reset, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_reset_gestures_confirm)
    yesBtn.setOnClickListener {
      runSendCommand(byteArrayOf(0x03), RESET_TO_FACTORY_SETTINGS_NEW_VM, 50)

      RxUpdateMainEvent.getInstance().updateResetAdvancedSettings(true)

      Handler().postDelayed({
        readStartData(true)
        System.err.println("-------> displayDataNew showCalibrationDialog runStartVM")
      }, 1000)
      myDialog.dismiss()
    }



    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_reset_gestures_cancel)
    noBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams")
  fun showCalibrationDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_confirm_calibration, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_calibration_confirm)
    yesBtn.setOnClickListener {
      if (mSettings!!.getInt(mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
        if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
          stage = "main activity"
          runSendCommand(byteArrayOf(0x09), CALIBRATION_NEW_VM, 50)
        } else {
          if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            runWriteData(byteArrayOf(0x09), CALIBRATION_NEW, WRITE)
          }
        }
      } else {
        if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
          stage = "main activity"
          runSendCommand(byteArrayOf(0x0a), CALIBRATION_NEW_VM, 50)
        } else {
          if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            runWriteData(byteArrayOf(0x0a), CALIBRATION_NEW, WRITE)
          }
        }
      }
      saveInt(mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 1)

      myDialog.dismiss()
    }
    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_calibration_cancel)
    noBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams")
  fun showSetSerialNumberDialog(serialNumber: String) {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_set_serial_numder, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_set_serial_number_confirm)
    yesBtn.setOnClickListener {
      if (mDeviceType!!.contains(DEVICE_TYPE_INDY)) {
        val charset = Charsets.UTF_8
        val byteArray = serialNumber.toByteArray(charset)

        System.err.println("byteArray serialNumber = $serialNumber byteArray = ${byteArray[0]} ${byteArray[1]} ${byteArray[2]} ${byteArray[3]} ${byteArray[4]} ${byteArray[5]} ${byteArray[6]} ${byteArray[7]} ${byteArray[8]} ${byteArray[9]} ${byteArray[10]} ${byteArray[11]}")
        bleCommandConnector(byteArray, SET_SERIAL_NUMBER, WRITE, 11)
      }
      if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
        bleCommandConnector(
          serialNumber.toByteArray(Charsets.UTF_8),
          SERIAL_NUMBER_NEW,
          WRITE,
          17
        )
      }
      if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        runSendCommand(serialNumber.toByteArray(Charsets.UTF_8),
          SERIAL_NUMBER_NEW_VM, 50)
        this.mDeviceName = NameUtil.getCleanName(serialNumber)
        System.err.println("DEVICE_TYPE_FEST_X serialNumber=$serialNumber")
      } else {
        System.err.println("DEVICE_TYPE_FEST_X else serialNumber=$serialNumber")
      }

      this.mDeviceName = NameUtil.getCleanName(serialNumber)
      updateUIChart(100)


      myDialog.dismiss()
    }
    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_set_serial_number_cancel)
    noBtn.setOnClickListener {
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams")
  @Suppress("DEPRECATION")
  fun showPinCodeDialog(serialNumber: String) {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_enter_pin, null)
    val myDialog = Dialog(this)
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val pin = myDialog.findViewById<PasscodeView>(R.id.pincode_settings_view)
    pin.requestToShowKeyboard()
    Handler().postDelayed({
      pin.showKeyboard()
    }, 200)

    pin.setPasscodeEntryListener { passcode ->
        if (passcode == SECRET_PIN) {
          showSetSerialNumberDialog(serialNumber)
          saveBool(PreferenceKeys.ENTER_SECRET_PIN, true)
          Toast.makeText(this, "Верный пинкод!", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, "Неверный пинкод", Toast.LENGTH_SHORT).show()
        }
        hideKeyboard(pin)
        myDialog.dismiss()
    }

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_settings_pin_confirm)
    yesBtn.setOnClickListener {
      hideKeyboard(pin)
      myDialog.dismiss()
    }
  }
  private fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
  }
  private fun hideKeyboard(view: View) {
    val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
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

  private fun crcCalc(data: ByteArray): Byte {
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

  private fun saveBigGestureState() {
    for (i in 0 until 13) {
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + (i + 2), // проверить тут + 1
        gestureTableBig[i][0][0]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + (i + 2),
        gestureTableBig[i][0][1]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + (i + 2),
        gestureTableBig[i][0][2]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + (i + 2),
        gestureTableBig[i][0][3]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + (i + 2),
        gestureTableBig[i][0][4]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + (i + 2),
        gestureTableBig[i][0][5]
      )

      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + (i + 2),
        gestureTableBig[i][1][0]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + (i + 2),
        gestureTableBig[i][1][1]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + (i + 2),
        gestureTableBig[i][1][2]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + (i + 2),
        gestureTableBig[i][1][3]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + (i + 2),
        gestureTableBig[i][1][4]
      )
      saveInt(
        mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + (i + 2),
        gestureTableBig[i][1][5]
      )
    }
  }
  private fun saveGestureState() {
    if (mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      for (i in 0 until 7) {
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + (i + 2), // проверить тут + 1
          gestureTable[i][0][0]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + (i + 2),
          gestureTable[i][0][1]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + (i + 2),
          gestureTable[i][0][2]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + (i + 2),
          gestureTable[i][0][3]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + (i + 2),
          gestureTable[i][0][4]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + (i + 2),
          gestureTable[i][0][5]
        )

        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + (i + 2),
          gestureTable[i][1][0]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + (i + 2),
          gestureTable[i][1][1]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + (i + 2),
          gestureTable[i][1][2]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + (i + 2),
          gestureTable[i][1][3]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + (i + 2),
          gestureTable[i][1][4]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + (i + 2),
          gestureTable[i][1][5]
        )
      }
    }
    if (mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
      for (i in 0 until 7) {
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + (i + 1),
          gestureTable[i][0][0]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + (i + 1),
          gestureTable[i][0][1]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + (i + 1),
          gestureTable[i][0][2]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + (i + 1),
          gestureTable[i][0][3]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + (i + 1),
          gestureTable[i][0][4]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + (i + 1),
          gestureTable[i][0][5]
        )

        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + (i + 1),
          gestureTable[i][1][0]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + (i + 1),
          gestureTable[i][1][1]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + (i + 1),
          gestureTable[i][1][2]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + (i + 1),
          gestureTable[i][1][3]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + (i + 1),
          gestureTable[i][1][4]
        )
        saveInt(
          mDeviceAddress + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + (i + 1),
          gestureTable[i][1][5]
        )
      }
    }
  }
  internal fun saveInt(key: String, variable: Int) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putInt(key, variable)
    editor.apply()
  }
  internal fun saveBool(key: String, variable: Boolean) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putBoolean(key, variable)
    editor.apply()
  }
  fun saveText(key: String, text: String?) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putString(key, text)
    editor.apply()
  }
  fun loadText(key: String): String { return mSettings!!.getString(key, "null").toString() }

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


  private fun openScanActivity() {
    System.err.println("Check openScanActivity()")
    resetLastMAC()
    val intent = Intent(this@MainActivity, ScanActivity::class.java)
    startActivity(intent)
    finish()
  }
  private fun resetLastMAC() {
    saveText(PreferenceKeys.LAST_CONNECTION_MAC, "null")
  }
  fun disconnect () {
    System.err.println("Check disconnect()")
    mDisconnected = true
    if (mBluetoothLeService != null) {
      println("--> дисконнектим всё к хуям и анбайндим")
      mBluetoothLeService!!.disconnect()
      unbindService(mServiceConnection)
      mBluetoothLeService = null
    }
    mConnected = false
    endFlag = true
    runOnUiThread {
      mConnectView!!.visibility = View.GONE
      mDisconnectView!!.visibility = View.VISIBLE
      mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
    }
    invalidateOptionsMenu()
    percentSynchronize = 0
    openScanActivity()
  }
  private fun reconnect () {
    //полное завершение сеанса связи и создание нового в onResume
    System.err.println("Check reconnect()")
    if (mBluetoothLeService != null) {
      System.err.println("DeviceControlActivity-------> reconnect() $mDeviceAddress  unbindService")
      unbindService(mServiceConnection)
      mBluetoothLeService = null
      mBluetoothAdapter = null
    }

    val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    mBluetoothAdapter = bluetoothManager.adapter
    val gattServiceIntent = Intent(this, BluetoothLeService::class.java)

    System.err.println("DeviceControlActivity-------> reconnect() $mDeviceAddress  bindService")
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
  }
  /**
   * Запуск/остановка сканирования эфира на наличие BLE устройств
   * @param enable - true запуск | false остановка
   */
  @SuppressLint("MissingPermission")
  private fun scanLeDevice(enable: Boolean) {
    System.err.println("Check scanLeDevice() $enable")
    if (enable) {
      mScanning = true
      mBluetoothAdapter!!.startLeScan(mLeScanCallback)
      System.err.println("DeviceControlActivity-------> $mDeviceAddress  startLeScan")
    } else {
      mScanning = false
      mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
      System.err.println("DeviceControlActivity-------> $mDeviceAddress  stopLeScan")
    }
  }

  // Device scan callback.
  @SuppressLint("MissingPermission")
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

