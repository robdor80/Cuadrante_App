package es.robertodorado.cuadrante.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import es.robertodorado.cuadrante.model.AppSettings
import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `DataStore saves and loads the complete custom configuration`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.root, "app-settings.preferences_pb")
        }
        val repository = AppSettingsRepository(dataStore)
        val expected = AppSettings(
            patternType = ShiftPatternType.CUSTOM,
            shifts = listOf(
                ShiftType.NIGHT,
                ShiftType.POST_NIGHT,
                ShiftType.OFF,
                ShiftType.MORNING,
            ),
            referenceDate = LocalDate.of(2028, 2, 29),
        )

        try {
            repository.save(expected)

            assertEquals(expected, repository.settings.first())
        } finally {
            scope.cancel()
        }
    }
}
