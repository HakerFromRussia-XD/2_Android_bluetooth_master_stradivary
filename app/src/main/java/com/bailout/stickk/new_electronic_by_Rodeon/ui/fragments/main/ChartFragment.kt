package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.yandex.metrica.YandexMetrica
import io.reactivex.android.schedulers.AndroidSchedulers
import com.bailout.stickk.R
import com.bailout.stickk.databinding.LayoutChartBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.*
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.DecoratorChange
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.TypeGuides
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.dialogs.ChartFragmentCallback
import com.bailout.stickk.new_electronic_by_Rodeon.utils.NavigationUtils
import javax.inject.Inject


@Suppress("DEPRECATION")
class ChartFragment : Fragment(), DecoratorChange, ReactivatedChart, OnChartValueSelectedListener {

  @Inject
  lateinit var preferenceManager: PreferenceManager

  private var main: MainActivity? = null
  private var graphThread: Thread? = null
  private var graphThreadFlag = false
  private var testThreadFlag = true
  private var plotData = true
  private var showAdvancedSettings = false
  private var mSettings: SharedPreferences? = null
  private var scale = 0F
  private var oldStateSync = 0
  private var count: Int = 0
  private var callbackFromDialogChangeValue: ChartFragmentCallback = object: ChartFragmentCallback {
    override fun changeCorrelatorNoiseThreshold1(value: Int) {
      System.err.println("lol sendCommandToBLE CORRELATOR_NOISE_THRESHOLD_1_NUM TestCallback from ChartFragment!!!!")
      main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, (255 - value))
      sendCorrelatorNoiseThreshold(1)
      updateAllParameters()
    }

    override fun changeCorrelatorNoiseThreshold2(value: Int) {
      System.err.println("lol sendCommandToBLE CORRELATOR_NOISE_THRESHOLD_2_NUM TestCallback from ChartFragment!!!!")
      main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, (255 - value))
      sendCorrelatorNoiseThreshold(2)
      updateAllParameters()
    }
  }
  private lateinit var myAppContext: Context
  private var displayedNextTypeGuides: TypeGuides? = TypeGuides.SHOW_HELP_GUIDE
  private var modeEMGSend = 0

  private lateinit var binding: LayoutChartBinding

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    binding = LayoutChartBinding.inflate(layoutInflater)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }

    myAppContext = activity?.applicationContext!!




    //TODO выключить быстрое открытие после завершения тестов
