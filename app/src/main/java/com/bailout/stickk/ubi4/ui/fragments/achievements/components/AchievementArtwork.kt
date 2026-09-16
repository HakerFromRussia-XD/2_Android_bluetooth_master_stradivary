package com.bailout.stickk.ubi4.ui.fragments.achievements.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bailout.stickk.ubi4.ui.fragments.achievements.AchievementUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private val LockedArtworkFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) }
)

/** Keep the former Marathon height, independently of each illustration's proportions. */
@Composable
internal fun AchievementArtwork(achievement: AchievementUiModel, modifier: Modifier = Modifier) {
    val colorFilter = if (achievement.achievedTier == null) LockedArtworkFilter else null
    BoxWithConstraints(modifier) {
        // The previous Marathon artwork ended 10 dp before the progress bar's right edge.
        val artworkHeight = (maxWidth - 10.dp).coerceAtLeast(1.dp) * (822f / 1024f)
        val density = LocalDensity.current
        val target = with(density) {
            IntSize(maxWidth.roundToPx().coerceAtLeast(1), artworkHeight.roundToPx().coerceAtLeast(1))
        }
        val bounds = achievement.artworkBounds
        if (bounds == null) {
            Image(
                painter = painterResource(achievement.iconRes),
                contentDescription = null,
                modifier = Modifier.height(artworkHeight).fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                colorFilter = colorFilter
            )
        } else {
            val resources = LocalContext.current.resources
            val key = ArtworkKey(achievement.iconRes, bounds, target)
            val bitmap = produceState(AchievementBitmapCache.peek(key), key, resources) {
                value = AchievementBitmapCache.load(resources, key)
            }.value
            androidx.compose.foundation.layout.Box(Modifier.height(artworkHeight).fillMaxSize()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                        colorFilter = colorFilter
                    )
                }
            }
        }
    }
}

internal fun fittedArtworkSize(bounds: IntRect, target: IntSize): IntSize {
    val scale = min(1f, min(target.width.toFloat() / bounds.width, target.height.toFloat() / bounds.height))
    return IntSize(
        (bounds.width * scale).roundToInt().coerceAtLeast(1),
        (bounds.height * scale).roundToInt().coerceAtLeast(1)
    )
}

private data class ArtworkKey(val resource: Int, val bounds: IntRect, val target: IntSize)

/** Original PNGs stay untouched. Only cropped, screen-sized bitmaps are retained in RAM. */
private object AchievementBitmapCache {
    private val cache = object : LruCache<ArtworkKey, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: ArtworkKey, value: Bitmap) = value.allocationByteCount
        // Do not recycle evicted bitmaps: a visible Compose Image may still reference them.
    }
    private val decodeMutex = Mutex()

    fun peek(key: ArtworkKey): Bitmap? = cache.get(key)

    suspend fun load(resources: Resources, key: ArtworkKey): Bitmap = withContext(Dispatchers.IO) {
        // Bound concurrent decoding and deduplicate requests from cards re-entering the grid.
        decodeMutex.withLock {
            cache.get(key) ?: decode(resources, key).also { cache.put(key, it) }
        }
    }

    @Suppress("DEPRECATION")
    private fun decode(resources: Resources, key: ArtworkKey): Bitmap {
        val size = fittedArtworkSize(key.bounds, key.target)
        var sample = 1
        while (key.bounds.width / (sample * 2) >= size.width &&
            key.bounds.height / (sample * 2) >= size.height) {
            sample *= 2
        }
        val decoded = resources.openRawResource(key.resource).use { stream ->
            val decoder = requireNotNull(BitmapRegionDecoder.newInstance(stream, false))
            try {
                requireNotNull(decoder.decodeRegion(
                    Rect(key.bounds.left, key.bounds.top, key.bounds.right, key.bounds.bottom),
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                ))
            } finally {
                decoder.recycle()
            }
        }
        val scaled = Bitmap.createScaledBitmap(decoded, size.width, size.height, true)
        if (scaled !== decoded) decoded.recycle()
        scaled.prepareToDraw()
        return scaled
    }
}
