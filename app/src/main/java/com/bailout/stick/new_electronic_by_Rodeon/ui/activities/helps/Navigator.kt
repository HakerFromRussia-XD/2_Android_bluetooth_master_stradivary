package com.bailout.stick.new_electronic_by_Rodeon.ui.activities.helps
import androidx.fragment.app.Fragment
import com.bailout.stick.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

fun Fragment.navigator(): Navigator {
    return requireActivity() as Navigator
}

interface Navigator {
    fun showWhiteStatusBar(show: Boolean)
    fun showHelpScreen(chartFragmentClass: ChartFragment)
    fun showSensorsHelpScreen(chartFragmentClass: ChartFragment)
    fun showGesturesHelpScreen(chartFragmentClass: ChartFragment)
    fun showHelpMonoAdvancedSettingsScreen(chartFragmentClass: ChartFragment)
    fun showHelpMultyAdvancedSettingsScreen(chartFragmentClass: ChartFragment)
    fun showHowProsthesesWorksScreen()
    fun showHowProsthesesWorksMonoScreen()
    fun showHowPutOnTheProsthesesSocketScreen()
    fun showCompleteSetScreen()
    fun showChargingTheProsthesesScreen()
    fun showProsthesesCareScreen()
    fun showServiceAndWarrantyScreen()

    fun getBackStackEntryCount():Int

    fun goingBack()
}