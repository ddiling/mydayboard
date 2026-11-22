package io.github.ddiling.mydayboard.feature.tasks.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.domain.task.dashboardRows
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.navigation.TaskDetailDestination
import io.github.ddiling.mydayboard.feature.tasks.navigation.TaskFormDestination
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListDestination
import io.github.ddiling.mydayboard.feature.tasks.ui.TaskRowView
import io.github.ddiling.mydayboard.feature.tasks.ui.TasksOverviewViewModel
import io.github.ddiling.mydayboard.feature.tasks.ui.rememberSwipeDone
import javax.inject.Inject

internal class TasksDashboardCard
    @Inject
    constructor() : DashboardCard {
        override val key: String = "tasks"
        override val order: Int = 50

        @Composable
        override fun Content(
            host: DashboardHost,
            modifier: Modifier,
        ) {
            val viewModel: TasksOverviewViewModel = hiltViewModel()
            val overview by viewModel.overview.collectAsStateWithLifecycle()
            val swipeDone = rememberSwipeDone(viewModel, host.snackbarHostState)
            val current = overview ?: return
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Column(modifier) {
                Row(Modifier.fillMaxWidth().padding20(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stringResource(R.string.tasks_title)} ${current.doneToday}/${current.totalToday}",
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { host.navigate(TasksListDestination) },
                        muted,
                    )
                    IconButton(onClick = { host.navigate(TaskFormDestination()) }) {
                        Icon(Icons.Default.Add, stringResource(R.string.tasks_add), tint = muted)
                    }
                }
                current.dashboardRows(3).forEach { row ->
                    TaskRowView(
                        row,
                        onSwipeDone = { swipeDone(row.task.id) },
                        onClick = { host.navigate(TaskDetailDestination(row.task.id)) },
                    )
                }
            }
        }
    }

private fun Modifier.padding20(): Modifier = this.then(Modifier.padding(start = 20.dp))

@Module
@InstallIn(SingletonComponent::class)
internal interface TasksDashboardModule {
    @Binds
    @IntoSet
    fun bindTasksDashboardCard(card: TasksDashboardCard): DashboardCard
}
