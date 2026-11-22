package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.MAX_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.MIN_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskFormIntervalFieldsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 3, 2)
    private val everyThreeDays = TaskFormUiState(today = today, repeatType = RepeatType.EVERY_N_DAYS)
    private val monthly = TaskFormUiState(today = today, repeatType = RepeatType.MONTHLY)
    private val oneTime = TaskFormUiState(today = today, repeatType = RepeatType.ONE_TIME, dueDate = today)

    private fun show(
        state: TaskFormUiState,
        actions: TaskFormActions = TaskFormActions(),
    ) = composeRule.setContent { TaskFormScreen(state, emptyList(), actions) }

    @Test
    fun `every N days shows the stepper and both modes with their help and marks the chosen one`() {
        show(everyThreeDays)

        composeRule.onNodeWithTag("tasks_form_interval").assertTextEquals("3")
        composeRule
            .onNodeWithText(
                "Done on Wed, next on Sat. Done late on Thu, next on Sun.",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Always on the same dates, even if you tick it late.",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_form_mode_from_last").assertIsSelected()
        composeRule.onNodeWithTag("tasks_form_mode_fixed").assertIsNotSelected()
    }

    @Test
    fun `choosing a mode reports it`() {
        val chosen = mutableListOf<EveryNDaysMode>()
        show(everyThreeDays, TaskFormActions(onIntervalModeChange = { chosen += it }))

        composeRule.onNodeWithTag("tasks_form_mode_fixed").performScrollTo().performClick()

        assertEquals(listOf(EveryNDaysMode.FIXED_CALENDAR), chosen)
    }

    @Test
    fun `the stepper buttons report one more and one fewer`() {
        val intervals = mutableListOf<Int>()
        show(everyThreeDays, TaskFormActions(onIntervalChange = { intervals += it }))

        composeRule.onNodeWithTag("tasks_form_interval_more").performScrollTo().performClick()
        composeRule.onNodeWithTag("tasks_form_interval_less").performScrollTo().performClick()

        assertEquals(listOf(4, 2), intervals)
    }

    @Test
    fun `the minus button is off at the lowest interval`() {
        show(everyThreeDays.copy(intervalDays = MIN_INTERVAL_DAYS))

        composeRule.onNodeWithTag("tasks_form_interval_less").assertIsNotEnabled()
        composeRule.onNodeWithTag("tasks_form_interval_more").assertIsEnabled()
    }

    @Test
    fun `the plus button is off at the highest interval`() {
        show(everyThreeDays.copy(intervalDays = MAX_INTERVAL_DAYS))

        composeRule.onNodeWithTag("tasks_form_interval_more").assertIsNotEnabled()
        composeRule.onNodeWithTag("tasks_form_interval_less").assertIsEnabled()
    }

    @Test
    fun `the starting on field shows only in the fixed calendar mode`() {
        show(everyThreeDays)

        composeRule.onNodeWithTag("tasks_form_first_due").assertDoesNotExist()
    }

    @Test
    fun `the fixed calendar mode shows the starting on field`() {
        show(everyThreeDays.copy(intervalMode = EveryNDaysMode.FIXED_CALENDAR))

        composeRule.onNodeWithTag("tasks_form_first_due").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `every N days has no time field`() {
        show(everyThreeDays)

        composeRule.onNodeWithTag("tasks_form_time").assertDoesNotExist()
    }

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
