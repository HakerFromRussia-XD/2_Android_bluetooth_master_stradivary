package com.bailout.stickk.ubi4.achievements

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn

/** Calculates Anniversary progress from the prosthesis receipt date. */
object AnniversaryElapsedDaysCalculator {

    fun calculate(
        dateOfReceipt: String?,
        currentDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Long {
        val receiptDate = parseDate(dateOfReceipt) ?: return 0L
        if (receiptDate > currentDate) return 0L

        return receiptDate.daysUntil(currentDate).toLong()
    }

    private fun parseDate(value: String?): LocalDate? {
        val normalizedValue = value?.trim().orEmpty()
        if (normalizedValue.isEmpty()) return null

        return parseDayMonthYear(normalizedValue)
            ?: parseIsoDate(normalizedValue)
    }

    private fun parseDayMonthYear(value: String): LocalDate? {
        val parts = value.split('.')
        if (parts.size != 3) return null

        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null

        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    private fun parseIsoDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}
