package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.lifecycle.SavedStateHandle
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.testing.FakeTaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskStore
import io.github.ddiling.mydayboard.core.testing.FakeTaskTickRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import java.time.Instant
import java.time.LocalDate

/** Fake repositories sharing one store and a clock the test moves; it starts on Wednesday 2026-11-18, 12:00 Warsaw. */
internal class CategoryStatsFixture(
    now: Instant = warsaw(2026, 11, 18, 12),
) {
    val today: LocalDate = LocalDate.of(2026, 11, 18)

    /** Nine full days (2026-11-09 to 2026-11-17) are behind today, so the window holds exactly these. */
    val firstDay: LocalDate = today.minusDays(NINE_DAYS_AGO)
    val clock = MutableClock(now, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTaskStore()
    val tasks = FakeTaskRepository(clock, ids, store)
    val categories = FakeTaskCategoryRepository(clock, ids, store)
    val ticks = FakeTaskTickRepository(clock, ids, store)
    val ticker = MutableSharedFlow<Instant>(replay = 1)
    val editor = CategoryEditor(categories)
    val observe = ObserveCategoryStats(tasks, ticks, categories, clock) { ticker }

    init {
        ticker.tryEmit(clock.instant())
    }

    // Built after the Main dispatcher is set: the view model starts its scope on it.
    fun viewModel(categoryId: String) =
        CategoryStatsViewModel(SavedStateHandle(mapOf("categoryId" to categoryId)), observe, editor)

    suspend fun addCategory(name: String = "Meals"): TaskCategory = categories.createCategory(name)

    /** An every-day task of [category] that started on [firstDay] and was done on its first [doneDays] days. */
    suspend fun addMeal(
        category: TaskCategory,
        name: String,
        doneDays: Int,
    ): Task {
        val draft = TaskDraft(name, category.id, Repeat.EveryDay, null, null, null, OnMiss.EXPIRES)
        val task = tasks.createTask(draft, firstDay)
        repeat(doneDays) { ticks.addTick(task.id, firstDay.plusDays(it.toLong()), null) }
        return task
    }

    /** Collects the states of [categoryId] in the background; the last one is the latest. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.collectStates(categoryId: String): List<CategoryStatsUiState> {
        val seen = mutableListOf<CategoryStatsUiState>()
        backgroundScope.launch { observe(categoryId).collect { seen += it } }
        runCurrent()
        return seen
    }

    private companion object {
        const val NINE_DAYS_AGO = 9L
    }
}
