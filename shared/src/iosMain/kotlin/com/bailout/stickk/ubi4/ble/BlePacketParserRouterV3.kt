package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.utility.logging.platformLog

/**
 * iOS-only dispatcher for incoming BLE packets.
 * Keeps legacy UBI4 parser flow untouched and enables V3 parser in a separate path.
 */
object BlePacketParserRouterV3 {

    fun parseIncoming(data: ByteArray) {
        if (UiState.isInterfaceV3Activated) {
            platformLog("[BLE-PARSER-ROUTER]", "route=V3")
            BLEState.bleParserV3.parseReceivedData(data)
        } else {
            platformLog("[BLE-PARSER-ROUTER]", "route=LEGACY")
            BLEState.bleParser.parseReceivedData(data)
        }
    }
}
