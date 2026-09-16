package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementUiModel
import kotlin.math.roundToInt

/** Fill the available width and derive height from the visible PNG bounds, without distortion. */
@Composable
internal fun AchievementArtwork(achievement: AchievementUiModel, modifier: Modifier = Modifier) {
    val bounds = achievement.artworkBounds
    if (bounds == null) {
        Image(
            painter = painterResource(achievement.iconRes),
            contentDescription = null,
            modifier = modifier.aspectRatio(1f),
            alignment = Alignment.BottomCenter
        )
        return
    }
    val bitmap = ImageBitmap.imageResource(achievement.iconRes)
    Canvas(modifier.aspectRatio(bounds.width.toFloat() / bounds.height)) {
        val scale = size.width / bounds.width
        val width = (bounds.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bounds.height * scale).roundToInt().coerceAtLeast(1)
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(bounds.left, bounds.top),
            srcSize = IntSize(bounds.width, bounds.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height)
        )
    }
}
