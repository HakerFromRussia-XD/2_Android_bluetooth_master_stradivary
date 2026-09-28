package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.testing.RecordingBleCommandExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.test.*

class FirmwareRunTypeRoutingTest {
    @Test fun realRelayPacketsUpdateRequestedBoardAcrossBootAndMain() = runBlocking {
        FirmwareInfoState.runTypeReplyRouter.resolve(0, Long.MAX_VALUE)
        val executor = RecordingBleCommandExecutor()
        val manager = BleManagerKmm().also { it.setBleCommandExecutor(executor) }
        val parser = BLEParserV3(this, executor, manager)
        for ((address, code) in listOf(9 to 3, 9 to 1, 0 to 3, 0 to 1)) {
            val result = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(1000) { FirmwareInfoState.runProgramTypeFlow.first() }
            }
            BLECommandsV3.requestRunProgramTypeFw(address)
            parser.parseReceivedData(byteArrayOf(0, 3, 1, code.toByte(), (0x7f xor code).toByte()))
            assertEquals(address, result.await().first)
            assertEquals(code, result.await().second.code)
        }
    }
}
