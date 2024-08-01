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
}
