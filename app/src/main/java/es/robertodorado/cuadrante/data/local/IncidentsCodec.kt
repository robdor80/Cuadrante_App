package es.robertodorado.cuadrante.data.local

import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.Incident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import java.time.LocalDate

/** Stable, locale-independent representation for locally persisted incidents. */
object IncidentsCodec {
    private const val FIELD_SEPARATOR = "|"
    private const val DATE_SEPARATOR = ","
    private const val DATE_RECORD = "D"
    private const val RANGE_RECORD = "R"

    fun encode(incidents: List<Incident>): String = incidents.joinToString("\n", transform = ::encodeIncident)

    fun decode(value: String?): List<Incident> {
        if (value.isNullOrBlank()) return emptyList()
        return value.lineSequence().mapNotNull(::decodeIncident).toList()
    }

    private fun encodeIncident(incident: Incident): String = when (incident) {
        is DateIncident -> listOf(
            DATE_RECORD,
            incident.id,
            incident.type.name,
            incident.dates.joinToString(DATE_SEPARATOR),
        ).joinToString(FIELD_SEPARATOR)
        is VacationIncident -> listOf(
            RANGE_RECORD,
            incident.id,
            incident.type.name,
            incident.startDate,
            incident.endDate,
        ).joinToString(FIELD_SEPARATOR)
    }

    private fun decodeIncident(record: String): Incident? = runCatching {
        val fields = record.split(FIELD_SEPARATOR)
        when (fields.firstOrNull()) {
            DATE_RECORD -> {
                require(fields.size == 4)
                DateIncident(
                    id = fields[1],
                    type = IncidentType.valueOf(fields[2]),
                    dates = fields[3].split(DATE_SEPARATOR).map(LocalDate::parse).distinct().sorted(),
                )
            }
            RANGE_RECORD -> {
                require(fields.size == 5)
                require(IncidentType.valueOf(fields[2]) == IncidentType.VACATION)
                VacationIncident(
                    id = fields[1],
                    startDate = LocalDate.parse(fields[3]),
                    endDate = LocalDate.parse(fields[4]),
                )
            }
            else -> error("Unknown incident record")
        }
    }.getOrNull()
}
