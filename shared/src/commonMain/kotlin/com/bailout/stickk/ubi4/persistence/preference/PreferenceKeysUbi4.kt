package com.bailout.stickk.ubi4.persistence.preference
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.ParameterMetaV3
import com.bailout.stickk.ubi4.models.ble.WidgetKindV3
import com.bailout.stickk.ubi4.models.widgets.WidgetLabel
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.*
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_PLOT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_START_CALIBRATE_COMMAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_TEST_SWITCHER
import kotlin.native.ObjCName

//@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)
@ObjCName("PreferenceKeysUbi4", exact = true)
object PreferenceKeysUbi4 {
    const val END_PARAMETERS_ARRAY_KEY = 23654328
    const val APP_PREFERENCES = "APP_PREFERENCES"
    const val NUM_GESTURES = 14
    const val NUM_ACTIVE_GESTURES = 8
    const val CONNECTED_DEVICE_ADDRESS = "CONNECTED_DEVICE_ADDRESS"
    const val CONNECTED_DEVICE = "CONNECTED_DEVICE"
    const val DEVICE_ID_IN_SYSTEM_UBI4 = "DEVICE_ID_IN_SYSTEM_UBI4"
    const val PARAMETER_ID_IN_SYSTEM_UBI4 = "PARAMETER_ID_IN_SYSTEM_UBI4"
    const val GESTURE_ID_IN_SYSTEM_UBI4 = "GESTURE_ID_IN_SYSTEM_UBI4"
    const val SELECT_GESTURE_SETTINGS_NUM = "SELECT_GESTURE_SETTINGS_NUM"
    const val LAST_CONNECTION_MAC_UBI4 = "LAST_CONNECTION_MAC_UBI4"
    const val LAST_ACTIVE_GESTURE_FILTER = "LAST_ACTIVE_GESTURE_FILTER"
    const val LAST_ACTIVE_SETTINGS_FILTER = "LAST_ACTIVE_SETTINGS_FILTER"
    const val LAST_HIDE_COLLECTION_BTN_STATE = "LAST_HIDE_COLLECTION_BTN_STATE"
    const val UBI4_MODE_ACTIVATED = "UBI4_MODE_ACTIVATED"
    const val GAME_LAUNCH_RATE = "GAME_LAUNCH_RATE"
    const val MAXIMUM_POINTS = "MAXIMUM_POINTS"
    const val NUMBER_OF_CUPS = "NUMBER_OF_CUPS"
    const val FIRST_LOAD_ACCOUNT_INFO = "FIRST_LOAD_ACCOUNT_INFO"
    const val PREF_GESTURE_STATE = "pref_gesture_state"
    const val SET_MODE_SMART_CONNECTION = "SET_MODE_SMART_CONNECTION"
    const val NAME           = "ubi4_prefs"
    const val KEY_TOKEN      = "pref_key_token"
    const val KEY_SERIAL     = "pref_key_serial"
    const val KEY_PASSWORD   = "pref_key_password"
    const val TRAINING_PREFS     = "training_model_prefs"
    const val KEY_CHECKPOINT_NUMBER = "checkpoint_number"
    const val ARG_LAST_EMG8 = "arg_last_emg8"

    const val KEY_SECRET_ITEM_VISIBLE = "secret_item_visible"




    const val DRIVER_NUM = "DRIVER_NUM"
    const val SENS_NUM = "SENS_NUM"
    const val BMS_NUM = "BMS_NUM"


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



    enum class BaseCommands(val number: Byte) {
        DEVICE_INFORMATION          (0x01),
        DATA_MANAGER                (0x02),
        WRITE_FW_COMMAND            (0x03),
        DEVICE_ACCESS_COMMAND       (0x04),
        ECHO_COMMAND                (0x05),
        SUB_DEVICE_MANAGER          (0x06),
        GET_DEVICE_STATUS           (0x07),
        DATA_TRANSFER_SETTINGS      (0x08),
        COMPLEX_PARAMETER_TRANSFER  (0x09),
    }

    enum class deviceInformationCommand(val number: Byte) {
        INICIALIZE_INFORMATION                  (0x01),
        READ_DEVICE_PARAMETERS                  (0x02),
        READ_DEVICE_ADDITIONAL_PARAMETERS       (0x03),

        READ_SUB_DEVICES_FIRST_INFO             (0x04),
        READ_SUB_DEVICE_INFO                    (0x05),
        READ_SUB_DEVICE_PARAMETERS              (0x06),
        READ_SUB_DEVICE_ADDITIONAL_PARAMETER    (0x07),
        SUB_DEVICE_PARAMETER_INIT_READ          (0x08),
        SUB_DEVICE_PARAMETER_INIT_WRITE         (0x09),

        GET_SERIAL_NUMBER                       (0x0a),
        SET_SERIAL_NUMBER                       (0x0b),

        GET_DEVICE_NAME                         (0x0c),
        SET_DEVICE_NAME                         (0x0d),

        GET_DEVICE_ROLE                         (0x0e),
        SET_DEVICE_ROLE                         (0x0f),
        GET_DEVICE_CRC                          (0x17),
        GET_MASTER_CRC                          (0x19),
        GET_SYSTEM_CRC                          (0x1A)

    }

    enum class DataManagerCommand(val number: Byte) {
        READ_AVAILABLE_SLOTS    (0x01),
        WRITE_SLOT              (0x02),
        READ_DATA               (0x03),
        WRITE_DATA              (0x04),
        RESET_TO_FACTORY        (0x05),
        SAVE_DATA               (0x06)
    }

    //используется для определения типов данных в определённых вью
    enum class ParameterWidgetType(val number: Byte) {
        PWTE_UNKNOW                         (0x00),
        PWTE_COMMAND                        (0x01),
        PWTE_COMBOBOX_ENUM                  (0x02),
        PWTE_COMBOBOX_STRING                (0x03),
        PWTE_ONE_CHANNEL_PLOT               (0x04),
        PWTE_MULTY_CHANNEL_PLOT             (0x05),
        PWTE_ONE_CHANNEL_PLOT_LEGEND        (0x06),
        PWTE_MULTY_CHANNEL_PLOT_LEGEND      (0x07),
        PWTE_EMG_GESTURE_CHANGE_SETTINGS    (0x08),
        PWTE_GESTURE_SETTINGS               (0x09),
        PWTE_CALIB_STATUS                   (0x0a),
        PWTE_CONTROL_MODE                   (0x0b),
        PWTE_OPEN_CLOSE_THRESHOLD           (0x0c),
        PWTE_SCALAR                         (0x0d)
    }

