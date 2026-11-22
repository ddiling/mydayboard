package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskDetailSectionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val morning = LocalTime.of(8, 0)
    private val evening = LocalTime.of(20, 0)
    private val twiceADay = previewTask(Repeat.TimesPerDay(listOf(morning, evening)))
    private val plain = previewTask()
    private val missedDay = LocalDate.of(2026, 11, 11)

    private fun show(
        state: TaskDetailUiState,
        actions: TaskDetailActions = TaskDetailActions(),
        fontScale: Float = 1f,
    ) = composeRule.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PreviewSurface { TaskDetailScreen(state, actions) }
        }
    }

    private fun pressBigButton(state: TaskDetailUiState): Int {
        var presses = 0
        show(state, TaskDetailActions(onBigButton = { presses++ }))
        composeRule.onNodeWithTag("tasks_detail_big_button").performClick()
        return presses
    }

    private fun scrollToHistoryRow() {
        composeRule
            .onNodeWithTag("tasks_detail_content")
            .performScrollToNode(hasTestTag("tasks_detail_history_$missedDay"))
    }

    @Test
    fun `Mark as done calls onBigButton`() {
        assertEquals(1, pressBigButton(previewDetail(plain)))
    }

    @Test
    fun `Tick with the next time calls onBigButton`() {
        assertEquals(1, pressBigButton(previewDetail(twiceADay)))
    }

    @Test
    fun `plus one calls onBigButton`() {
        assertEquals(1, pressBigButton(previewDetail(plain.copy(goal = 3))))
    }

    @Test
    fun `Untick today calls onBigButton`() {
        assertEquals(1, pressBigButton(previewDetail(plain, listOf(previewTick(plain, LocalDate.of(2026, 11, 20))))))
    }

    @Test
    fun `a task that is not due has no big button and shows when it is next due`() {
        val monday = previewTask(Repeat.Weekdays(setOf(DayOfWeek.MONDAY)))
        show(previewDetail(monday))

        composeRule.onNodeWithTag("tasks_detail_big_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tasks_detail_next").assertIsDisplayed()
    }

    @Test
    fun `tapping a time chip calls onChipClick with that time`() {
        val clicked = mutableListOf<LocalTime>()
        show(previewDetail(twiceADay), TaskDetailActions(onChipClick = { clicked += it }))

        composeRule.onNodeWithTag("tasks_detail_chip_20:00").performClick()

        assertEquals(listOf(evening), clicked)
    }

    @Test
    fun `a chip says whether tapping ticks or unticks it`() {
        val ticked = listOf(previewTick(twiceADay, LocalDate.of(2026, 11, 20), morning))
        show(previewDetail(twiceADay, ticked))

        composeRule.onNodeWithTag("tasks_detail_chip_08:00").assert(hasContentDescription("08:00, done. Tap to untick"))
        composeRule
            .onNodeWithTag(
                "tasks_detail_chip_20:00",
            ).assert(hasContentDescription("20:00, not done. Tap to tick"))
    }

    @Test
    fun `tapping a history row calls onHistoryRowClick with its day`() {
        val clicked = mutableListOf<LocalDate>()
        show(previewDetail(plain), TaskDetailActions(onHistoryRowClick = { clicked += it }))

        composeRule
            .onNodeWithTag(
                "tasks_detail_content",
            ).performScrollToNode(hasTestTag("tasks_detail_history_$missedDay"))
        composeRule.onNodeWithTag("tasks_detail_history_$missedDay").performClick()

        assertEquals(listOf(missedDay), clicked)
    }

    @Test
    fun `a history row is a button that reads its day and result`() {
        show(previewDetail(plain))
        scrollToHistoryRow()

        composeRule
            .onNodeWithTag("tasks_detail_history_$missedDay")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasContentDescription("Wed 11 Nov, Missed. Tap to change"))
    }

    @Test
    fun `the history title is a heading`() {
        show(previewDetail(plain))

        composeRule
            .onNodeWithTag("tasks_detail_history_title")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `an untick shows its message with Undo that calls onUndo`() {
        var undos = 0
        val outcome = TickOutcome.Removed("t", "tick", done = 0, target = 1)
        val state = previewDetail(plain).copy(pendingUndo = DetailUndo.Unticked("English", outcome))
        show(state, TaskDetailActions(onUndo = { undos++ }))

        composeRule
            .onNode(
                androidx.compose.ui.test
                    .hasText("English unticked"),
            ).assertIsDisplayed()
        composeRule
            .onNode(
                androidx.compose.ui.test
                    .hasText("Undo"),
            ).performClick()

        composeRule.waitForIdle()
        assertEquals(1, undos)
    }

    @Test
    fun `the big button is reachable at a double font size`() {
        show(previewDetail(twiceADay), fontScale = 2f)

        composeRule.onNodeWithTag("tasks_detail_big_button").assertIsDisplayed()
    }
}
