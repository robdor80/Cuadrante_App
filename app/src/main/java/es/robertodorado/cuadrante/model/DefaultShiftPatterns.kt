package es.robertodorado.cuadrante.model

/** Reusable built-in patterns. They are data, not calculation rules. */
object DefaultShiftPatterns {
    val SIX_BY_SIX = ShiftPattern(
        listOf(
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
        ),
    )
}
