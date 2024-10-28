package com.bailout.stickk.ubi4.contract
import androidx.fragment.app.Fragment


fun Fragment.navigator(): NavigatorUBI4 {
    return requireActivity() as NavigatorUBI4
}

interface NavigatorUBI4 {

    fun showToast(massage: String)
    fun saveString(key: String, text: String)


    fun getBackStackEntryCount():Int
    fun goingBack()
    fun goToMenu()
}