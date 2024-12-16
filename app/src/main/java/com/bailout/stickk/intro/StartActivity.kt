package com.bailout.stickk.introimport android.content.Intentimport android.content.SharedPreferencesimport android.os.Bundleimport com.bailout.stickk.Rimport com.bailout.stickk.new_electronic_by_Rodeon.WDApplicationimport com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManagerimport com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseViewimport com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeysimport com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceManagerimport com.bailout.stickk.new_electronic_by_Rodeon.persistence.sqlite.SqliteManagerimport com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivityimport com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.intro.SlideFragmentimport com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4import com.github.paolorotolo.appintro.AppIntroimport javax.inject.Inject@Suppress("NAME_SHADOWING")class StartActivity : AppIntro(), BaseView {  private var mDeviceName: String? = null  private var mDeviceAddress: String? = null  private var mDeviceType: String? = null  private var gestureNameList =  ArrayList<String>()  private var mSettings: SharedPreferences? = null  @Inject  lateinit var preferenceManager: PreferenceManager  @Inject  lateinit var sqliteManager: SqliteManager  override fun onCreate(savedInstanceState: Bundle?) {    super.onCreate(savedInstanceState)//    System.err.println(" LOLOLOEFWEF --->  StartActivity onCreate")    WDApplication.component.inject(this)    mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, MODE_PRIVATE)    var intent = intent    mDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME)    mDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS)    mDeviceType = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_TYPE)    if (checkUBI4(mDeviceName)) {      saveBool(PreferenceKeysUBI4.UBI4_MODE_ACTIVATED, true)      intent = Intent(this, MainActivityUBI4::class.java)      mySaveText(key = PreferenceKeysUBI4.CONNECTED_DEVICE, text = mDeviceName)      mySaveText(key = PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS, text = mDeviceAddress)    } else {      saveBool(PreferenceKeysUBI4.UBI4_MODE_ACTIVATED, false)      intent = Intent(this, MainActivity::class.java)    }    intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, mDeviceName)    intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)    intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE, mDeviceType)    startActivity(intent)    myLoadGesturesList()    System.err.println("88 LAST_CONNECTION_MAC: " + gestureNameList[0])    if (gestureNameList[0] == "lol") { firstSetGesturesName () }    finish()//    preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, true)//для выключения интро при последующем запуске    if (preferenceManager.getBoolean(PreferenceKeys.NEWBE.first, false)) {      //блок кода выполняется при последующих запусках приложения      System.err.println(" Start activity second start ")      return    } else {      //блок кода выполняется при первом запуске приложения      saveBool(PreferenceKeys.SET_MODE_SMART_CONNECTION, true)      preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, true)      System.err.println(" Start activity first start ")      return    }  }  override fun initializeUI() {    System.err.println("initializeUI!!!!")    addSlide(SlideFragment.newInstance(R.layout.intro1))    addSlide(SlideFragment.newInstance(R.layout.intro2))    addSlide(SlideFragment.newInstance(R.layout.intro3))    addSlide(SlideFragment.newInstance(R.layout.intro4))    setDoneText("start")  }  private fun checkUBI4(deviceName: String?): Boolean {    return deviceName?.contains("UBIv4") ?: false  }  private fun firstSetGesturesName () { //функция работает при установке новой версии приложения поверх старой    gestureNameList.clear()    gestureNameList.add(getString(R.string.gesture_1_btn))    gestureNameList.add(getString(R.string.gesture_2_btn))    gestureNameList.add(getString(R.string.gesture_3_btn))    gestureNameList.add(getString(R.string.gesture_4_btn))    gestureNameList.add(getString(R.string.gesture_5_btn))    gestureNameList.add(getString(R.string.gesture_6_btn))    gestureNameList.add(getString(R.string.gesture_7_btn))    gestureNameList.add(getString(R.string.gesture_8_btn))    gestureNameList.add(getString(R.string.gesture_9_btn))    gestureNameList.add(getString(R.string.gesture_10_btn))    gestureNameList.add(getString(R.string.gesture_11_btn))    gestureNameList.add(getString(R.string.gesture_12_btn))    gestureNameList.add(getString(R.string.gesture_13_btn))    gestureNameList.add(getString(R.string.gesture_14_btn))    gestureNameList.add(getString(R.string.gesture_15_btn))    gestureNameList.add(getString(R.string.gesture_16_btn))    gestureNameList.add(getString(R.string.gesture_17_btn))    gestureNameList.add(getString(R.string.gesture_18_btn))    gestureNameList.add(getString(R.string.gesture_19_btn))    gestureNameList.add(getString(R.string.gesture_20_btn))    gestureNameList.add(getString(R.string.gesture_21_btn))    gestureNameList.add(getString(R.string.gesture_22_btn))    gestureNameList.add(getString(R.string.gesture_23_btn))    gestureNameList.add(getString(R.string.gesture_24_btn))    gestureNameList.add(getString(R.string.gesture_25_btn))    for (i in 0 until gestureNameList.size) {      System.err.println("8 LAST_CONNECTION_MAC: "+ PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + mDeviceAddress + i)      mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM  + mDeviceAddress + i, gestureNameList[i])    }  }  private fun mySaveText(key: String, text: String?) {    val editor: SharedPreferences.Editor = mSettings!!.edit()    editor.putString(key, text)    editor.apply()  }  private fun saveBool(key: String, variable: Boolean) {    val editor: SharedPreferences.Editor = mSettings!!.edit()    editor.putBoolean(key, variable)    editor.apply()  }  private fun myLoadGesturesList() {    System.err.println("8 LAST_CONNECTION_MAC: $mDeviceAddress")    for (i in 0 until PreferenceKeys.NUM_GESTURES) {      gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + mDeviceAddress + i, "lol").toString())    }  }}