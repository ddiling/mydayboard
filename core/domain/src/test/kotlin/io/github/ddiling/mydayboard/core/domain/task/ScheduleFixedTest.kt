package io.github.ddiling.mydayboard.core.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleFixedTest {
    private val nineAm = LocalTime.of(9, 0)
    private val eveningPills = LocalTime.of(21, 0)
    private val twiceDaily = Repeat.TimesPerDay(listOf(nineAm, eveningPills))

    @Test
    fun `every day gives one occurrence per day from the start date to today`() {
        val start = wednesday.minusDays(2)

        val result = occurrencesOf(task(startDate = start), emptyList(), wednesday)

        assertEquals(listOf(start, start.plusDays(1), wednesday), result.dues())
    }

    @Test
    fun `nothing is planned before the start date`() {
        val result = occurrencesOf(task(startDate = wednesday.plusDays(1)), emptyList(), wednesday)

        assertEquals(emptyList<Occurrence>(), result)
    }

    @Test
    fun `several times with every time ticked is done 2 of 2`() {
        val ticks = listOf(tick(wednesday, nineAm), tick(wednesday, eveningPills))

        val result = occurrencesOf(task(twiceDaily), ticks, wednesday).single()

        assertEquals(Triple(2, 2, OccurrenceStatus.DONE), Triple(result.done, result.target, result.status))
    }

    @Test
    fun `several times with one tick is done 1 of 2`() {
        val result = occurrencesOf(task(twiceDaily), listOf(tick(wednesday, nineAm)), wednesday).single()

        assertEquals(Triple(1, 2, OccurrenceStatus.DUE_TODAY), Triple(result.done, result.target, result.status))
    }

    @Test
    fun `several times ignores a tick whose slot is no longer one of the times`() {
        val removedSlot = LocalTime.of(12, 0)

        val result = occurrencesOf(task(twiceDaily), listOf(tick(wednesday, removedSlot)), wednesday).single()

        assertEquals(0, result.done)
    }

    @Test
    fun `several times counts the same slot ticked twice once`() {
        val duplicate = tick(wednesday, nineAm).copy(id = "second")

        val result = occurrencesOf(task(twiceDaily), listOf(tick(wednesday, nineAm), duplicate), wednesday).single()

        assertEquals(1, result.done)
    }

    @Test
    fun `chosen weekdays give only those days`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val monday = wednesday.minusDays(2)

        val result =
            occurrencesOf(
                task(Repeat.Weekdays(days), monday),
                emptyList(),
                wednesday.plusDays(2),
                wednesday.plusDays(2),
            )

        assertEquals(listOf(monday, wednesday, wednesday.plusDays(2)), result.dues())
    }

    @Test
    fun `a weekday that is not chosen is not an occurrence`() {
        val tuesday = wednesday.minusDays(1)

        val result = occurrencesOf(task(Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY)), tuesday), emptyList(), wednesday)

        assertEquals(listOf(wednesday), result.dues())
    }

    @Test
    fun `every 30 days keeps its calendar even after a late tick`() {
        val first = LocalDate.of(2026, 11, 1)
        val repeat = Repeat.EveryNDays(30, EveryNDaysMode.FIXED_CALENDAR, first)
        val lateTick = tick(LocalDate.of(2026, 11, 10))

        val result = occurrencesOf(task(repeat, first, OnMiss.STAYS), listOf(lateTick), LocalDate.of(2026, 12, 5))

        assertEquals(listOf(first, LocalDate.of(2026, 12, 1)), result.dues())
    }

    @Test
    fun `every 3 days from the first due date gives day 0, 3 and 6`() {
        val first = wednesday
        val repeat = Repeat.EveryNDays(3, EveryNDaysMode.FIXED_CALENDAR, first)

        val result = occurrencesOf(task(repeat, first), emptyList(), first.plusDays(7))

        assertEquals(listOf(first, first.plusDays(3), first.plusDays(6)), result.dues())
    }

    @Test
    fun `every N days starts at the first due date on or after the start date`() {
        val repeat = Repeat.EveryNDays(3, EveryNDaysMode.FIXED_CALENDAR, wednesday.minusDays(7))

        val result = occurrencesOf(task(repeat, wednesday), emptyList(), wednesday.plusDays(2))

        assertEquals(listOf(wednesday.plusDays(2)), result.dues())
    }

    @Test
    fun `monthly day 5 gives 5 November and 5 December`() {
        val start = LocalDate.of(2026, 11, 1)

        val result = occurrencesOf(task(Repeat.Monthly(5), start), emptyList(), LocalDate.of(2026, 12, 20))

        assertEquals(listOf(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 12, 5)), result.dues())
    }

    @Test
    fun `monthly day 31 falls on the last day of shorter months`() {
        val start = LocalDate.of(2027, 1, 1)

        val result = occurrencesOf(task(Repeat.Monthly(31), start), emptyList(), LocalDate.of(2027, 4, 30))

        val expected = listOf(31, 28, 31, 30).mapIndexed { i, day -> LocalDate.of(2027, i + 1, day) }
        assertEquals(expected, result.dues())
    }

    @Test
    fun `monthly day 29 is the leap day in 2028 and the 28th in 2027`() {
        val start = LocalDate.of(2027, 2, 1)

        val result = occurrencesOf(task(Repeat.Monthly(29), start), emptyList(), LocalDate.of(2028, 2, 29))

        assertEquals(LocalDate.of(2027, 2, 28), result.first().due)
        assertEquals(LocalDate.of(2028, 2, 29), result.last().due)
    }

    @Test
    fun `monthly skips a month whose due day is before the start date`() {
        val start = LocalDate.of(2026, 11, 10)

        val result = occurrencesOf(task(Repeat.Monthly(5), start), emptyList(), LocalDate.of(2026, 12, 31))

        assertEquals(listOf(LocalDate.of(2026, 12, 5)), result.dues())
    }

    @Test
    fun `monthly crosses the year change`() {
        val start = LocalDate.of(2026, 12, 20)

        val result = occurrencesOf(task(Repeat.Monthly(15), start), emptyList(), LocalDate.of(2027, 1, 20))

        assertEquals(listOf(LocalDate.of(2027, 1, 15)), result.dues())
    }

    @Test
    fun `a one time task due in the future gives no occurrence yet`() {
        val oneTime = task(Repeat.OneTime(wednesday.plusDays(5)))

        assertEquals(emptyList<Occurrence>(), occurrencesOf(oneTime, emptyList(), wednesday))
    }

    @Test
    fun `a due day after today is upcoming and today stays due today`() {
        val weekdays = Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        val result = occurrencesOf(task(weekdays), emptyList(), wednesday, wednesday.plusDays(2))

        assertEquals(listOf(OccurrenceStatus.DUE_TODAY, OccurrenceStatus.UPCOMING), result.map { it.status })
    }

    @Test
    fun `a staying window ends the day before the next due day`() {
        val weekdays = Repeat.Weekdays(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val friday = wednesday.plusDays(2)
        val nextWednesday = wednesday.plusDays(7)

        val result = occurrencesOf(task(weekdays, friday, OnMiss.STAYS), listOf(tick(nextWednesday)), nextWednesday)

        assertEquals(
            listOf(OccurrenceStatus.ENDED, OccurrenceStatus.DONE),
            result.map { it.status },
        )
    }

    @Test
    fun `every N days includes a start date that is exactly a due date`() {
        val repeat = Repeat.EveryNDays(3, EveryNDaysMode.FIXED_CALENDAR, wednesday)
        val start = wednesday.plusDays(3)

        val result = occurrencesOf(task(repeat, start), emptyList(), start)

        assertEquals(listOf(start), result.dues())
    }

    @Test
    fun `monthly includes a start date equal to the due day`() {
        val start = LocalDate.of(2026, 11, 5)

        val result = occurrencesOf(task(Repeat.Monthly(5), start), emptyList(), start)

        assertEquals(listOf(start), result.dues())
    }

    @Test
    fun `monthly day 31 starting on the clamped last day of April includes it`() {
        val start = LocalDate.of(2027, 4, 30)

        val result = occurrencesOf(task(Repeat.Monthly(31), start), emptyList(), start)

        assertEquals(listOf(start), result.dues())
    }

    @Test
    fun `a one time task due in the past that stays and is undone is late without a last day`() {
        val oneTime = task(Repeat.OneTime(wednesday.minusDays(3)), onMiss = OnMiss.STAYS)

        val result = occurrencesOf(oneTime, emptyList(), wednesday).single()

        assertEquals(OccurrenceStatus.LATE, result.status)
        assertNull(result.lastDay)
    }

    @Test
    fun `a one time task due in the past that expires and is undone has ended`() {
        val oneTime = task(Repeat.OneTime(wednesday.minusDays(3)), onMiss = OnMiss.EXPIRES)

        assertEquals(OccurrenceStatus.ENDED, occurrencesOf(oneTime, emptyList(), wednesday).single().status)
    }

    @Test
    fun `a one time task that stays and is ticked on a later day is done`() {
        val oneTime = task(Repeat.OneTime(wednesday.minusDays(3)), onMiss = OnMiss.STAYS)

        val result = occurrencesOf(oneTime, listOf(tick(wednesday.minusDays(1))), wednesday).single()

        assertEquals(OccurrenceStatus.DONE, result.status)
    }
}
