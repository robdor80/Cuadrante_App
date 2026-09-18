package es.robertodorado.cuadrante.data.local

import es.robertodorado.cuadrante.model.AppSettings
import es.robertodorado.cuadrante.model.DefaultShiftPatterns
import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AppSettingsCodecTest {
    @Test
    fun `shift sequence round trip preserves every item and its order`() {
        val shifts = listOf(
            ShiftType.OFF,
            ShiftType.MORNING,
            ShiftType.NIGHT,
            ShiftType.POST_NIGHT,
            ShiftType.AFTERNOON,
            ShiftType.MORNING,
        )

        val encoded = AppSettingsCodec.encodeShiftSequence(shifts)

        assertEquals(
            "OFF,MORNING,NIGHT,POST_NIGHT,AFTERNOON,MORNING",
            encoded,
        )
        assertEquals(shifts, AppSettingsCodec.decodeShiftSequence(encoded))
    }

    @Test
    fun `date is stored and restored as ISO LocalDate`() {
        val date = LocalDate.of(2028, 2, 29)

        val encoded = AppSettingsCodec.encodeDate(date)

        assertEquals("2028-02-29", encoded)
        assertEquals(date, AppSettingsCodec.decodeDate(encoded))
    }

    @Test
    fun `complete settings decode restores six by six configuration`() {
        val expected = AppSettings(
            patternType = ShiftPatternType.SIX_BY_SIX,
            shifts = DefaultShiftPatterns.SIX_BY_SIX.shifts,
            referenceDate = LocalDate.of(2026, 9, 18),
        )

        val decoded = AppSettingsCodec.decode(
            patternTypeValue = expected.patternType.name,
            shiftSequenceValue = AppSettingsCodec.encodeShiftSequence(expected.shifts),
            referenceDateValue = AppSettingsCodec.encodeDate(expected.referenceDate),
        )

        assertEquals(expected, decoded)
    }

    @Test
    fun `unknown persisted shift is rejected safely`() {
        assertNull(AppSettingsCodec.decodeShiftSequence("MORNING,UNKNOWN,OFF"))
    }

    @Test
    fun `invalid persisted date is rejected safely`() {
        assertNull(AppSettingsCodec.decodeDate("2026-02-29"))
    }

    @Test
    fun `empty custom configuration is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            AppSettings(
                patternType = ShiftPatternType.CUSTOM,
                shifts = emptyList(),
                referenceDate = LocalDate.of(2026, 9, 18),
            )
        }
    }

    @Test
    fun `incomplete persisted settings are ignored`() {
        assertNull(
            AppSettingsCodec.decode(
                patternTypeValue = ShiftPatternType.CUSTOM.name,
                shiftSequenceValue = null,
                referenceDateValue = "2026-09-18",
            ),
        )
    }
}
