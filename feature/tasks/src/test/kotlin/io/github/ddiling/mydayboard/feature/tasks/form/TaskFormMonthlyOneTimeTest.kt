package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
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
    private val oneTime = TaskFormUiState(today = today, repeatType = RepeatType.ONE_TIME, dueDate = today)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, emptyList(), actions) }

    @Test
    fun `once a month shows the day with its help and reports a typed day`() {
        val days = mutableListOf<Int>()
        show(monthly.copy(dayOfMonth = 5), TaskFormActions(onDayOfMonthChange = { days += it }))

        composeRule.onNodeWithText("If a month is shorter, the last day is used.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_form_day_of_month").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("tasks_form_day_of_month").performScrollTo().performTextInput("17")

        assertEquals(listOf(17), days)
    }

    @Test
    fun `a day of month out of range shows its error`() {
        show(monthly.copy(dayOfMonth = 32, errors = listOf(TaskFormError.DayOfMonthOutOfRange)))

        composeRule.onNodeWithText("Enter a day from 1 to 31").performScrollTo().assertIsDisplayed()
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
    fun `confirming the date picker reports the date it opened on`() {
        val picked = mutableListOf<LocalDate>()
        show(oneTime, TaskFormActions(onDueDateChange = { picked += it }))

        composeRule.onNodeWithTag("tasks_form_due_date").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf(today), picked)
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