    //используется для отрисовки вью
    enum class ParameterWidgetCode(val number: Byte) {
        PWCE_UNKNOW                         (0x00),
        PWCE_BUTTON                         (0x01),
        PWCE_SWITCH                         (0x02),
        PWCE_COMBOBOX                       (0x03),
        PWCE_SLIDER                         (0x04),
        PWCE_PLOT                           (0x05),
        PWCE_SPINBOX                        (0x06), //окно ввода цифр со стрелочками инкриментации/декрементации
        PWCE_EMG_GESTURE_CHANGE_SETTINGS    (0x07),
        PWCE_GESTURE_SETTINGS               (0x08),
        PWCE_CALIB_STATUS                   (0x09),
        PWCE_CONTROL_MODE                   (0x0a),
        PWCE_OPEN_CLOSE_THRESHOLD           (0x0b),
        PWCE_PLOT_AND_1_THRESHOLD           (0x0c),
        PWCE_PLOT_AND_2_THRESHOLD           (0x0d),
        PWCE_GESTURES_WINDOW                (0x0e),
        PWCE_OPTIC_LEARNING_WIDGET          (0x0f),
        PWCE_SERVICE_INFO                   (0x10),
        PWCE_TOGGLE_SLIDER                  (0x11),

        PWCE_BUTTON_V3                      (0x12),
        PWCE_SWITCH_V3                      (0x13),
        PWCE_COMBOBOX_V3                    (0x14),
        PWCE_SLIDER_V3                      (0x15),
        PWCE_PLOT_V3                        (0x16),
        PWCE_TOGGLE_SLIDER_V3               (0x17),
        PWCE_GESTURES_WINDOW_V3             (0x18),
        PWCE_SPINBOX_V3                     (0x19),
        PWCE_TEXT_INPUT_V3                  (0x1A)
    }


    val parameterWidgetLabelRu: Map<String, WidgetLabel> = mapOf(
        "0" to WidgetLabel("Нет имени"),
        "1" to WidgetLabel("Открыть"),
        "2" to WidgetLabel("Закрыть"),
        "3" to WidgetLabel("Калибровка моторов"),
        "4" to WidgetLabel("Общая чувствительность"),
        "5" to WidgetLabel("Общая сила"),
        "6" to WidgetLabel("Чувствительность открытия"),
        "7" to WidgetLabel("Чувствительность закрытия"),
        "8" to WidgetLabel("Выключение дисплея"),
        "9" to WidgetLabel("Время работы дисплея", " сек"),
        "10" to WidgetLabel("Ориентация дисплея"),
        "11" to WidgetLabel("Управление ОМГ"),
        "12" to WidgetLabel("Время входа в ОМГ", "*50 мс"),
        "13" to WidgetLabel("Поменять сенсоры"),
        "14" to WidgetLabel("Управление моторами с датчиков"),
        "15" to WidgetLabel("Пропорциональное управление силой"),
        "16" to WidgetLabel("Пропорциональное управление скоростью"),
        "17" to WidgetLabel("Пропорциональное управление позицией"),
        "18" to WidgetLabel("Блокировка движения с датчиков", " сек"),
        "custom_name" to WidgetLabel("Кастомное имя")
    )

    val unitCodesRu: Map<Int, String > = mapOf(
        1 to "сек"
    )



    val parameterWidgetLabelEn: Map<String, WidgetLabel> = mapOf(
        "0" to WidgetLabel("Unknown"),
        "1" to WidgetLabel("Open"),
        "2" to WidgetLabel("Close"),
        "3" to WidgetLabel("Motor calibration"),
        "4" to WidgetLabel("Global sensitivity"),
        "5" to WidgetLabel("Global force"),
        "6" to WidgetLabel("Open sensitivity"),
        "7" to WidgetLabel("Close sensitivity"),
        "8" to WidgetLabel("Turning off the display"),
        "9" to WidgetLabel("Display timeout", " sec"),
        "10" to WidgetLabel("Display orientation"),
        "11" to WidgetLabel("OMG control"),
        "12" to WidgetLabel("OMG entry time", "*50 ms"),
        "13" to WidgetLabel("Switch sensors"),
        "14" to WidgetLabel("EMG motor control"),
        "15" to WidgetLabel("Proportional force control"),
        "16" to WidgetLabel("Proportional speed control"),
        "17" to WidgetLabel("Proportional position control"),
        "18" to WidgetLabel("EMG movement block time", " sec")
    )

    val parameterWidgetLabel: Map<String, Map<String, WidgetLabel>> = mapOf(
        "ru" to parameterWidgetLabelRu,
        "en" to parameterWidgetLabelEn
    )


    enum class ParameterWidgetDisplayCode(val number: Byte) {
        PWDCE_UNKNOW        (0x00),
        PWDCE_MAIN_DISPLAY  (0x01)
    }

    enum class ParameterWidgetLabelType(val number: Byte) {
        PWLTE_CODE_LABEL    (0x00),
        PWLTE_STRING_LABEL  (0x01)
    }

    enum class AdditionalParameterInfoType(val number: Int) {
        WIDGET(5)
    }

    enum class ParameterLimitTypeEnum(val number: Byte) {
        PLTE_LIMIT_NO_LIMIT (0x00),
        PLTE_LIMIT_BY_TYPE  (0x01),
        PLTE_LIMIT_CUSTOM   (0x02),
        PLTE_LIMIT_100      (0x03),
        PLTE_LIMIT_NUM      (0x04)
    }



    @ObjCName("ParameterTypeEnum", exact = true)
    enum class ParameterTypeEnum (val number: Int, val sizeOf: Int) {
        PARTE_UNKNOW            (0,0),
        PARTE_BOOL_TYPE         (1,1),
        PARTE_BOOL_ARRAY_TYPE   (2,1),
        PARTE_BOOL_MAP_TYPE     (3,1),

        // INTEGER TYPE
        PARTE_INT32_TYPE        (4,4),
        PARTE_INT32_ARRAY_TYPE  (5,4),
        PARTE_INT32_MAP_TYPE    (6,4),

        PARTE_INT16_TYPE        (7,2),
        PARTE_INT16_ARRAY_TYPE  (8,2),
        PARTE_INT16_MAP_TYPE    (9,2),