//    navigator().showSecretSettingsScreen()
//    navigator().showNeuralScreen()



    return binding.root
  }
  @Deprecated("Deprecated in Java")
  @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "Recycle")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
    showAdvancedSettings = NavigationUtils.showAdvancedSettings
    main?.setSwapOpenCloseButton(preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false))
    scale = resources.displayMetrics.density


    initializedSensorGraph()
    initializedUI()


    binding.nameTv.setOnClickListener {
      main?.showDisconnectDialog()
    }

    binding.driverTv.setOnLongClickListener {
      main?.readStartData(true)
      showAdvancedSettings = if (showAdvancedSettings) {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        false
      } else {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        true
      }
      false
    }
    binding.bmsTv.setOnLongClickListener {
      main?.readStartData(true)
      showAdvancedSettings = if (showAdvancedSettings) {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        false
      } else {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        true
      }
      false
    }
    binding.sensorTv.setOnLongClickListener {
      main?.readStartData(true)
      showAdvancedSettings = if (showAdvancedSettings) {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        false
      } else {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        true
      }
      false
    }
    binding.syncTv.setOnLongClickListener {
      main?.readStartData(true)
      showAdvancedSettings = if (showAdvancedSettings) {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        false
      } else {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        true
      }
      false
    }
    binding.syncSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (main?.locate?.contains("ru")!!) {
          binding.syncTv.text = "снхро "+seekBar.progress.toString()+"%"
        } else {
          binding.syncTv.text = "sync "+seekBar.progress.toString()+"%"
        }
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    })


    val eventYandexMetricaParametersOpenCH = "{\"Screen chart\":\"Change threshold open sensor\"}"

    binding.openCHSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH1 = $progress")
        ObjectAnimator.ofFloat(binding.limitCH1, "y", 320 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()//  10f -> 60f
        ObjectAnimator.ofFloat(binding.openBorder, "y", 320 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()//  10f -> 60f

        binding.openThresholdTv.text = progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if ((!main?.lockWriteBeforeFirstRead!!)) {//отправка команды изменения порога на протез только если блокировка не активна
          if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_NEW_VM, 50)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_NEW, WRITE
                )
              } else {
                main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_HDLE, WRITE, 4
                )
              }
            }
            if (main?.savingSettingsWhenModified == true) {
              main?.saveInt(main?.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, seekBar.progress)
            }
            YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersOpenCH)
          } else {
            updateAllParameters()
            main?.showToast(resources.getString(R.string.settings_blocking_massage))
          }
        }
        else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
        }
      }
    })
    val eventYandexMetricaParametersCloseCH = "{\"Screen chart\":\"Change threshold close sensor\"}"
    binding.closeCHSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH2 = $progress")
        ObjectAnimator.ofFloat(binding.limitCH2, "y", 320 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()
        ObjectAnimator.ofFloat(binding.closeBorder, "y", 320 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()//  10f -> 60f
        binding.closeThresholdTv.text = progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if ((!main?.lockWriteBeforeFirstRead!!)) {//отправка команды изменения порога на протез только если блокировка не активна
          if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_NEW_VM, 50)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_HDLE, WRITE, 5)
              }
            }
            if (main?.savingSettingsWhenModified == true) {
              main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, seekBar.progress)
            }
            YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersCloseCH)
          } else {
            updateAllParameters()
            main?.showToast(resources.getString(R.string.settings_blocking_massage))
          }
        }
        else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
        }
      }
    })



    binding.correlatorNoiseThreshold1Tv.setOnClickListener {
      if ( (!main?.lockWriteBeforeFirstRead!!)) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
          main!!.openValueChangeDialog(
            PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM,
            callbackFromDialogChangeValue
          )
        } else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.settings_blocking_massage))
        }
      } else {
        updateAllParameters()
        main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
      }
    }
    binding.correlatorNoiseThreshold2Tv.setOnClickListener {
      if ( (!main?.lockWriteBeforeFirstRead!!)) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
          main!!.openValueChangeDialog(
            PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM,
            callbackFromDialogChangeValue
          )
        } else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.settings_blocking_massage))
        }
      } else {
        updateAllParameters()
        main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
      }
    }
    val eventYandexMetricaParametersNoiseThresholdOpen = "{\"Screen chart\":\"Change noise threshold open sensor\"}"
    binding.correlatorNoiseThreshold1Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.correlatorNoiseThreshold1Tv.text = progress.toString()
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if ( (!main?.lockWriteBeforeFirstRead!!)) {
          if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
            System.err.println("test save correlator value" + main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM)
            if (main?.savingSettingsWhenModified == true) {
              System.err.println("test save correlator value" + main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM)
              main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, (255 - seekBar.progress))
            }
            sendCorrelatorNoiseThreshold(1)
            YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersNoiseThresholdOpen)
          } else {
            updateAllParameters()
            main?.showToast(resources.getString(R.string.settings_blocking_massage))
          }
        } else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
        }
      }
    })
    val eventYandexMetricaParametersNoiseThresholdClose = "{\"Screen chart\":\"Change noise threshold close sensor\"}"
    binding.correlatorNoiseThreshold2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.correlatorNoiseThreshold2Tv.text = seekBar.progress.toString()
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if ( (!main?.lockWriteBeforeFirstRead!!)) {
          if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
            if (main?.savingSettingsWhenModified == true) {
              main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, (255 - seekBar.progress))
            }
            sendCorrelatorNoiseThreshold(2)
            YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersNoiseThresholdClose)
          } else {
            updateAllParameters()
            main?.showToast(resources.getString(R.string.settings_blocking_massage))
          }
        } else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
        }
      }
    })


    val eventYandexMetricaParametersSwapSensors = "{\"Screen chart\":\"Tup swap sensors switch\"}"
    binding.swapSensorsSw.setOnClickListener {
      if ( (!main?.lockWriteBeforeFirstRead!!)) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) {
          if (binding.swapSensorsSw.isChecked) {
            binding.swapSensorsTv.text = resources.getString(R.string.on_sw)
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(0x01), SET_REVERSE_NEW_VM, 50)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x01), SET_REVERSE_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x01), SET_REVERSE, WRITE, 14)
              }
            }
            main?.saveBool(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, true)
