package es.robertodorado.cuadrante.ui.theme

import androidx.compose.ui.graphics.Color
import es.robertodorado.cuadrante.model.IncidentType

fun IncidentType.visualColor(): Color = when (this) {
    IncidentType.AP -> IncidentAp
    IncidentType.VACATION -> IncidentVacation
    IncidentType.PERMISSION -> IncidentPermission
}
