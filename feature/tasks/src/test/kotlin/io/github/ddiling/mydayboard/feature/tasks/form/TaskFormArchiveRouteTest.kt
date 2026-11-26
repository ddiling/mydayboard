package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.ui.test.junit4.v2.createComposeRule
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

    private fun showFormFor(viewModel: TaskFormViewModel): List<TasksListNotice> {
        val notices = mutableListOf<TasksListNotice>()
        composeRule.setContent {
            TaskFormRoute(onBack = {}, onFinish = { notices += it }, viewModel = viewModel)
        }
        return notices
    }

    @Test
    fun `archiving hands the archived notice to the caller once`() {
        val stored = runBlocking { fixture.givenTask("Pills") }
        val viewModel = fixture.viewModelFor(stored.id)
        val notices = showFormFor(viewModel)
        composeRule.waitUntil {
            viewModel.uiState.value.name
                .isNotEmpty()
        }

        viewModel.actions.onArchive()
        composeRule.waitUntil { notices.isNotEmpty() }

        assertEquals(listOf<TasksListNotice>(TasksListNotice.Archived(stored.id, "Pills")), notices)
    }

    @Test
    fun `deleting hands the deleted notice to the caller once`() {
        val stored = runBlocking { fixture.givenTask("Pills") }
        val viewModel = fixture.viewModelFor(stored.id)
        val notices = showFormFor(viewModel)
        composeRule.waitUntil {
            viewModel.uiState.value.name
                .isNotEmpty()
        }

        viewModel.actions.onDeleteRequest()
        viewModel.actions.onDeleteConfirm()
        composeRule.waitUntil { notices.isNotEmpty() }

        assertEquals(listOf<TasksListNotice>(TasksListNotice.Deleted("Pills")), notices)
    }
}
