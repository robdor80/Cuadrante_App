package es.robertodorado.cuadrante.ui.theme

import androidx.compose.ui.graphics.Color
import es.robertodorado.cuadrante.model.ShiftType

data class ShiftVisualColors(
    val background: Color,
    val foreground: Color,
)

fun ShiftType.visualColors(): ShiftVisualColors = when (this) {
    ShiftType.MORNING -> ShiftVisualColors(ShiftMorning, OnShiftLight)
    ShiftType.AFTERNOON -> ShiftVisualColors(ShiftAfternoon, OnShiftAfternoon)
    ShiftType.NIGHT -> ShiftVisualColors(ShiftNight, OnShiftLight)
    ShiftType.POST_NIGHT -> ShiftVisualColors(ShiftPostNight, OnShiftLight)
    ShiftType.OFF -> ShiftVisualColors(ShiftOff, OnShiftLight)
}
