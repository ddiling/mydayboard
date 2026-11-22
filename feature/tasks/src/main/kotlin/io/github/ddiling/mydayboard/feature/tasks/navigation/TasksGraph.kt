package io.github.ddiling.mydayboard.feature.tasks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ddiling.mydayboard.feature.tasks.form.TaskFormRoute
import io.github.ddiling.mydayboard.feature.tasks.list.TasksListRoute

/**
 * Adds the task screens to a graph. The app calls this instead of knowing the screens' internals.
 * The form and the list exist so far; the other task routes are added with their screens. Until the details
 * screen exists, tapping a task opens it in the form for editing.
 */
fun NavGraphBuilder.tasksGraph(navController: NavController) {
    composable<TaskFormDestination> {
        TaskFormRoute(onBack = { navController.popBackStack() })
    }
    composable<TasksListDestination> {
        TasksListRoute(
            onBack = { navController.popBackStack() },
            onAddTask = { navController.navigate(TaskFormDestination()) },
            onTaskClick = { taskId -> navController.navigate(TaskFormDestination(taskId)) },
        )
    }
}
