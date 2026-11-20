package io.github.ddiling.mydayboard.feature.trackers.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StartDateFieldTest {
    private val today = LocalDate.of(2026, 3, 15)

    private fun isSelectable(day: LocalDate) = isSelectableStartDate(day.toEpochDay() * 86_400_000L, today)

    @Test
    fun `today can be picked as the start date`() = assertTrue(isSelectable(today))

    @Test
    fun `tomorrow cannot be picked as the start date`() = assertFalse(isSelectable(today.plusDays(1)))

    @Test
    fun `a past day can be picked as the start date`() = assertTrue(isSelectable(today.minusDays(30)))
}