//          main?.setReverseNum = 1
          } else {
            binding.swapSensorsTv.text = resources.getString(R.string.off_sw)
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(0x00), SET_REVERSE_NEW_VM, 50)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x00), SET_REVERSE_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x00), SET_REVERSE, WRITE, 14)
              }
            }
            main?.saveBool(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)
//          main?.setReverseNum = 0
          }
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSwapSensors)
        } else {
          updateAllParameters()
          main?.showToast(resources.getString(R.string.settings_blocking_massage))
        }
      } else {
        updateAllParameters()
        main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
      }
    }


    val eventYandexMetricaParametersThresholdsBlocking = "{\"Screen chart\":\"Tup settings blocking switch\"}"
    binding.thresholdsBlockingSw.setOnClickListener{
      if (binding.thresholdsBlockingSw.isChecked) {
        binding.thresholdsBlockingTv.text = resources.getString(R.string.on_sw)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, true)
      } else {
        binding.thresholdsBlockingTv.text = resources.getString(R.string.off_sw)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersThresholdsBlocking)
    }



    binding.calibrationBtn.setOnClickListener {
      System.err.println("запись глобальной калибровки тык")
      main?.showCalibrationDialog()
    }


//    binding.startGameBtn.setOnClickListener {
//      navigator().showArcanoidScreen()
//      navigator().showGrayStatusBar(true)
//    }

    val eventYandexMetricaParametersClose = "{\"Screen chart\":\"Tup close button\"}"
    val eventYandexMetricaParametersOpen = "{\"Screen chart\":\"Tup open button\"}"
    binding.closeBtn.setOnTouchListener { _, event ->
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (!main?.getSwapOpenCloseButton()!!) {
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersClose)
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(0x01), CLOSE_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x01), CLOSE_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x01, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
              }
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(0x00), CLOSE_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x00), CLOSE_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x00, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
              }
            }
          }
        } else {
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersOpen)
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(0x01), OPEN_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x01), OPEN_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x01, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
              }
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              main?.runSendCommand(byteArrayOf(0x00), OPEN_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x00), OPEN_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x00, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
              }
            }
          }
        }
      }
      false
    }
    binding.openBtn.setOnTouchListener { _, event ->
      System.err.println("openBtn 0")
      if (!main?.lockWriteBeforeFirstRead!!) {
        System.err.println("openBtn 1")
        if (!main?.getSwapOpenCloseButton()!!) {
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersOpen)
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              System.err.println("openBtn 2 1")
              main?.runSendCommand(byteArrayOf(0x01), OPEN_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x01), OPEN_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x01, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
              }
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              System.err.println("openBtn 2 2")
              main?.runSendCommand(byteArrayOf(0x00), OPEN_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x00), OPEN_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x00, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
              }
            }
          }
        } else {
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersClose)
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              System.err.println("openBtn 2 3")
              main?.runSendCommand(byteArrayOf(0x01), CLOSE_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x01), CLOSE_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x01, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
              }
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
              main?.stage = "chart activity"
              System.err.println("openBtn 2 4")
              main?.runSendCommand(byteArrayOf(0x00), CLOSE_MOTOR_NEW_VM, 3)
            } else {
              if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(0x00), CLOSE_MOTOR_NEW, WRITE)
              } else {
                main?.bleCommandConnector(byteArrayOf(0x00, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
              }
            }
          }
        }
      }
      false
    }

    binding.helpBtn.setOnClickListener {
      graphThreadFlag = false
      navigator().showWhiteStatusBar(true)
      navigator().showHelpScreen(this)
    }
    binding.saveProfileBtn.setOnClickListener {
      main!!.showToast("save profile btn tup")
    }


    if (mSettings!!.getInt(PreferenceKeys.SHOW_HELP_ACCENT, 4) == 1) {} else {
      main!!.saveInt(PreferenceKeys.SHOW_HELP_ACCENT, 1)
      Handler().postDelayed({
        main!!.setDecorator(TypeGuides.SHOW_HELP_GUIDE, binding.helpBtn, this)
      }, 500)
    }
  }

  override fun setStartDecorator() {
    for(i in 1..navigator().getBackStackEntryCount()){ navigator().goingBack() }
    main!!.setDecorator(TypeGuides.SHOW_VERSION_GUIDE, binding.versionHelpV, this)
    displayedNextTypeGuides = TypeGuides.SHOW_SENSORS_SENSITIVITY_GUIDE
  }
  override fun setNextDecorator() {
    System.err.println("onClick buttonNext displayedNextTypeGuides = $displayedNextTypeGuides")
    when(displayedNextTypeGuides) {
      TypeGuides.SHOW_HELP_GUIDE -> { main!!.hideDecorator() }
      TypeGuides.SHOW_VERSION_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_VERSION_GUIDE, binding.versionHelpV, this)
        displayedNextTypeGuides = TypeGuides.SHOW_SENSORS_SENSITIVITY_GUIDE
      }
      TypeGuides.SHOW_SENSORS_SENSITIVITY_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_SENSORS_SENSITIVITY_GUIDE, binding.openSensorsSensitivityRl, this)
        displayedNextTypeGuides = TypeGuides.SHOW_SENSORS_SENSITIVITY_CLARIFICATION_GUIDE
      }
      TypeGuides.SHOW_SENSORS_SENSITIVITY_CLARIFICATION_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_SENSORS_SENSITIVITY_CLARIFICATION_GUIDE, binding.correlatorNoiseThreshold1Tv, this)
        displayedNextTypeGuides = TypeGuides.SHOW_SENSORS_THRESHOLD_LEVELS_GUIDE
      }
      TypeGuides.SHOW_SENSORS_THRESHOLD_LEVELS_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_SENSORS_THRESHOLD_LEVELS_GUIDE, binding.openThresholdHelpV, this)
        displayedNextTypeGuides = TypeGuides.SHOW_OPEN_SENSORS_THRESHOLD_AREA_GUIDE
      }
      TypeGuides.SHOW_OPEN_SENSORS_THRESHOLD_AREA_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_OPEN_SENSORS_THRESHOLD_AREA_GUIDE, binding.openCHV, this)
        displayedNextTypeGuides = TypeGuides.SHOW_CLOSE_SENSORS_THRESHOLD_AREA_GUIDE
      }
      TypeGuides.SHOW_CLOSE_SENSORS_THRESHOLD_AREA_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_CLOSE_SENSORS_THRESHOLD_AREA_GUIDE, binding.closeCHV, this)
        displayedNextTypeGuides = TypeGuides.SHOW_SENSORS_SWAP_GUIDE
      }
      TypeGuides.SHOW_SENSORS_SWAP_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_SENSORS_SWAP_GUIDE, binding.swapSensorsRl, this)
        displayedNextTypeGuides = TypeGuides.SHOW_BLOCKING_GUIDE
      }
      TypeGuides.SHOW_BLOCKING_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_BLOCKING_GUIDE, binding.thresholdsBlockingRl, this)
        displayedNextTypeGuides = TypeGuides.SHOW_MOVEMENT_BUTTONS_GUIDE
      }
      TypeGuides.SHOW_MOVEMENT_BUTTONS_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_MOVEMENT_BUTTONS_GUIDE, binding.movementButtonsRl, this)
        displayedNextTypeGuides = TypeGuides.SHOW_DEVICE_NAME_GUIDE
      }
      TypeGuides.SHOW_DEVICE_NAME_GUIDE -> {
        main!!.hideDecorator()
        main!!.setDecorator(TypeGuides.SHOW_DEVICE_NAME_GUIDE, binding.nameTv, this)
        displayedNextTypeGuides = TypeGuides.END_GUIDE
      }
      TypeGuides.END_GUIDE -> {
        main!!.hideDecorator()
        navigator().showWhiteStatusBar(true)
        navigator().showHelpScreen(this)
        navigator().showSensorsHelpScreen(this)
      }
      else -> {}
    }
  }
  override fun reactivatedChart() {
    System.err.println("Test reactivatedChart ChartFragment")
    graphThreadFlag = true
    startGraphEnteringDataThread()
  }

  private fun showUIRotationGroup(enabled: Boolean) {
    if (enabled) {
      binding.loopGesturesLl.visibility = View.VISIBLE
    } else {
      binding.loopGesturesLl.visibility = View.GONE
    }
  }
  @SuppressLint("SetTextI18n")
  private fun enabledSensorsUIBeforeConnection (enabled: Boolean) {
    binding.swapSensorsSw.isEnabled = enabled
    binding.closeBtn.isEnabled = enabled
    binding.openBtn.isEnabled = enabled
    binding.calibrationBtn.isEnabled = enabled
//    binding.startGameBtn.isEnabled = enabled
    binding.thresholdsBlockingSw.isEnabled = enabled
    binding.correlatorNoiseThreshold1Sb.isEnabled = enabled
    binding.correlatorNoiseThreshold2Sb.isEnabled = enabled
    binding.correlatorNoiseThreshold1Tv.isEnabled = enabled
    binding.correlatorNoiseThreshold2Tv.isEnabled = enabled

    val startGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, 0)
    val endGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, 0)
    binding.startLoopGestureTv.text = (startGestureInLoopNum + 1).toString()
    binding.endLoopGestureTv.text = (endGestureInLoopNum + 1).toString()
