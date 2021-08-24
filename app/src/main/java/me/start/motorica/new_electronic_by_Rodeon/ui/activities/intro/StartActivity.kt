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
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
  private var gestureNameList: ArrayList<String>? = ArrayList()

  @Inject
  lateinit var preferenceManager: PreferenceManager
  @Inject
  lateinit var sqliteManager: SqliteManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WDApplication.component.inject(this)

    val intent = intent
    mDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME)
    mDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS)
    mDeviceType = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_TYPE)
//    preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, true)//для выключения интро при последующем запуске

    if (!preferenceManager.getBoolean(PreferenceKeys.NEWBE.first, PreferenceKeys.NEWBE.second)) {
      val intent = Intent(this, MainActivity::class.java)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, mDeviceName)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE, mDeviceType)
      startActivity(intent)
      loadData()
      System.err.println("SAVE before if")
      if (gestureNameList?.get(0) ?: "lol" == "lol") {
        System.err.println("SAVE in if")
        firstSetGesturesName ()
      }
      finish()
      return
    } else {
      val intent = Intent(this, MainActivity::class.java)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, mDeviceName)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
      intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE, mDeviceType)
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
    intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE, mDeviceType)
    startActivity(intent)
    preferenceManager.putBoolean(PreferenceKeys.NEWBE.first, false)//для выключения интро при последующем запуске
    finish()
  }

  private fun firstSetGesturesName () {
    gestureNameList?.add(getString(R.string.gesture_1))
    gestureNameList?.add(getString(R.string.gesture_2))
    gestureNameList?.add(getString(R.string.gesture_3))
    gestureNameList?.add(getString(R.string.gesture_4))
    gestureNameList?.add(getString(R.string.gesture_5))
    gestureNameList?.add(getString(R.string.gesture_6))
    gestureNameList?.add(getString(R.string.gesture_7))
    gestureNameList?.add(getString(R.string.gesture_8))
    saveData()
  }
  private fun saveData() {
    val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val gson = Gson()
    val json = gson.toJson(gestureNameList)
    editor.putString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, json)
    editor.apply()
    System.err.println("SAVE gestureNameList")
  }

  private fun loadData() {
    val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
    val gson = Gson()
    val json = sharedPreferences.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, null)
    val type = object : TypeToken<ArrayList<String>>() {}.type
    gestureNameList = gson.fromJson<ArrayList<String>>(json, type)
  }
}
