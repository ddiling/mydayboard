package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

/** Robolectric supplies the real `Bundle` that `toRoute` needs to read the route arguments. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailButtonTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fixture = TaskDetailFixture()
    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)

    private suspend fun twiceADay() = fixture.addTask("Pills", Repeat.TimesPerDay(listOf(morning, evening)))

    private fun TestScope.press(viewModel: TaskDetailViewModel) {
        viewModel.onBigButton()
        runCurrent()
    }

    @Test
    fun `the state is Loading until the first data arrives`() =
        runTest {
            val task = fixture.addTask("Pills")

            assertEquals(TaskDetailUiState.Loading, fixture.viewModel(task.id).uiState.value)
        }

    @Test
    fun `a plain task offers Mark done and then Untick today`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            val viewModel = fixture.viewModel(task.id)
            assertEquals(BigButton.MarkDone, collectContent(viewModel).bigButton)

            press(viewModel)

            assertEquals(BigButton.UntickToday, viewModel.content.bigButton)
        }

    @Test
    fun `a several-times task offers the next undone time`() =
        runTest {
            val task = twiceADay()
            fixture.tickOn(task, fixture.today, morning)

            assertEquals(BigButton.TickNext(evening), collectContent(fixture.viewModel(task.id)).bigButton)
        }

    @Test
    fun `a count goal offers plus one until the goal is reached`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3)
            val viewModel = fixture.viewModel(task.id)
            assertEquals(BigButton.PlusOne, collectContent(viewModel).bigButton)

            repeat(3) { press(viewModel) }

            assertEquals(BigButton.UntickToday, viewModel.content.bigButton)
        }

    @Test
    fun `Untick today on a reached goal takes back one count`() =
        runTest {
            val task = fixture.addTask("Send CVs", goal = 3)
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)
            repeat(3) { press(viewModel) }

            press(viewModel)

            assertEquals(Hero.CountProgress(2, 3), viewModel.content.hero)
            assertEquals(BigButton.PlusOne, viewModel.content.bigButton)
        }

    @Test
    fun `tapping an open time chip ticks that time even when it is not the next one`() =
        runTest {
            val task = twiceADay()
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)

            viewModel.onChipClick(evening)
            runCurrent()

            assertEquals(listOf(TimeChip(morning, false), TimeChip(evening, true)), viewModel.content.chips)
        }

    @Test
    fun `tapping a ticked time chip unticks only that time`() =
        runTest {
            val task = twiceADay()
            fixture.tickOn(task, fixture.today, morning)
            fixture.tickOn(task, fixture.today, evening)
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)

            viewModel.onChipClick(morning)
            runCurrent()

            assertEquals(listOf(TimeChip(morning, false), TimeChip(evening, true)), viewModel.content.chips)
        }

    @Test
    fun `a tick offers Undo with the task name and Undo removes the tick`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)
            press(viewModel)
            val offer = (viewModel.content.pendingUndo as DetailUndo.Ticked).offer
            assertEquals("Vacuum", offer.name)

            viewModel.onUndo()
            runCurrent()

            assertEquals(BigButton.MarkDone, viewModel.content.bigButton)
            assertNull(viewModel.content.pendingUndo)
        }

    @Test
    fun `an untick offers Undo with the task name and Undo restores the tick`() =
        runTest {
            val task = fixture.addTask("Pills")
            fixture.tickOn(task, fixture.today)
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)
            press(viewModel)
            val undo = viewModel.content.pendingUndo as DetailUndo.Unticked
            assertEquals("Pills", undo.name)

            viewModel.onUndo()
            runCurrent()

            assertEquals(Hero.DoneToday, viewModel.content.hero)
            assertEquals(
                1,
                fixture.ticks
                    .observeTicks(task.id)
                    .first()
                    .size,
            )
        }

    @Test
    fun `a button press that does nothing keeps the Undo offer of the earlier tick`() =
        runTest {
            val task = fixture.addTask("Vacuum", Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY)))
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)
            press(viewModel)
            val firstOffer = viewModel.content.pendingUndo
            fixture.clock.advanceBy(Duration.ofDays(1))
            fixture.ticker.tryEmit(fixture.clock.instant())
            runCurrent()
            assertEquals(BigButton.None, viewModel.content.bigButton)

            press(viewModel)

            assertEquals(firstOffer, viewModel.content.pendingUndo)
        }

    @Test
    fun `dismissing the message keeps the tick and clears the offer`() =
        runTest {
            val task = fixture.addTask("Vacuum")
            val viewModel = fixture.viewModel(task.id)
            collectContent(viewModel)
            press(viewModel)

            viewModel.onUndoDismissed()
            runCurrent()

            assertNull(viewModel.content.pendingUndo)
            assertEquals(Hero.DoneToday, viewModel.content.hero)
        }
}
