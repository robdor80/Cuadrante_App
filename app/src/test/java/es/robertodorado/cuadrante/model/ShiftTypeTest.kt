package es.robertodorado.cuadrante.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftTypeTest {
    @Test
    fun `contains language independent shift identifiers`() {
        assertEquals(
            listOf("MORNING", "AFTERNOON", "NIGHT", "POST_NIGHT", "OFF"),
            ShiftType.entries.map(ShiftType::name),
        )
    }
}
