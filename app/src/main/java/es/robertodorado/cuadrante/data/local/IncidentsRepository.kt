package es.robertodorado.cuadrante.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.robertodorado.cuadrante.domain.ap.blocks
import es.robertodorado.cuadrante.model.*
import es.robertodorado.cuadrante.model.Incident
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val INCIDENTS_DATA_STORE_NAME = "incidents"

val Context.incidentsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = INCIDENTS_DATA_STORE_NAME,
)

class IncidentsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val incidents: Flow<List<Incident>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            IncidentsCodec.decode(preferences[INCIDENTS_KEY]).sortedBy(Incident::firstDate)
        }

    suspend fun add(incident: Incident) {
        require(incident.type != IncidentType.AP) { "Usa AP_POLICY_V1 para solicitar AP" }
        dataStore.edit { preferences ->
            val raw = preferences[INCIDENTS_KEY]
            val current = IncidentsCodec.decode(raw)
            val ap = ApCodec.importLegacy(ApCodec.decode(preferences[ApRepository.apKey]), current)
            require(current.none { it.id == incident.id && (it.type == IncidentType.AP || it.type != incident.type) } &&
                ap.requests.none { it.id == incident.id }) { "El ID pertenece a otra incidencia" }
            val dates = when (incident) {
                is DateIncident -> incident.dates.asSequence()
                is VacationIncident -> generateSequence(incident.startDate) { if (it == incident.endDate) null else it.plusDays(1) }
                is ApRequest -> error("Tipo incompatible")
            }
            require(dates.none { date -> ap.days.any { it.date == date && it.status.consumes } ||
                current.any { it.id != incident.id && it.type != IncidentType.AP && it.blocks(date) } }) {
                "Solapamiento con una incidencia existente"
            }
            val preserved = raw?.lineSequence()?.filter { line ->
                IncidentsCodec.decode(line).none { it.id == incident.id }
            }.orEmpty().toList()
            preferences[INCIDENTS_KEY] = (preserved + IncidentsCodec.encode(listOf(incident))).joinToString("\n")
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            require(IncidentsCodec.decode(preferences[INCIDENTS_KEY]).none { it.id == id && it.type == IncidentType.AP }) {
                "Un AP solo puede cancelarse conservando histórico"
            }
            preferences[INCIDENTS_KEY] = preferences[INCIDENTS_KEY].orEmpty().lineSequence().filter { line ->
                IncidentsCodec.decode(line).none { it.id == id }
            }.joinToString("\n")
        }
    }

    private companion object {
        val INCIDENTS_KEY = stringPreferencesKey("incidents_v1")
    }
}
