package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskFormMonthlyOneTimeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 3, 2)
    private val monthly = TaskFormUiState(today = today, repeatType = RepeatType.MONTHLY)
    private val dueLater = LocalDate.of(2026, 4, 20)
    private val oneTime = TaskFormUiState(today = today, repeatType = RepeatType.ONE_TIME, dueDate = today)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, emptyList(), actions) }

    /** Runs the real reducer, so what is typed goes through the same path as in the app. */
    private fun showEditable(initial: TaskFormUiState) =
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            TaskFormScreen(
                state,
                emptyList(),
                TaskFormActions(onDayOfMonthChange = { state = state.withDayOfMonth(it) }),
            )
        }

    private fun dayField() = composeRule.onNodeWithTag("tasks_form_day_of_month").performScrollTo()

    @Test
    fun `once a month shows its help and reports a typed day`() {
        val days = mutableListOf<Int>()
        show(monthly.copy(dayOfMonth = 5), TaskFormActions(onDayOfMonthChange = { days += it }))

        composeRule.onNodeWithText("If a month is shorter, the last day is used.").performScrollTo().assertIsDisplayed()
        dayField().performTextReplacement("17")

        assertEquals(listOf(17), days)
    }

    @Test
    fun `typing a day above 31 shows the range error`() {
        showEditable(monthly.copy(dayOfMonth = 5))

        dayField().performTextReplacement("32")

        composeRule.onNodeWithText("Enter a day from 1 to 31").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `typing day 0 shows the range error`() {
        showEditable(monthly.copy(dayOfMonth = 5))

        dayField().performTextReplacement("0")

        composeRule.onNodeWithText("Enter a day from 1 to 31").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `clearing the day shows the range error and leaves the box empty`() {
        showEditable(monthly.copy(dayOfMonth = 5))

        dayField().performTextClearance()

        composeRule.onNodeWithText("Enter a day from 1 to 31").performScrollTo().assertIsDisplayed()
        dayField().assertTextEquals("")
    }

    @Test
    fun `a valid day shows no range error`() {
        showEditable(monthly.copy(dayOfMonth = 5))

        dayField().performTextReplacement("31")

        composeRule.onNodeWithText("Enter a day from 1 to 31").assertDoesNotExist()
    }

    @Test
    fun `the day field is at least 48 dp tall`() {
        show(monthly)

        dayField().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `one time shows its due date and no error once it is set`() {
        show(oneTime)

        composeRule.onNodeWithTag("tasks_form_due_date").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Choose a due date").assertDoesNotExist()
    }

    @Test
    fun `one time without a due date shows that it is required`() {
        show(oneTime.copy(dueDate = null))

        composeRule.onNodeWithText("Choose a due date").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the date picker opens on the due date in the state, not on today`() {
        show(oneTime.copy(dueDate = dueLater))

        composeRule.onNodeWithTag("tasks_form_due_date").performScrollTo().performClick()

        // One copy is the field behind the dialog, the other is the picker headline.
        composeRule.onAllNodesWithText("Apr 20, 2026").assertCountEquals(2)
    }

    @Test
    fun `confirming the date picker reports the day that was picked`() {
        val picked = mutableListOf<LocalDate>()
        show(oneTime.copy(dueDate = dueLater), TaskFormActions(onDueDateChange = { picked += it }))

        composeRule.onNodeWithTag("tasks_form_due_date").performScrollTo().performClick()
        composeRule.onNodeWithText("April 25, 2026", substring = true).performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf(dueLater.withDayOfMonth(25)), picked)
    }

    @Test
    fun `cancelling the date picker reports nothing`() {
        val picked = mutableListOf<LocalDate>()
        show(oneTime, TaskFormActions(onDueDateChange = { picked += it }))

        composeRule.onNodeWithTag("tasks_form_due_date").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<LocalDate>(), picked)
    }
}
