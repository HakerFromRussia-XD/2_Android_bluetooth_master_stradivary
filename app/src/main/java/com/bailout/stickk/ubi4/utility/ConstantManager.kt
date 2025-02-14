package com.bailout.stickk.ubi4.utility

interface ConstantManager {
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
    }
}