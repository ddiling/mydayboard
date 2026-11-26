package io.github.ddiling.mydayboard.feature.tasks.day

import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.core.testing.warsaw
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailFixture
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DaySheetTimeTest {
    private val started = LocalDate.of(2026, 1, 1)

    private fun sheetOf(fixture: TaskDetailFixture) =
        DaySheet(TaskActions(fixture.tasks, fixture.ticks, fixture.clock), fixture.clock)

    @Test
    fun `at ten past midnight yesterday is the day that just ended`() =
        runTest {
            val fixture = TaskDetailFixture(warsaw(2026, 11, 19, 0, 10))
            val task = fixture.addTask("Vacuum", startDate = started)

            assertTrue(sheetOf(fixture).canOpen(task, LocalDate.of(2026, 11, 18)))
            assertFalse(sheetOf(fixture).canOpen(task, LocalDate.of(2026, 11, 19)))
        }

    @Test
    fun `on the spring-forward day yesterday and today are different dates`() =
        runTest {
            val fixture = TaskDetailFixture(warsaw(2026, 3, 29, 0, 30))
            val task = fixture.addTask("Vacuum", startDate = started)

            assertTrue(sheetOf(fixture).canOpen(task, LocalDate.of(2026, 3, 28)))
            assertFalse(sheetOf(fixture).canOpen(task, LocalDate.of(2026, 3, 29)))
        }

    @Test
    fun `on the fall-back day yesterday and today are different dates`() =
        runTest {
            val fixture = TaskDetailFixture(warsaw(2026, 10, 25, 0, 30))
            val task = fixture.addTask("Vacuum", startDate = started)

            assertTrue(sheetOf(fixture).canOpen(task, LocalDate.of(2026, 10, 24)))
            assertFalse(sheetOf(fixture).canOpen(task, LocalDate.of(2026, 10, 25)))
        }

    @Test
    fun `after a zone change the allowed days follow the new zone`() =
        runTest {
            val fixture = TaskDetailFixture(warsaw(2026, 11, 18, 23, 30))
            val task = fixture.addTask("Vacuum", startDate = started)
            val sheet = sheetOf(fixture)
            val evening = LocalDate.of(2026, 11, 18)
            assertFalse(sheet.canOpen(task, evening))

            fixture.clock.zone = ZoneId.of("Asia/Tokyo")

            assertTrue(sheet.canOpen(task, evening))
        }
}
