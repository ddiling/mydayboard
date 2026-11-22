package io.github.ddiling.mydayboard.core.domain.task

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * The ways the user changes ticks: swipe (done), untick, fix a past day, and Undo for each.
 *
 * Every call reads the clock once and takes "today" from it, never from what the screen showed earlier, so a
 * swipe made just after midnight ticks the new day. One swipe is always one step. Archived and deleted tasks
 * are left alone: their ticks must not change behind the user's back.
 *
 * Swipe, untick and markDay read the ticks and then write, so a double tap could read the same state twice
 * and e.g. push a count goal past its target. They take one lock per instance, so each sees the result of
 * the one before it.
 */
class TaskActions
    @Inject
    constructor(
        private val tasks: TaskRepository,
        private val ticks: TaskTickRepository,
        private val clock: Clock,
    ) {
        private val lock = Mutex()

        /**
         * Ticks the next undone time of a several-times task, adds one to a count goal, or ticks a plain task.
         * Null when the task is not due or late today, or already complete.
         */
        suspend fun swipeDone(taskId: String): TickOutcome.Added? =
            lock.withLock {
                val today = today()
                val task = liveTask(taskId) ?: return null
                val ticksNow = ticks.observeTicks(taskId).first()
                val open =
                    occurrencesOf(task, ticksNow, today).any {
                        it.status == OccurrenceStatus.LATE || it.status == OccurrenceStatus.DUE_TODAY
                    }
                val slot = nextSlot(task, ticksNow, today)
                val tick = if (open && slot.available) ticks.addTick(taskId, today, slot.time) else null
                tick?.let {
                    val progress = progressOn(task, today)
                    TickOutcome.Added(taskId, it.id, progress?.done ?: 1, progress?.target ?: 1, slot.time)
                }
            }

        /** Removes the newest tick of today (one step back for a count goal). Null when nothing is ticked today. */
        suspend fun untickToday(taskId: String): TickOutcome.Removed? =
            lock.withLock {
                val today = today()
                val task = liveTask(taskId) ?: return null
                val newest =
                    ticks
                        .observeTicks(taskId)
                        .first()
                        .filter { it.day == today }
                        .maxWithOrNull(compareBy<TaskTick> { it.createdAt }.thenBy { it.id })
                newest?.let {
                    ticks.deleteTick(it.id)
                    val progress = progressOn(task, today)
                    TickOutcome.Removed(taskId, it.id, progress?.done ?: 0, progress?.target ?: targetOf(task))
                }
            }

        /**
         * Ticks one [chosen] time of a several-times task for today, for the details screen where the user picks
         * the time (a swipe always takes the next one). The time is cut to whole minutes first, because the task's
         * times have none, so 08:00:30 counts as 08:00. Null when the task is not live, has no such time, is not
         * due today, or that time is already ticked.
         */
        suspend fun tickSlotToday(
            taskId: String,
            chosen: LocalTime,
        ): TickOutcome.Added? =
            lock.withLock {
                val slot = chosen.truncatedTo(ChronoUnit.MINUTES)
                val today = today()
                val task = liveTask(taskId) ?: return null
                val times = (task.repeat as? Repeat.TimesPerDay)?.times ?: return null
                val ticksNow = ticks.observeTicks(taskId).first()
                val dueToday = occurrencesOf(task, ticksNow, today).any { it.status == OccurrenceStatus.DUE_TODAY }
                val free = ticksNow.none { it.day == today && it.slot == slot }
                val tick = if (dueToday && slot in times && free) ticks.addTick(taskId, today, slot) else null
                tick?.let {
                    val progress = progressOn(task, today)
                    TickOutcome.Added(taskId, it.id, progress?.done ?: 1, progress?.target ?: 1, slot)
                }
            }

        /** Removes today's tick of one [chosen] time (cut to whole minutes). Null when that time is not ticked. */
        suspend fun untickSlotToday(
            taskId: String,
            chosen: LocalTime,
        ): TickOutcome.Removed? =
            lock.withLock {
                val slot = chosen.truncatedTo(ChronoUnit.MINUTES)
                val today = today()
                val task = liveTask(taskId) ?: return null
                val tick = ticks.observeTicks(taskId).first().firstOrNull { it.day == today && it.slot == slot }
                tick?.let {
                    ticks.deleteTick(it.id)
                    val progress = progressOn(task, today)
                    TickOutcome.Removed(taskId, it.id, progress?.done ?: 0, progress?.target ?: targetOf(task))
                }
            }

        /**
         * Makes [day] match [slots]. Allowed from the task's start date up to yesterday; today and the future use
         * the swipe and the big button, so a mistaken tap cannot pre-fill days that have not happened. Null when
         * the day is not allowed, or a several-times task gets a time that is not one of its own.
         */
        suspend fun markDay(
            taskId: String,
            day: LocalDate,
            slots: List<LocalTime?>,
        ): TickOutcome.DayReplaced? =
            lock.withLock {
                val today = today()
                val task = liveTask(taskId) ?: return null
                val allowed = !day.isBefore(task.startDate) && day.isBefore(today) && slotsFit(task, slots)
                if (allowed) TickOutcome.DayReplaced(taskId, day, ticks.replaceDay(taskId, day, slots)) else null
            }

        /** Reverts [outcome] by deleting or restoring the ticks it touched. */
        suspend fun undo(outcome: TickOutcome) {
            when (outcome) {
                is TickOutcome.Added -> ticks.deleteTick(outcome.tickId)
                is TickOutcome.Removed -> ticks.restoreTick(outcome.tickId)
                is TickOutcome.DayReplaced -> ticks.undoDayChange(outcome.change)
            }
        }

        private fun slotsFit(
            task: Task,
            slots: List<LocalTime?>,
        ): Boolean = (task.repeat as? Repeat.TimesPerDay)?.let { slots.all { slot -> slot in it.times } } ?: true

        private fun today(): LocalDate = clock.instant().atZone(clock.zone).toLocalDate()

        private suspend fun liveTask(taskId: String): Task? =
            tasks.observeTask(taskId).first()?.takeIf { it.archivedAt == null }

        /** The occurrence whose window holds [today], after the latest change; it is what the UI shows as progress. */
        private suspend fun progressOn(
            task: Task,
            today: LocalDate,
        ): Occurrence? =
            occurrencesOf(task, ticks.observeTicks(task.id).first(), today)
                .lastOrNull { !it.due.isAfter(today) && (it.lastDay == null || !it.lastDay.isBefore(today)) }
    }

/** The time to tick next; [available] is false when a several-times task has every time done today. */
private data class NextSlot(
    val time: LocalTime?,
    val available: Boolean,
)

private fun nextSlot(
    task: Task,
    ticksNow: List<TaskTick>,
    today: LocalDate,
): NextSlot =
    when (val repeat = task.repeat) {
        is Repeat.TimesPerDay -> {
            val done = ticksNow.filter { it.day == today }.map { it.slot }.toSet()
            val next = repeat.times.firstOrNull { it !in done }
            NextSlot(next, next != null)
        }

        else -> {
            NextSlot(null, true)
        }
    }
