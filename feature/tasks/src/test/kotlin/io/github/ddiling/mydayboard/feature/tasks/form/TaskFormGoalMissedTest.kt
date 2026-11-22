package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.github.ddiling.mydayboard.core.domain.task.MAX_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.MIN_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskFormGoalMissedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val weekdays = TaskFormUiState(today = LocalDate.of(2026, 3, 2), repeatType = RepeatType.WEEKDAYS)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, emptyList(), actions) }

    @Test
    fun `the goal switch off hides the goal field`() {
        show(weekdays)

        composeRule.onNodeWithTag("tasks_form_goal_switch").performScrollTo().assertIsOff()
        composeRule.onNodeWithTag("tasks_form_goal").assertDoesNotExist()
    }

    @Test
    fun `the goal switch on shows the goal field`() {
        show(weekdays.copy(goalOn = true, goal = 4))

        composeRule.onNodeWithTag("tasks_form_goal_switch").performScrollTo().assertIsOn()
        composeRule.onNodeWithTag("tasks_form_goal").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `tapping the goal switch reports the new state`() {
        val changes = mutableListOf<Boolean>()
        show(weekdays, TaskFormActions(onGoalOnChange = { changes += it }))

        composeRule.onNodeWithTag("tasks_form_goal_switch").performScrollTo().performClick()

        assertEquals(listOf(true), changes)
    }

    @Test
    fun `typing a goal reports the number`() {
        val goals = mutableListOf<Int>()
        show(weekdays.copy(goalOn = true), TaskFormActions(onGoalChange = { goals += it }))

        composeRule.onNodeWithTag("tasks_form_goal").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("tasks_form_goal").performTextInput("12")

        assertEquals(listOf(12), goals)
    }

    @Test
    fun `a goal out of range shows its error`() {
        show(
            weekdays.copy(
                goalOn = true,
                goal = 1,
                errors = listOf(TaskFormError.GoalOutOfRange(MIN_TASK_GOAL, MAX_TASK_GOAL)),
            ),
        )

        composeRule.onNodeWithText("Enter a number from 2 to 99").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `several times a day has no goal section`() {
        show(weekdays.copy(repeatType = RepeatType.TIMES_PER_DAY))

        composeRule.onNodeWithTag("tasks_form_goal_switch").assertDoesNotExist()
    }

    @Test
    fun `the missed radios show their help and mark the chosen one`() {
        show(weekdays.copy(onMiss = OnMiss.STAYS))

        composeRule.onNodeWithText("Shows as late. Good for a bill.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Counts as missed. Good for vacuuming.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_form_missed_stays").assertIsSelected()
        composeRule.onNodeWithTag("tasks_form_missed_expires").assertIsNotSelected()
    }

    @Test
    fun `choosing a missed radio reports it`() {
        val chosen = mutableListOf<OnMiss>()
        show(weekdays, TaskFormActions(onOnMissChange = { chosen += it }))

        composeRule.onNodeWithTag("tasks_form_missed_stays").performScrollTo().performClick()

        assertEquals(listOf(OnMiss.STAYS), chosen)
    }

    @Test
    fun `every day has no missed section`() {
        show(weekdays.copy(repeatType = RepeatType.EVERY_DAY))

        composeRule.onNodeWithTag("tasks_form_missed_stays").assertDoesNotExist()
    }

    @Test
    fun `several times a day has no missed section`() {
        show(weekdays.copy(repeatType = RepeatType.TIMES_PER_DAY))

        composeRule.onNodeWithTag("tasks_form_missed_stays").assertDoesNotExist()
    }
}
