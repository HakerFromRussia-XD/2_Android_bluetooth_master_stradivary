package com.bailout.stickk.ubi4.ui.fragments.achievements

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.bailout.stickk.ubi4.ui.fragments.achievements.components.fittedArtworkSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class AchievementArtworkSizeTest {
    @Test
    fun `all illustrations fit phone and tablet slots without changing proportions`() {
        for (target in listOf(IntSize(180, 123), IntSize(402, 300), IntSize(900, 700))) {
            for (item in AchievementsCatalog.items) {
                val bounds = requireNotNull(item.artworkBounds)
                val size = fittedArtworkSize(bounds, target)
                assertTrue(size.width in 1..target.width && size.height in 1..target.height)
                assertTrue(size.width <= bounds.width && size.height <= bounds.height)
                val scale = size.width.toDouble() / bounds.width
                assertTrue(abs(size.height - bounds.height * scale) <= 1.5)
            }
        }
    }

    @Test
    fun `transparent borders do not count toward fitted size and sources are never upscaled`() {
        val bounds = IntRect(13, 179, 1241, 1089)
        assertEquals(IntSize(405, 300), fittedArtworkSize(bounds, IntSize(500, 300)))
        assertEquals(IntSize(1228, 910), fittedArtworkSize(bounds, IntSize(2000, 2000)))
        assertEquals(IntSize(1, 1), fittedArtworkSize(bounds, IntSize(1, 1)))
    }
}
