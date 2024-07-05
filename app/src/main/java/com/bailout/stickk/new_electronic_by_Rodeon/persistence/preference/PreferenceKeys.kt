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

package com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference

import android.util.Pair


object PreferenceKeys {
  val NEWBE = Pair("NEWBE", true)
  const val DEVICE_NAME = "DEVICE_NAME"
  const val DEVICE_ADDR = "DEVICE_ADDR"
  const val NUM_GESTURES = 14 //максимальное количество жестов для формирования массивов данных
  const val NUM_ACTIVE_GESTURES = 8 //максимальное количество жестов для отрисовки в UI (ограничение для России 8)
  const val APP_PREFERENCES = "APP_PREFERENCES"
  const val DEVICE_ADDRESS_CONNECTED = "DEVICE_ADDRESS_CONNECTED"
  const val THRESHOLDS_BLOCKING = "THRESHOLDS_BLOCKING"
  const val ADVANCED_SETTINGS = "ADVANCED_SETTINGS"
  const val OPEN_CH_NUM = "OPEN_CH_NUM"
  const val CLOSE_CH_NUM = "CLOSE_CH_NUM"
  const val CORRELATOR_NOISE_THRESHOLD_1_NUM = "CORRELATOR_NOISE_THRESHOLD_1_NUM"
  const val CORRELATOR_NOISE_THRESHOLD_2_NUM = "CORRELATOR_NOISE_THRESHOLD_2_NUM"
//  const val SET_GESTURE_NUM = "SET_GESTURE_NUM"
  const val DRIVER_NUM = "DRIVER_NUM"
  const val DRIVER_VERSION_STRING = "DRIVER_VERSION_STRING"
  const val BMS_NUM = "BMS_NUM"
  const val SENS_NUM = "SENS_NUM"
  const val ACTIVE_GESTURE_NUM = "ACTIVE_GESTURE_NUM"
  const val SWAP_LEFT_RIGHT_SIDE = "SWAP_LEFT_RIGHT_SIDE"
  const val SET_FINGERS_DELAY = "SET_FINGERS_DELAY"
  const val CALIBRATING_STATUS = "CALIBRATING_STATUS"

