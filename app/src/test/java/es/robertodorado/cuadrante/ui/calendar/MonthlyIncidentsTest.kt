package es.robertodorado.cuadrante.ui.calendar

import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.Incident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyIncidentsTest {
    @Test
    fun `single day AP belongs only to its month`() {
        val ap = dates("ap", IncidentType.AP, date(2026, 9, 21))

        assertEquals(listOf(ap), filter(2026, 9, ap).map { it.incident })
        assertTrue(filter(2026, 10, ap).isEmpty())
    }

    @Test
    fun `AP preserves several dates in the same month`() {
        val expectedDates = listOf(date(2026, 9, 5), date(2026, 9, 21), date(2026, 9, 29))
        val ap = dates("ap", IncidentType.AP, *expectedDates.toTypedArray())

        assertEquals(expectedDates, filter(2026, 9, ap).single().datesInMonth)
    }

    @Test
    fun `AP split between months exposes only relevant dates in each month`() {
        val septemberDate = date(2026, 9, 29)
        val octoberDate = date(2026, 10, 2)
        val ap = dates("ap", IncidentType.AP, septemberDate, octoberDate)

        assertEquals(listOf(septemberDate), filter(2026, 9, ap).single().datesInMonth)
        assertEquals(listOf(octoberDate), filter(2026, 10, ap).single().datesInMonth)
    }

    @Test
    fun `permission supports several arbitrary dates`() {
        val expectedDates = listOf(date(2026, 10, 3), date(2026, 10, 18))
        val permission = dates("permission", IncidentType.PERMISSION, *expectedDates.toTypedArray())

        assertEquals(expectedDates, filter(2026, 10, permission).single().datesInMonth)
    }

    @Test
    fun `vacation fully inside one month appears only there`() {
        val vacation = vacation("vacation", date(2026, 9, 20), date(2026, 9, 25))

        assertEquals(listOf(vacation), filter(2026, 9, vacation).map { it.incident })
        assertTrue(filter(2026, 10, vacation).isEmpty())
    }

    @Test
    fun `vacation crossing two months appears in both`() {
        val vacation = vacation("vacation", date(2026, 9, 28), date(2026, 10, 5))

        assertEquals(listOf(vacation), filter(2026, 9, vacation).map { it.incident })
        assertEquals(listOf(vacation), filter(2026, 10, vacation).map { it.incident })
        assertTrue(filter(2026, 11, vacation).isEmpty())
    }

    @Test
    fun `vacation crossing December and January appears in both years`() {
        val vacation = vacation("vacation", date(2026, 12, 28), date(2027, 1, 5))

        assertEquals(1, filter(2026, 12, vacation).size)
        assertEquals(1, filter(2027, 1, vacation).size)
        assertTrue(filter(2027, 2, vacation).isEmpty())
    }

    @Test
    fun `month without incidents returns an empty list`() {
        val ap = dates("ap", IncidentType.AP, date(2026, 9, 21))
        val vacation = vacation("vacation", date(2026, 10, 1), date(2026, 10, 5))

        assertTrue(incidentsForMonth(listOf(ap, vacation), YearMonth.of(2026, 11)).isEmpty())
    }

    @Test
    fun `entries are ordered by first relevant date rather than type`() {
        val lateAp = dates("late-ap", IncidentType.AP, date(2026, 9, 28))
        val permission = dates("permission", IncidentType.PERMISSION, date(2026, 9, 12))
        val vacation = vacation("vacation", date(2026, 9, 20), date(2026, 9, 25))
        val earlyAp = dates("early-ap", IncidentType.AP, date(2026, 9, 5))

        assertEquals(
            listOf("early-ap", "permission", "vacation", "late-ap"),
            incidentsForMonth(
                listOf(lateAp, vacation, permission, earlyAp),
                YearMonth.of(2026, 9),
            ).map { it.incident.id },
        )
    }

    @Test
    fun `changing YearMonth produces the corresponding list`() {
        val september = dates("september", IncidentType.AP, date(2026, 9, 24))
        val october = dates("october", IncidentType.PERMISSION, date(2026, 10, 10))
        val shared = vacation("shared", date(2026, 9, 28), date(2026, 10, 5))
        val incidents = listOf(september, october, shared)

        assertEquals(
            listOf("september", "shared"),
            incidentsForMonth(incidents, YearMonth.of(2026, 9)).map { it.incident.id },
        )
        assertEquals(
            listOf("shared", "october"),
            incidentsForMonth(incidents, YearMonth.of(2026, 10)).map { it.incident.id },
        )
    }

    @Test
    fun `AP marks its exact LocalDate`() {
        val target = date(2026, 10, 31)

        assertEquals(
            listOf(IncidentType.AP),
            markers(2026, 10, dates("ap", IncidentType.AP, target))[target],
        )
    }

    @Test
    fun `permission marks its exact LocalDate`() {
        val target = date(2026, 11, 15)

        assertEquals(
            listOf(IncidentType.PERMISSION),
            markers(2026, 11, dates("permission", IncidentType.PERMISSION, target))[target],
        )
    }

    @Test
    fun `vacation marks every date in its range`() {
        val vacation = vacation("vacation", date(2026, 9, 28), date(2026, 10, 2))
        val octoberMarkers = markers(2026, 10, vacation)

        assertEquals(listOf(date(2026, 10, 1), date(2026, 10, 2)), octoberMarkers.keys.toList())
        assertTrue(octoberMarkers.values.all { it == listOf(IncidentType.VACATION) })
    }

    @Test
    fun `dates outside vacation range are not marked`() {
        val vacation = vacation("vacation", date(2026, 10, 10), date(2026, 10, 12))
        val markers = markers(2026, 10, vacation)

        assertTrue(date(2026, 10, 9) !in markers)
        assertTrue(date(2026, 10, 13) !in markers)
    }

    @Test
    fun `multiple incidents on the same date produce multiple associated types`() {
        val target = date(2026, 10, 31)
        val ap = dates("ap", IncidentType.AP, target)
        val permission = dates("permission", IncidentType.PERMISSION, target)
        val vacation = vacation("vacation", date(2026, 10, 30), date(2026, 11, 2))

        assertEquals(
            listOf(IncidentType.AP, IncidentType.PERMISSION, IncidentType.VACATION),
            markers(2026, 10, ap, permission, vacation)[target],
        )
    }

    @Test
    fun `month without incidents produces no markers`() {
        val ap = dates("ap", IncidentType.AP, date(2026, 9, 21))

        assertTrue(markers(2026, 10, ap).isEmpty())
    }

    @Test
    fun `two separate AP records in one month are grouped`() {
        val groups = groups(
            2026,
            10,
            dates("ap-1", IncidentType.AP, date(2026, 10, 29)),
            dates("ap-2", IncidentType.AP, date(2026, 10, 31)),
        )

        assertEquals(1, groups.size)
        assertEquals(IncidentType.AP, groups.single().type)
    }

    @Test
    fun `grouped dates are chronological`() {
        val group = groups(
            2026,
            10,
            dates("late", IncidentType.AP, date(2026, 10, 31)),
            dates("early", IncidentType.AP, date(2026, 10, 5)),
        ).single()

        assertEquals(listOf(date(2026, 10, 5), date(2026, 10, 31)), group.datesInMonth)
    }

    @Test
    fun `multi-date AP and independent AP merge for presentation`() {
        val group = groups(
            2026,
            10,
            dates("multi", IncidentType.AP, date(2026, 10, 29), date(2026, 10, 30)),
            dates("single", IncidentType.AP, date(2026, 10, 31)),
        ).single()

        assertEquals(
            listOf(date(2026, 10, 29), date(2026, 10, 30), date(2026, 10, 31)),
            group.datesInMonth,
        )
    }

    @Test
    fun `separate permissions are grouped`() {
        val group = groups(
            2026,
            10,
            dates("permission-1", IncidentType.PERMISSION, date(2026, 10, 17)),
            dates("permission-2", IncidentType.PERMISSION, date(2026, 10, 8)),
        ).single()

        assertEquals(IncidentType.PERMISSION, group.type)
        assertEquals(listOf(date(2026, 10, 8), date(2026, 10, 17)), group.datesInMonth)
    }

    @Test
    fun `separate vacation ranges are grouped chronologically`() {
        val first = vacation("vacation-1", date(2026, 10, 2), date(2026, 10, 6))
        val second = vacation("vacation-2", date(2026, 10, 21), date(2026, 10, 25))
        val group = groups(2026, 10, second, first).single()

        assertEquals(IncidentType.VACATION, group.type)
        assertEquals(listOf(first, second), group.vacationRanges)
    }

    @Test
    fun `monthly groups do not mix dates from different months`() {
        val ap = dates(
            "ap",
            IncidentType.AP,
            date(2026, 9, 30),
            date(2026, 10, 1),
        )

        assertEquals(listOf(date(2026, 9, 30)), groups(2026, 9, ap).single().datesInMonth)
        assertEquals(listOf(date(2026, 10, 1)), groups(2026, 10, ap).single().datesInMonth)
    }

    @Test
    fun `grouping removes duplicate dates`() {
        val repeatedDate = date(2026, 10, 29)
        val group = groups(
            2026,
            10,
            dates("ap-1", IncidentType.AP, repeatedDate),
            dates("ap-2", IncidentType.AP, repeatedDate),
        ).single()

        assertEquals(listOf(repeatedDate), group.datesInMonth)
    }

    private fun filter(year: Int, month: Int, vararg incidents: Incident): List<MonthlyIncidentEntry> =
        incidentsForMonth(incidents.toList(), YearMonth.of(year, month))

    private fun markers(year: Int, month: Int, vararg incidents: Incident) =
        incidentTypesByDate(incidents.toList(), YearMonth.of(year, month))

    private fun groups(year: Int, month: Int, vararg incidents: Incident) =
        groupMonthlyIncidents(filter(year, month, *incidents))

    private fun dates(id: String, type: IncidentType, vararg dates: LocalDate) =
        DateIncident(id, type, dates.toList().sorted())

    private fun vacation(id: String, start: LocalDate, end: LocalDate) =
        VacationIncident(id, start, end)

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)
}
