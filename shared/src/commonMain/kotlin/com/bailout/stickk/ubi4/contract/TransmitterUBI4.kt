package com.bailout.stickk.ubi4.contract

interface TransmitterUBI4 {
    fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit)
}