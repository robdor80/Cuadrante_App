package es.robertodorado.cuadrante.data.local

import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentsCodecTest {
    @Test
    fun `AP with arbitrary non consecutive dates survives round trip`() {
        val incident = DateIncident(
            id = "ap-1",
            type = IncidentType.AP,
            dates = listOf(
                LocalDate.of(2026, 9, 24),
                LocalDate.of(2026, 9, 26),
                LocalDate.of(2026, 10, 2),
            ),
        )

        assertEquals(listOf(incident), IncidentsCodec.decode(IncidentsCodec.encode(listOf(incident))))
    }

    @Test
    fun `permission preserves multiple selected dates`() {
        val incident = DateIncident(
            id = "permission-1",
            type = IncidentType.PERMISSION,
            dates = listOf(LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 18)),
        )

        assertEquals(listOf(incident), IncidentsCodec.decode(IncidentsCodec.encode(listOf(incident))))
    }

    @Test
    fun `vacation range can cross month and year`() {
        val vacation = VacationIncident(
            id = "vacation-1",
            startDate = LocalDate.of(2026, 12, 28),
            endDate = LocalDate.of(2027, 1, 5),
        )

        assertEquals(listOf(vacation), IncidentsCodec.decode(IncidentsCodec.encode(listOf(vacation))))
    }

    @Test
    fun `invalid stored records are ignored without losing valid records`() {
        val vacation = VacationIncident(
            id = "vacation-2",
            startDate = LocalDate.of(2026, 9, 28),
            endDate = LocalDate.of(2026, 10, 5),
        )
        val stored = IncidentsCodec.encode(listOf(vacation)) + "\ninvalid record"

        assertEquals(listOf(vacation), IncidentsCodec.decode(stored))
        assertTrue(IncidentsCodec.decode(null).isEmpty())
    }
}