        PARTE_INT8_TYPE         (10,1),
        PARTE_INT8_ARRAY_TYPE   (11,1),
        PARTE_INT8_MAP_TYPE     (12,1),

        // UNSIGNED INTEGER TYPE
        PARTE_UINT32_TYPE       (13,4),
        PARTE_UINT32_ARRAY_TYPE (14,4),
        PARTE_UINT32_MAP_TYPE   (15,4),

        PARTE_UINT16_TYPE       (16,2),
        PARTE_UINT16_ARRAY_TYPE (17,2),
        PARTE_UINT16_MAP_TYPE   (18,2),

        PARTE_UINT8_TYPE        (19,1),
        PARTE_UINT8_ARRAY_TYPE  (20,1),
        PARTE_UINT8_MAP_TYPE    (21,1),

        // FLOAT TYPE
        PARTE_FLOAT_TYPE        (22,4),
        PARTE_FLOAT_ARRAY_TYPE  (23,4),
        PARTE_FLOAT_MAP_TYPE    (24,4),

        // STRUCT TYPE
        PARTE_STRUCT_TYPE       (25,0),
        PARTE_STRUCT_ARRAY_TYPE (26,0),
        PARTE_STRUCT_MAP_TYPE   (27,0),

        PARTE_CHAR_TYPE         (28,1),
        PARTE_NUM               (29,0)
    }

    enum class DataTableSlotsEnum (val number: Byte) {
        DTE_UNKNOW                  (0x00),
        DTE_BOOTLOADER_INFO_TYPE    (0x01),
        DTE_FW_INFO_TYPE            (0x02),
        DTE_DEVICE_INFO_TYPE        (0x03),
        DTE_BOARD_INFO_TYPE         (0x04),
        DTE_PRODUCT_INFO_TYPE       (0x05),
        DTE_SERVICE_INFO            (0x06),
        DTE_SYSTEM_DEVICES          (0x07),

        DTE_GESTURE_COLLECTION      (0x08),
        DTE_DRIVE_SETTINGS          (0x09),

        DTE_EMG_SETTINGS            (0x0a),
        DTE_INDY_SETTINGS           (0x0b),
        DTE_BMS_SETTING             (0x0c),
        DTE_FEST_X_SETTINGS         (0x0d),

        DTE_FREE_SLOT               (0xff.toByte()),
    }

    enum class ParameterDataCodeEnum (val number: Int) {
        PDCE_SIMPLE_COMMAND                 (0 ),
        PDCE_ACTION_REQUEST                 (30),
        //Global settings
        PDCE_SELECT_GESTURE                 (1 ), // uint8_t select_gesture;
        PDCE_SELECT_PROFILE                 (2 ), // uint8_t select_profile;
        PDCE_GLOBAL_FORCE                   (3 ), // uint8_t global_force;
        PDCE_GLOBAL_SENSITIVITY             (4 ), // uint8_t global_sensitivity;
        PDCE_GLOBAL_THRESHOLD               (5 ), // uint8_t global_threshold;
        PDCE_UNIVERSAL_CONTROL_INPUT        (6 ),
        PDCE_OPEN_CLOSE_SIGNAL              (7 ), // uint8_t open uint8_t close signal
        PDCE_EMG_CH_1_3_VAL                 (8 ),
        PDCE_EMG_CH_4_6_VAL                 (9 ),
        PDCE_EMG_CH_7_9_VAL                 (10),
        PDCE_EMG_CH_1_3_GAIN                (11),
        PDCE_EMG_CH_4_6_GAIN                (12),
        PDCE_EMG_CH_7_9_GAIN                (13),
        // Drive control set group
        PDCE_MOVE_DRIVE_PERCENT             (14),  //int8_t move_drive[DRIVE_NUM]; // drive forward: 1-100(speed percent) | drive stop 0 | drive reverse: (-1)-(-100)(speed percent)
        PDCE_TARGET_DRIVE_POSITION_PERCENT  (15),  //int8_t drive_pos[DRIVE_NUM]; // drive forward: 0-100(pos percent) 0 - open, 100-close
        PDCE_TARGET_DRIVE_SPEED_PERCENT     (16),
        PDCE_TARGET_DRIVE_FORCE_PERCENT     (17),
        // Drive control get group
        PDCE_CURRENT_DRIVE_POSITION         (18),
        PDCE_CURRENT_DRIVE_CURRENT_UINT8    (19),
        PDCE_CURRENT_DRIVE_CURRENT_UINT16   (20),
        PDCE_CURRENT_DRIVE_FORCE_UINT8      (21),
        PDCE_CURRENT_DRIVE_FORCE_UINT16     (22),
        PDCE_GESTURES_CHANGE_SETTINGS       (23),
        PDCE_CONTROL_MODE_SETTINGS          (24),
        PDCE_DRIVE_SETTINGS                 (25),
        PDCE_OPEN_CLOSE_THRESHOLD           (26),  // open_close_threshold_param_struct
        PDCE_CALIB_STATUS                   (27),
        PDCE_CURRENT_LIMITS                 (28),
        PDCE_BMS_STATUS_COMBINED_PARAM      (29),
        PDCE_GESTURE_SETTINGS               (31),
        PDCE_GESTURE_GROUP                  (32),
        PDCE_OPTIC_LEARNING_DATA            (33),
        PDCE_EMG_ENV_E_VAL                  (34),
        PDCE_DMS_OUTPUT                     (35),
        PDCE_DATE_AND_TIME                  (36),
        PDCE_INTERFECE_ERROR_COUNTER        (37),
        PDCE_CALIBRATION_CURRENT_PERCENT    (38),
        PDCE_ENERGY_SAVE_MODE               (39),
        PDCE_LEFT_RIGHT_HAND_MODE           (40),
        PDCE_SWITCH_SENSORS                 (41),
        PDCE_OPTIC_CHECKPOINT_DATA_RECEIVER (42),
        PDCE_OPTIC_BINDING_DATA             (43),
        PDCE_OPTIC_ENABLE                   (44),
        PDCE_OPTIC_SELECT_GESTURE_TIMEOUT   (45),
        PDCE_OPTIC_MODE_SELECT_GESTURE      (47),
        PDCE_GENERIC_9                      (246),
        PDCE_GENERIC_8                      (247),
        PDCE_GENERIC_7                      (248),
        PDCE_GENERIC_6                      (249),
        PDCE_GENERIC_5                      (250),
        PDCE_GENERIC_4                      (251),
        PDCE_GENERIC_3                      (252),
        PDCE_GENERIC_2                      (253),
        PDCE_GENERIC_1                      (254),
        PDCE_GENERIC_0                      (255)
    }

