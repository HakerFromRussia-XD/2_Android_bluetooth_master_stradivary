package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps
import androidx.fragment.app.Fragment
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main.ChartFragment

fun Fragment.navigator(): Navigator {
    return requireActivity() as Navigator
}

interface Navigator {
    fun showWhiteStatusBar(show: Boolean)
    fun showGrayStatusBar(show: Boolean)
    fun showSecretSettingsScreen()
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
    fun showNeuralScreen()
    fun showArcanoidScreen(chartFragmentClass: ChartFragment)
    fun showAccountScreen(chartFragmentClass: ChartFragment)
    fun showAccountCustomerServiceScreen()
    fun showAccountProsthesisInformationScreen()

    fun getBackStackEntryCount():Int

    fun goingBack()
}