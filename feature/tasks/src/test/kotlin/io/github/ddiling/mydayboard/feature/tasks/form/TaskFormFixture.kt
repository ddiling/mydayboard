package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.lifecycle.SavedStateHandle
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.testing.FakeTaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskStore
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryEditor
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

/** The fakes and the clock a form test needs. The clock starts on 2026-03-02 10:30 in Warsaw. */
internal class TaskFormFixture(
    now: Instant = warsaw(2026, 3, 2, 10, 30),
) {
    val clock = MutableClock(now, TestZones.WARSAW)
    val today: LocalDate = LocalDate.of(2026, 3, 2)
    private val ids = SequentialIdGenerator()
    private val store = FakeTaskStore()
    val tasks = FakeTaskRepository(clock, ids, store)
    val categories = FakeTaskCategoryRepository(clock, ids, store)

    fun viewModelFor(taskId: String? = null) =
        TaskFormViewModel(
            SavedStateHandle(mapOf("taskId" to taskId)),
            tasks,
            categories,
            CategoryEditor(categories),
            clock,
        )

    suspend fun storedTasks(): List<Task> = tasks.observeTasks().first()

    fun draftOf(
        name: String,
        repeat: Repeat = Repeat.EveryDay,
    ) = TaskDraft(name, categoryId = null, repeat, time = null, note = null, goal = null, onMiss = OnMiss.EXPIRES)

    suspend fun givenTask(
        name: String = "Pills",
        repeat: Repeat = Repeat.EveryDay,
        startDate: LocalDate = LocalDate.of(2026, 2, 1),
    ): Task = tasks.createTask(draftOf(name, repeat), startDate)
}
