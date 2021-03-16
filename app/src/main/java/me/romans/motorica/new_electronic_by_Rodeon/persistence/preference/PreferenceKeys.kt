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

package me.romans.motorica.new_electronic_by_Rodeon.persistence.preference

import android.util.Pair


object PreferenceKeys {
  val NEWBE = Pair("NEWBE", true)
  const val THRESHOLDS_BLOCKING = "THRESHOLDS_BLOCKING"
  val INIT_CAPACITY = Pair("INIT_CAPACITY", false)
  val WATER_GOAL = Pair("WaterGoal", "2000")
  val LOCALINDEX = Pair("localIndex", 0)
  val CUP_CAPICITY = Pair("MyCup", "0")
  val ALARM_WEAHTER = Pair("setWeatherAlarm", false)
  val BUBBLE_COLOR = Pair("BUBBLE_COLOR", "#1c9ade")
}
