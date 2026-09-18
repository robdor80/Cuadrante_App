package es.robertodorado.cuadrante.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IncidentsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `repository adds orders and deletes persisted incidents`() = runBlocking {
        val repository = IncidentsRepository(InMemoryPreferencesDataStore())
        val ap = DateIncident(
            id = "ap",
            type = IncidentType.AP,
            dates = listOf(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14)),
        )
        val permission = DateIncident(
            id = "permission",
            type = IncidentType.PERMISSION,
            dates = listOf(LocalDate.of(2026, 9, 24)),
        )
        val vacation = VacationIncident(
            id = "vacation",
            startDate = LocalDate.of(2026, 12, 28),
            endDate = LocalDate.of(2027, 1, 5),
        )

        repository.add(vacation)
        repository.add(ap)
        repository.add(permission)

        assertEquals(listOf(permission, ap, vacation), repository.incidents.first())

        repository.delete(ap.id)

        assertEquals(listOf(permission, vacation), repository.incidents.first())
    }

    @Test
    fun `DataStore persists an incident`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.root, "incidents.preferences_pb")
        }
        val repository = IncidentsRepository(dataStore)
        val expected = VacationIncident(
            id = "persisted",
            startDate = LocalDate.of(2026, 12, 28),
            endDate = LocalDate.of(2027, 1, 5),
        )

        try {
            repository.add(expected)
            assertEquals(listOf(expected), repository.incidents.first())
        } finally {
            scope.cancel()
        }
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
