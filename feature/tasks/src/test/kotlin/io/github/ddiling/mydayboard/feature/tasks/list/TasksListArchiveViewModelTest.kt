package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.lifecycle.SavedStateHandle
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.feature.tasks.navigation.TASKS_LIST_NOTICE_KEY
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksListArchiveViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fixture = TasksListFixture()
    private val savedState = SavedStateHandle()
    private val viewModel by lazy { fixture.viewModel(savedState) }

    private fun TestScope.state(): TasksListUiState {
        backgroundScope.launch { viewModel.uiState.collect {} }
        backgroundScope.launch { viewModel.notice.collect {} }
        runCurrent()
        return viewModel.uiState.value
    }

    @Test
    fun `an archived task is gone from the rows and counted as archived`() =
        runTest {
            val pills = fixture.addTask("Pills")
            fixture.addTask("Vacuum")
            fixture.tasks.archiveTask(pills.id)

            val content = state() as TasksListUiState.Content

            assertEquals(listOf("Vacuum"), content.groups.flatMap { it.rows }.map { it.task.name })
            assertEquals(1, content.archivedCount)
        }

    @Test
    fun `when every task is archived the list shows the archive and not the empty state`() =
        runTest {
            fixture.tasks.archiveTask(fixture.addTask("Pills").id)

            val content = state() as TasksListUiState.Content

            assertTrue(content.groups.isEmpty())
            assertEquals(1, content.archivedCount)
        }

    @Test
    fun `without any task the list is empty`() =
        runTest {
            assertEquals(TasksListUiState.Empty, state())
        }

    @Test
    fun `undo brings the archived task back to the rows`() =
        runTest {
            val pills = fixture.addTask("Pills")
            fixture.tasks.archiveTask(pills.id)
            state()

            viewModel.onUndoArchive(pills.id)
            runCurrent()

            val content = state() as TasksListUiState.Content
            assertEquals(listOf("Pills"), content.groups.flatMap { it.rows }.map { it.task.name })
            assertEquals(0, content.archivedCount)
        }

    @Test
    fun `the notice from the form is exposed once and forgotten when shown`() =
        runTest {
            val archived = TasksListNotice.Archived("task-1", "Pills")
            savedState[TASKS_LIST_NOTICE_KEY] = archived
            state()
            assertEquals(archived, viewModel.notice.value)

            viewModel.onNoticeShown()
            runCurrent()

            assertNull(viewModel.notice.value)
        }
}
