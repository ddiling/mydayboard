package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskFormArchiveRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fixture = TaskFormFixture()

    private fun showFormFor(taskId: String): List<TasksListNotice> {
        val notices = mutableListOf<TasksListNotice>()
        composeRule.setContent {
            TaskFormRoute(onBack = {}, onFinish = { notices += it }, viewModel = fixture.viewModelFor(taskId))
        }
        return notices
    }

    @Test
    fun `archiving from the form hands the archived notice to the caller once`() {
        val stored = runBlocking { fixture.givenTask("Pills") }
        val notices = showFormFor(stored.id)

        composeRule.waitUntil { composeRule.onAllNodesWithTag("tasks_form_archive").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("tasks_form_archive").performScrollTo().performClick()
        composeRule.waitUntil { notices.isNotEmpty() }

        assertEquals(listOf<TasksListNotice>(TasksListNotice.Archived(stored.id, "Pills")), notices)
    }

    @Test
    fun `deleting from the form asks first and then hands the deleted notice to the caller`() {
        val stored = runBlocking { fixture.givenTask("Pills") }
        val notices = showFormFor(stored.id)

        composeRule.waitUntil { composeRule.onAllNodesWithTag("tasks_form_delete").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("tasks_form_delete").performScrollTo().performClick()
        composeRule.onNodeWithTag("tasks_delete_confirm").performClick()
        composeRule.waitUntil { notices.isNotEmpty() }

        assertEquals(listOf<TasksListNotice>(TasksListNotice.Deleted("Pills")), notices)
    }
}
