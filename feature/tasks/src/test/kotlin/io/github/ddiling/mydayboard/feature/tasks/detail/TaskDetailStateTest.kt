package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailStateTest {
    private val fixture = TaskDetailFixture(warsaw(2026, 11, 18, 23, 59))

    @Test
    fun `an unknown id is not found`() =
        runTest {
            assertEquals(TaskDetailUiState.NotFound, fixture.observe("missing").first())
        }

    @Test
    fun `an archived task is not found`() =
        runTest {
            val task = fixture.addTask("Pills")
            fixture.tasks.archiveTask(task.id)

            assertEquals(TaskDetailUiState.NotFound, fixture.observe(task.id).first())
        }

    @Test
    fun `the category name comes from the task's category`() =
        runTest {
            val home = fixture.categories.createCategory("Home")
            val draft = TaskDraft("Vacuum", home.id, Repeat.EveryDay, null, null, null, OnMiss.EXPIRES)
            val task = fixture.tasks.createTask(draft, fixture.today)

            assertEquals("Home", (fixture.observe(task.id).first() as TaskDetailUiState.Content).categoryName)
        }

    @Test
    fun `at midnight a done task goes back to not done yet`() =
        runTest {
            val task = fixture.addTask("Pills")
            fixture.tickOn(task, fixture.today)
            val states = fixture.run { collectStates(task.id) }
            assertEquals(Hero.DoneToday, (states.last() as TaskDetailUiState.Content).hero)

            fixture.clock.advanceBy(Duration.ofMinutes(2))
            fixture.ticker.tryEmit(fixture.clock.instant())
            runCurrent()

            val content = states.last() as TaskDetailUiState.Content
            assertEquals(Hero.NotDone, content.hero)
            assertEquals(BigButton.MarkDone, content.bigButton)
        }
}
