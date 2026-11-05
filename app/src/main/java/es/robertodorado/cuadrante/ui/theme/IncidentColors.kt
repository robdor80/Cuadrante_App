package es.robertodorado.cuadrante.ui.theme

import androidx.compose.ui.graphics.Color
import es.robertodorado.cuadrante.model.IncidentType

fun IncidentType.visualColor(): Color = when (this) {
    IncidentType.AP -> IncidentAp
    IncidentType.VACATION -> IncidentVacation
    IncidentType.PERMISSION -> IncidentPermission
}

/** Contrast on light and dark RobStyle surfaces; type/state remain available as text. */
fun es.robertodorado.cuadrante.domain.ap.ApStatus.statusColor(dark: Boolean): Color = when (this) {
    es.robertodorado.cuadrante.domain.ap.ApStatus.SOLICITADO -> if (dark) Color(0xFF8AB4F8) else Color(0xFF1565C0)
    es.robertodorado.cuadrante.domain.ap.ApStatus.CONCEDIDO -> if (dark) Color(0xFF81C995) else Color(0xFF146B32)
    es.robertodorado.cuadrante.domain.ap.ApStatus.DENEGADO -> if (dark) Color(0xFFF28B82) else Color(0xFFB3261E)
    es.robertodorado.cuadrante.domain.ap.ApStatus.CANCELADO -> Color.Transparent
}
