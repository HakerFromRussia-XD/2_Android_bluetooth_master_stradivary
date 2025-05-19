package com.bailout.stickk.ubi4.contract
import androidx.fragment.app.Fragment


fun Fragment.navigator(): NavigatorUBI4 {
    return requireActivity() as NavigatorUBI4
}

interface NavigatorUBI4 {

    fun showGesturesScreen()
    fun showSensorsScreen()
    fun showAdvancedScreen()
    //optic
    fun showOpticGesturesScreen()
    fun showOpticTrainingGesturesScreen()
    fun showMotionTrainingScreen(onFinish:()->Unit)
    fun manageTrainingLifecycle()
    fun getPercentProgressLearningModel() : Int
    fun showSpecialScreen()

    fun showAccountScreen()
    fun showAccountCustomerServiceScreen()
    fun showAccountProsthesisInformationScreen()



    fun showToast(massage: String)
    fun saveString(key: String, text: String)
    fun getString(key: String) :String

    fun getBackStackEntryCount():Int
    fun goingBackUbi4()
    fun goToMenu()



}