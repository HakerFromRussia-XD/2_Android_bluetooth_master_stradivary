package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementTier
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementUiModel
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsColors
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsFontFamily

@Composable
internal fun AchievementCard(
    achievement: AchievementUiModel,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(achievement.titleRes)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AchievementsColors.Card,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = AchievementsColors.White,
                    fontFamily = AchievementsFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onInfoClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_achievement_info),
                        contentDescription = stringResource(R.string.achievement_show_description, title),
                        modifier = Modifier.size(24.dp),
                        tint = AchievementsColors.White
                    )
                }
            }

            AchievementArtwork(
                achievement = achievement,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            // The same gap separates the visible artwork, counter and progress bar.
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.achievement_stage_count,
                    achievement.achievedTier?.level ?: 0,
                    AchievementTier.entries.size
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = AchievementsColors.White,
                fontFamily = AchievementsFontFamily,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            AchievementStageProgress(
                achievedTier = achievement.achievedTier,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AchievementStageProgress(
    achievedTier: AchievementTier?,
    modifier: Modifier = Modifier
) {
    val activeColor = colorResource(R.color.ubi4_active)
    val progressFraction = (achievedTier?.level ?: 0) / AchievementTier.entries.size.toFloat()
    val progressText = when (achievedTier) {
        AchievementTier.BRONZE -> stringResource(R.string.achievement_progress_bronze)
        AchievementTier.SILVER -> stringResource(R.string.achievement_progress_silver)
        AchievementTier.GOLD -> stringResource(R.string.achievement_progress_gold)
        null -> stringResource(R.string.achievement_progress_none)
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AchievementsColors.ProgressTrack)
                .semantics {
                    contentDescription = progressText
                }
        ) {
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .background(activeColor)
                )
            }
        }
    }
}
