package io.github.ddiling.mydayboard.feature.tasks.day

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.core.domain.task.DayChange
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class DaySheetTextsTest {
    private val resources: Resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val saturday = LocalDate.of(2026, 11, 21)

    private fun marked(kind: DaySheetKind) =
        DayMarked(
            DaySheetState("lunch", "Lunch", saturday, kind),
            TickOutcome.DayReplaced("lunch", saturday, DayChange(emptyList(), emptyList())),
        )

    @Test
    fun `a day marked done says so`() {
        assertEquals("Lunch · Sat 21.11 marked as done", dayMarkedText(resources, marked(DaySheetKind.Single(true))))
    }

    @Test
    fun `a day marked not done says so`() {
        val text = dayMarkedText(resources, marked(DaySheetKind.Single(false)))

        assertEquals("Lunch · Sat 21.11 marked as not done", text)
    }

    @Test
    fun `times and counts say the day was updated`() {
        val times = DaySheetKind.Times(listOf(LocalTime.of(8, 0)), setOf(LocalTime.of(8, 0)))

        assertEquals("Lunch · Sat 21.11 updated", dayMarkedText(resources, marked(times)))
        assertEquals("Lunch · Sat 21.11 updated", dayMarkedText(resources, marked(DaySheetKind.Count(3, 2))))
    }
}
