package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.LocalDate

/** Robolectric supplies the real `Bundle` that `toRoute` needs to read the route arguments. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val fixture = TaskFormFixture()

    // Lazy: viewModelScope must be created after the rule has installed the test Main dispatcher.
    private val viewModel by lazy { fixture.viewModelFor() }

    @Test
    fun `typing a name and saving creates the task counting from today`() =
        runTest {
            viewModel.actions.onNameChange("Pills")
            viewModel.onSave()
            advanceUntilIdle()

            val task = fixture.storedTasks().single()
            assertEquals("Pills", task.name)
            assertEquals(fixture.today, task.startDate)
        }

    @Test
    fun `a blank name is an error and blocks saving`() =
        runTest {
            viewModel.actions.onNameChange("   ")
            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(TaskFormError.BlankName in viewModel.uiState.value.errors)
            assertFalse(viewModel.uiState.value.canSave)
            assertTrue(fixture.storedTasks().isEmpty())
        }

    @Test
    fun `a one-time task due in the past counts from its due date`() =
        runTest {
            val pastDue = LocalDate.of(2026, 2, 20)
            viewModel.actions.onNameChange("Electricity bill")
            viewModel.actions.onRepeatTypeChange(RepeatType.ONE_TIME)
            viewModel.actions.onDueDateChange(pastDue)
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(pastDue, fixture.storedTasks().single().startDate)
        }

    @Test
    fun `a one-time task due in the future counts from today`() =
        runTest {
            viewModel.actions.onNameChange("Electricity bill")
            viewModel.actions.onRepeatTypeChange(RepeatType.ONE_TIME)
            viewModel.actions.onDueDateChange(LocalDate.of(2026, 3, 20))
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(fixture.today, fixture.storedTasks().single().startDate)
        }

    @Test
    fun `a finished save is marked for navigation until it is handled`() =
        runTest {
            viewModel.actions.onNameChange("Pills")
            viewModel.onSave()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.saved)

            viewModel.onSavedHandled()

            assertFalse(viewModel.uiState.value.saved)
        }

    @Test
    fun `saving twice before the first save finishes creates one task`() =
        runTest {
            viewModel.actions.onNameChange("Pills")
            viewModel.onSave()
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(1, fixture.storedTasks().size)
        }

    @Test
    fun `the saved task keeps what the form chose`() =
        runTest {
            val category = fixture.categories.createCategory("Health")
            viewModel.actions.onNameChange("Pills")
            viewModel.actions.onCategoryChange(category.id)
            viewModel.actions.onNoteChange("  with water ")
            viewModel.actions.onRepeatTypeChange(RepeatType.MONTHLY)
            viewModel.actions.onDayOfMonthChange(31)
            viewModel.onSave()
            advanceUntilIdle()

            val task = fixture.storedTasks().single()
            assertEquals(category.id, task.categoryId)
            assertEquals("with water", task.note)
            assertEquals(Repeat.Monthly(31), task.repeat)
        }
}
