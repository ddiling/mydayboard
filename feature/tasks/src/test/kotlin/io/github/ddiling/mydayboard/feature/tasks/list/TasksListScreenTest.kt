package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Density
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskGroup
import io.github.ddiling.mydayboard.feature.tasks.common.rowOf
import io.github.ddiling.mydayboard.feature.tasks.common.taskOf
import io.github.ddiling.mydayboard.feature.tasks.common.textsToday
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class TasksListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val home = TaskCategory("home", "Home", Instant.EPOCH, Instant.EPOCH)
    private val vacuum = taskOf(Repeat.EveryDay).copy(id = "vacuum", name = "Vacuum")
    private val bills = taskOf(Repeat.EveryDay).copy(id = "bills", name = "Pay bills")
    private val pending = rowOf(vacuum, RowState.PENDING).copy(category = home)

    private fun content(
        groups: List<TaskGroup> = listOf(TaskGroup(home, listOf(pending))),
        lateCount: Int = 0,
    ) = TasksListUiState.Content(groups, textsToday, lateCount, archivedCount = 0, undo = null)

    private fun show(
        state: TasksListUiState,
        actions: TasksListActions = TasksListActions(),
    ) = composeRule.setContent { PreviewSurface { TasksListScreen(state, actions) } }

    @Test
    fun `the top bar shows how many tasks are late`() {
        show(content(lateCount = 1))

        composeRule.onNodeWithTag("tasks_list_late").assertIsDisplayed()
        composeRule.onNode(hasText("1 late")).assertIsDisplayed()
    }

    @Test
    fun `the top bar shows no late count when nothing is late`() {
        show(content(lateCount = 0))

        composeRule.onNodeWithTag("tasks_list_late").assertDoesNotExist()
    }

    @Test
    fun `the plus button calls onAddTask`() {
        var adds = 0
        show(content(), TasksListActions(onAddTask = { adds++ }))

        composeRule.onNodeWithTag("tasks_list_add").performClick()

        assertEquals(1, adds)
    }

    @Test
    fun `the back button calls onBack`() {
        var backs = 0
        show(content(), TasksListActions(onBack = { backs++ }))

        composeRule.onNodeWithTag("tasks_list_back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `tapping a row calls onTaskClick with its id`() {
        val clicked = mutableListOf<String>()
        show(content(), TasksListActions(onTaskClick = { clicked += it }))

        composeRule.onNodeWithTag("tasks_list_row_vacuum").performClick()

        assertEquals(listOf("vacuum"), clicked)
    }

    @Test
    fun `swiping a row right calls onSwipeDone with its id`() {
        val swiped = mutableListOf<String>()
        show(content(), TasksListActions(onSwipeDone = { swiped += it }))

        composeRule.onNodeWithTag("tasks_list_row_vacuum").performTouchInput { swipeRight() }

        composeRule.waitForIdle()
        assertEquals(listOf("vacuum"), swiped)
    }

    @Test
    fun `a task that is not due cannot be swiped`() {
        val swiped = mutableListOf<String>()
        val upcoming = rowOf(vacuum, RowState.UPCOMING, nextDue = textsToday.plusDays(2))
        show(content(listOf(TaskGroup(null, listOf(upcoming)))), TasksListActions(onSwipeDone = { swiped += it }))

        composeRule.onNodeWithTag("tasks_list_row_vacuum").performTouchInput { swipeRight() }

        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), swiped)
    }

    @Test
    fun `the empty state shows the hint and an Add a task button that calls onAddTask`() {
        var adds = 0
        show(TasksListUiState.Empty, TasksListActions(onAddTask = { adds++ }))

        composeRule.onNodeWithTag("tasks_list_empty_hint").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_list_empty_add").performClick()
        assertEquals(1, adds)
    }

    @Test
    fun `loading shows a spinner`() {
        show(TasksListUiState.Loading)

        composeRule.onNodeWithTag("tasks_list_loading").assertIsDisplayed()
    }

    @Test
    fun `group titles are headings`() {
        show(content())

        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNode(heading and hasText("HOME")).assertIsDisplayed()
    }

    @Test
    fun `a group without a category is titled No category`() {
        show(content(listOf(TaskGroup(null, listOf(rowOf(bills, RowState.PENDING))))))

        composeRule.onNode(hasText("NO CATEGORY")).assertIsDisplayed()
    }

    @Test
    fun `with a doubled font size the last group can still be reached`() {
        val groups = listOf(TaskGroup(home, listOf(pending)), TaskGroup(null, listOf(rowOf(bills, RowState.PENDING))))
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2f)) {
                PreviewSurface { TasksListScreen(content(groups), TasksListActions()) }
            }
        }

        composeRule.onNodeWithTag("tasks_list_content").performScrollToNode(hasText("NO CATEGORY"))

        composeRule.onNode(hasText("NO CATEGORY")).assertIsDisplayed()
    }
}
