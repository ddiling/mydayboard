package io.github.ddiling.mydayboard.feature.trackers.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.ddiling.mydayboard.core.navigation.Destination
import io.github.ddiling.mydayboard.feature.trackers.detail.TrackerDetailRoute
import io.github.ddiling.mydayboard.feature.trackers.editor.EditorResult
import io.github.ddiling.mydayboard.feature.trackers.editor.TrackerEditorRoute
import io.github.ddiling.mydayboard.feature.trackers.list.TrackersListRoute
import io.github.ddiling.mydayboard.feature.trackers.reasons.TrackerReasonsRoute
import kotlinx.serialization.Serializable

/** Route of the tracker list. It has no arguments, hence an object. */
@Serializable
data object TrackersListDestination : Destination

/** Route of one tracker's detail screen. */
@Serializable
data class TrackerDetailDestination(
    val trackerId: String,
) : Destination

/** Route of the tracker editor: [trackerId] null creates a tracker, otherwise edits that one. */
@Serializable
data class TrackerEditorDestination(
    val trackerId: String? = null,
) : Destination

/** Route of the reasons of one tracker, the lines shown before a log to remind the user why they track it. */
@Serializable
data class TrackerReasonsDestination(
    val trackerId: String,
) : Destination

/**
 * Adds the tracker screens to a graph. The app calls this instead of knowing the screens' internals.
 * Leaving the editor: a created tracker replaces the editor with its detail screen (so Back does not return to
 * an empty form), a rename goes back, and a delete goes back to the list because the detail screen under the
 * editor would show a tracker that no longer exists.
 */
fun NavGraphBuilder.trackersGraph(navController: NavController) {
    composable<TrackersListDestination> {
        TrackersListRoute(
            onTrackerClick = { id -> navController.navigate(TrackerDetailDestination(id)) },
            onAddClick = { navController.navigate(TrackerEditorDestination()) },
        )
    }
    composable<TrackerDetailDestination> { entry ->
        val id = entry.toRoute<TrackerDetailDestination>().trackerId
        TrackerDetailRoute(
            onEditClick = { navController.navigate(TrackerEditorDestination(id)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<TrackerReasonsDestination> {
        TrackerReasonsRoute(onBack = { navController.popBackStack() })
    }
    composable<TrackerEditorDestination> { entry ->
        val editedId = entry.toRoute<TrackerEditorDestination>().trackerId
        TrackerEditorRoute(
            onOpenReasons = { if (editedId != null) navController.navigate(TrackerReasonsDestination(editedId)) },
            onResult = { result, isEditing -> navController.leaveEditor(result, isEditing) },
            onBack = { navController.popBackStack() },
        )
    }
}

internal fun NavController.leaveEditor(
    result: EditorResult,
    isEditing: Boolean,
) {
    when {
        result == EditorResult.Deleted -> {
            popBackStack(TrackersListDestination, inclusive = false)
        }

        isEditing -> {
            popBackStack()
        }

        result is EditorResult.Saved -> {
            navigate(TrackerDetailDestination(result.trackerId)) {
                popUpTo<TrackerEditorDestination> { inclusive = true }
            }
        }
    }
}
