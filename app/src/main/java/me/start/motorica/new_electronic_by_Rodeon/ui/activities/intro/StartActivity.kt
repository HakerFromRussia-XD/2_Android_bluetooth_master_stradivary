/*Licensed under the Apache License, Version 2.0 (the "License");
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

package me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseView
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.intro.SlideFragment
import javax.inject.Inject


@Suppress("NAME_SHADOWING")
class StartActivity : AppIntro(), BaseView {

  private var mDeviceName: String? = null
  private var mDeviceAddress: String? = null
  private var mDeviceType: String? = null
  private var gestureNameList =  ArrayList<String>()
  private var mSettings: SharedPreferences? = null

  @Inject
  lateinit var preferenceManager: PreferenceManager
  @Inject
  lateinit var sqliteManager: SqliteManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WDApplication.component.inject(this)
    mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, MODE_PRIVATE)


    val intent = intent
    mDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME)
    mDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS)
    mDeviceType = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
//    preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, true)//для выключения интро при последующем запуске

    if (!preferenceManager.getBoolean(PreferenceKeys.NEWBE.first, PreferenceKeys.NEWBE.second)) {
      val intent = Intent(this, MainActivity::class.java)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, mDeviceName)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A, mDeviceType)
      startActivity(intent)
      myLoadGesturesList()
      if (gestureNameList[0] == "lol") { firstSetGesturesName () }
      finish()
      return
    } else {
      val intent = Intent(this, MainActivity::class.java)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, mDeviceName)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A, mDeviceType)
      startActivity(intent)
      finish()
      preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, false)
      firstSetGesturesName()
      return
//      initializeUI()
    }
  }

  override fun initializeUI() {
    addSlide(SlideFragment.newInstance(R.layout.intro1))
    addSlide(SlideFragment.newInstance(R.layout.intro2))
    addSlide(SlideFragment.newInstance(R.layout.intro3))
    addSlide(SlideFragment.newInstance(R.layout.intro4))
    setDoneText("start")
  }

  override fun onSkipPressed(currentFragment: Fragment?) {
    super.onSkipPressed(currentFragment)
    activityStart()
  }

  override fun onDonePressed(currentFragment: Fragment?) {
    super.onDonePressed(currentFragment)
    activityStart()
  }

  private fun activityStart() {
//    startActivity(Intent(this, SetWeightActivity::class.java))//было
    val intent = Intent(this, MainActivity::class.java)
    intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, mDeviceName)
    intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
    intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A, mDeviceType)
    startActivity(intent)
    preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, false)//для выключения интро при последующем запуске
    finish()
  }

  private fun firstSetGesturesName () { //функция работает при установке новой версии приложения поверх старой
    gestureNameList.clear()
    gestureNameList.add(getString(R.string.gesture_1))
    gestureNameList.add(getString(R.string.gesture_2))
    gestureNameList.add(getString(R.string.gesture_3))
    gestureNameList.add(getString(R.string.gesture_4))
    gestureNameList.add(getString(R.string.gesture_5))
    gestureNameList.add(getString(R.string.gesture_6))
    gestureNameList.add(getString(R.string.gesture_7))
    gestureNameList.add(getString(R.string.gesture_8))

    for (i in 0 until gestureNameList.size) {
      mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, gestureNameList[i])
    }
  }

//  private fun firstInitGesturesName () {
//
//  }

  private fun mySaveText(key: String, text: String) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putString(key, text)
    editor.apply()
  }

  private fun myLoadGesturesList() {
    val text = "lol"
    for (i in 0 until PreferenceKeys.NUM_GESTURES) {
      gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
    }
  }
}
