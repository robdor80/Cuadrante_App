package es.robertodorado.cuadrante.ui.calendar

import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.ShiftType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(
    val date: LocalDate,
    val isInDisplayedMonth: Boolean,
    val shift: ShiftType,
)

data class CalendarMonth(
    val yearMonth: YearMonth,
    val days: List<CalendarDay>,
) {
    val weeks: List<List<CalendarDay>>
        get() = days.chunked(DAYS_PER_WEEK)

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}

/** Builds a stable six-week calendar starting on Monday using real dates only. */
object MonthlyCalendarGenerator {
    private const val DAYS_PER_WEEK = 7
    private const val WEEKS_PER_GRID = 6
    private const val DAYS_PER_GRID = DAYS_PER_WEEK * WEEKS_PER_GRID

    fun generate(
        yearMonth: YearMonth,
        shiftCalculator: ShiftCalculator,
    ): CalendarMonth {
        val firstOfMonth = yearMonth.atDay(1)
        val daysAfterMonday = firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
        val gridStart = firstOfMonth.minusDays(daysAfterMonday.toLong())
        val days = List(DAYS_PER_GRID) { offset ->
            val date = gridStart.plusDays(offset.toLong())
            CalendarDay(
                date = date,
                isInDisplayedMonth = YearMonth.from(date) == yearMonth,
                shift = shiftCalculator.shiftFor(date),
            )
        }

        return CalendarMonth(yearMonth = yearMonth, days = days)
    }
}
