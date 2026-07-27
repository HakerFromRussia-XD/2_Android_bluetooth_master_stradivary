package com.bailout.stickk.ubi4.ui.fragments.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.bailout.stickk.ubi4.data.state.AchievementsState
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4

class AchievementsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val progressByAchievement by
                AchievementsState.progressByAchievement.collectAsState()
            val achievements = remember(progressByAchievement) {
                AchievementsCatalog.items.map { achievement ->
                    progressByAchievement[achievement.id]
                        ?.let { progress -> achievement.copy(progress = progress) }
                        ?: achievement
                }
            }
            AchievementsScreen(
                onBackClick = { navigator().goingBackUbi4() },
                achievements = achievements
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivityUBI4)?.apply {
            hideTopStatusBar()
            if (UiState.isInterfaceV3Activated) {
                getBLEController()?.requestTelemetryDataV3()
            }
        }
    }

    override fun onDestroyView() {
        (activity as? MainActivityUBI4)?.apply {
            showTopStatusBar()
            setStatusBarBackMode(enabled = true)
        }
        super.onDestroyView()
    }
}
