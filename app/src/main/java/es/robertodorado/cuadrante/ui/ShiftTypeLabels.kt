package es.robertodorado.cuadrante.ui

import androidx.annotation.StringRes
import es.robertodorado.cuadrante.R
import es.robertodorado.cuadrante.model.ShiftType

/** Keeps localized labels in the UI layer, separate from domain identifiers. */
@StringRes
fun ShiftType.labelResource(): Int = when (this) {
    ShiftType.MORNING -> R.string.shift_morning
    ShiftType.AFTERNOON -> R.string.shift_afternoon
    ShiftType.NIGHT -> R.string.shift_night
    ShiftType.POST_NIGHT -> R.string.shift_post_night
    ShiftType.OFF -> R.string.shift_off
}

@StringRes
fun ShiftType.abbreviationResource(): Int = when (this) {
    ShiftType.MORNING -> R.string.shift_abbreviation_morning
    ShiftType.AFTERNOON -> R.string.shift_abbreviation_afternoon
    ShiftType.NIGHT -> R.string.shift_abbreviation_night
    ShiftType.POST_NIGHT -> R.string.shift_abbreviation_post_night
    ShiftType.OFF -> R.string.shift_abbreviation_off
}
