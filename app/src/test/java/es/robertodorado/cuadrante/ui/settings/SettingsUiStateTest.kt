package es.robertodorado.cuadrante.ui.settings

import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun `empty custom sequence cannot be saved`() {
        val state = SettingsUiState(
            isLoading = false,
            patternType = ShiftPatternType.CUSTOM,
            customShifts = emptyList(),
        )

        assertFalse(state.canSave)
    }

    @Test
    fun `custom sequence can be saved after adding a shift`() {
        val state = SettingsUiState(
            isLoading = false,
            patternType = ShiftPatternType.CUSTOM,
            customShifts = listOf(ShiftType.MORNING),
        )

        assertTrue(state.canSave)
    }
}
