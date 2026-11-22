package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormEditTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val fixture = TaskFormFixture()
    private val storedStart = LocalDate.of(2026, 2, 1)

    @Test
    fun `edit mode loads the stored values into the form`() =
        runTest {
            val stored = fixture.givenTask("Vacuum", Repeat.Weekdays(setOf(DayOfWeek.SATURDAY)))
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Vacuum", state.name)
            assertEquals(RepeatType.WEEKDAYS, state.repeatType)
            assertEquals(setOf(DayOfWeek.SATURDAY), state.weekdays)
            assertTrue(state.isEditing)
            assertEquals(FormLoad.Ready, state.load)
        }

    @Test
    fun `saving in edit mode updates the task and keeps its start date`() =
        runTest {
            val stored = fixture.givenTask("Vacuum", startDate = storedStart)
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()

            viewModel.actions.onNameChange("Vacuum the stairs")
            viewModel.onSave()
            advanceUntilIdle()

            val task = fixture.storedTasks().single()
            assertEquals("Vacuum the stairs", task.name)
            assertEquals(storedStart, task.startDate)
        }

    @Test
    fun `an unknown id shows not found and cannot be saved`() =
        runTest {
            val viewModel = fixture.viewModelFor("missing")
            advanceUntilIdle()

            assertEquals(FormLoad.NotFound, viewModel.uiState.value.load)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `the form lists the categories to pick from`() =
        runTest {
            val category = fixture.categories.createCategory("Home")
            val viewModel = fixture.viewModelFor()
            backgroundScope.launch { viewModel.categories.collect {} }
            advanceUntilIdle()

            assertEquals(listOf(category), viewModel.categories.value)
        }
}
