package com.bailout.stickk.ubi4.contract

interface TransmitterUBI4 {
    fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String)
    fun bleCommandV3(byteArray: ByteArray?)
    fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit)
}