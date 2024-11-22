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
    const val END_PARAMETERS_ARRAY_KEY = 23654328
    const val CONNECTED_DEVICE_ADDRESS = "CONNECTED_DEVICE_ADDRESS"
    const val CONNECTED_DEVICE = "CONNECTED_DEVICE"
    const val DEVICE_ID_IN_SYSTEM_UBI4 = "DEVICE_ID_IN_SYSTEM_UBI4"
    const val PARAMETER_ID_IN_SYSTEM_UBI4 = "PARAMETER_ID_IN_SYSTEM_UBI4"
    const val GESTURE_ID_IN_SYSTEM_UBI4 = "GESTURE_ID_IN_SYSTEM_UBI4"

    const val UBI4_MODE_ACTIVATED = "UBI4_MODE_ACTIVATED"


    enum class BaseCommands(val number: Byte) {
        DEVICE_INFORMATION(0x01),
        DATA_MANAGER(0x02),
        WRITE_FW_COMMAND(0x03),
        DEVICE_ACCESS_COMMAND(0x04),
        ECHO_COMMAND(0x05),
        SUB_DEVICE_MANAGER(0x06),
        GET_DEVICE_STATUS(0x07),
        DATA_TRANSFER_SETTINGS(0x08),
        COMPLEX_PARAMETER_TRANSFER(0x09),
    }

    enum class DeviceInformationCommand(val number: Byte) {
        INICIALIZE_INFORMATION(0x01),
        READ_DEVICE_PARAMETRS(0x02),
        READ_DEVICE_ADDITIONAL_PARAMETRS(0x03),

        READ_SUB_DEVICES_FIRST_INFO(0x04),
        READ_SUB_DEVICE_INFO(0x05),
        READ_SUB_DEVICE_PARAMETERS(0x06),
        READ_SUB_DEVICE_ADDITIONAL_PARAMETER(0x07),
        SUB_DEVICE_PARAMETER_INIT_READ(0x08),
        SUB_DEVICE_PARAMETER_INIT_WRITE(0x09),

        GET_SERIAL_NUMBER(0x0a),
        SET_SERIAL_NUMBER(0x0b),

        GET_DEVICE_NAME(0x0c),
        SET_DEVICE_NAME(0x0d),

        GET_DEVICE_ROLE(0x0e),
        SET_DEVICE_ROLE(0x0f)
    }

    enum class DataManagerCommand(val number: Byte) {
        READ_AVAILABLE_SLOTS(0x01),
        WRITE_SLOT(0x02),
        READ_DATA(0x03),
        WRITE_DATA(0x04),
        RESET_TO_FACTORY(0x05),
        SAVE_DATA(0x06)
    }

    //используется для определения типов данных в определённых вью
    enum class ParameterWidgetType(val number: Byte) {
        PWTE_UNKNOW(0x00),
        PWTE_COMMAND(0x01),
        PWTE_COMBOBOX_ENUM(0x02),
        PWTE_COMBOBOX_STRING(0x03),
        PWTE_ONE_CHANNEL_PLOT(0x04),
        PWTE_MULTY_CHANNEL_PLOT(0x05),
        PWTE_ONE_CHANNEL_PLOT_LEGEND(0x06),
        PWTE_MULTY_CHANNEL_PLOT_LEGEND(0x07),
        PWTE_EMG_GESTURE_CHANGE_SETTINGS(0x08),
        PWTE_GESTURE_SETTINGS(0x09),
        PWTE_CALIB_STATUS(0x0a),
        PWTE_CONTROL_MODE(0x0b),
        PWTE_OPEN_CLOSE_THRESHOLD(0x0c),
        PWTE_SCALAR(0x0d)
    }

    //используется для отрисовки вью
    enum class ParameterWidgetCode(val number: Byte) {
        PWCE_UNKNOW(0x00),
        PWCE_BUTTON(0x01),
        PWCE_SWITCH(0x02),
        PWCE_COMBOBOX(0x03),
        PWCE_SLIDER(0x04),
        PWCE_PLOT(0x05),
        PWCE_SPINBOX(0x06), //окно ввода цифр со стрелочками инкриментации/декрементации

        PWCE_EMG_GESTURE_CHANGE_SETTINGS(0x07),
        PWCE_GESTURE_SETTINGS(0x08),
        PWCE_CALIB_STATUS(0x09),
        PWCE_CONTROL_MODE(0x0a),
        PWCE_OPEN_CLOSE_THRESHOLD(0x0b),
        PWCE_PLOT_AND_1_THRESHOLD(0x0c),
        PWCE_PLOT_AND_2_THRESHOLD(0x0d),
        PWCE_GESTURES_WINDOW(0x0e),
        PWCE_OPTIC_LEARNING_WIDGET(0x0f),
    }

    enum class ParameterWidgetLabel(val number: Int, val label: String) {
        PWLE_UNKNOW(0x00, "UNKNOW"),
        PWLE_OPEN(0x01, "OPEN"),
        PWLE_CLOSE(0x02, "CLOSE"),
        PWLE_CALIBRATE(0x03, "CALIBRATE"),
        PWLE_RESET(0x04, "RESET"),
        PWLE_CONTROL_SETTINGS(0x05, "CONTROL SETTINGS"),
        PWLE_OPEN_CLOSE_THRESHOLD(0x06, "OPEN CLOSE THRESHOLD"),
        PWLE_SELECT_GESTURE(0x07, "SELECT GESTURE")
    }

    enum class ParameterWidgetDisplayCode(val number: Byte) {
        PWDCE_UNKNOW(0x00),
        PWDCE_MAIN_DISPLAY(0x01)
    }

    enum class ParameterWidgetLabelType(val number: Byte) {
        PWLTE_CODE_LABEL(0x00),
        PWLTE_STRING_LABEL(0x01)
    }

    enum class AdditionalParameterInfoType(val number: Int) {
        WIDGET(5)
    }

    enum class ParameterLimitTypeEnum(val number: Byte) {
        PLTE_LIMIT_NO_LIMIT(0x00),
        PLTE_LIMIT_BY_TYPE(0x01),
        PLTE_LIMIT_CUSTOM(0x02),
        PLTE_LIMIT_100(0x03),
        PLTE_LIMIT_NUM(0x04)
    }

    enum class ParameterTypeEnum(val number: Int, val sizeOf: Int) {
        PARTE_UNKNOW(0, 0),
        PARTE_BOOL_TYPE(1, 0),
        PARTE_BOOL_ARRAY_TYPE(2, 0),
        PARTE_BOOL_MAP_TYPE(3, 0),

        // INTEGER TYPE
        PARTE_INT32_TYPE(4, 4),
        PARTE_INT32_ARRAY_TYPE(5, 0),
        PARTE_INT32_MAP_TYPE(6, 0),

        PARTE_INT16_TYPE(7, 2),
        PARTE_INT16_ARRAY_TYPE(8, 0),
        PARTE_INT16_MAP_TYPE(9, 0),

        PARTE_INT8_TYPE(10, 1),
        PARTE_INT8_ARRAY_TYPE(11, 0),
        PARTE_INT8_MAP_TYPE(12, 0),

        // UNSIGNED INTEGER TYPE
        PARTE_UINT32_TYPE(13, 4),
        PARTE_UINT32_ARRAY_TYPE(14, 0),
        PARTE_UINT32_MAP_TYPE(15, 0),

        PARTE_UINT16_TYPE(16, 2),
        PARTE_UINT16_ARRAY_TYPE(17, 0),
        PARTE_UINT16_MAP_TYPE(18, 0),

        PARTE_UINT8_TYPE(19, 1),
        PARTE_UINT8_ARRAY_TYPE(20, 0),
        PARTE_UINT8_MAP_TYPE(21, 0),

        // FLOAT TYPE
        PARTE_FLOAT_TYPE(22, 4),
        PARTE_FLOAT_ARRAY_TYPE(23, 0),
        PARTE_FLOAT_MAP_TYPE(24, 0),

        // STRUCT TYPE
        PARTE_STRUCT_TYPE(25, 0),
        PARTE_STRUCT_ARRAY_TYPE(26, 0),
        PARTE_STRUCT_MAP_TYPE(27, 0),

        PARTE_CHAR_TYPE(28, 0),
        PARTE_NUM(29, 0)
    }

    enum class DataTableSlotsEnum(val number: Byte) {
        DTE_UNKNOW(0x00),
        DTE_BOOTLOADER_INFO_TYPE(0x01),
        DTE_FW_INFO_TYPE(0x02),
        DTE_DEVICE_INFO_TYPE(0x03),
        DTE_BOARD_INFO_TYPE(0x04),
        DTE_PRODUCT_INFO_TYPE(0x05),
        DTE_SERVICE_INFO(0x06),
        DTE_SYSTEM_DEVICES(0x07),

        DTE_GESTURE_COLLECTION(0x08),
        DTE_DRIVE_SETTINGS(0x09),

        DTE_EMG_SETTINGS(0x0a),
        DTE_INDY_SETTINGS(0x0b),
        DTE_BMS_SETTING(0x0c),
        DTE_FEST_X_SETTINGS(0x0d),

        DTE_FREE_SLOT(0xff.toByte()),
    }

    enum class ParameterDataCodeEnum(val number: Int) {
        PDCE_SIMPLE_COMMAND(0),
        PDCE_ACTION_REQUEST(30),

        //Global settings
        PDCE_SELECT_GESTURE(1),          // uint8_t select_gesture;
        PDCE_SELECT_PROFILE(2),          // uint8_t select_profile;
        PDCE_GLOBAL_FORCE(3),            // uint8_t global_force;
        PDCE_GLOBAL_SENSITIVITY(4),        // uint8_t global_sensitivity;
        PDCE_GLOBAL_THRESHOLD(5),          // uint8_t global_threshold;

        PDCE_UNIVERSAL_CONTROL_INPUT(6),
        PDCE_OPEN_CLOSE_SIGNAL(7),                 // uint8_t open uint8_t close signal

        PDCE_EMG_CH_1_3_VAL(8),
        PDCE_EMG_CH_4_6_VAL(9),
        PDCE_EMG_CH_7_9_VAL(10),

        PDCE_EMG_CH_1_3_GAIN(11),
        PDCE_EMG_CH_4_6_GAIN(12),
        PDCE_EMG_CH_7_9_GAIN(13),

        // Drive control set group
        PDCE_MOVE_DRIVE_PERCENT(14),          //int8_t move_drive[DRIVE_NUM]; // drive forward: 1-100(speed percent) | drive stop 0 | drive reverse: (-1)-(-100)(speed percent)
        PDCE_TARGET_DRIVE_POSITION_PERCENT(15),    //int8_t drive_pos[DRIVE_NUM]; // drive forward: 0-100(pos percent) 0 - open, 100-close
        PDCE_TARGET_DRIVE_SPEED_PERCENT(16),      //
        PDCE_TARGET_DRIVE_FORCE_PERCENT(17),      //

        // Drive control get group
        PDCE_CURRENT_DRIVE_POSITION(18),      //
        PDCE_CURRENT_DRIVE_CURRENT_UINT8(19),    //
        PDCE_CURRENT_DRIVE_CURRENT_UINT16(20),    //
        PDCE_CURRENT_DRIVE_FORCE_UINT8(21),    //
        PDCE_CURRENT_DRIVE_FORCE_UINT16(22),    //

        PDCE_GESTURES_CHANGE_SETTINGS(23),        //
        PDCE_CONTROL_MODE_SETTINGS(24),        //
        PDCE_DRIVE_SETTINGS(25),            //
        PDCE_OPEN_CLOSE_THRESHOLD(26),          // open_close_threshold_param_struct

        PDCE_CALIB_STATUS(27),              //
        PDCE_CURRENT_LIMITS(28),            //
        PDCE_BMS_STATUS_COMBINED_PARAM(29),             //
        PDCE_GESTURE_SETTINGS(31),
        PDCE_GESTURE_GROUP(32),
        PDCE_OPTIC_LEARNING_DATA(33),
        PDCE_EMG_ENV_E_VAL(34),
        PDCE_DMS_OUTPUT(35),
        PDCE_DATE_AND_TIME(36)
    }

    enum class GestureEnum(val number: Int) {
        GESTURE_NO_GESTURE(0),
        GESTURE_FIST(1),
        GESTURE_POINT(2),
        GESTURE_PINCH(3),
        GESTURE_FIST_THUMB_OVER(4),
        GESTURE_KEY(5),
        GESTURE_ROCK(6),
        GESTURE_TWIZZERS(7),
        GESTURE_CUPHOLDER(8),
        GESTURE_HALF_GRAB(9),
        GESTURE_OK(10),
        GESTURE_THUMB_UP(11),
        GESTURE_MIDDLE_FINGER(12),
        GESTURE_DOUBLE_POINT(13),
        GESTURE_CALL_ME(14),
        GESTURE_NATURAL_POSITION(15),
        GESTURE_CUSTOM_0(64),
        GESTURE_CUSTOM_1(65),
        GESTURE_CUSTOM_2(66),
        GESTURE_CUSTOM_3(67),
        GESTURE_CUSTOM_4(68),
        GESTURE_CUSTOM_5(69),
        GESTURE_CUSTOM_6(70),
        GESTURE_CUSTOM_7(71),
        GESTURE_CUSTOM_8(72),
        GESTURE_CUSTOM_9(73),
        GESTURE_CUSTOM_10(74),
        GESTURE_CUSTOM_11(75),
        GESTURE_CUSTOM_12(76),
        GESTURE_CUSTOM_13(77)
    }

    enum class TrainingModelState {
        BASE, EXPORT, RUN,
    }

}
