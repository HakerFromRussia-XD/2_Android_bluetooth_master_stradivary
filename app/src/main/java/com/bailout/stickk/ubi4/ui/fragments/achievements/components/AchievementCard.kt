package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.R
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
        modifier = modifier.aspectRatio(1f),
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
                    .size(88.dp),
                colorFilter = ColorFilter.tint(AchievementsColors.White)
            )
        }
    }
}
