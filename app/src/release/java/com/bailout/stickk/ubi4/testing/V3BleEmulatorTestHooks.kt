package com.bailout.stickk.ubi4.testing

import com.bailout.stickk.ubi4.data.parser.BLEParserV3

object V3BleEmulatorTestHooks {
    const val EXTRA_ENABLED = "com.bailout.stickk.ubi4.testing.V3_BLE_EMULATOR_ENABLED"
    const val EXTRA_OPEN_GESTURES = "com.bailout.stickk.ubi4.testing.V3_BLE_EMULATOR_OPEN_GESTURES"

    fun enable() = Unit
    fun disable() = Unit
    fun reset() = Unit
    fun isEnabled(): Boolean = false
    fun outgoingPacketsSnapshot(): List<ByteArray> = emptyList()
    fun hasOutgoingSubcommand(subcommand: Int): Boolean = false
    fun bindingPairsSnapshot(): List<Pair<Int, Int>> = emptyList()
    fun injectInitialResponses(parser: BLEParserV3?) = Unit
    fun tryHandleOutgoing(
        byteArray: ByteArray?,
        parser: BLEParserV3?,
        onChunkSent: () -> Unit
    ): Boolean = false
}
