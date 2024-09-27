package com.bailout.stickk.ubi4.utility

interface ConstantManager {
    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val RECONNECT_BLE_PERIOD = 1000

        //кол-во байт в посылках
        const val HEADER_BLE_OFFSET = 7
        const val READ_DEVICE_ADDITIONAL_PARAMETR_DATA = 2
        const val ADDITIONAL_INFO_SEG = 8*2
        const val BASE_PARAMETER_INFO_STRUCT_SIZE = 16*2
        const val ADDITIONAL_INFO_SIZE_STRUCT_SIZE = 8*2
    }
}