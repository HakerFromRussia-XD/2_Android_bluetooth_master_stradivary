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
  const val APP_PREFERENCES = "APP_PREFERENCES"
  const val THRESHOLDS_BLOCKING = "THRESHOLDS_BLOCKING"
  const val ADVANCED_SETTINGS = "ADVANCED_SETTINGS"
  const val OPEN_CH_NUM = "OPEN_CH_NUM"
  const val CLOSE_CH_NUM = "CLOSE_CH_NUM"
  const val CORRELATOR_NOISE_THRESHOLD_1_NUM = "CORRELATOR_NOISE_THRESHOLD_1_NUM"
  const val CORRELATOR_NOISE_THRESHOLD_2_NUM = "CORRELATOR_NOISE_THRESHOLD_2_NUM"
  const val DRIVER_NUM = "DRIVER_NUM"
  const val BMS_NUM = "BMS_NUM"
  const val SENS_NUM = "SENS_NUM"



  const val SHUTDOWN_CURRENT_NUM = "SHUTDOWN_CURRENT_NUM"
  const val STAR_UP_STEP_NUM = "STAR_UP_STEP_NUM"
  const val DEAD_ZONE_NUM = "DEAD_ZONE_NUM"
  const val USE_BRAKE_MOTOR_NUM = "USE_BRAKE_MOTOR_NUM"
  const val SET_REVERSE_NUM = "SET_REVERSE_NUM"
  const val SWAP_OPEN_CLOSE_NUM = "SWAP_OPEN_CLOSE_NUM"

  val INIT_CAPACITY = Pair("INIT_CAPACITY", false)
  val WATER_GOAL = Pair("WaterGoal", "2000")
  val LOCALINDEX = Pair("localIndex", 0)
  val CUP_CAPICITY = Pair("MyCup", "0")
  val ALARM_WEAHTER = Pair("setWeatherAlarm", false)
  val BUBBLE_COLOR = Pair("BUBBLE_COLOR", "#1c9ade")
}
