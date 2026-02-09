package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleEnvironment
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.MainScope

/**
 * Provides singleton instances for BLE-related components on iOS.
 * The same [BleManagerKmm] and [BleCommandExecutorIos] are shared between
 * the parser and any other callers.
 */
@ExperimentalForeignApi
object BLEComponents {
    /** Shared BLE manager for the entire app. */
    @OptIn(ExperimentalForeignApi::class)
    val bleManager: BleManagerKmm = BleManagerKmm()

    /** Command executor used by both [bleManager] and [bleParser]. */
    @OptIn(ExperimentalForeignApi::class)
    private val commandExecutor = bleManager.bleCommandExecutor

    /** Expose the shared command executor if Swift code needs direct access. */
    val bleCommandExecutor: BleCommandExecutor = commandExecutor

    /** Parser that works with the same manager and command executor. */
    @OptIn(ExperimentalForeignApi::class)
    val bleParser: BLEParser = BLEParser(
        coroutineScope = MainScope(),
        bleCommandExecutor = commandExecutor,
        bleManager = bleManager
    )

    init {
        // Expose parser globally so notifications from BleManager can be handled.
        BLEState.bleParser = bleParser
        BleEnvironment.register(bleManager, commandExecutor, bleParser)
    }
}