package com.bailout.stickk.ubi4.ble

object SampleGattAttributes {

    private val attributes: MutableMap<String, String> = mutableMapOf()

//    const val MAIN_CHANNEL_SERVICE = "43686172-4D74-1001-726B-526F64696F6E"
//    const val MAIN_CHANNEL_CHARACTERISTIC = "43680201-4D74-1001-726B-526F64696F6E"

    const val MAIN_CHANNEL_SERVICE =        "43686172-4d74-1001-726b-526f64696f6e"
    const val MAIN_CHANNEL_CHARACTERISTIC = "43680201-4d74-1001-726b-526f64696f6e"

    const val SENSORS_STREAM_UUID =         "43680201-4d74-1001-726b-526f64696f6e"
    const val SERIALPORTCHAR_UUID =         "43681000-4d74-1001-726b-526f64696f6e"

    var showEveryoneReceiveByte: Boolean = false

    const val READ = "READ"
    const val WRITE = "WRITE"
    const val NOTIFY = "NOTIFY"

    fun lookup(uuid: String, defaultName: String): String =
        attributes[uuid] ?: defaultName

    /**
     * Позволяет добавить или обновить UUID → имя для lookup.
     */
    fun register(uuid: String, name: String) {
        attributes[uuid] = name
    }
}