    enum class GestureEnum (val number: Int) {
        GESTURE_NO_GESTURE          (0 ),
        GESTURE_FIST                (1 ),
        GESTURE_POINT               (2 ),
        GESTURE_PINCH               (3 ),
        GESTURE_FIST_THUMB_OVER     (4 ),
        GESTURE_KEY                 (5 ),
        GESTURE_ROCK                (6 ),
        GESTURE_TWIZZERS            (7 ),
        GESTURE_CUPHOLDER           (8 ),
        GESTURE_HALF_GRAB           (9 ),
        GESTURE_OK                  (10),
        GESTURE_THUMB_UP            (11),
        GESTURE_MIDDLE_FINGER       (12),
        GESTURE_DOUBLE_POINT        (13),
        GESTURE_CALL_ME             (14),
        GESTURE_NATURAL_POSITION    (15),
        GESTURE_CUSTOM_0            (64),
        GESTURE_CUSTOM_1            (65),
        GESTURE_CUSTOM_2            (66),
        GESTURE_CUSTOM_3            (67),
        GESTURE_CUSTOM_4            (68),
        GESTURE_CUSTOM_5            (69),
        GESTURE_CUSTOM_6            (70),
        GESTURE_CUSTOM_7            (71),
        GESTURE_CUSTOM_8            (72),
        GESTURE_CUSTOM_9            (73),
        GESTURE_CUSTOM_10           (74),
        GESTURE_CUSTOM_11           (75),
        GESTURE_CUSTOM_12           (76),
        GESTURE_CUSTOM_13           (77)
    }

    enum class TrainingModelState {
        BASE, EXPORT, RUN,
    }

    enum class DataTableSlotsCode(val number: Byte) {
        DTCE_UNKNOW                         (0   ) ,
        DTCE_BOOTLOADER_INFO_TYPE           (1   ) ,
        DTCE_FW_INFO_TYPE                   (2   ) ,
        DTCE_DEVICE_INFO_TYPE               (3   ) ,
        DTCE_BOARD_INFO_TYPE                (4   ) ,
        DTCE_PRODUCT_INFO_TYPE              (5   ) ,
        DTCE_SERVICE_INFO                   (6   ) ,
        DTCE_SYSTEM_DEVICES                 (7   ) ,
        DTCE_DRIVE_INFO                     (8   ) ,
        DTCE_GESTURE_COLLECTION             (9   ) ,
        DTCE_USER_GESTURES                  (10  ) ,
        DTCE_DRIVE_SETTINGS                 (11  ) ,
        DTCE_EMG_SETTINGS                   (12  ) ,
        DTCE_INDY_SETTINGS                  (13  ) ,
        DTCE_BMS_SETTING                    (14  ) ,
        DTCE_FEST_X_SETTINGS                (15  ) ,
        DTCE_MOTOR_SETTINGS                 (16  ) ,
        DTCE_STATIC_BINDING_STRUCTS         (17  ) ,
        DTCE_DYNAMIC_BINDING_STRUCTS        (18  ) ,
        DTCE_UPPER_LIMB_PROSTHESIS_SETTINGS (19  ) ,
        DCTE_GESTURES_KEY_DESCRIPTION       (20  ) ,
        DCTE_GESTURES_STRING_DESCRIPTION    (21  ) ,
        DCTE_GESTURE_GROUP                  (22  ) ,
        DCTE_DMS_BINDING_DATA               (23  ) ,
        DTCE_OPTIC_SETTINGS                 (24  ) ,
        DTCE_GUI_SETTINGS                   (25  ) ,
        DTCE_PRESSURE_SETTINGS              (26  ) ,
        DTCE_ML_MODEL_DATA                  (27  ) ,
        DTCE_DEVICES_CACHE                  (28  ) ,
        DTCE_EMG_MASTER_SETTINGS            (29  ) ,
        DTCE_FREE_SLOT                      ((0xFF).toByte())
    }

    enum class DeviceCode(
        val id: Int,
        private val baseTitle: String
    ) {
        CPU_MODULE         ( 1, "Cpu module"            ),
        FEST_H_AND_F       ( 2, "Fest h and f"          ),
        INDY               ( 3, "Indy"                  ),
        EMG_SENSE          ( 4, "Emg sense"             ),
        BMS                ( 5, "Bms"                   ),
        GUI                ( 6, "Gui"                   ),
        OMG_MODULE         ( 7, "Omg module"            ),
        FINGERS_DC_DRIVER  ( 8, "Fingers dc driver"     ),
        BLDC_FINGER_DRIVER ( 9, "Bldc finger driver"    ),
        DC_FINGER_DRIVER   (10, "Dc finger driver"      ),
        DIGITAL_ELECTROD   (11, "Digital electrod"      ),
        UNKNOWN            ( 0, "Unknown"               );

        val title: String get() = "$baseTitle version"

        companion object Companion {
            /** Быстрый поиск по `id`; если нет совпадения – `UNKNOWN` */
            fun from(id: Int) = values().firstOrNull { it.id == id } ?: UNKNOWN
        }
    }

