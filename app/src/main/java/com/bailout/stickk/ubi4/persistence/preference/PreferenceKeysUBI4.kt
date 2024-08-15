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
package com.bailout.stickk.ubi4.persistence.preference


object PreferenceKeysUBI4 {
    //кол-во байт в посылках
    const val HEADER_BLE_OFFSET = 7
    const val READ_DEVICE_ADDITIONAL_PARAMETR_DATA = 2
    const val ADDITIONAL_INFO_SEG = 8

    const val END_PARAMETERS_ARRAY_KEY = 23654328
    const val DEVICE_NAME = "DEVICE_NAME"
    const val CONNECTED_DEVICE_ADDRESS = "CONNECTED_DEVICE_ADDRESS"
    const val CONNECTED_DEVICE = "CONNECTED_DEVICE"

    const val APP_PREFERENCES = "APP_PREFERENCES_UBI4"
    const val UBI4_MODE_ACTIVATED = "UBI4_MODE_ACTIVATED"

    enum class BaseCommands(val number: Byte) {
        DEVICE_INFORMATION (0x01),
        DATA_MANAGER (0x02),
        WRITE_FW_COMMAND (0x03),
        DEVICE_ACCESS_COMMAND (0x04),
        ECHO_COMMAND (0x05),
        SUB_DEVICE_MANAGER (0x06),
        GET_DEVICE_STATUS (0x07),
        DATA_TRANSFER_SETTINGS (0x08)
    }

    enum class DeviceInformationCommand(val number: Byte) {
        INICIALIZE_INFORMATION (0x01),
        READ_DEVICE_PARAMETRS (0x02),
        READ_DEVICE_ADDITIONAL_PARAMETR (0x03),

        GET_SERIAL_NUMBER (0x04),
        SET_SERIAL_NUMBER (0x05),

        GET_DEVICE_NAME (0x06),
        SET_DEVICE_NAME (0x07),

        GET_DEVICE_ROLE (0x08),
        SET_DEVICE_ROLE (0x09)
    }

    enum class DataManagerCommand(val number: Byte) {
        READ_AVAILABLE_SLOTS (0x01),
        WRITE_SLOT (0x02),
        READ_DATA (0x03),
        WRITE_DATA (0x04),
        RESET_TO_FACTORY (0x05),
        SAVE_DATA (0x06)
    }

    enum class ParameterWidgetType(val number: Byte) {
        PWTE_UNKNOW (0x00),
        PWTE_COMMAND (0x01),
        PWTE_COMBOBOX_ENUM (0x02),
        PWTE_COMBOBOX_STRING (0x03),
        PWTE_ONE_CHANNEL_PLOT (0x04),
        PWTE_MULTY_CHANNEL_PLOT (0x05),
        PWTE_ONE_CHANNEL_PLOT_LEGEND (0x06),
        PWTE_MULTY_CHANNEL_PLOT_LEGEND (0x07),
        PWTE_EMG_GESTURE_CHANGE_SETTINGS (0x08),
        PWTE_GESTURE_SETTINGS (0x09),
        PWTE_CALIB_STATUS (0x0a),
        PWTE_CONTROL_MODE (0x0b),
        PWTE_OPEN_CLOSE_THRESHOLD (0x0c),
        PWTE_SCALAR (0x0d)
    }

    enum class ParameterWidgetCode(val number: Byte) {
        PWCE_UNKNOW (0x00),
        PWCE_BUTTON (0x01),
        PWCE_SWITCH (0x02),
        PWCE_COMBOBOX (0x03),
        PWCE_SLIDER (0x04),
        PWCE_PLOT (0x05),
        PWCE_SPINBOX (0x06), //окно ввода цифр со стрелочками инкриментации/декрементации
        PWCE_EMG_GESTURE_CHANGE_SETTINGS (0x07),
        PWCE_GESTURE_SETTINGS (0x08),
        PWCE_CALIB_STATUS (0x09),
        PWCE_CONTROL_MODE (0x0a),
        PWCE_OPEN_CLOSE_THRESHOLD (0x0b)
    }

    enum class ParameterWidgetLabel(val number: Byte) {
        PWLE_UNKNOW (0x00),
        PWLE_OPEN (0x01),
        PWLE_CLOSE (0x02),
        PWLE_CALIBRATE (0x03),
        PWLE_RESET (0x04),
        PWLE_CONTROL_SETTINGS (0x05),
        PWLE_OPEN_CLOSE_THRESHOLD (0x06),
        PWLE_SELECT_GESTURE (0x07)
    }

    enum class ParameterWidgetDisplayCode(val number: Byte) {
        PWDCE_UNKNOW (0x00),
        PWDCE_MAIN_DISPLAY (0x01)
    }

    enum class ParameterWidgetLabelType(val number: Byte) {
        PWLTE_CODE_LABEL(0x00),
        PWLTE_STRING_LABEL(0x01)
    }
}