//    showUIRotationGroup(mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false))
  }
  @SuppressLint("SetTextI18n")
  private fun initializedUI() {
    binding.thresholdsBlockingSw.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) binding.thresholdsBlockingTv.text = resources.getString(R.string.on_sw)
    enabledSensorsUIBeforeConnection(false)
    //скрываем кнопку калибровки для всех моделей кроме FEST_H и FEST_X
    if ((main?.mDeviceType?.contains(DEVICE_TYPE_FEST_H) == false && main?.mDeviceType?.contains(DEVICE_TYPE_FEST_X) == false)) {
      binding.chartCalibrationRl.visibility = View.GONE
    }
  }
  @SuppressLint("CheckResult")
  override fun onResume() {
    super.onResume()
    System.err.println("ChartFragment onResume")
    graphThreadFlag = true
    testThreadFlag = true
    startGraphEnteringDataThread()
    RxUpdateMainEvent.getInstance().uiChart
      .compose(main?.bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        if (context != null) {
          updateAllParameters()
          enabledSensorsUIBeforeConnection(it)

          //показываем индикацию выбранной группы ротации
          if (main?.driverVersionS != null) {
            val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
//            System.err.println("context ChartFragment NULL! ${mSettings!!.getBoolean(PreferenceKeys.SHOW_SECRET_SETTINGS, false)}")
            if (driverNum.toInt() >= 237) {
              showUIRotationGroup(mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false))
            } else {
              showUIRotationGroup(false)
            }
          }else {
            showUIRotationGroup(false)
          }
        } else {
          System.err.println("context ChartFragment NULL!")
        }
      }
  }
  override fun onPause() {
    super.onPause()
    graphThreadFlag = false
    System.err.println("ChartFragment onPause")
  }
  override fun onDestroy() {
    super.onDestroy()
    testThreadFlag = false
  }

  //////////////////////////////////////////////////////////////////////////////
  /**                          работа с графиками                            **/
  //////////////////////////////////////////////////////////////////////////////
  private fun createSet(): LineDataSet {
    val set = LineDataSet(null, null)
    set.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
    set.lineWidth = 2f
    set.color = Color.rgb(255, 171, 0)
    set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set.setCircleColor(Color.TRANSPARENT)
    set.circleHoleColor = Color.TRANSPARENT
    set.fillColor = ColorTemplate.getHoloBlue()
    set.highLightColor = Color.rgb(244, 117, 177)
    set.valueTextColor = Color.TRANSPARENT

    return set
  }
  private fun createSet2(): LineDataSet {
    val set2 = LineDataSet(null, null)
    set2.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
    set2.lineWidth = 2f
    set2.color = Color.WHITE
    set2.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set2.setCircleColor(Color.TRANSPARENT)
    set2.circleHoleColor = Color.TRANSPARENT
    set2.fillColor = ColorTemplate.getHoloBlue()
    set2.highLightColor = Color.rgb(244, 117, 177)
    set2.valueTextColor = Color.TRANSPARENT
    return set2
  }
  private fun createSet3(): LineDataSet {
    val set3 = LineDataSet(null, null)
    set3.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
    set3.lineWidth = 0.1f
    set3.color = Color.WHITE
    set3.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set3.setCircleColor(Color.TRANSPARENT)
    set3.circleHoleColor = Color.TRANSPARENT
    set3.fillColor = ColorTemplate.getHoloBlue()
    set3.highLightColor = Color.rgb(244, 117, 177)
    set3.valueTextColor = Color.TRANSPARENT
    return set3
  }
  private fun addEntry(sens1: Int, sens2: Int) {
    val data: LineData = binding.chartMainchart.data!!
    var set = data.getDataSetByIndex(0)
    var set2 = data.getDataSetByIndex(1)
    val set3: ILineDataSet
    if (set == null) {
      set = createSet()
      set2 = createSet2()
      set3 = createSet3()
      data.addDataSet(set)
      data.addDataSet(set2)
      data.addDataSet(set3)
    }


    if (set.entryCount > 200 ) {
      main?.runOnUiThread {
        set.removeFirst()
        set2.removeFirst()
        set.addEntryOrdered(Entry(1f, 255f))
      }
    } else {
      main?.runOnUiThread {
        data.addEntry(Entry(count.toFloat(), 255f), 2)
      }
    }


    main?.runOnUiThread {
      data.addEntry(Entry(count.toFloat(), sens1.toFloat()), 0)
      data.addEntry(Entry(count.toFloat(), sens2.toFloat()), 1)
      data.notifyDataChanged()

      binding.chartMainchart.notifyDataSetChanged()
      binding.chartMainchart.setVisibleXRangeMaximum(200f)
      binding.chartMainchart.moveViewToX(set.entryCount - 200.toFloat())
    }
    count += 1
  }
  private fun initializedSensorGraph() {
    binding.chartMainchart.contentDescription
    binding.chartMainchart.setTouchEnabled(false)
    binding.chartMainchart.isDragEnabled = false
    binding.chartMainchart.isDragDecelerationEnabled = false
    binding.chartMainchart.setScaleEnabled(false)
    binding.chartMainchart.setDrawGridBackground(false)
    binding.chartMainchart.setPinchZoom(false)
    binding.chartMainchart.setBackgroundColor(Color.TRANSPARENT)
    binding.chartMainchart.getHighlightByTouchPoint(1f, 1f)
    val data = LineData()
    val data2 = LineData()
    binding.chartMainchart.data = data
    binding.chartMainchart.data = data2
    binding.chartMainchart.legend.isEnabled = false
    binding.chartMainchart.description.textColor = Color.TRANSPARENT
    binding.chartMainchart.animateY(2000)

    val x: XAxis = binding.chartMainchart.xAxis
    x.textColor = Color.TRANSPARENT
    x.setDrawGridLines(false)
    x.axisMaximum = 4000000f
    x.setAvoidFirstLastClipping(true)
    x.position = XAxis.XAxisPosition.BOTTOM

    val y: YAxis = binding.chartMainchart.axisLeft
    y.textColor = Color.WHITE
    y.mAxisMaximum = 255f
    y.mAxisMinimum = 255f
    y.textSize = 0f
    y.textColor = Color.TRANSPARENT
    y.setDrawGridLines(true)
    y.setDrawAxisLine(false)
    y.setStartAtZero(true)
    y.gridColor = Color.WHITE
    binding.chartMainchart.axisRight.gridColor = Color.TRANSPARENT
    binding.chartMainchart.axisRight.axisLineColor = Color.TRANSPARENT
    binding.chartMainchart.axisRight.textColor = Color.TRANSPARENT
  }

  private fun startGraphEnteringDataThread() {
    graphThread = Thread {
      while (graphThreadFlag) {
          if (plotData) {
            addEntry(10, 255)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(115, 150)
            plotData = false
          }
          addEntry(main!!.getDataSens1(), main!!.getDataSens2())

        try {
          Thread.sleep(GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) {
        }
      }
    }
    graphThread?.start()
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {}
  override fun onNothingSelected() {}


  @SuppressLint("SetTextI18n", "Recycle")
  private fun updateAllParameters() {
    activity?.runOnUiThread  {
      binding.activeGestureTv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.ACTIVE_GESTURE_NUM, 0).toString()
      binding.nameTv.text = main?.mDeviceName
      binding.openCHSb.progress =
        mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, 30)
      binding.closeCHSb.progress =
        mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, 30)
      binding.swapSensorsSw.isChecked =
        mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)
      if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)) {
        binding.swapSensorsTv.text = resources.getString(R.string.on_sw)
      } else {
        binding.swapSensorsTv.text = resources.getString(R.string.off_sw)
      }
      if (!(main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X) ||
                main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H) ||
                main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_A))
      ) {
        binding.driverTv.text = resources.getString(R.string.driver) + (mSettings!!.getInt(
          main?.mDeviceAddress + PreferenceKeys.DRIVER_NUM,
          1
        )).toFloat() / 100 + "v"
      } else {
        binding.driverTv.text = resources.getString(R.string.driver) +main?.driverVersionS + "v"
      }
      binding.bmsTv.text = resources.getString(R.string.bms) + (mSettings!!.getInt(
        main?.mDeviceAddress + PreferenceKeys.BMS_NUM,
        1
      )).toFloat() / 100 + "v"
      binding.sensorTv.text = resources.getString(R.string.sens) + (mSettings!!.getInt(
        main?.mDeviceAddress + PreferenceKeys.SENS_NUM,
        1
      )).toFloat() / 100 + "v"
      ObjectAnimator.ofFloat(
        binding.limitCH1, "y", 320 * scale - 5f - (binding.openCHSb.progress.times(scale) * 1.04f)
      ).setDuration(200).start()
      ObjectAnimator.ofFloat(
        binding.limitCH2, "y", 320 * scale - 5f - (binding.closeCHSb.progress.times(scale) * 1.04f)
      ).setDuration(200).start()
      ObjectAnimator.ofFloat(
        binding.openBorder, "y", 320 * scale - 5f - (binding.openCHSb.progress.times(scale) * 1.04f)
      ).setDuration(200).start()
      ObjectAnimator.ofFloat(
        binding.closeBorder, "y", 320 * scale - 5f - (binding.closeCHSb.progress.times(scale) * 1.04f)
      ).setDuration(200).start()
      ObjectAnimator.ofInt(
        binding.correlatorNoiseThreshold1Sb,
        "progress",
        255 - (mSettings!!.getInt(
          main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM,
          16
        ))
      ).setDuration(200).start()
      ObjectAnimator.ofInt(
        binding.correlatorNoiseThreshold2Sb,
        "progress",
        255 - (mSettings!!.getInt(
          main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM,
          16
        ))
      ).setDuration(200).start()
      if (oldStateSync != main?.percentSynchronize!!) {
        ObjectAnimator.ofInt(binding.syncSb, "progress", oldStateSync, main?.percentSynchronize!!)
          .setDuration(1000).start()
        oldStateSync = main?.percentSynchronize!!
      }
    }
    modeEMGSend = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,9)
  }
  private fun sendCorrelatorNoiseThreshold(value: Int) {
    if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      main?.stage = "chart activity"
      main?.runSendCommand(byteArrayOf(
        (255 - binding.correlatorNoiseThreshold1Sb.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
        64, (255 - binding.correlatorNoiseThreshold2Sb.progress).toByte(), 6, 1, 0x10, 36, 18,
        44, 52, 64, 72, 0x40, 5, 64, modeEMGSend.toByte()
      ), SENS_OPTIONS_NEW_VM, 50)
    } else {
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
        main?.runWriteData(
          byteArrayOf(
            (255 - binding.correlatorNoiseThreshold1Sb.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
            64, (255 - binding.correlatorNoiseThreshold2Sb.progress).toByte(), 6, 1, 0x10, 36, 18,
            44, 52, 64, 72, 0x40, 5, 64
          ), SENS_OPTIONS_NEW, WRITE
        )
      } else {
        if (value == 1) {
          main?.bleCommandConnector(
            byteArrayOf(0x01, (255 - binding.correlatorNoiseThreshold1Sb.progress).toByte(), 0x01),
            SENS_OPTIONS,
            WRITE,
            11
          )
        }
        if (value == 2) {
          main?.bleCommandConnector(
            byteArrayOf(0x01, (255 - binding.correlatorNoiseThreshold2Sb.progress).toByte(), 0x02),
            SENS_OPTIONS,
            WRITE,
            11
          )
        }
      }
    }
  }
}
