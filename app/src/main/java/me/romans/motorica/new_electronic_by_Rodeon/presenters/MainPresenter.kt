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

package me.romans.motorica.new_electronic_by_Rodeon.presenters

import android.content.Context
import android.os.Bundle

import me.romans.motorica.new_electronic_by_Rodeon.WDApplication
import me.romans.motorica.new_electronic_by_Rodeon.compose.BasePresenter
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.romans.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.romans.motorica.new_electronic_by_Rodeon.viewTypes.MainActivityView

import javax.inject.Inject
import kotlin.experimental.and
import kotlin.experimental.xor

class MainPresenter : BasePresenter<MainActivityView>() {

  @Inject
  lateinit var preferenceManager: PreferenceManager
  @Inject
  lateinit var sqliteManager: SqliteManager

  override fun onCreate(context: Context, savedInstanceState: Bundle?) {
    super.onCreate(context, savedInstanceState)
    WDApplication.component.inject(this)
  }

  fun addRecord(value: String) = sqliteManager.addRecord(value)

  var weatherAlarm: Boolean
    get() = preferenceManager.getBoolean(PreferenceKeys.ALARM_WEAHTER.first, PreferenceKeys.ALARM_WEAHTER.second)
    set(value) = preferenceManager.putBoolean(PreferenceKeys.ALARM_WEAHTER.first, value)

  fun getShowAdvancedSettings(): Boolean {
    return preferenceManager.getBoolean(PreferenceKeys.ADVANCED_SETTINGS, false)
  }
}
