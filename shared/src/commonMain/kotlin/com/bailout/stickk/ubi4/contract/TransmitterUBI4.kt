package com.bailout.stickk.ubi4.contract

interface TransmitterUBI4 {
    fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String)
    fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit)
}