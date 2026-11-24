package io.github.ddiling.mydayboard.feature.tasks.navigation

import androidx.navigation.NavController
import java.io.Serializable

/** Key under which a screen leaving towards the tasks list hands it a [TasksListNotice]. */
internal const val TASKS_LIST_NOTICE_KEY = "tasks_list_notice"

/**
 * What the list must tell the user after a screen closed on it. Archived carries the id so the list can offer Undo;
 * Deleted has no id because a delete cannot be undone. [Serializable] so the result survives process death.
 */
internal sealed interface TasksListNotice : Serializable {
    data class Archived(
        val id: String,
        val name: String,
    ) : TasksListNotice

    data class Deleted(
        val name: String,
    ) : TasksListNotice
}

/**
 * Closes the current screen and hands [notice] to the tasks list. The list is not always the screen below (the
 * details or the dashboard may be): screens in between are popped when the list is on the stack, so an archived
 * task's details are not left open. Without a list on the stack the screen only closes.
 */
internal fun NavController.finishWithNotice(notice: TasksListNotice) {
    if (popBackStack<TasksListDestination>(inclusive = false)) {
        currentBackStackEntry?.savedStateHandle?.set(TASKS_LIST_NOTICE_KEY, notice)
    } else {
        popBackStack()
    }
}
