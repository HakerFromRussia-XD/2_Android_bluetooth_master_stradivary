package com.bailout.stickk.ubi4.data.local.repository

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class AchievementEventManagerTest {

    @Test
    fun `connection subject uses local calendar day`() {
        val subjectId = connectionDaySubjectId(
            instant = Instant.parse("2026-07-28T21:30:00Z"),
            timeZone = TimeZone.of("Europe/Moscow")
        )

        assertEquals("2026-07-29", subjectId)
    }
}
