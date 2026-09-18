package es.robertodorado.cuadrante.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.robertodorado.cuadrante.model.AppSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val DATA_STORE_NAME = "app_settings"

val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
)

class AppSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings?> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppSettingsCodec.decode(
                patternTypeValue = preferences[Keys.patternType],
                shiftSequenceValue = preferences[Keys.shiftSequence],
                referenceDateValue = preferences[Keys.referenceDate],
            )
        }

    suspend fun save(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.patternType] = settings.patternType.name
            preferences[Keys.shiftSequence] = AppSettingsCodec.encodeShiftSequence(settings.shifts)
            preferences[Keys.referenceDate] = AppSettingsCodec.encodeDate(settings.referenceDate)
        }
    }

    private object Keys {
        val patternType = stringPreferencesKey("pattern_type")
        val shiftSequence = stringPreferencesKey("shift_sequence")
        val referenceDate = stringPreferencesKey("reference_date")
    }
}
