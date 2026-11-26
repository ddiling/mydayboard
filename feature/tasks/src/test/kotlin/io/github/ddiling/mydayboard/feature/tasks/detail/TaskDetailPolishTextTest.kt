package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.feature.tasks.common.textsToday
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pl")
class TaskDetailPolishTextTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val since = LocalDate.of(2026, 10, 3)

    @Test
    fun `the streak follows the Polish plural rules`() {
        val streaks =
            listOf(1, 3, 5).map {
                val stats =
                    DetailStats.Recurring(
                        since,
                        it,
                        0.0,
                        0,
                        0.0,
                        countsTicks = false,
                        skipped = 0,
                        halfDays = null,
                    )
                statCells(resources, stats)[1].value
            }

        assertEquals(listOf("1 dzień", "3 dni", "5 dni"), streaks)
    }

    @Test
    fun `the status words are in Polish`() {
        assertEquals("Jeszcze nie zrobione", heroText(resources, Hero.NotDone, textsToday))
        assertEquals("1 z 2 dzisiaj", heroText(resources, Hero.TimesProgress(1, 2), textsToday))
    }

    @Test
    fun `a late history row says it is not done yet in Polish`() {
        assertEquals("Spóźnione · jeszcze nie zrobione", historyResultText(resources, HistoryResult.Late))
    }

    @Test
    fun `the stats grid uses the Polish decimal comma and month names`() {
        val stats = DetailStats.Recurring(since, 4, 7.5, 9, 0.85, countsTicks = false, skipped = 1, halfDays = null)

        val cells = statCells(resources, stats).map { it.value }

        assertEquals(listOf("3 paź", "4 dni", "7,5 z 9", "0,9", "1"), cells)
    }

    @Test
    fun `the next due day uses the Polish weekday and date format`() {
        assertEquals("Następne: sob. 28.11", nextDueText(resources, LocalDate.of(2026, 11, 28)))
    }
}