    enum class DeviceCodeV3(
        val code: Int,
        private val baseTitle: String
    ) {
        DCE_CPU_MODULE(1, "Cpu module"),
        DCE_FEST_H_AND_F(2, "Fest H and F"),
        DCE_INDY(3, "Indy"),
        DCE_EMG_SENSE(4, "Emg sense"),
        DCE_BMS(5, "Bms"),
        DCE_GUI(6, "Gui"),
        DCE_OMG_MODULE(7, "Omg module"),
        DCE_FINGERS_DC_DRIVER(8, "Fingers DC driver"),
        DCE_BLDC_FINGER_DRIVER(9, "BLDC finger driver"),
        DCE_DC_FINGER_DRIVER(10, "DC finger driver"),
        DCE_DIGITAL_ELECTROD(11, "Digital electrode"),

        UNKNOWN(-1, "Unknown");

        val title: String
            get() = "$baseTitle board"

        companion object {
            private val byCode = entries.associateBy(DeviceCodeV3::code)

            fun fromCode(code: Int): DeviceCodeV3 =
                byCode[code] ?: UNKNOWN
        }
    }
    enum class DeviceType(
        val code: Int
    ) {
        DTE_UNKNOWN(0x00),
        DTE_CPU(0x01),
        DTE_SUB_CPU(0x02),

        DTE_DRIVER(0x10),
        DTE_MOTOR_DRIVER(0x11),
        DTE_MOTOR_DRIVER_MULTY_CH(0x12),

        DTE_SENSOR(0x20),
        DTE_EMG_1CH(0x21),
        DTE_EMG_2CH(0x22),
        DTE_EMG_MULTY_CH(0x23),
        DTE_OMG_SEGMENT(0x24),
        DTE_OMG_MULTY_CH(0x25),
        DTE_OMG_EMG_SEGMENT(0x26),
        DTE_OMG_EMG_MULTY_CH(0x27),
        DTE_STIMULATOR(0x30),

        DTE_HMI(0x40),
        DTE_DISPLAY(0x41),
        DTE_BUTTONS(0x42),
        DTE_SOUNDS(0x43),
        DTE_DISPLAY_BUTTONS(0x44),
        DTE_DISPLAY_SOUNDS(0x45),
        DTE_BUTTONS_SOUNDS(0x46),
        DTE_DISPLAY_BUTTONS_SOUNDS(0x47),
        DTE_TOUCH_DISPLAY(0x48),
        DTE_TOUCH_DISPLAY_SOUNDS(0x49),
        DTE_POWER(0x50),
        DTE_BMS(0x51),
        DTE_ACB(0x52),
        DTE_POWER_CONVERTER(0x53),
        DTE_POWER_ISOLATOR(0x54),
        DTE_POWER_COMBO(0x55),
        DTE_COMBO(0x60),
        DTE_OTHER(0x70);

        companion object {
            private val map = entries.associateBy(DeviceType::code)

            fun fromCode(code: Int): DeviceType =
                map[code] ?: DTE_UNKNOWN
        }
    }
//    enum class SubDeviceBoardV3(
//        val address: Int,
//        private val baseTitle: String
//    ) {
//        CPU                 (0,  "Cpu module"),
//        INDEX_FINGER        (32, "Index finger"),
//        MIDDLE_FINGER       (33, "Middle finger") ,
//        RING_FINGER         (34, "Ring finger"),
//        LITTLE_FINGER       (35, "Little finger"),
//        THUMB               (36, "Thumb finger"),
//        ROTATION            (37, "Rotation"),
//        EMG_HUB             (8,  "Emg hub"),
//        EMG_CHANNEL_1       (16, "Emg channel 1"),
//        EMG_CHANNEL_2       (17, "Emg channel 2"),
//
//        UNKNOWN             (-1, "Unknown");
//
//        val title: String
//            get() = "$baseTitle board"
//
//        companion object {
//            /** Быстрый поиск по address */
//            fun from(address: Int): SubDeviceBoardV3 =
//                values().firstOrNull { it.address == address } ?: UNKNOWN
//        }
//    }

    enum class FirmwareManagerCommand(val number: Byte) {
        GET_RUN_PROGRAM_TYPE   (0x01),
        JUMP_TO_BOOTLOADER     (0x02),
        CHECK_NEW_FW           (0x03),
        PRELOAD_INFO           (0x04),
        LOAD_NEW_FW            (0x05),
        GET_BOOTLOADER_STATUS  (0x06),
        CALCULATE_CRC          (0x07),
        COMPLETE_UPDATE        (0x08),
        GET_BOOTLOADER_INFO    (0x09),
        CHECK_NEW_BOOTLOADER   (0x0A),
        LOAD_NEW_BOOTLOADER    (0x0B),
        GET_MAX_CHANK_SIZE     (0x0C),
        START_SYSTEM_UPDATE    (0x0D),
        FINISH_SYSTEM_UPDATE   (0x0E)
    }

    /** Ответ на GET_RUN_PROGRAM_TYPE */
    enum class RunProgramType(val code: Int) {
        MAIN_APP   (0x01),
        BOOTLOADER (0x02);
        //from(v: Int) берёт число v (байт, который прислала плата)
        // и ищет в списке констант RunProgramType ту одну, у которой поле code равно этому числу.
        // В итоге вы получаете не «сырое» число, а понятную константу MAIN_APP или BOOTLOADER.
        companion object Companion { fun from(code: Int) = values().first { it.code == code } }
    }

    /** Ответ на GET_BOOTLOADER_STATUS */
    enum class BootloaderStatus(val code: Int) {
        IDLE                   (0x01),
        PROCESSING_CLEAR       (0x02),
        DONE_CLEAR             (0x03),
        PRELOAD_STATE          (0x04),
        CONTINUE_WRITE_FW      (0x05),
        CHECK_INFO_FW          (0x06),
        PROCESSING_CRC         (0x07),
        DONE_CRC               (0x08);
        companion object Companion { fun from(code: Int) = values().first { it.code == code } }
    }


    enum class StartSystemUpdateStatus(val code: Int) {
        NEW_FW_ACCEPT   (1),
        READY_TO_UPDATE (2),
        PART_WRITEN     (3),
        UPDATE_COMPLETE (4),
        NEW_FW_NOT_ACCEPT  (5),
        MEMORY_NOT_READY   (6),
        PART_NOT_WRITEN    (7),
        UNKNOW_ERROR       (8);

        companion object Companion {
            fun from(code: Int) = values().firstOrNull { it.code == code }
                ?: UNKNOW_ERROR
        }
    }
    enum class CheckNewFwStatus(val code: Int) {
        NEW_FW_ACCEPT      (1),
        READY_TO_UPDATE    (2),
        PART_WRITTEN       (3),
        UPDATE_COMPLETE    (4),

        NEW_FW_NOT_ACCEPT  (5),
        MEMORY_NOT_READY   (6),
        PART_NOT_WRITTEN   (7),
        UNKNOWN_ERROR      (8);

        companion object Companion {
            fun from(code: Int) = values().firstOrNull { it.code == code }
                ?: UNKNOWN_ERROR
        }
    }

    enum class CrcResult(val code: Int) {
        BAD_CRC_FIRMWARE(0x1E),
        GOOD_CRC_FIRMWARE(0x2E);
        companion object Companion { fun from(code: Int) = values().firstOrNull { it.code == code } }
    }

    //Разделение на все мобильные строки
    enum class MobileSettingsKey(val key: String) {
        AUTO_LOGIN ("AUTO_LOGIN"),
    }


