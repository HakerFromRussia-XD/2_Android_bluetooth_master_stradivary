package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BleEnvironment {
    private val lock = Mutex()

    private var bleManager: BleManagerKmm? = null
    private var bleCommandExecutor: BleCommandExecutor? = null
    private var bleParser: BLEParser? = null
    private var bleParserV3: BLEParserV3? = null

    fun register(
        manager: BleManagerKmm,
        executor: BleCommandExecutor,
        parser: BLEParser,
        parserV3: BLEParserV3
    )  {
        runBlocking {
            lock.withLock {
                bleManager = manager
                bleCommandExecutor = executor
                bleParser = parser
                bleParserV3 = parserV3
            }
        }
    }

    fun getBleManager(): BleManagerKmm =
        bleManager ?: error("BleEnvironment manager is not registered")

    fun getBleCommandExecutor(): BleCommandExecutor =
        bleCommandExecutor ?: error("BleEnvironment executor is not registered")

    fun getBleParser(): BLEParser =
        bleParser ?: error("BleEnvironment parser is not registered")

    fun getBleParserV3(): BLEParserV3 =
        bleParserV3 ?: error("BleEnvironment parserV3 is not registered")
}
