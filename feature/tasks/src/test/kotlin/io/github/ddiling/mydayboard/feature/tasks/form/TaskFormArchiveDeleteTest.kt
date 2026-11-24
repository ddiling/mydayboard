package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormArchiveDeleteTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val fixture = TaskFormFixture()

    @Test
    fun `archive stamps the task from the clock and reports an archived notice with its id and name`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()

            viewModel.actions.onArchive()
            advanceUntilIdle()

            assertEquals(fixture.clock.instant(), fixture.storedTasks().single().archivedAt)
            assertEquals(TasksListNotice.Archived(stored.id, "Pills"), viewModel.uiState.value.finished)
        }

    @Test
    fun `archive ignores what was typed and not saved`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()
            viewModel.actions.onNameChange("Pills and vitamins")

            viewModel.actions.onArchive()
            advanceUntilIdle()

            assertEquals("Pills", fixture.storedTasks().single().name)
            assertEquals(TasksListNotice.Archived(stored.id, "Pills"), viewModel.uiState.value.finished)
        }

    @Test
    fun `archive on a form for a new task does nothing`() =
        runTest {
            val viewModel = fixture.viewModelFor()
            advanceUntilIdle()

            viewModel.actions.onArchive()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.finished)
        }

    @Test
    fun `delete request opens the question with the stored name and changes nothing`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()

            viewModel.actions.onDeleteRequest()
            advanceUntilIdle()

            assertEquals("Pills", viewModel.uiState.value.deleteDialogName)
            assertNotNull(fixture.tasks.observeTask(stored.id).first())
            assertNull(viewModel.uiState.value.finished)
        }

    @Test
    fun `keeping the task closes the question and changes nothing`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()
            viewModel.actions.onDeleteRequest()

            viewModel.actions.onDeleteDismiss()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.deleteDialogName)
            assertNotNull(fixture.tasks.observeTask(stored.id).first())
            assertNull(viewModel.uiState.value.finished)
        }

    @Test
    fun `confirming deletes the task and reports a deleted notice with no id to undo`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()
            viewModel.actions.onDeleteRequest()

            viewModel.actions.onDeleteConfirm()
            advanceUntilIdle()

            assertNull(fixture.tasks.observeTask(stored.id).first())
            assertEquals(TasksListNotice.Deleted("Pills"), viewModel.uiState.value.finished)
            assertNull(viewModel.uiState.value.deleteDialogName)
        }

    @Test
    fun `confirming without the question having been opened deletes nothing`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()

            viewModel.actions.onDeleteConfirm()
            advanceUntilIdle()

            assertNotNull(fixture.tasks.observeTask(stored.id).first())
            assertNull(viewModel.uiState.value.finished)
        }

    @Test
    fun `handling the notice clears it so it is sent only once`() =
        runTest {
            val stored = fixture.givenTask("Pills")
            val viewModel = fixture.viewModelFor(stored.id)
            advanceUntilIdle()
            viewModel.actions.onArchive()
            advanceUntilIdle()

            viewModel.actions.onFinishedHandled()

            assertNull(viewModel.uiState.value.finished)
        }
}
