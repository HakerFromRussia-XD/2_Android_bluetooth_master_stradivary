package com.bailout.stickk.ubi4.ui.fragments.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4

class AchievementsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AchievementsScreen(
                onBackClick = { navigator().goingBackUbi4() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivityUBI4)?.hideTopStatusBar()
    }

    override fun onDestroyView() {
        (activity as? MainActivityUBI4)?.apply {
            showTopStatusBar()
            setStatusBarBackMode(enabled = true)
        }
        super.onDestroyView()
    }
}
