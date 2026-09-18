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
