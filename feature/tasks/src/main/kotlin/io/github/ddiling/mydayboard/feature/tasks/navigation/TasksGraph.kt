package io.github.ddiling.mydayboard.feature.tasks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.ddiling.mydayboard.feature.tasks.detail.TaskDetailRoute
import io.github.ddiling.mydayboard.feature.tasks.form.TaskFormRoute
import io.github.ddiling.mydayboard.feature.tasks.list.TasksListRoute

/** Adds the task screens to a graph. DRAFT: simple wiring for trying the feature. */
fun NavGraphBuilder.tasksGraph(navController: NavController) {
    composable<TaskFormDestination> {
        TaskFormRoute(onBack = { navController.popBackStack() })
    }
    composable<TasksListDestination> {
        TasksListRoute(
            onTaskClick = { navController.navigate(TaskDetailDestination(it)) },
            onAddClick = { navController.navigate(TaskFormDestination()) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<TaskDetailDestination> { entry ->
        val id = entry.toRoute<TaskDetailDestination>().taskId
        TaskDetailRoute(
            onEdit = { navController.navigate(TaskFormDestination(id)) },
            onBack = { navController.popBackStack() },
        )
    }
}
