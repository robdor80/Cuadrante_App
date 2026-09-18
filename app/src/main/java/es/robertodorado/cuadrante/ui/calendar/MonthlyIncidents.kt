package es.robertodorado.cuadrante.ui.calendar

import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.Incident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import java.time.LocalDate
import java.time.YearMonth

data class MonthlyIncidentEntry(
    val incident: Incident,
    val datesInMonth: List<LocalDate>,
    val firstRelevantDate: LocalDate,
)

data class MonthlyIncidentGroup(
    val type: IncidentType,
    val datesInMonth: List<LocalDate>,
    val vacationRanges: List<VacationIncident>,
)

fun incidentsForMonth(
    incidents: List<Incident>,
    yearMonth: YearMonth,
): List<MonthlyIncidentEntry> {
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()

    return incidents.mapNotNull { incident ->
        when (incident) {
            is DateIncident -> {
                val datesInMonth = incident.dates.filter { YearMonth.from(it) == yearMonth }
                datesInMonth.takeIf(List<LocalDate>::isNotEmpty)?.let { dates ->
                    MonthlyIncidentEntry(
                        incident = incident,
                        datesInMonth = dates,
                        firstRelevantDate = dates.first(),
                    )
                }
            }
            is VacationIncident -> {
                val overlapsMonth = !incident.startDate.isAfter(monthEnd) &&
                    !incident.endDate.isBefore(monthStart)
                if (overlapsMonth) {
                    MonthlyIncidentEntry(
                        incident = incident,
                        datesInMonth = emptyList(),
                        firstRelevantDate = maxOf(incident.startDate, monthStart),
                    )
                } else {
                    null
                }
            }
        }
    }.sortedBy(MonthlyIncidentEntry::firstRelevantDate)
}

fun groupMonthlyIncidents(
    entries: List<MonthlyIncidentEntry>,
): List<MonthlyIncidentGroup> {
    val entriesByType = entries.groupBy { it.incident.type }

    return MONTHLY_INCIDENT_TYPE_ORDER.mapNotNull { type ->
        val typeEntries = entriesByType[type].orEmpty()
        if (typeEntries.isEmpty()) return@mapNotNull null

        MonthlyIncidentGroup(
            type = type,
            datesInMonth = typeEntries
                .flatMap(MonthlyIncidentEntry::datesInMonth)
                .distinct()
                .sorted(),
            vacationRanges = typeEntries
                .mapNotNull { it.incident as? VacationIncident }
                .distinctBy { it.startDate to it.endDate }
                .sortedWith(compareBy(VacationIncident::startDate, VacationIncident::endDate)),
        )
    }
}

fun incidentTypesByDate(
    incidents: List<Incident>,
    yearMonth: YearMonth,
): Map<LocalDate, List<IncidentType>> {
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()
    val typesByDate = mutableMapOf<LocalDate, MutableList<IncidentType>>()

    incidents.forEach { incident ->
        val affectedDates = when (incident) {
            is DateIncident -> incident.dates.filter { YearMonth.from(it) == yearMonth }
            is VacationIncident -> {
                val firstDate = maxOf(incident.startDate, monthStart)
                val lastDate = minOf(incident.endDate, monthEnd)
                if (firstDate.isAfter(lastDate)) {
                    emptyList()
                } else {
                    generateSequence(firstDate) { date ->
                        if (date == lastDate) null else date.plusDays(1)
                    }.toList()
                }
            }
        }
        affectedDates.forEach { date ->
            typesByDate.getOrPut(date, ::mutableListOf).add(incident.type)
        }
    }

    return typesByDate.mapValues { (_, types) -> types.distinct() }
}

private val MONTHLY_INCIDENT_TYPE_ORDER = listOf(
    IncidentType.AP,
    IncidentType.PERMISSION,
    IncidentType.VACATION,
)
