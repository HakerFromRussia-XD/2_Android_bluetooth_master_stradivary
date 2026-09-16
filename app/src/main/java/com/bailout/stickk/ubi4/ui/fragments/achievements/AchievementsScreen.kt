package com.bailout.stickk.ubi4.ui.fragments.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.achievements.AchievementCelebration
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.ui.fragments.achievements.components.AchievementCard
import com.bailout.stickk.ubi4.ui.fragments.achievements.components.AchievementCupAnimationOverlay
import com.bailout.stickk.ubi4.ui.fragments.achievements.components.AchievementInfoDialog
import kotlinx.coroutines.delay

internal object AchievementsColors {
    val Background = Color(0xFF2A2A2A)
    val Card = Color(0xFF373737)
    val ProgressTrack = Color(0xFF242424)
    val Border = Color(0xFF444444)
    val White = Color(0xFFFCFCFC)
    val Accent = Color(0xFFC6F158)
    val SystemBlue = Color(0xFF0A84FF)
    val Bronze = Color(0xFFCD7F32)
    val Silver = Color(0xFFC0C0C0)
    val Gold = Color(0xFFFFD700)
}


internal val AchievementsFontFamily = FontFamily(
    Font(R.font.sf_pro_display_light, weight = FontWeight.Normal),
    Font(R.font.sf_pro_text_bold, weight = FontWeight.Bold)
)

@Composable
fun AchievementsScreen(
    modifier: Modifier = Modifier,
    achievements: List<AchievementUiModel> = AchievementsCatalog.items,
    pendingCelebration: AchievementCelebration? = null,
    onCelebrationAcknowledged: (AchievementCelebration) -> Unit = {}
) {

    val gridState = rememberLazyGridState()
    var selectedAchievementId by rememberSaveable {
        mutableStateOf<AchievementId?>(null)
    }
    var activeCelebration by remember {
        mutableStateOf<AchievementCelebration?>(null)
    }
    var showCupAnimation by remember {
        mutableStateOf(false)
    }
    val selectedAchievement = achievements.firstOrNull {
        it.id == selectedAchievementId
    }

    LaunchedEffect(pendingCelebration) {
        val celebration = pendingCelebration ?: return@LaunchedEffect
        if (activeCelebration != null) return@LaunchedEffect

        val achievementIndex = achievements.indexOfFirst {
            it.id == celebration.achievementId
        }
        if (achievementIndex < 0) {
            onCelebrationAcknowledged(celebration)
            return@LaunchedEffect
        }

        activeCelebration = celebration
        selectedAchievementId = null
        gridState.animateScrollToItem(achievementIndex)
        delay(250L)
        showCupAnimation = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AchievementsColors.Background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = achievements,
                key = { achievement -> achievement.id.name }
            ) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    onInfoClick = {
                        selectedAchievementId = achievement.id
                    }
                )
            }
        }
    }

    selectedAchievement?.let { achievement ->
        AchievementInfoDialog(
            achievement = achievement,
            onDismiss = { selectedAchievementId = null }
        )
    }

    if (showCupAnimation) {
        val celebrationAchievement = activeCelebration?.let { celebration ->
            achievements.firstOrNull { it.id == celebration.achievementId }
        }

        AchievementCupAnimationOverlay(
            achievementTitle = celebrationAchievement
                ?.let { stringResource(it.titleRes) }
                .orEmpty(),
            onFinished = {
                showCupAnimation = false
                activeCelebration?.let { celebration ->
                    activeCelebration = null
                    onCelebrationAcknowledged(celebration)
                }
            }
        )
    }
}
