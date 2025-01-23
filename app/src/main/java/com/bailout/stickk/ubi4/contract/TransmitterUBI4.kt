package com.bailout.stickk.ubi4.contract
import androidx.fragment.app.Fragment


fun Fragment.transmitter(): TransmitterUBI4 {
    return requireActivity() as TransmitterUBI4
}

interface TransmitterUBI4 {
    fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String)
    fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit)
}