package es.robertodorado.cuadrante.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        dataStore.edit { preferences ->
            val current = IncidentsCodec.decode(preferences[INCIDENTS_KEY])
            preferences[INCIDENTS_KEY] = IncidentsCodec.encode(
                (current.filterNot { it.id == incident.id } + incident).sortedBy(Incident::firstDate),
            )
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            val updated = IncidentsCodec.decode(preferences[INCIDENTS_KEY]).filterNot { it.id == id }
            preferences[INCIDENTS_KEY] = IncidentsCodec.encode(updated)
        }
    }

    private companion object {
        val INCIDENTS_KEY = stringPreferencesKey("incidents_v1")
    }
}
