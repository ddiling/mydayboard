package io.github.ddiling.mydayboard.feature.trackers.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StartDateFieldTest {
    private val today = LocalDate.of(2026, 3, 15)

    private fun utcMidnight(day: LocalDate): Long = day.toEpochDay() * 86_400_000L

    @Test
    fun `today can be picked as the start date`() {
        assertTrue(isSelectableStartDate(utcMidnight(today), today))
    }

    @Test
    fun `tomorrow cannot be picked as the start date`() {
        assertFalse(isSelectableStartDate(utcMidnight(today.plusDays(1)), today))
    }

    @Test
    fun `a past day can be picked as the start date`() {
        assertTrue(isSelectableStartDate(utcMidnight(today.minusDays(30)), today))
    }
}
