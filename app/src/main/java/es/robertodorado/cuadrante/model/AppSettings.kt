package es.robertodorado.cuadrante.model

import java.time.LocalDate

/** User configuration required to calculate the work calendar. */
data class AppSettings(
    val patternType: ShiftPatternType,
    val shifts: List<ShiftType>,
    val referenceDate: LocalDate,
) {
    init {
        require(shifts.isNotEmpty()) { "A saved shift sequence cannot be empty" }
    }
}
