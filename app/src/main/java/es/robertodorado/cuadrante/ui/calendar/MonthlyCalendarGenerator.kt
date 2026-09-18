package es.robertodorado.cuadrante.ui.calendar

import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.ShiftType
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(
    val date: LocalDate,
    val shift: ShiftType,
)

data class CalendarMonth(
    val yearMonth: YearMonth,
    val days: List<CalendarDay>,
) {
    val weeks: List<List<CalendarDay?>>
        get() {
            val leadingEmptyCells = yearMonth.atDay(1).dayOfWeek.value - 1
            val requiredCells = leadingEmptyCells + days.size
            val weekCount = (requiredCells + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK

            return List(weekCount) { weekIndex ->
                List(DAYS_PER_WEEK) { dayOfWeekIndex ->
                    val cellIndex = weekIndex * DAYS_PER_WEEK + dayOfWeekIndex
                    val dayIndex = cellIndex - leadingEmptyCells
                    days.getOrNull(dayIndex)
                }
            }
        }

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}

/** Builds only the dates in [yearMonth]; visual padding is represented by null cells. */
object MonthlyCalendarGenerator {
    fun generate(
        yearMonth: YearMonth,
        shiftCalculator: ShiftCalculator,
    ): CalendarMonth {
        val days = (1..yearMonth.lengthOfMonth()).map { dayOfMonth ->
            val date = yearMonth.atDay(dayOfMonth)
            CalendarDay(
                date = date,
                shift = shiftCalculator.shiftFor(date),
            )
        }

        return CalendarMonth(yearMonth = yearMonth, days = days)
    }
}
