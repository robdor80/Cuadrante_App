package es.robertodorado.cuadrante.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDatePickerConversionTest {
    @Test
    fun `leap date round trip has no day displacement`() {
        val date = LocalDate.of(2028, 2, 29)

        assertEquals(date, date.toDatePickerMillis().toLocalDateFromDatePicker())
    }

    @Test
    fun `date before Unix epoch round trip has no day displacement`() {
        val date = LocalDate.of(1960, 1, 2)

        assertEquals(date, date.toDatePickerMillis().toLocalDateFromDatePicker())
    }
}
