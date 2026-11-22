package io.github.ddiling.mydayboard.feature.tasks.day

import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import io.github.ddiling.mydayboard.core.domain.time.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** What a Save changed: the [state] that was saved and the [outcome] that Undo needs. */
internal data class DayMarked(
    val state: DaySheetState,
    val outcome: TickOutcome.DayReplaced,
)

/**
 * The logic behind the day sheet, with no Compose in it so it can be tested alone. It edits a draft and writes
 * once on [save], through [TaskActions.markDay]; that call re-checks the day against the clock, so a sheet left
 * open past midnight cannot write a day that is no longer allowed. Each ViewModel gets its own instance.
 */
internal class DaySheet
    @Inject
    constructor(
        private val actions: TaskActions,
        private val clock: Clock,
    ) {
        private val _state = MutableStateFlow<DaySheetState?>(null)

        /** The draft to show, or null when the sheet is closed. */
        val state: StateFlow<DaySheetState?> = _state.asStateFlow()

        /**
         * True for the days a sheet can open: from the start date up to yesterday. Today and the future have the swipe
         * and the big button, so a mistaken tap cannot pre-fill days that have not happened. Read from the clock on
         * each call, so it follows midnight and a change of zone.
         */
        fun canOpen(
            task: Task,
            day: LocalDate,
        ): Boolean = !day.isBefore(task.startDate) && day.isBefore(clock.today())

        /** Opens [day] of [task] with its current [ticks]; false, and nothing opens, when the day is not allowed. */
        fun open(
            task: Task,
            ticks: List<TaskTick>,
            day: LocalDate,
        ): Boolean {
            if (!canOpen(task, day)) return false
            val dayTicks = ticks.filter { it.day == day }
            _state.value = DaySheetState(task.id, task.name, day, kindOf(task, dayTicks))
            return true
        }

        /** Ticks [slot] when it is open and unticks it when it is ticked; times the task does not have are ignored. */
        fun toggle(slot: LocalTime) =
            updateKind<DaySheetKind.Times> { kind ->
                val chosen = if (slot in kind.chosen) kind.chosen - slot else kind.chosen + slot
                if (slot in kind.slots) kind.copy(chosen = chosen) else kind
            }

        fun setSingle(done: Boolean) = updateKind<DaySheetKind.Single> { it.copy(done = done) }

        /** Moves the count by [delta]; it stops at 0 and at the goal. */
        fun step(delta: Int) =
            updateKind<DaySheetKind.Count> { count ->
                count.copy(value = (count.value + delta).coerceIn(0, count.target))
            }

        /** Closes the sheet and saves nothing: swipe down, a tap outside and Back all end here. */
        fun dismiss() {
            _state.value = null
        }

        /** Closes the sheet and writes the draft; null when nothing was open or the day was refused. */
        suspend fun save(): DayMarked? {
            val draft = _state.getAndUpdate { null } ?: return null
            return actions.markDay(draft.taskId, draft.day, slotsOf(draft.kind))?.let { DayMarked(draft, it) }
        }

        private inline fun <reified K : DaySheetKind> updateKind(change: (K) -> DaySheetKind) {
            _state.update { current ->
                val kind = current?.kind as? K
                if (current == null || kind == null) current else current.copy(kind = change(kind))
            }
        }
    }

private fun kindOf(
    task: Task,
    dayTicks: List<TaskTick>,
): DaySheetKind {
    val repeat = task.repeat
    val goal = task.goal
    return when {
        repeat is Repeat.TimesPerDay -> {
            val ticked = dayTicks.mapNotNull { it.slot }.filter { it in repeat.times }
            DaySheetKind.Times(repeat.times, ticked.toSet())
        }

        goal != null -> {
            DaySheetKind.Count(goal, dayTicks.size.coerceAtMost(goal))
        }

        else -> {
            DaySheetKind.Single(dayTicks.isNotEmpty())
        }
    }
}

/** The slots `markDay` takes: a count is that many untimed ticks, a yes is one untimed tick. */
private fun slotsOf(kind: DaySheetKind): List<LocalTime?> =
    when (kind) {
        is DaySheetKind.Single -> if (kind.done) listOf(null) else emptyList()
        is DaySheetKind.Times -> kind.slots.filter { it in kind.chosen }
        is DaySheetKind.Count -> List(kind.value) { null }
    }
