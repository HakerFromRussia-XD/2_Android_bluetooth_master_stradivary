package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.parser.BLEParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BleEnvironment {
    private val lock = Mutex()

    private var bleManager: BleManagerKmm? = null
    private var bleCommandExecutor: BleCommandExecutor? = null
    private var bleParser: BLEParser? = null

    fun register(
        manager: BleManagerKmm,
        executor: BleCommandExecutor,
        parser: BLEParser
    )  {
        runBlocking {
            lock.withLock {
                bleManager = manager
                bleCommandExecutor = executor
                bleParser = parser
            }
        }
    }

    fun getBleManager(): BleManagerKmm =
        bleManager ?: error("BleEnvironment manager is not registered")

    fun getBleCommandExecutor(): BleCommandExecutor =
        bleCommandExecutor ?: error("BleEnvironment executor is not registered")

    fun getBleParser(): BLEParser =
        bleParser ?: error("BleEnvironment parser is not registered")
}
