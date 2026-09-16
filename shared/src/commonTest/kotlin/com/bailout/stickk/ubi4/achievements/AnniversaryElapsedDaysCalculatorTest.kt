package com.bailout.stickk.ubi4.achievements

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AnniversaryElapsedDaysCalculatorTest {

    @Test
    fun `calculates elapsed days from displayed receipt date`() {
        val elapsedDays = AnniversaryElapsedDaysCalculator.calculate(
            dateOfReceipt = "01.01.2025",
            currentDate = LocalDate(2025, 1, 31)
        )

        assertEquals(30L, elapsedDays)
    }

    @Test
    fun `supports ISO date returned by backend`() {
        val elapsedDays = AnniversaryElapsedDaysCalculator.calculate(
            dateOfReceipt = "2024-01-01T12:30:00Z",
            currentDate = LocalDate(2024, 6, 29)
        )

        assertEquals(180L, elapsedDays)
    }

    @Test
    fun `invalid or future receipt date has zero progress`() {
        val currentDate = LocalDate(2025, 1, 1)

        assertEquals(
            0L,
            AnniversaryElapsedDaysCalculator.calculate("not a date", currentDate)
        )
        assertEquals(
            0L,
            AnniversaryElapsedDaysCalculator.calculate("02.01.2025", currentDate)
        )
    }
}