    /** //////////////////////////////////////////////////////////////////////////////////// */
    /**                     Протокол V3 (то что стало поле UBIv4)                            */
    /** //////////////////////////////////////////////////////////////////////////////////// */
    enum class BaseCommandsV3(val number: Byte) {
        DEVICE_INFORMATION          (0x01), /**< 1 Запрос базовой информации девайса */
        DATA_MANAGER                (0x02), /**< 2 Работа с Data Table */
        WRITE_FW_COMMAND            (0x03), /**< 3 Запись прошивки */
        DEVICE_ACCESS_COMMAND       (0x04), /**< 4 Доступ к устройству */
        ECHO_COMMAND                (0x05), /**< 5 Эхо-команда для проверки связи */
        SUB_DEVICE_MANAGER          (0x06), /**< 6 Управление подустройствами */
        GET_DEVICE_STATUS           (0x07), /**< 7 Запрос статуса устройства */
        DATA_TRANSFER_SETTINGS      (0x08), /**< 8 Настройки передачи данных */
        COMPLEX_PARAMETER_TRANSFER  (0x09), /**< 9 Передача сложных параметров */
        POWER_CONTROL               (0x0A), /**< 10 Управление питанием */
        PROTOCOL_PING               (0x0B), /**< 11 Проверка соединения по протоколу */
        SYSTEM_MODE                 (0x0C),
        COMPLEX_RUNTIME_LOG_TRANSFER(0x0D), /**< 13 Передача лога в реальном времени */
        MOVEMENT_CONTROL            (0x0E),
        PROSTHESIS_MODULE_CONTROL   (0x0F),
        GUI_CONTROL                 (0x10),
        SENSOR_CONTROL              (0x11),
        EMG_MASTER_CONTROL          (0x12),
    }

    enum class ProsthesisModuleControlEnum (val number: Byte) {
        PMCE_STOP_COMMAND                 (0x00),
        PMCE_OPEN_COMMAND                 (0x01),
        PMCE_CLOSE_COMMAND                (0x02),
        PMCE_START_CALIBRATE_COMMAND      (0x03),
        PMCE_GET_CALIBRATION_STATUS       (0x04),
        PMCE_GET_CALIBRATION_RESULT       (0x05),

        PWCE_MOVE_AXIS_TO_POSITION        (0x06),
        PWCE_MOVE_AXIS_SPEED              (0x07),
        PWCE_MOVE_AXIS_FORCE              (0x08),
        PWCE_SET_AXIS_MAX_CURRENT         (0x09),
        PWCE_STOP_AXIS                    (0x0A),

        PWCE_GET_AXIS_TARGET_POSITION     (0x0B),
        PWCE_GET_AXIS_TARGET_SPEED        (0x0C),
        PWCE_GET_AXIS_TARGET_FORCE        (0x0D),
        PWCE_GET_AXIS_MAX_CURRENT         (0x0E),

        PWCE_GET_AXIS_CURRENT_POSITION    (0x0F),
        PWCE_GET_AXIS_CURRENT_SPEED       (0x10),
        PWCE_GET_AXIS_CURRENT_FORCE       (0x11),
        PWCE_GET_AXIS_CURRENT_CURRENT     (0x12),

        PWCE_GET_CURRENT_PRESSURE         (0x13),
        PWCE_SET_TARGET_PRESSURE          (0x14),

        PWCE_SET_EMG_CHANGE_GESTURE       (0x15),
        PWCE_GET_EMG_CHANGE_GESTURE       (0x16),

        PWCE_MOVE_FINGER_TO_POSITION      (0x17),
        PWCE_MOVE_FINGER_BY_SPEED         (0x18),
        PWCE_MOVE_FINGER_BY_FORCE         (0x19),
        PWCE_SET_FINGER_MAX_CURRENT       (0x1A),
        PWCE_STOP_FINGER                  (0x1B),

        PWCE_GET_FINGER_TARGET_POSITION   (0x1C),
        PWCE_GET_FINGER_TARGET_SPEED      (0x1D),
        PWCE_GET_FINGER_TARGET_FORCE      (0x1E),
        PWCE_GET_FINGER_MAX_CURRENT       (0x1F),

        PWCE_GET_FINGER_CURRENT_POSITION  (0x20),
        PWCE_GET_FINGER_CURRENT_SPEED     (0x21),
        PWCE_GET_FINGER_CURRENT_FORCE     (0x22),
        PWCE_GET_FINGER_CURRENT_CURRENT   (0x23),

        PWCE_GET_CURRENT_GESTURE_NUM      (0x24),
        PWCE_SET_CURRENT_GESTURE_NUM      (0x25),

        PWCE_GET_GESTURE_SETTING          (0x26),
        PWCE_SET_GESTURE_SETTING          (0x27),
        PWCE_SAVE_GESTURE                 (0x28),

        PWCE_GET_GESTURE_COUNT            (0x29),
        PWCE_SET_GESTURE_COUNT            (0x2A),

        PWCE_GET_COLLECTION_GESTURE_INFO  (0x2B),
        PWCE_GET_USER_GESTURE_INFO        (0x2C),

//      PWCE_SET_EMG_GAIN_VALUE           (0X2D),
//      PWCE_GET_EMG_GAIN_VALUE           (0X2E),

        PWCE_SET_THRESHOLD_VALUE          (0X2F),
        PWCE_GET_THRESHOLD_VALUE          (0X30),

        PWCE_MOVE_FINGERS_TO_POSITIONS    (0X31),
        PWCE_MOVE_FINGERS_SET_SPEED       (0X32),
        PWCE_MOVE_FINGERS_SET_FORCE       (0X33),

        PWCE_COLLECT_TELEMETRY_CONTORL    (0x34),

        PWCE_GET_GESTURE_GROUPE           (0x35),
        PWCE_SET_GESTURE_GROUPE           (0x36),

        PWCE_SET_COLLECTION_GESTURE_INFO  (0x37),
        PWCE_SET_USER_GESTURE_INFO        (0x38),

        PWCE_SET_EMG_MOVEMENT_LOCK        (0x39), // тогл слайдер для настройки блокировки протеза и времени для перевода его в заблокированное состояние (предполагается блокировать сигналом закрытия, отсчёт времени с момента старта закрытия)
        PWCE_GET_EMG_MOVEMENT_LOCK        (0x3A),

        PWCE_SET_HAND_CONTROL_MODE        (0x3B), // моды управления рукой (туда хотим добавить мод при котором сразу при обратном пересечении порога управляющим сигналом управление отдаётся другому сигналу - спорт режим)
        PWCE_GET_HAND_CONTROL_MODE        (0x3C),

