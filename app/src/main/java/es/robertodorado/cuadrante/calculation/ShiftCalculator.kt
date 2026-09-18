package es.robertodorado.cuadrante.calculation

import es.robertodorado.cuadrante.model.ShiftPattern
import es.robertodorado.cuadrante.model.ShiftType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Resolves the shift assigned to any date in a repeating pattern.
 *
 * [referenceDate] is always associated with the first element of [pattern].
 */
class ShiftCalculator(
    val referenceDate: LocalDate,
    private val pattern: ShiftPattern,
) {
    constructor(referenceDate: LocalDate, shifts: List<ShiftType>) :
        this(referenceDate, ShiftPattern(shifts))

    fun shiftFor(date: LocalDate): ShiftType {
        val dayOffset = ChronoUnit.DAYS.between(referenceDate, date)
        val patternIndex = Math.floorMod(dayOffset, pattern.size.toLong()).toInt()
        return pattern[patternIndex]
    }
}
