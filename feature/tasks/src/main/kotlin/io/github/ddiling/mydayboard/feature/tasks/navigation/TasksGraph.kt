package io.github.ddiling.mydayboard.feature.tasks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryStatsRoute
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailRoute
import io.github.ddiling.mydayboard.feature.tasks.form.TaskFormRoute
import io.github.ddiling.mydayboard.feature.tasks.list.TasksListRoute

/**
 * Adds the task screens to a graph. The app calls this instead of knowing the screens' internals.
 * The form, the list, the details and the category stats exist so far; the other task routes are added with their
 * screens. Tapping a task opens its details, and the pencil there opens the form for that task.
 */
fun NavGraphBuilder.tasksGraph(navController: NavController) {
    composable<TaskFormDestination> {
        TaskFormRoute(onBack = { navController.popBackStack() })
    }
    composable<TasksListDestination> {
        TasksListRoute(
            onBack = { navController.popBackStack() },
            onAddTask = { navController.navigate(TaskFormDestination()) },
            onTaskClick = { taskId -> navController.navigate(TaskDetailDestination(taskId)) },
        )
    }
    composable<CategoryStatsDestination> {
        CategoryStatsRoute(
            onBack = { navController.popBackStack() },
            onTaskClick = { taskId -> navController.navigate(TaskDetailDestination(taskId)) },
        )
    }
    composable<TaskDetailDestination> { entry ->
        val taskId = entry.toRoute<TaskDetailDestination>().taskId
        TaskDetailRoute(
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(TaskFormDestination(taskId)) },
        )
    }
}