        PWCE_SET_GESTURE_CHANGE_MODE      (0x2D),
        PWCE_GET_GESTURE_CHANGE_MODE      (0x2E),

        PWCE_SET_SPEED_SETTINGS           (0x3D),
        PWCE_GET_SPEED_SETTINGS           (0x3E),

        PWCE_SET_FORCE_SETTINGS           (0x3F),
        PWCE_GET_FORCE_SETTINGS           (0x40),
        PWCE_GET_TELEMETRY_DATA           (0x41),

        PWCE_TEST_SWITCHER                (0XFF.toByte()),

    }

    enum class EmgMasterControlEnum (val number: Byte)
    {
        EMCE_SET_EMG_GAIN_VALUE           (0X01),
        EMCE_GET_EMG_GAIN_VALUE           (0X02),

        EMCE_SET_EMG_MAX_GAIN_VALUE       (0x03),
        EMCE_GET_EMG_MAX_GAIN_VALUE       (0x04),

        EMCE_SET_EMG_MODE                 (0x05),
        EMCE_GET_EMG_MODE                 (0x06),
    }

    enum class GuiModuleControlEnum(val number: Byte)
    {
        GMCE_SET_SCREEN                 (0x01),
        GMCE_SET_BATTERY                (0x02),
        GMCE_SET_GESTURE                (0x03),
        GMCE_SET_GESTURE_GROUP          (0x04),
        GMCE_SET_EMG_SIGNALS            (0x05),
        GMCE_SET_EMG_THRESHOLDS         (0x06),
        GMCE_SET_EMG_GAINS              (0x07),
        GMCE_SET_SCREEN_TIMEOUT         (0x08),
        GMCE_SET_GUI_SETTINGS           (0x09),
        GMCE_SET_INIT_INFORMATION       (0x0a),
        GMCE_SET_DATE_TIME              (0x0b),
        GMCE_SET_MOVEMENT_BLOCK_DATA    (0x0c),
        GMCE_GET_SCREEN_TIMEOUT         (0x0d),
        GMCE_SET_LEFT_RIGHT_HAND        (0x0e),
        GMCE_GET_LEFT_RIGHT_HAND        (0x0f),
        GMCE_GET_BATTERY                (0x10),
    }

    enum class DeviceInformationCommandV3(val number: Int) {
        INICIALIZE_INFORMATION                      (1),  //< 1 Инициализация информации */
        READ_DEVICE_PARAMETERS                      (2),  //< 2 Чтение параметров устройства */
        READ_DEVICE_FIRMWARE_VERSION                (3),  //< 3 Чтение дополнительных параметров устройства */
        READ_DEVICE_ADDITIONAL_PARAMETER_SIZE       (16), //< 16 Размер дополнительных параметров устройства */
        READ_SUB_DEVICES_FIRST_INFO                 (4),  //< 4 Чтение первой информации о подустройствах */
        READ_SUB_DEVICE_INFO                        (5),  //< 5 Чтение информации о подустройстве */
        READ_SUB_DEVICE_PARAMETERS                  (6),  //< 6 Чтение параметров подустройства */
        READ_SUB_DEVICE_ADDITIONAL_PARAMETER        (7),  //< 7 Чтение дополнительных параметров подустройства */
        READ_SUB_DEVICE_ADDITIONAL_PARAMETER_SIZE   (17), //< 17 Размер дополнительных параметров подустройства */
        SUB_DEVICE_PARAMETER_INIT_READ              (8),  //< 8 Инициализация чтения параметров подустройства */
        SUB_DEVICE_PARAMETER_INIT_WRITE             (9),  //< 9 Инициализация записи параметров подустройства */
        GET_SERIAL_NUMBER                           (10), //< 10 Получить серийный номер */
        SET_SERIAL_NUMBER                           (11), //< 11 Установить серийный номер */
        GET_DEVICE_NAME                             (12), //< 12 Получить имя устройства */
        SET_DEVICE_NAME                             (13), //< 13 Установить имя устройства */
        GET_DEVICE_ROLE                             (14), //< 14 Получить роль устройства */
        SET_DEVICE_ROLE                             (15), //< 15 Установить роль устройства */
        GET_DEVICE_ADDRESS                          (18), //< 18 Получить адрес устройства */
        SET_DEVICE_ADDRESS                          (19), //< 19 Установить адрес устройства */

        GET_DEVICE_INFO_CRC                         (20),
        GET_PARAM_INFO_CRC                          (21),
        GET_PARAM_DATA_CRC                          (22),
        GET_DEVICE_CRC                              (23),
        SET_SUB_DEVICE_ADDRESS                      (24),
        GET_MASTER_CRC                              (25),
        GET_SYSTEM_CRC                              (26)
    }


    enum class SubDeviceManager (val number: Byte) {
        GET_ALL_SUB_DEVICE      (0x01),        /** Получить список всех подустройств */
        ADD_SUB_DEVICE          (0x02),        /** Добавить подустройство */
        REMOVE_SUB_DEVICE       (0x03)
    }

