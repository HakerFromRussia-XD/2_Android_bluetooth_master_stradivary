package com.bailout.stickk.ubi4.ui.fragments.account.statisticsFragmentV3

import android.content.Context
import android.graphics.Color
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3GestureUsage
import com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics.V3AccountStatisticsUiState
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

class V3AccountStatisticsChartMapperTest {
    @BeforeEach fun setUp() {
        mockkStatic(Color::class)
        every { Color.rgb(any<Int>(), any<Int>(), any<Int>()) } answers {
            (0xff shl 24) or (firstArg<Int>() shl 16) or (secondArg<Int>() shl 8) or thirdArg<Int>()
        }
    }
    @AfterEach fun tearDown() { unmockkStatic(Color::class) }

    @Test fun `factory labels keep localized resources and hidden twelve fallback in current locale`() {
        val context = mockk<Context>()
        var locale = "ru"
        every { context.getString(any()) } answers { "$locale:${firstArg<Int>()}" }
        val state = V3AccountStatisticsUiState(listOf(V3GestureUsage(1, 7), V3GestureUsage(12, 3), V3GestureUsage(15, 2)))
        val rows = V3AccountStatisticsChartMapper.map(context, state)
        assertEquals(listOf("ru:${SharedRes.strings.fist.resourceId}", "Gesture 12", "ru:${SharedRes.strings.gesture_natural_position.resourceId}"), rows.map { it.title })
        assertEquals(listOf(7L, 3L, 2L), rows.map { it.count })
        locale = "en"
        assertEquals("en:${SharedRes.strings.fist.resourceId}", V3AccountStatisticsChartMapper.map(context, state).first().title)
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
    }

    @Test fun `saved empty names fifteen defaults and further indexes retain previous labels`() {
        val context = mockk<Context>()
        every { context.getString(any()) } answers { "res:${firstArg<Int>()}" }
        val state = V3AccountStatisticsUiState(listOf(
            V3GestureUsage(64, 10, 0, "Saved"), V3GestureUsage(65, 9, 1, ""),
            V3GestureUsage(77, 8, 13), V3GestureUsage(78, 7, 14),
            V3GestureUsage(79, 6, 15), V3GestureUsage(80, 5, 16, "Beyond defaults"),
        ))
        val rows = V3AccountStatisticsChartMapper.map(context, state)
        assertEquals(listOf("Saved", "", "res:${R.string.gesture_14_btn}", "res:${R.string.gesture_15_btn}", "res:${R.string.gesture_1_btn}", "Beyond defaults"), rows.map { it.title })
        assertEquals(state.gestures.map { it.gestureId }, rows.map { it.gestureId })
        assertTrue(V3AccountStatisticsChartMapper.map(context, V3AccountStatisticsUiState()).isEmpty())
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
    }
}
