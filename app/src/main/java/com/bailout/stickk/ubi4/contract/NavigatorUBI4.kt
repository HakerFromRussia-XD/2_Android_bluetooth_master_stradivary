package com.bailout.stickk.ubi4.contract
import androidx.fragment.app.Fragment


fun Fragment.navigator(): NavigatorUBI4 {
    return requireActivity() as NavigatorUBI4
}

interface NavigatorUBI4 {

    fun saveString(key: String, text: String)
    fun initBLEStructure()
    fun scanLeDevice(enable: Boolean)
    fun disconnect ()
    fun reconnect ()
    fun bleCommand(byteArray: ByteArray?, command: String, typeCommand: String)

    fun getBackStackEntryCount():Int
    fun goingBack()
    fun goToMenu()
}