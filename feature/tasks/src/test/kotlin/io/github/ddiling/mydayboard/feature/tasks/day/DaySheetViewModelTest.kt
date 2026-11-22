package io.github.ddiling.mydayboard.feature.tasks.day

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.feature.tasks.detail.DetailStats
import io.github.ddiling.mydayboard.feature.tasks.detail.DetailUndo
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailFixture
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailUiState
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailViewModel
import io.github.ddiling.mydayboard.feature.tasks.detail.collectContent
import io.github.ddiling.mydayboard.feature.tasks.detail.content
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

/** Robolectric supplies the real `Bundle` that `toRoute` needs to read the route arguments. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DaySheetViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fixture = TaskDetailFixture()
    private val started = fixture.today.minusDays(10)
    private val yesterday = fixture.today.minusDays(1)

    private fun streakOf(content: TaskDetailUiState.Content) = (content.stats as DetailStats.Recurring).streak

    private suspend fun slotsOn(taskId: String) =
        fixture.ticks
            .observeTicks(taskId)
            .first()
            .map { it.slot }

    private fun TestScope.saveYesterdayAsDone(viewModel: TaskDetailViewModel) {
        viewModel.onDayClick(yesterday)
        runCurrent()
        viewModel.daySheet.setSingle(done = true)
        viewModel.onDaySave()
        runCurrent()
    }

    @Test
    fun `after Save the streak grows through the same view model`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)
            fixture.tickOn(task, fixture.today)
            val viewModel = fixture.viewModel(task.id)
            val before = streakOf(collectContent(viewModel))

            saveYesterdayAsDone(viewModel)

            assertEquals(before + 1, streakOf(viewModel.content))
        }

    @Test
    fun `Undo restores the previous state of the day`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)
            fixture.tickOn(task, yesterday)
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)
            viewModel.onDayClick(yesterday)
            runCurrent()
            viewModel.daySheet.setSingle(done = false)
            viewModel.onDaySave()
            runCurrent()
            assertTrue(slotsOn(task.id).isEmpty())

            viewModel.onUndo()
            runCurrent()

            assertEquals(listOf<LocalTime?>(null), slotsOn(task.id))
            assertNull(viewModel.content.pendingUndo)
        }

    @Test
    fun `Save offers a message with Undo until it is used`() =
        runTest {
            val task = fixture.addTask("Vacuum", startDate = started)
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)

            saveYesterdayAsDone(viewModel)

            assertEquals(yesterday, (viewModel.content.pendingUndo as DetailUndo.Marked).marked.state.day)
        }

    @Test
    fun `a history row for today does not open the sheet`() =
        runTest {
            val task = fixture.addTask("Vacuum", Repeat.EveryDay, startDate = started)
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)

            viewModel.onDayClick(fixture.today)
            runCurrent()

            assertNull(viewModel.daySheet.state.value)
        }
}
