package es.robertodorado.cuadrante.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.domain.ap.*
import es.robertodorado.cuadrante.model.*
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A reviewable immutable proposal bound to the exact data/settings it summarized. */
class ApProposal internal constructor(
    val change: ApChange,
    val before: ApState,
    val after: ApState,
    val settings: AppSettings,
    internal val rawLegacy: String?,
    internal val rawAp: String?,
    val at: Instant,
)

class ApRepository(
    private val dataStore: DataStore<Preferences>,
    private val settings: AppSettingsRepository,
) {
    val state = dataStore.data.map { readState(it) }
    val incidents = dataStore.data.map { p ->
        val legacy = IncidentsCodec.decode(p[legacyKey])
        legacy.filterNot { it.type == IncidentType.AP } + readState(p).requests
    }
    suspend fun preview(change: ApChange, at: Instant): ApProposal {
        val p = dataStore.data.first()
        val saved = requireNotNull(settings.settings.first()) { "Guarda antes la configuración de turnos" }
        val before = readState(p)
        return ApProposal(change, before, ApPolicy.apply(before, otherIncidents(p), calculator(saved), change, at),
            saved, p[legacyKey], p[apKey], at)
    }
    suspend fun confirm(proposal: ApProposal, confirmation: Confirmation) {
        require(confirmation == Confirmation.CONFIRM) { "Falta confirmación explícita" }
        settings.withCurrentSettings { saved ->
            dataStore.edit { p ->
                require(p[legacyKey] == proposal.rawLegacy && p[apKey] == proposal.rawAp && saved == proposal.settings) {
                    "Los datos o turnos han cambiado. Revisa y confirma un resumen nuevo."
                }
                val after = ApPolicy.apply(readState(p), otherIncidents(p), calculator(proposal.settings), proposal.change, proposal.at)
                require(after == proposal.after) { "El resumen ya no está vigente" }
                p[apKey] = ApCodec.encode(after)
            }
        }
    }

    private fun readState(p: Preferences): ApState = ApCodec.importLegacy(ApCodec.decode(p[apKey]), IncidentsCodec.decode(p[legacyKey]))
    private fun otherIncidents(p: Preferences) = IncidentsCodec.decode(p[legacyKey]).filterNot { it.type == IncidentType.AP }
    private fun calculator(s: AppSettings) = ShiftCalculator(s.referenceDate, s.shifts)
    companion object {
        val apKey = stringPreferencesKey("ap_policy_v1")
        val legacyKey = stringPreferencesKey("incidents_v1")
    }
}
