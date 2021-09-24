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

package me.start.motorica.new_electronic_by_Rodeon.persistence.preference

import android.util.Pair


object PreferenceKeys {
  val NEWBE = Pair("NEWBE", true)
  const val NUM_GESTURES = 8
  const val APP_PREFERENCES = "APP_PREFERENCES"
  const val DEVICE_ADDRESS_CONNECTED = "DEVICE_ADDRESS_CONNECTED"
  const val THRESHOLDS_BLOCKING = "THRESHOLDS_BLOCKING"
  const val ADVANCED_SETTINGS = "ADVANCED_SETTINGS"
  const val OPEN_CH_NUM = "OPEN_CH_NUM"
  const val CLOSE_CH_NUM = "CLOSE_CH_NUM"
  const val CORRELATOR_NOISE_THRESHOLD_1_NUM = "CORRELATOR_NOISE_THRESHOLD_1_NUM"
  const val CORRELATOR_NOISE_THRESHOLD_2_NUM = "CORRELATOR_NOISE_THRESHOLD_2_NUM"
  const val SET_GESTURE_NUM = "SET_GESTURE_NUM"
  const val DRIVER_NUM = "DRIVER_NUM"
  const val BMS_NUM = "BMS_NUM"
  const val SENS_NUM = "SENS_NUM"
  const val SWAP_LEFT_RIGHT_SIDE = "SWAP_LEFT_RIGHT_SIDE"

  const val SHUTDOWN_CURRENT_NUM = "SHUTDOWN_CURRENT_NUM"
  const val STAR_UP_STEP_NUM = "STAR_UP_STEP_NUM"
  const val DEAD_ZONE_NUM = "DEAD_ZONE_NUM"
  const val USE_BRAKE_MOTOR_NUM = "USE_BRAKE_MOTOR_NUM"
  const val SET_REVERSE_NUM = "SET_REVERSE_NUM"
  const val SWAP_OPEN_CLOSE_NUM = "SWAP_OPEN_CLOSE_NUM"
  const val SET_ONE_CHANNEL_NUM = "SET_ONE_CHANNEL_NUM"
  const val SET_SENSORS_GESTURE_SWITCHES_NUM = "SET_SENSORS_GESTURE_SWITCHES_NUM"
  const val SET_MODE_NUM = "SET_MODE_NUM"
  const val SET_PEAK_TIME_NUM = "SET_PEAK_TIME_NUM"
  const val SET_DOWNTIME_NUM = "SET_DOWNTIME_NUM"


  const val GESTURE_OPEN_STATE_NUM = "GESTURE_OPEN_STATE_NUM"
  const val GESTURE_CLOSE_STATE_NUM = "GESTURE_CLOSE_STATE_NUM"

  const val GESTURE_CLOSE_STATE_FINGER_1_NUM =  "GESTURE_CLOSE_STATE_FINGER_1_NUM"
  const val GESTURE_CLOSE_STATE_FINGER_2_NUM =  "GESTURE_CLOSE_STATE_FINGER_2_NUM"
  const val GESTURE_CLOSE_STATE_FINGER_3_NUM =  "GESTURE_CLOSE_STATE_FINGER_3_NUM"
  const val GESTURE_CLOSE_STATE_FINGER_4_NUM =  "GESTURE_CLOSE_STATE_FINGER_4_NUM"
  const val GESTURE_CLOSE_STATE_FINGER_5_NUM =  "GESTURE_CLOSE_STATE_FINGER_5_NUM"
  const val GESTURE_CLOSE_STATE_FINGER_6_NUM =  "GESTURE_CLOSE_STATE_FINGER_6_NUM"
  const val GESTURE_OPEN_STATE_FINGER_1_NUM =  "GESTURE_OPEN_STATE_FINGER_1_NUM"
  const val GESTURE_OPEN_STATE_FINGER_2_NUM =  "GESTURE_OPEN_STATE_FINGER_2_NUM"
  const val GESTURE_OPEN_STATE_FINGER_3_NUM =  "GESTURE_OPEN_STATE_FINGER_3_NUM"
  const val GESTURE_OPEN_STATE_FINGER_4_NUM =  "GESTURE_OPEN_STATE_FINGER_4_NUM"
  const val GESTURE_OPEN_STATE_FINGER_5_NUM =  "GESTURE_OPEN_STATE_FINGER_5_NUM"
  const val GESTURE_OPEN_STATE_FINGER_6_NUM =  "GESTURE_OPEN_STATE_FINGER_6_NUM"

  const val SELECT_GESTURE_NUM = "SELECT_GESTURE_NUM"
  const val SELECT_GESTURE_SETTINGS_NUM = "SELECT_GESTURE_SETTINGS_NUM"


  val INIT_CAPACITY = Pair("INIT_CAPACITY", false)
  val WATER_GOAL = Pair("WaterGoal", "2000")
  val LOCALINDEX = Pair("localIndex", 0)
  val CUP_CAPICITY = Pair("MyCup", "0")
  val ALARM_WEAHTER = Pair("setWeatherAlarm", false)
  val BUBBLE_COLOR = Pair("BUBBLE_COLOR", "#1c9ade")
}
