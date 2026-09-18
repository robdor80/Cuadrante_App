package es.robertodorado.cuadrante.data.local

import es.robertodorado.cuadrante.model.AppSettings
import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
import java.time.LocalDate

/** Stable, locale-independent representation used by Preferences DataStore. */
object AppSettingsCodec {
    fun encodeShiftSequence(shifts: List<ShiftType>): String =
        shifts.joinToString(separator = ",", transform = ShiftType::name)

    fun decodeShiftSequence(value: String): List<ShiftType>? {
        if (value.isBlank()) return null

        return runCatching {
            value.split(',').map(ShiftType::valueOf)
        }.getOrNull()
    }

    fun encodeDate(date: LocalDate): String = date.toString()

    fun decodeDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    fun decode(
        patternTypeValue: String?,
        shiftSequenceValue: String?,
        referenceDateValue: String?,
    ): AppSettings? {
        val patternType = patternTypeValue
            ?.let { runCatching { ShiftPatternType.valueOf(it) }.getOrNull() }
            ?: return null
        val shifts = shiftSequenceValue?.let(::decodeShiftSequence) ?: return null
        val referenceDate = referenceDateValue?.let(::decodeDate) ?: return null

        return runCatching {
            AppSettings(patternType, shifts, referenceDate)
        }.getOrNull()
    }
}
