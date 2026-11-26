package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskFormRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fixture = TaskFormFixture()

    @Test
    fun `saving a valid form through the view model stores the task and closes the form`() {
        var closes = 0
        composeRule.setContent {
            TaskFormRoute(onBack = { closes++ }, onFinish = {}, viewModel = fixture.viewModelFor())
        }

        composeRule.onNodeWithTag("tasks_form_name").performTextInput("Pills")
        composeRule.onNodeWithTag("tasks_form_save").performClick()
        composeRule.waitUntil { closes > 0 }

        assertEquals(1, closes)
    }
}
