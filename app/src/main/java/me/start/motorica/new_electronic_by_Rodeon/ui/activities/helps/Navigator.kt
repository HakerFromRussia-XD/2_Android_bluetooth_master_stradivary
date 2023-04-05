package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps
import androidx.fragment.app.Fragment

fun Fragment.navigator(): Navigator {
    return requireActivity() as Navigator
}

interface Navigator {
    fun showHelpContainerView(show: Boolean)
    fun showHelpScreen()
    fun showTestScreen()

    fun goingBack()
}