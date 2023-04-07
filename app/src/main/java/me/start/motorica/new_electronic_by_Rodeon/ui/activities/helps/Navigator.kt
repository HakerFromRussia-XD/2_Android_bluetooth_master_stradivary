package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps
import androidx.fragment.app.Fragment

fun Fragment.navigator(): Navigator {
    return requireActivity() as Navigator
}

interface Navigator {
    fun showWhiteStatusBar(show: Boolean)
    fun showHelpScreen()
    fun showSensorsHelpScreen()
    fun showTestScreen()
    fun showHowProsthesisWorksScreen()

    fun goingBack()
}