package io.github.ddiling.mydayboard.feature.tasks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ddiling.mydayboard.feature.tasks.form.TaskFormRoute

/**
 * Adds the task screens to a graph. The app calls this instead of knowing the screens' internals.
 * Only the form exists so far; the other task routes are added with their screens.
 */
fun NavGraphBuilder.tasksGraph() {
    composable<TaskFormDestination> {
        TaskFormRoute()
    }
}
