package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps
import androidx.fragment.app.Fragment
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

fun Fragment.navigator(): Navigator {
    return requireActivity() as Navigator
}

interface Navigator {
    fun showWhiteStatusBar(show: Boolean)
    fun showHelpScreen(chartFragmentClass: ChartFragment)
    fun showSensorsHelpScreen()
    fun showGesturesHelpScreen()
    fun showHowProsthesisWorksScreen()
    fun showHowProsthesisWorksMonoScreen()
    fun showHowPutOnTheProthesisSocketScreen()
    fun showCompleteSetScreen()
    fun showChargingTheProthesisScreen()
    fun showProsthesisCareScreen()
    fun showServiceAndWarrantyScreen()

    fun goingBack()
}