    object ParameterInfoRegistry {
        // [new widgets V3] тут описываем метаданные параметра: ParameterInfo + codecId + widgetKind + valuePath
        val parameterMetaMapV3: Map<String, ParameterMetaV3> = mapOf(
            P_KEY_PLOT to ParameterMetaV3(
                parameterInfo = ParameterInfo(1, 1, 1, 0),
                codecId = ParameterCodecIdV3.NONE,
                widgetKind = WidgetKindV3.PLOT,
                valuePath = "plot[]"
            ),
            P_KEY_OPEN_CLOSE_THRESHOLD to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_THRESHOLD_VALUE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.THRESHOLDS,
                widgetKind = WidgetKindV3.PLOT,
                valuePath = "openThreshold,closeThreshold"
            ),
            P_KEY_EMG_GAIN_OPEN_VALUE to ParameterMetaV3(
                parameterInfo = ParameterInfo(EMG_MASTER_CONTROL.number.toInt(), EMCE_SET_EMG_GAIN_VALUE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.EMG_GAINS,
                widgetKind = WidgetKindV3.SLIDER,
                valuePath = "openGain"
            ),
            P_KEY_EMG_GAIN_CLOSE_VALUE to ParameterMetaV3(
                parameterInfo = ParameterInfo(EMG_MASTER_CONTROL.number.toInt(), EMCE_SET_EMG_GAIN_VALUE.number.toInt(), 1, 1),
                codecId = ParameterCodecIdV3.EMG_GAINS,
                widgetKind = WidgetKindV3.SLIDER,
                valuePath = "closeGain"
            ),
            P_KEY_CURRENT_GESTURE to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_CURRENT_GESTURE_NUM.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.CURRENT_GESTURE,
                widgetKind = WidgetKindV3.GESTURES,
                valuePath = "currentGesture"
            ),
            P_KEY_GESTURE_SETTING to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_GESTURE_SETTING.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.GESTURE_SETTINGS,
                widgetKind = WidgetKindV3.GESTURES,
                valuePath = "gestureSettings"
            ),
            P_KEY_GESTURE_GROUPE to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_GESTURE_GROUPE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.ROTATION_GROUP,
                widgetKind = WidgetKindV3.GESTURES,
                valuePath = "rotationGroup[]"
            ),
            P_KEY_EMG_CHANGE_GESTURE to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_EMG_CHANGE_GESTURE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.TOGGLE,
                widgetKind = WidgetKindV3.TOGGLE_SLIDER,
                valuePath = "toggleValue"
            ),
            P_KEY_START_CALIBRATE_COMMAND to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PMCE_START_CALIBRATE_COMMAND.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.NONE,
                widgetKind = WidgetKindV3.COMMAND,
                valuePath = "commandOnly"
            ),
            P_KEY_EMG_CONTROL_MODE to ParameterMetaV3(
                parameterInfo = ParameterInfo(EMG_MASTER_CONTROL.number.toInt(), EMCE_SET_EMG_MODE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SPINNER,
                widgetKind = WidgetKindV3.SPINNER,
                valuePath = "spinnerValue"
            ),
            P_KEY_HAND_CONTROL_MODE to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_HAND_CONTROL_MODE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SPINNER,
                widgetKind = WidgetKindV3.SPINNER,
                valuePath = "spinnerValue"
            ),
            P_KEY_GESTURE_CHANGE_MODE to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_GESTURE_CHANGE_MODE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SPINNER,
                widgetKind = WidgetKindV3.SPINNER,
                valuePath = "spinnerValue"
            ),
            P_KEY_SCREEN_TIMEOUT to ParameterMetaV3(
                parameterInfo = ParameterInfo(GUI_CONTROL.number.toInt(), GMCE_SET_SCREEN_TIMEOUT.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.TOGGLE,
                widgetKind = WidgetKindV3.TOGGLE_SLIDER,
                valuePath = "toggleValue"
            ),
            P_KEY_EMG_MOVEMENT_LOCK to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_EMG_MOVEMENT_LOCK.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.TOGGLE,
                widgetKind = WidgetKindV3.TOGGLE_SLIDER,
                valuePath = "toggleValue"
            ),
            P_KEY_LEFT_RIGHT_HAND to ParameterMetaV3(
                parameterInfo = ParameterInfo(GUI_CONTROL.number.toInt(), GMCE_SET_LEFT_RIGHT_HAND.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SPINNER,
                widgetKind = WidgetKindV3.SPINNER,
                valuePath = "spinnerValue"
            ),
            P_KEY_SET_DEVICE_NAME to ParameterMetaV3(
                parameterInfo = ParameterInfo(DEVICE_INFORMATION.number.toInt(), SET_DEVICE_NAME.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.TEXT,
                widgetKind = WidgetKindV3.TEXT_INPUT,
                valuePath = "text"
            ),
            P_KEY_SET_SERIAL_NUMBER to ParameterMetaV3(
                parameterInfo = ParameterInfo(DEVICE_INFORMATION.number.toInt(), SET_SERIAL_NUMBER.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.TEXT,
                widgetKind = WidgetKindV3.TEXT_INPUT,
                valuePath = "serialNumber"
            ),
            P_KEY_TEST_SWITCHER to ParameterMetaV3(
                parameterInfo = ParameterInfo(GUI_CONTROL.number.toInt(), PWCE_TEST_SWITCHER.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SWITCHER,
                widgetKind = WidgetKindV3.SWITCHER,
                valuePath = "checked"
            ),
            P_KEY_EMG_MAX_GAIN_VALUE to ParameterMetaV3(
                parameterInfo = ParameterInfo(EMG_MASTER_CONTROL.number.toInt(), EMCE_SET_EMG_MAX_GAIN_VALUE.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SLIDER,
                widgetKind = WidgetKindV3.SLIDER,
                valuePath = "sliderValue"
            ),
            P_KEY_SPEED_SETTINGS to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_SPEED_SETTINGS.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SLIDER,
                widgetKind = WidgetKindV3.SLIDER,
                valuePath = "sliderValue"
            ),
            P_KEY_FORCE_SETTINGS to ParameterMetaV3(
                parameterInfo = ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PWCE_SET_FORCE_SETTINGS.number.toInt(), 1, 0),
                codecId = ParameterCodecIdV3.SLIDER,
                widgetKind = WidgetKindV3.SLIDER,
                valuePath = "sliderValue"
            ),
        )

        val parameterInfoMapV3: Map<String, ParameterInfo<Int, Int, Int, Int>> =
            parameterMetaMapV3.mapValues { it.value.parameterInfo }

        private val parameterMetaByInfoV3: Map<ParameterInfo<Int, Int, Int, Int>, ParameterMetaV3> =
            parameterMetaMapV3.values.associateBy { it.parameterInfo }

        fun get(key: String): ParameterInfo<Int, Int, Int, Int>? = parameterInfoMapV3[key]
        fun require(key: String): ParameterInfo<Int, Int, Int, Int> = parameterInfoMapV3.getValue(key)
        fun getMeta(key: String): ParameterMetaV3? = parameterMetaMapV3[key]
        fun requireMeta(key: String): ParameterMetaV3 = parameterMetaMapV3.getValue(key)
        fun getMeta(parameterInfo: ParameterInfo<Int, Int, Int, Int>): ParameterMetaV3? =
            parameterMetaByInfoV3[parameterInfo]
    }
}

//
//(address=0, ) //CPU
//(address=32) //Указательный
//(address=33) //Срединй
//(address=34) // Безымянный
//(address=35) // Мизинец
//(address=36) // Большой палец
//(address=37) // Ротация
//(address=8, // ЕМГ хаб
//(address=16) // ЕМГ 1
//(address=17) // ЕМГ 2
