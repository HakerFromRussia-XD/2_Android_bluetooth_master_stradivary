package com.bailout.stickk.ubi4.ble

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendFlag
import okhttp3.internal.notifyAll
import okhttp3.internal.wait

/** Shared V3/UBI4 write gate. Completion retains the existing GATT callback semantics. */
class BleCommandWriter(
    private val dispatch: (ByteArray?, String, String) -> Boolean,
) {
    private val writeLock = Any()

    fun reset() {
        synchronized(writeLock) {
            canSendFlag = false
            writeLock.notifyAll()
        }
    }

    fun onWriteCompleted() {
        synchronized(writeLock) {
            canSendFlag = true
            writeLock.notifyAll()
        }
    }

    fun writeAsync(
        scope: CoroutineScope,
        packet: ByteArray?,
        command: String,
        type: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        onSent: () -> Unit,
    ) {
        scope.launch(dispatcher) {
            write(packet, command, type)
            onSent()
        }
    }

    fun write(byteArray: ByteArray?, command: String, typeCommand: String) {
        synchronized(writeLock) {
            canSendFlag = false
            Log.d(
                "BLE_Q",
                "SEND cmd=$typeCommand uuid=$command size=${byteArray?.size ?: -1} thread=${Thread.currentThread().name}"
            )
            val dispatched = dispatch(byteArray, command, typeCommand)
            if (!dispatched) {
                Log.w("BLE_Q", "Command not dispatched cmd=$typeCommand uuid=$command; release queue slot")
                canSendFlag = true
                return
            }
            Log.d("TestSendByteArray","send!!!!")
            while (!canSendFlag) {
                writeLock.wait()
            }
            Log.d("TestSendByteArray","CallBack is BLEService was complete")
        }
    }
}
