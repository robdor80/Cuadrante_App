package es.robertodorado.cuadrante.ui.calendar

import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.DefaultShiftPatterns
import es.robertodorado.cuadrante.model.ShiftType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyCalendarGeneratorTest {
    private val referenceDate = LocalDate.of(2026, 9, 1)
    private val calculator = ShiftCalculator(referenceDate, DefaultShiftPatterns.SIX_BY_SIX)

    @Test
    fun `month beginning on Monday starts in the first cell`() {
        val calendar = generate(2026, 6)

        assertEquals(LocalDate.of(2026, 6, 1), calendar.weeks.first().first()?.date)
        assertEquals(DayOfWeek.MONDAY, calendar.weeks.first().first()?.date?.dayOfWeek)
    }

    @Test
    fun `month beginning on Sunday has six empty leading cells`() {
        val calendar = generate(2026, 2)

        assertTrue(calendar.weeks.first().take(6).all { it == null })
        assertEquals(LocalDate.of(2026, 2, 1), calendar.weeks.first()[6]?.date)
    }

    @Test
    fun `normal February contains 28 dates and no adjacent dates`() {
        val calendar = generate(2026, 2)

        assertEquals(28, calendar.days.size)
        assertEquals(LocalDate.of(2026, 2, 1), calendar.days.first().date)
        assertEquals(LocalDate.of(2026, 2, 28), calendar.days.last().date)
        assertOnlyCurrentMonthDates(calendar)
    }

    @Test
    fun `leap February contains 29 dates`() {
        val calendar = generate(2028, 2)

        assertEquals(29, calendar.days.size)
        assertEquals(LocalDate.of(2028, 2, 29), calendar.days.last().date)
        assertOnlyCurrentMonthDates(calendar)
    }

    @Test
    fun `thirty day month contains only its dates`() {
        val calendar = generate(2026, 9)

        assertEquals(30, calendar.days.size)
        assertOnlyCurrentMonthDates(calendar)
    }

    @Test
    fun `thirty one day month contains only its dates`() {
        val calendar = generate(2026, 7)

        assertEquals(31, calendar.days.size)
        assertOnlyCurrentMonthDates(calendar)
    }

    @Test
    fun `calendar uses five weeks when five are sufficient`() {
        assertEquals(5, generate(2026, 9).weeks.size)
    }

    @Test
    fun `calendar uses six weeks when required`() {
        val calendar = generate(2026, 3)

        assertEquals(6, calendar.weeks.size)
        assertTrue(calendar.weeks.last().drop(2).all { it == null })
    }

    @Test
    fun `calendar can use four weeks for a complete four week February`() {
        assertEquals(4, generate(2027, 2).weeks.size)
    }

    @Test
    fun `December and January do not include dates from the adjacent year`() {
        val december = generate(2026, 12)
        val january = generate(2027, 1)

        assertOnlyCurrentMonthDates(december)
        assertOnlyCurrentMonthDates(january)
        assertNull(december.weeks.first().first())
        assertTrue(january.weeks.first().take(4).all { it == null })
        assertEquals(LocalDate.of(2027, 1, 1), january.weeks.first()[4]?.date)
    }

    @Test
    fun `every current month date receives its shift from ShiftCalculator`() {
        val calendar = generate(2026, 9)

        calendar.days.forEach { day ->
            assertEquals(calculator.shiftFor(day.date), day.shift)
        }
        assertEquals(
            ShiftType.MORNING,
            calendar.days.single { it.date == referenceDate }.shift,
        )
    }

    private fun assertOnlyCurrentMonthDates(calendar: CalendarMonth) {
        assertTrue(calendar.days.all { YearMonth.from(it.date) == calendar.yearMonth })
        assertEquals(calendar.days, calendar.weeks.flatten().filterNotNull())
        calendar.weeks.forEach { assertEquals(7, it.size) }
    }

    private fun generate(year: Int, month: Int): CalendarMonth =
        MonthlyCalendarGenerator.generate(YearMonth.of(year, month), calculator)
}
