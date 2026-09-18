package es.robertodorado.cuadrante.ui.calendar

import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.DefaultShiftPatterns
import es.robertodorado.cuadrante.model.ShiftType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyCalendarGeneratorTest {
    private val referenceDate = LocalDate.of(2026, 9, 1)
    private val calculator = ShiftCalculator(referenceDate, DefaultShiftPatterns.SIX_BY_SIX)

    @Test
    fun `month beginning on Monday starts grid on its first day`() {
        val calendar = generate(2026, 6)

        assertEquals(DayOfWeek.MONDAY, calendar.days.first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 6, 1), calendar.days.first().date)
        assertTrue(calendar.days.first().isInDisplayedMonth)
    }

    @Test
    fun `month beginning on Sunday includes six previous real dates`() {
        val calendar = generate(2026, 2)

        assertEquals(LocalDate.of(2026, 1, 26), calendar.days.first().date)
        assertEquals(LocalDate.of(2026, 2, 1), calendar.days[6].date)
        assertFalse(calendar.days.first().isInDisplayedMonth)
        assertTrue(calendar.days[6].isInDisplayedMonth)
    }

    @Test
    fun `normal February contains exactly 28 current month dates`() {
        val calendar = generate(2026, 2)

        assertEquals(28, calendar.days.count(CalendarDay::isInDisplayedMonth))
        assertTrue(calendar.days.any { it.date == LocalDate.of(2026, 2, 28) })
    }

    @Test
    fun `leap February contains February 29`() {
        val calendar = generate(2028, 2)

        assertEquals(29, calendar.days.count(CalendarDay::isInDisplayedMonth))
        assertTrue(calendar.days.any { it.date == LocalDate.of(2028, 2, 29) })
    }

    @Test
    fun `December and January remain consecutive across year boundary`() {
        val december = YearMonth.of(2026, 12)
        val january = december.plusMonths(1)

        assertEquals(YearMonth.of(2027, 1), january)
        assertTrue(generate(2026, 12).days.any { it.date == LocalDate.of(2027, 1, 1) })
        assertTrue(generate(2027, 1).days.any { it.date == LocalDate.of(2026, 12, 31) })
    }

    @Test
    fun `calendar always contains six complete Monday to Sunday weeks`() {
        listOf(
            YearMonth.of(2026, 2),
            YearMonth.of(2026, 6),
            YearMonth.of(2026, 9),
            YearMonth.of(2028, 2),
        ).forEach { month ->
            val calendar = MonthlyCalendarGenerator.generate(month, calculator)

            assertEquals(42, calendar.days.size)
            assertEquals(6, calendar.weeks.size)
            calendar.weeks.forEach { week ->
                assertEquals(7, week.size)
                assertEquals(DayOfWeek.MONDAY, week.first().date.dayOfWeek)
                assertEquals(DayOfWeek.SUNDAY, week.last().date.dayOfWeek)
                week.zipWithNext().forEach { (current, next) ->
                    assertEquals(current.date.plusDays(1), next.date)
                }
            }
        }
    }

    @Test
    fun `every visible date receives its shift from ShiftCalculator`() {
        val calendar = generate(2026, 9)

        calendar.days.forEach { day ->
            assertEquals(calculator.shiftFor(day.date), day.shift)
        }
        assertEquals(
            ShiftType.MORNING,
            calendar.days.single { it.date == referenceDate }.shift,
        )
        assertEquals(
            ShiftType.OFF,
            calendar.days.single { it.date == referenceDate.minusDays(1) }.shift,
        )
    }

    private fun generate(year: Int, month: Int): CalendarMonth =
        MonthlyCalendarGenerator.generate(YearMonth.of(year, month), calculator)
}
