package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.achievements.AchievementDefinitions
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementProgress
import com.bailout.stickk.ubi4.achievements.AchievementProgressCalculator
import com.bailout.stickk.ubi4.achievements.AchievementTier
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementUiModel
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsCatalog
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsColors
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementsFontFamily
import java.text.NumberFormat

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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 10.dp, end = 44.dp),
                color = AchievementsColors.White,
                fontFamily = AchievementsFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_achievement_info),
                    contentDescription = stringResource(
                        R.string.achievement_show_description,
                        title
                    ),
                    modifier = Modifier.size(24.dp),
                    tint = AchievementsColors.White
                )
            }

            androidx.compose.foundation.Image(
                painter = painterResource(achievement.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(88.dp)
                    .achievementTierOutline(achievement.achievedTier)
            )

            AchievementStageProgress(
                progress = achievement.progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            )
        }
    }
}

private fun Modifier.achievementTierOutline(tier: AchievementTier?): Modifier {
    val achievedTier = tier ?: return this
    val outlineColors = achievedTier.outlineColors()
    val highlightColor = achievedTier.outlineHighlightColor()

    return drawWithContent {
        drawContent()

        val strokeWidth = 4.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        drawCircle(
            brush = Brush.sweepGradient(outlineColors),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = highlightColor.copy(alpha = 0.9f),
            radius = radius - 1.5.dp.toPx(),
            style = Stroke(width = 0.75.dp.toPx())
        )
    }
}

private fun AchievementTier.outlineColors(): List<Color> = when (this) {
    AchievementTier.BRONZE -> listOf(
        Color(0xFFFFD19A),
        Color(0xFF7A3E12),
        Color(0xFFDC8A3D),
        Color(0xFFA75C24),
        Color(0xFFFFD19A)
    )

    AchievementTier.SILVER -> listOf(
        Color(0xFFFFFFFF),
        Color(0xFF8E8E8E),
        Color(0xFFF5F5F5),
        Color(0xFFB8B8B8),
        Color(0xFFFFFFFF)
    )

    AchievementTier.GOLD -> listOf(
        Color(0xFFFFF2A8),
        Color(0xFFB57900),
        Color(0xFFFFD700),
        Color(0xFFD69A00),
        Color(0xFFFFF2A8)
    )
}

private fun AchievementTier.outlineHighlightColor(): Color = when (this) {
    AchievementTier.BRONZE -> Color(0xFFFFE1BD)
    AchievementTier.SILVER -> AchievementsColors.White
    AchievementTier.GOLD -> Color(0xFFFFF6C7)
}

@Composable
private fun AchievementStageProgress(
    progress: AchievementProgress,
    modifier: Modifier = Modifier
) {
    val activeColor = colorResource(R.color.ubi4_active)
    val numberFormat = NumberFormat.getIntegerInstance()
    val progressValueText = stringResource(
        R.string.achievement_progress_value,
        numberFormat.format(progress.currentValue.coerceAtMost(progress.nextTarget)),
        numberFormat.format(progress.nextTarget)
    )
    val progressText = when (progress.achievedTier) {
        AchievementTier.BRONZE -> stringResource(R.string.achievement_progress_bronze)
        AchievementTier.SILVER -> stringResource(R.string.achievement_progress_silver)
        AchievementTier.GOLD -> stringResource(R.string.achievement_progress_gold)
        null -> stringResource(R.string.achievement_progress_none)
    }

    Column(modifier = modifier) {
        Text(
            text = progressValueText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            color = AchievementsColors.White,
            fontFamily = AchievementsFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

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
            if (progress.progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.progressFraction.coerceIn(0f, 1f))
                        .background(activeColor)
                )
            }
        }
    }
}

@Preview(
    name = "Achievement item with progress",
    showBackground = true,
    backgroundColor = 0xFF2A2A2A,
    widthDp = 190,
    heightDp = 190
)
@Composable
private fun AchievementCardPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AchievementsColors.Background)
            .padding(16.dp)
    ) {
        AchievementCard(
            achievement = AchievementsCatalog.items
                .first { it.id == AchievementId.CYBORG }
                .copy(
                    progress = AchievementProgressCalculator.calculate(
                        currentValue = 1_000L,
                        definition = AchievementDefinitions[AchievementId.CYBORG]
                    )
                ),
            onInfoClick = {}
        )
    }
}
