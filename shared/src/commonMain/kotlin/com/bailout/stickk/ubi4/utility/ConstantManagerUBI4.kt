package com.bailout.stickk.ubi4.utility

interface ConstantManagerUBI4 {
    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val DURATION_ANIMATION = 500L
        const val RECONNECT_BLE_PERIOD = 1000

        //кол-во байт в посылках
        const val HEADER_BLE_OFFSET = 7
        const val BASE_SUB_DEVICE_STRUCT_SIZE = 9
        const val READ_DEVICE_ADDITIONAL_PARAMETR_DATA = 2
        const val READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA = 3
        const val ADDITIONAL_INFO_SEG = 8*2
        const val BASE_PARAMETER_INFO_STRUCT_SIZE = 16*2
        const val ADDITIONAL_INFO_SIZE_STRUCT_SIZE = 8*2
        const val CHECKPOINT_NAME = "checkpoint.ckpt"
        const val PARAMS_BIN_NAME = "params.bin"
        const val DATA_MANAGER_PAYLOAD_OFFSET = (HEADER_BLE_OFFSET + 3) * 2

        const val DEVICE_TYPE_FEST_X: String = "FEST-X"
        const val DEVICE_TYPE_FEST_F: String = "FEST-F"

        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME";
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        //parameters V3
        // [new widgets V3] тут добавляем ключи новых параметров
        const val P_KEY_PLOT = "PI_KEY_1"
        const val P_KEY_OPEN_CLOSE_THRESHOLD = "PI_KEY_OPEN_CLOSE_THRESHOLD"
        const val P_KEY_EMG_GAIN_OPEN_VALUE = "P_KEY_EMG_GAIN_OPEN_VALUE"
        const val P_KEY_EMG_GAIN_CLOSE_VALUE = "P_KEY_EMG_GAIN_CLOSE_VALUE"
        const val P_KEY_CURRENT_GESTURE = "P_KEY_CURRENT_GESTURE"
        const val P_KEY_GESTURE_SETTING = "P_KEY_GESTURE_SETTING"
        const val P_KEY_GESTURE_GROUPE = "P_KEY_GESTURE_GROUPE"
        const val P_KEY_EMG_CHANGE_GESTURE = "P_KEY_EMG_CHANGE_GESTURE"
        const val P_KEY_TEST_SWITCHER = "P_KEY_TEST_SWITCHER"
        const val P_KEY_START_CALIBRATE_COMMAND = "P_KEY_START_CALIBRATE_COMMAND"
        const val P_KEY_HAND_CONTROL_MODE = "P_KEY_HAND_CONTROL_MODE"
        const val P_KEY_SCREEN_TIMEOUT = "P_KEY_SCREEN_TIMEOUT"
        const val P_KEY_EMG_MOVEMENT_LOCK = "P_KEY_EMG_MOVEMENT_LOCK"
        const val P_KEY_LEFT_RIGHT_HAND = "P_KEY_LEFT_RIGHT_HAND"



        val CRC_TABLE = intArrayOf(
            0, 94, 188, 226, 97, 63, 221, 131, 194, 156, 126, 32, 163, 253, 31, 65,
            157, 195, 33, 127, 252, 162, 64, 30, 95, 1, 227, 189, 62, 96, 130, 220,
            35, 125, 159, 193, 66, 28, 254, 160, 225, 191, 93, 3, 128, 222, 60, 98,
            190, 224, 2, 92, 223, 129, 99, 61, 124, 34, 192, 158, 29, 67, 161, 255,
            70, 24, 250, 164, 39, 121, 155, 197, 132, 218, 56, 102, 229, 187, 89, 7,
            219, 133, 103, 57, 186, 228, 6, 88, 25, 71, 165, 251, 120, 38, 196, 154,
            101, 59, 217, 135, 4, 90, 184, 230, 167, 249, 27, 69, 198, 152, 122, 36,
            248, 166, 68, 26, 153, 199, 37, 123, 58, 100, 134, 216, 91, 5, 231, 185,
            140, 210, 48, 110, 237, 179, 81, 15, 78, 16, 242, 172, 47, 113, 147, 205,
            17, 79, 173, 243, 112, 46, 204, 146, 211, 141, 111, 49, 178, 236, 14, 80,
            175, 241, 19, 77, 206, 144, 114, 44, 109, 51, 209, 143, 12, 82, 176, 238,
            50, 108, 142, 208, 83, 13, 239, 177, 240, 174, 76, 18, 145, 207, 45, 115,
            202, 148, 118, 40, 171, 245, 23, 73, 8, 86, 180, 234, 105, 55, 213, 139,
            87, 9, 235, 181, 54, 104, 138, 212, 149, 203, 41, 119, 244, 170, 72, 22,
            233, 183, 85, 11, 136, 214, 52, 106, 43, 117, 151, 201, 74, 20, 246, 168,
            116, 42, 200, 150, 21, 75, 169, 247, 182, 232, 10, 84, 215, 137, 107, 53
        )
    }
}