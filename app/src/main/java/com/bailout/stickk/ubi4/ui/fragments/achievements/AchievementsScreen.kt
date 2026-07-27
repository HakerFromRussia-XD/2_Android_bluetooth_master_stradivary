package com.bailout.stickk.ubi4.ui.fragments.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.ui.fragments.achievements.components.AchievementCard
import com.bailout.stickk.ubi4.ui.fragments.achievements.components.AchievementInfoDialog

internal object AchievementsColors {
    val Background = Color(0xFF2A2A2A)
    val Card = Color(0xFF373737)
    val ProgressTrack = Color(0xFF242424)
    val Border = Color(0xFF444444)
    val White = Color(0xFFFCFCFC)
    val Accent = Color(0xFFC6F158)
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    achievements: List<AchievementUiModel> = AchievementsCatalog.items
) {
    var selectedAchievementId by rememberSaveable {
        mutableStateOf<AchievementId?>(null)
    }
    val selectedAchievement = achievements.firstOrNull { it.id == selectedAchievementId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AchievementsColors.Background)
    ) {
        AchievementsTopBar(onBackClick = onBackClick)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
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
                    onInfoClick = { selectedAchievementId = achievement.id }
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
}

@Composable
private fun AchievementsTopBar(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 18.dp, end = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(34.dp)
                .clip(CircleShape)
                .background(AchievementsColors.Card)
                .border(1.dp, AchievementsColors.Border, CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.achievements_back),
                modifier = Modifier.size(22.dp),
                tint = AchievementsColors.White
            )
        }

        Text(
            text = stringResource(R.string.achievements_title),
            color = AchievementsColors.White,
            fontFamily = AchievementsFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AchievementsColors.Border)
    )
}
