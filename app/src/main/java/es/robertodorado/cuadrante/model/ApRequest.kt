package es.robertodorado.cuadrante.model

import es.robertodorado.cuadrante.domain.ap.ApDay
import java.time.Instant
import java.time.LocalDate

data class ApRequest(
    override val id: String,
    val requestedAt: Instant?,
    val days: List<ApDay>,
    val legacy: Boolean = false,
) : Incident {
    init {
        require(id.isNotBlank() && days.isNotEmpty())
        require(days.map { it.id }.distinct().size == days.size)
    }
    override val type: IncidentType get() = IncidentType.AP
    override val firstDate: LocalDate get() = days.minOf { it.date }
}
