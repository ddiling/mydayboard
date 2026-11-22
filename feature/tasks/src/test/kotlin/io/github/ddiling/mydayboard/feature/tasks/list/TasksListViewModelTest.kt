package io.github.ddiling.mydayboard.feature.tasks.list

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TasksListViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fixture = TasksListFixture()
    private val viewModel by lazy { fixture.viewModel() }

    /** The state is only live while someone collects it, as on screen. */
    private fun TestScope.content(): TasksListUiState.Content {
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel.uiState.value as TasksListUiState.Content
    }

    private fun TasksListUiState.Content.rowStates() = groups.flatMap { it.rows }.associate { it.task.name to it.state }

    @Test
    fun `groups come in alphabetical order with No category last`() =
        runTest {
            val work = fixture.categories.createCategory("Work")
            val home = fixture.categories.createCategory("Home")
            fixture.addTask("Pills")
            fixture.addTask("Send CVs", categoryId = work.id)
            fixture.addTask("Vacuum", categoryId = home.id)

            val names = content().groups.map { it.category?.name }

            assertEquals(listOf("Home", "Work", null), names)
        }

    @Test
    fun `a late bill stays in its category and is counted as late`() =
        runTest {
            val home = fixture.categories.createCategory("Home")
            val yesterday = fixture.today.minusDays(1)
            val bill = fixture.addTask("Electricity bill", Repeat.OneTime(yesterday), home.id, onMiss = OnMiss.STAYS)

            val state = content()

            assertEquals(
                listOf(bill.id),
                state.groups
                    .single { it.category == home }
                    .rows
                    .map { it.task.id },
            )
            assertEquals(RowState.LATE, state.rowStates().getValue("Electricity bill"))
            assertEquals(1, state.lateCount)
        }

    @Test
    fun `a swipe ticks the task and offers Undo with its name and progress`() =
        runTest {
            val cvs = fixture.addTask("Send CVs", goal = 3)
            content()

            viewModel.onSwipeDone(cvs.id)
            runCurrent()

            val undo = (viewModel.uiState.value as TasksListUiState.Content).undo
            assertEquals("Send CVs", undo?.name)
            assertEquals(1 to 3, undo?.outcome?.let { it.done to it.target })
            assertFalse(undo?.completes == true)
        }

    @Test
    fun `a swipe that finishes the task offers Undo as done`() =
        runTest {
            val breakfast = fixture.addTask("Breakfast")
            content()

            viewModel.onSwipeDone(breakfast.id)
            runCurrent()

            val undo = (viewModel.uiState.value as TasksListUiState.Content).undo
            assertTrue(undo?.completes == true)
        }

    @Test
    fun `Undo restores the previous state and clears the message`() =
        runTest {
            val breakfast = fixture.addTask("Breakfast")
            content()
            viewModel.onSwipeDone(breakfast.id)
            runCurrent()

            viewModel.onUndo()
            runCurrent()

            val state = viewModel.uiState.value as TasksListUiState.Content
            assertEquals(RowState.PENDING, state.rowStates().getValue("Breakfast"))
            assertNull(state.undo)
        }

    @Test
    fun `a newer swipe replaces the older Undo`() =
        runTest {
            val breakfast = fixture.addTask("Breakfast")
            val pills = fixture.addTask("Pills")
            content()
            viewModel.onSwipeDone(breakfast.id)
            runCurrent()

            viewModel.onSwipeDone(pills.id)
            runCurrent()

            val state = viewModel.uiState.value as TasksListUiState.Content
            assertEquals("Pills", state.undo?.name)
        }

    @Test
    fun `dismissing the message keeps the tick and clears the offer`() =
        runTest {
            val breakfast = fixture.addTask("Breakfast")
            content()
            viewModel.onSwipeDone(breakfast.id)
            runCurrent()

            viewModel.onUndoDismissed()
            runCurrent()

            val state = viewModel.uiState.value as TasksListUiState.Content
            assertNull(state.undo)
            assertEquals(RowState.DONE, state.rowStates().getValue("Breakfast"))
        }

    @Test
    fun `empty repositories give the Empty state`() =
        runTest {
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(TasksListUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun `a done task stays in the list as done`() =
        runTest {
            val breakfast = fixture.addTask("Breakfast")
            content()

            viewModel.onSwipeDone(breakfast.id)
            runCurrent()

            val state = viewModel.uiState.value as TasksListUiState.Content
            assertEquals(mapOf("Breakfast" to RowState.DONE), state.rowStates())
        }

    @Test
    fun `at midnight a done task becomes pending again`() =
        runTest {
            fixture.clock.now = warsaw(2026, 11, 18, 23, 59)
            val breakfast = fixture.addTask("Breakfast")
            content()
            viewModel.onSwipeDone(breakfast.id)
            runCurrent()

            passOneMinute()

            val state = viewModel.uiState.value as TasksListUiState.Content
            assertEquals(LocalDate.of(2026, 11, 19), state.today)
            assertEquals(RowState.PENDING, state.rowStates().getValue("Breakfast"))
        }

    @Test
    fun `at midnight a missed stays task becomes late`() =
        runTest {
            fixture.clock.now = warsaw(2026, 11, 18, 23, 59)
            fixture.addTask("Electricity bill", Repeat.OneTime(fixture.today), onMiss = OnMiss.STAYS)
            val before = content()

            passOneMinute()

            val after = viewModel.uiState.value as TasksListUiState.Content
            assertEquals(0, before.lateCount)
            assertEquals(1, after.lateCount)
        }

    private fun TestScope.passOneMinute() {
        fixture.clock.advanceBy(Duration.ofMinutes(1))
        testScheduler.advanceTimeBy(Duration.ofMinutes(1).toMillis())
        runCurrent()
    }
}