  const val SHUTDOWN_CURRENT_NUM = "SHUTDOWN_CURRENT_NUM"
  const val SHUTDOWN_CURRENT_NUM_1 = "SHUTDOWN_CURRENT_NUM_1"
  const val SHUTDOWN_CURRENT_NUM_2 = "SHUTDOWN_CURRENT_NUM_2"
  const val SHUTDOWN_CURRENT_NUM_3 = "SHUTDOWN_CURRENT_NUM_3"
  const val SHUTDOWN_CURRENT_NUM_4 = "SHUTDOWN_CURRENT_NUM_4"
  const val SHUTDOWN_CURRENT_NUM_5 = "SHUTDOWN_CURRENT_NUM_5"
  const val SHUTDOWN_CURRENT_NUM_6 = "SHUTDOWN_CURRENT_NUM_6"
//  const val STAR_UP_STEP_NUM = "STAR_UP_STEP_NUM"
//  const val DEAD_ZONE_NUM = "DEAD_ZONE_NUM"
//  const val USE_BRAKE_MOTOR_NUM = "USE_BRAKE_MOTOR_NUM"
  const val SET_SCALE = "SET_SCALE"
  const val SET_REVERSE_NUM = "SET_REVERSE_NUM"
  const val SWAP_OPEN_CLOSE_NUM = "SWAP_OPEN_CLOSE_NUM"
  const val SET_ONE_CHANNEL_NUM = "SET_ONE_CHANNEL_NUM"
  const val SET_SENSORS_LOCK_NUM = "SET_SENSORS_LOCK_NUM"
  const val HOLD_TO_LOCK_TIME_NUM = "HOLD_TO_LOCK_TIME_NUM"
  const val SET_SENSORS_GESTURE_SWITCHES_NUM = "SET_SENSORS_GESTURE_SWITCHES_NUM"
  const val SET_MODE_NUM = "SET_MODE_NUM"
  const val SET_PEAK_TIME_VM_NUM = "SET_PEAK_TIME_VM_NUM"
  const val SET_PEAK_TIME_NUM = "SET_PEAK_TIME_NUM"
  const val SET_DOWNTIME_NUM = "SET_DOWNTIME_NUM"
  const val SET_MODE_NEW_NUM = "SET_MODE_NEW_NUM"
  const val SET_MODE_SMART_CONNECTION = "SET_MODE_SMART_CONNECTION"
  const val SET_MODE_EMG_SENSORS = "SET_MODE_EMG_SENSORS"
  const val SET_MODE_PROSTHESIS = "SET_MODE_PROSTHESIS"
  const val LAST_CONNECTION_MAC = "LAST_CONNECTION_MAC"
  const val FILTERING_OUR_DEVISES = "FILTERING_OUR_DEVISES"
  const val ACTIVATE_RSSI_SHOW = "ACTIVATE_RSSI_SHOW"
  const val ENTER_SECRET_PIN = "ENTER_SECRET_PIN"
  const val MAX_STAND_CYCLES = "MAX_STAND_CYCLES"
  const val AUTOCALIBRATION_MODE = "AUTOCALIBRATION_MODE"
  const val GESTURE_TYPE = "GESTURE_TYPE"
  const val FIRST_LOAD_ACCOUNT_INFO = "FIRST_LOAD_ACCOUNT_INFO"
  const val USE_APP_PIN_CODE = "USE_APP_PIN_CODE"
  const val APP_PIN_CODE = "APP_PIN_CODE"
  const val GAME_LAUNCH_RATE = "GAME_LAUNCH_RATE"


  const val ACCOUNT_MODEL_PROSTHESIS = "ACCOUNT_MODEL_PROSTHESIS"
  const val ACCOUNT_SIZE_PROSTHESIS = "ACCOUNT_SIZE_PROSTHESIS"
  const val ACCOUNT_SIDE_PROSTHESIS = "ACCOUNT_SIDE_PROSTHESIS"
  const val ACCOUNT_STATUS_PROSTHESIS = "ACCOUNT_STATUS_PROSTHESIS"
  const val ACCOUNT_DATE_TRANSFER_PROSTHESIS = "ACCOUNT_DATE_TRANSFER_PROSTHESIS"
  const val ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS = "ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS"
  const val ACCOUNT_ROTATOR_PROSTHESIS = "ACCOUNT_ROTATOR_PROSTHESIS"
  const val ACCOUNT_ACCUMULATOR_PROSTHESIS = "ACCOUNT_ACCUMULATOR_PROSTHESIS"
  const val ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS = "ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS"
  const val ACCOUNT_MANAGER_FIO = "ACCOUNT_MANAGER_FIO"
  const val ACCOUNT_MANAGER_PHONE = "ACCOUNT_MANAGER_PHONE"

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

  const val GESTURE_CLOSE_DELAY_FINGER =  "GESTURE_CLOSE_DELAY_FINGER"
  const val GESTURE_OPEN_DELAY_FINGER =  "GESTURE_OPEN_DELAY_FINGER"


  const val SELECT_GESTURE_NUM = "SELECT_GESTURE_NUM"
  const val SELECT_GESTURE_SETTINGS_NUM = "SELECT_GESTURE_SETTINGS_NUM"
  const val RECEIVE_FINGERS_DELAY_BOOL = "RECEIVE_FINGERS_DELAY_BOOL"

  const val START_GESTURE_IN_LOOP = "START_GESTURE_IN_LOOP"
  const val END_GESTURE_IN_LOOP = "END_GESTURE_IN_LOOP"

  const val SHOW_HELP_ACCENT = "SHOW_HELP_ACCENT"
}
