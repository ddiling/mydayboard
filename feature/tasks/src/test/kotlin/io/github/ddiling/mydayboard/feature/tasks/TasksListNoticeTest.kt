package io.github.ddiling.mydayboard.feature.tasks.navigation

import android.content.Context
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TasksListNoticeTest {
    private val archived = TasksListNotice.Archived("task-1", "Pills")

    private fun givenNavController(): NavHostController {
        val controller = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        controller.graph =
            controller.createGraph(startDestination = TasksListDestination) {
                composable<TasksListDestination> {}
                composable<TaskDetailDestination> {}
                composable<TaskFormDestination> {}
            }
        return controller
    }

    private fun NavHostController.routes(): List<String> =
        currentBackStack.value
            .filter { it.destination.route != null }
            .map {
                it.destination.route
                    .orEmpty()
                    .substringBefore('/')
                    .substringBefore('?')
                    .substringAfterLast('.')
            }

    private fun NavHostController.noticeOnList(): TasksListNotice? =
        currentBackStackEntry?.savedStateHandle?.get<TasksListNotice>(TASKS_LIST_NOTICE_KEY)

    @Test
    fun `finishing from the form opened over the list hands the notice to the list`() {
        val controller = givenNavController()
        controller.navigate(TaskFormDestination("task-1"))

        controller.finishWithNotice(archived)

        assertEquals(listOf("TasksListDestination"), controller.routes())
        assertEquals(archived, controller.noticeOnList())
    }

    @Test
    fun `finishing drops the details of the task in between`() {
        val controller = givenNavController()
        controller.navigate(TaskDetailDestination("task-1"))
        controller.navigate(TaskFormDestination("task-1"))

        controller.finishWithNotice(TasksListNotice.Deleted("Pills"))

        assertEquals(listOf("TasksListDestination"), controller.routes())
        assertEquals(TasksListNotice.Deleted("Pills"), controller.noticeOnList())
    }

    @Test
    fun `finishing without a list on the stack only closes the form`() {
        val controller = givenNavController()
        controller.graph =
            controller.createGraph(startDestination = TaskDetailDestination("task-1")) {
                composable<TasksListDestination> {}
                composable<TaskDetailDestination> {}
                composable<TaskFormDestination> {}
            }
        controller.navigate(TaskFormDestination("task-1"))

        controller.finishWithNotice(archived)

        assertEquals(listOf("TaskDetailDestination"), controller.routes())
        assertNull(controller.noticeOnList())
    }
}
