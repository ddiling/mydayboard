package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/** The start date is the local day of the clock when Save is pressed, including around midnight and DST. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormStartDateTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private suspend fun TestScope.startDateWhenSavedAt(instant: Instant): LocalDate {
        val fixture = TaskFormFixture(instant)
        val viewModel = fixture.viewModelFor()
        viewModel.actions.onNameChange("Pills")
        viewModel.onSave()
        advanceUntilIdle()
        return fixture.storedTasks().single().startDate
    }

    @Test
    fun `saving one millisecond before midnight stores that day`() =
        runTest {
            val lastMillisecond = warsaw(2026, 3, 3).minusMillis(1)

            assertEquals(LocalDate.of(2026, 3, 2), startDateWhenSavedAt(lastMillisecond))
        }

    @Test
    fun `saving at midnight stores the next day`() =
        runTest {
            assertEquals(LocalDate.of(2026, 3, 3), startDateWhenSavedAt(warsaw(2026, 3, 3)))
        }

    @Test
    fun `saving at half past midnight on the spring-forward day stores that local date`() =
        runTest {
            assertEquals(LocalDate.of(2026, 3, 29), startDateWhenSavedAt(warsaw(2026, 3, 29, 0, 30)))
        }

    @Test
    fun `saving at half past midnight on the fall-back day stores that local date`() =
        runTest {
            assertEquals(LocalDate.of(2026, 10, 25), startDateWhenSavedAt(warsaw(2026, 10, 25, 0, 30)))
        }

    @Test
    fun `a zone change after opening the form moves the start date with the clock zone`() =
        runTest {
            val fixture = TaskFormFixture(warsaw(2026, 3, 2, 23, 30))
            val viewModel = fixture.viewModelFor()
            viewModel.actions.onNameChange("Pills")

            fixture.clock.zone = TestZones.TOKYO
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(LocalDate.of(2026, 3, 3), fixture.storedTasks().single().startDate)
        }
}
