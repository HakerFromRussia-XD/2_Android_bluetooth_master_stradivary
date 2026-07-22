package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementStageUiModel
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementTier
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementUiModel
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsColors
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsFontFamily

@Composable
internal fun AchievementInfoDialog(
    achievement: AchievementUiModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(achievement.titleRes),
                color = AchievementsColors.White,
                fontFamily = AchievementsFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                achievement.stages.forEach { stage ->
                    AchievementStage(stage = stage)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.achievements_close),
                    color = AchievementsColors.Accent,
                    fontFamily = AchievementsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = AchievementsColors.Card,
        contentColor = AchievementsColors.White
    )
}

@Composable
private fun AchievementStage(stage: AchievementStageUiModel) {
    val tierColor = stage.tier.color()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tierColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(stage.tier.titleRes()),
            color = tierColor,
            fontFamily = AchievementsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(stage.descriptionRes),
            color = AchievementsColors.White,
            fontFamily = AchievementsFontFamily,
            fontSize = 12.sp
        )
    }
}

private fun AchievementTier.titleRes(): Int = when (this) {
    AchievementTier.BRONZE -> R.string.achievement_tier_bronze
    AchievementTier.SILVER -> R.string.achievement_tier_silver
    AchievementTier.GOLD -> R.string.achievement_tier_gold
}

private fun AchievementTier.color(): Color = when (this) {
    AchievementTier.BRONZE -> AchievementsColors.Bronze
    AchievementTier.SILVER -> AchievementsColors.Silver
    AchievementTier.GOLD -> AchievementsColors.Gold
}
