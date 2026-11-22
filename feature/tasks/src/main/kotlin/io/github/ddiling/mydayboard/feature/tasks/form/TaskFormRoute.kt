package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R

/** Placeholder until the form screen exists, so the route is already wired into the app. */
@Composable
internal fun TaskFormRoute(modifier: Modifier = Modifier) {
    Text(text = stringResource(R.string.tasks_form_placeholder), modifier = modifier)
}
