package es.robertodorado.cuadrante.util

import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/** Converts the Material date picker's UTC day representation without using a time zone. */
fun LocalDate.toDatePickerMillis(): Long = toEpochDay() * MILLIS_PER_DAY

/** Converts the Material date picker's UTC milliseconds directly into a calendar date. */
fun Long.toLocalDateFromDatePicker(): LocalDate =
    LocalDate.ofEpochDay(Math.floorDiv(this, MILLIS_PER_DAY))
