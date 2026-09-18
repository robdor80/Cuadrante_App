package es.robertodorado.cuadrante.model

import java.time.LocalDate

enum class IncidentType {
    AP,
    PERMISSION,
    VACATION,
}

sealed interface Incident {
    val id: String
    val type: IncidentType
    val firstDate: LocalDate
}

data class DateIncident(
    override val id: String,
    override val type: IncidentType,
    val dates: List<LocalDate>,
) : Incident {
    init {
        require(id.isNotBlank())
        require(type == IncidentType.AP || type == IncidentType.PERMISSION)
        require(dates.isNotEmpty())
        require(dates == dates.distinct().sorted())
    }

    override val firstDate: LocalDate
        get() = dates.first()
}

data class VacationIncident(
    override val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
) : Incident {
    init {
        require(id.isNotBlank())
        require(!endDate.isBefore(startDate))
    }

    override val type: IncidentType = IncidentType.VACATION
    override val firstDate: LocalDate
        get() = startDate
}
