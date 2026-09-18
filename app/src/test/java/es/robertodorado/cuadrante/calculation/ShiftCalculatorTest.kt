package es.robertodorado.cuadrante.calculation

import es.robertodorado.cuadrante.model.DefaultShiftPatterns
import es.robertodorado.cuadrante.model.ShiftPattern
import es.robertodorado.cuadrante.model.ShiftType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShiftCalculatorTest {
    private val referenceDate = LocalDate.of(2026, 9, 1)
    private val defaultShifts = listOf(
        ShiftType.MORNING,
        ShiftType.MORNING,
        ShiftType.AFTERNOON,
        ShiftType.AFTERNOON,
        ShiftType.NIGHT,
        ShiftType.NIGHT,
        ShiftType.POST_NIGHT,
        ShiftType.OFF,
        ShiftType.OFF,
        ShiftType.OFF,
        ShiftType.OFF,
        ShiftType.OFF,
    )
    private val calculator = ShiftCalculator(referenceDate, DefaultShiftPatterns.SIX_BY_SIX)

    @Test
    fun `reference date returns first shift`() {
        assertEquals(ShiftType.MORNING, calculator.shiftFor(referenceDate))
    }

    @Test
    fun `walks the complete six by six pattern`() {
        defaultShifts.forEachIndexed { dayOffset, expectedShift ->
            assertEquals(expectedShift, calculator.shiftFor(referenceDate.plusDays(dayOffset.toLong())))
        }
    }

    @Test
    fun `day after the last off restarts at morning`() {
        assertEquals(ShiftType.MORNING, calculator.shiftFor(referenceDate.plusDays(12)))
    }

    @Test
    fun `several consecutive cycles preserve the exact pattern`() {
        repeat(20) { cycle ->
            defaultShifts.forEachIndexed { index, expectedShift ->
                val dayOffset = cycle * defaultShifts.size + index
                assertEquals(expectedShift, calculator.shiftFor(referenceDate.plusDays(dayOffset.toLong())))
            }
        }
    }

    @Test
    fun `dates before reference walk the pattern backwards`() {
        val expectations = mapOf(
            -1L to ShiftType.OFF,
            -6L to ShiftType.POST_NIGHT,
            -7L to ShiftType.NIGHT,
            -11L to ShiftType.MORNING,
            -12L to ShiftType.MORNING,
        )

        expectations.forEach { (dayOffset, expectedShift) ->
            assertEquals(expectedShift, calculator.shiftFor(referenceDate.plusDays(dayOffset)))
        }
    }

    @Test
    fun `day immediately before a morning reference is last cycle element`() {
        assertEquals(ShiftType.OFF, calculator.shiftFor(referenceDate.minusDays(1)))
    }

    @Test
    fun `month boundary does not alter the sequence`() {
        val monthCalculator = ShiftCalculator(LocalDate.of(2026, 1, 30), defaultShifts)

        assertEquals(ShiftType.MORNING, monthCalculator.shiftFor(LocalDate.of(2026, 1, 30)))
        assertEquals(ShiftType.MORNING, monthCalculator.shiftFor(LocalDate.of(2026, 1, 31)))
        assertEquals(ShiftType.AFTERNOON, monthCalculator.shiftFor(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun `year boundary from December to January does not alter the sequence`() {
        val yearCalculator = ShiftCalculator(LocalDate.of(2026, 12, 31), defaultShifts)

        assertEquals(ShiftType.MORNING, yearCalculator.shiftFor(LocalDate.of(2026, 12, 31)))
        assertEquals(ShiftType.MORNING, yearCalculator.shiftFor(LocalDate.of(2027, 1, 1)))
        assertEquals(ShiftType.AFTERNOON, yearCalculator.shiftFor(LocalDate.of(2027, 1, 2)))
    }

    @Test
    fun `non leap February 2026 advances directly to March`() {
        val februaryCalculator = ShiftCalculator(LocalDate.of(2026, 2, 27), defaultShifts)

        assertEquals(28, LocalDate.of(2026, 2, 1).lengthOfMonth())
        assertEquals(ShiftType.MORNING, februaryCalculator.shiftFor(LocalDate.of(2026, 2, 27)))
        assertEquals(ShiftType.MORNING, februaryCalculator.shiftFor(LocalDate.of(2026, 2, 28)))
        assertEquals(ShiftType.AFTERNOON, februaryCalculator.shiftFor(LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `leap February 2028 includes February 29`() {
        val leapCalculator = ShiftCalculator(LocalDate.of(2028, 2, 27), defaultShifts)

        assertEquals(29, LocalDate.of(2028, 2, 1).lengthOfMonth())
        assertEquals(ShiftType.MORNING, leapCalculator.shiftFor(LocalDate.of(2028, 2, 27)))
        assertEquals(ShiftType.MORNING, leapCalculator.shiftFor(LocalDate.of(2028, 2, 28)))
        assertEquals(ShiftType.AFTERNOON, leapCalculator.shiftFor(LocalDate.of(2028, 2, 29)))
        assertEquals(ShiftType.AFTERNOON, leapCalculator.shiftFor(LocalDate.of(2028, 3, 1)))
    }

    @Test
    fun `February 28 February 29 and March 1 remain consecutive`() {
        val leapCalculator = ShiftCalculator(LocalDate.of(2028, 2, 28), defaultShifts)

        assertEquals(ShiftType.MORNING, leapCalculator.shiftFor(LocalDate.of(2028, 2, 28)))
        assertEquals(ShiftType.MORNING, leapCalculator.shiftFor(LocalDate.of(2028, 2, 29)))
        assertEquals(ShiftType.AFTERNOON, leapCalculator.shiftFor(LocalDate.of(2028, 3, 1)))
    }

    @Test
    fun `date many years forward keeps its cycle position`() {
        val distantDate = referenceDate.plusDays(12L * 500 + 4)

        assertEquals(ShiftType.NIGHT, calculator.shiftFor(distantDate))
    }

    @Test
    fun `date many years backward keeps its cycle position`() {
        val distantDate = referenceDate.minusDays(12L * 500 + 1)

        assertEquals(ShiftType.OFF, calculator.shiftFor(distantDate))
    }

    @Test
    fun `custom pattern with different length repeats generically`() {
        val customShifts = listOf(ShiftType.NIGHT, ShiftType.OFF, ShiftType.MORNING)
        val customCalculator = ShiftCalculator(referenceDate, customShifts)

        repeat(5) { cycle ->
            customShifts.forEachIndexed { index, expectedShift ->
                val dayOffset = cycle * customShifts.size + index
                assertEquals(expectedShift, customCalculator.shiftFor(referenceDate.plusDays(dayOffset.toLong())))
            }
        }
        assertEquals(ShiftType.MORNING, customCalculator.shiftFor(referenceDate.minusDays(1)))
    }

    @Test
    fun `single element pattern always returns the same shift`() {
        val singleShiftCalculator = ShiftCalculator(referenceDate, listOf(ShiftType.OFF))

        listOf(-100_000L, -1L, 0L, 1L, 100_000L).forEach { dayOffset ->
            assertEquals(ShiftType.OFF, singleShiftCalculator.shiftFor(referenceDate.plusDays(dayOffset)))
        }
    }

    @Test
    fun `empty pattern is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ShiftCalculator(referenceDate, emptyList())
        }
    }

    @Test
    fun `every date from 2000 through 2100 advances exactly one cycle position`() {
        val uniquePattern = ShiftPattern(ShiftType.entries)
        val rangeCalculator = ShiftCalculator(LocalDate.of(2050, 6, 15), uniquePattern)
        var date = LocalDate.of(2000, 1, 1)
        val finalDate = LocalDate.of(2100, 12, 31)
        var previousShift = rangeCalculator.shiftFor(date)
        var visitedDates = 1

        while (date < finalDate) {
            date = date.plusDays(1)
            val currentShift = rangeCalculator.shiftFor(date)
            val expectedIndex = (uniquePattern.shifts.indexOf(previousShift) + 1) % uniquePattern.size
            assertEquals(expectedIndex, uniquePattern.shifts.indexOf(currentShift))
            previousShift = currentShift
            visitedDates++
        }

        assertEquals(36_890, visitedDates)
    }
}
