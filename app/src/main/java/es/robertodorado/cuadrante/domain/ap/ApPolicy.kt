package es.robertodorado.cuadrante.domain.ap

import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.*
import java.time.Instant
import java.time.LocalDate

/** AP_POLICY_V1. Dates refer to the start of the original shift, including NIGHT. */
enum class ApStatus { SOLICITADO, CONCEDIDO, DENEGADO, CANCELADO;
    val consumes: Boolean get() = this == SOLICITADO || this == CONCEDIDO
}

data class ApPool(val year: Int, val quota: Int = 8, val extensionUntil: LocalDate? = null) {
    init {
        require(year in LocalDate.MIN.year..LocalDate.MAX.year) { "Año de bolsa no válido" }
        require(quota >= 8) { "La cuota mínima es 8 AP" }
        require(extensionUntil == null || (extensionUntil.year > year && extensionUntil.isAfter(LocalDate.of(year + 1, 1, 1)))) {
            "La prórroga debe prolongar la bolsa más allá del 1 de enero siguiente"
        }
    }
    fun expiredOn(date: LocalDate): Boolean = extensionUntil?.let { !date.isBefore(it) } ?: (date.year > year)
    fun accepts(date: LocalDate): Boolean = date.year == year ||
        (date.year > year && extensionUntil?.let { date.isBefore(it) } == true)
}

data class ApEvent(
    val at: Instant,
    val action: String,
    val date: LocalDate,
    val status: ApStatus,
    val poolYear: Int,
    val resolvedAt: Instant?,
    val notes: String?,
    val reason: String?,
)

data class ApDay(
    val id: String,
    val date: LocalDate,
    val status: ApStatus,
    val poolYear: Int,
    val resolvedAt: Instant? = null,
    val notes: String? = null,
    val reason: String? = null,
    val history: List<ApEvent> = emptyList(),
) {
    fun recorded(at: Instant, action: String): ApDay = copy(history = history + ApEvent(
        at, action, date, status, poolYear, resolvedAt, notes, reason,
    ))
}


data class ApState(val pools: List<ApPool> = emptyList(), val requests: List<ApRequest> = emptyList()) {
    val days: List<ApDay> get() = requests.flatMap { it.days }
}

data class ApBalance(
    val pool: ApPool,
    val requested: Int,
    val granted: Int,
    val released: Int,
    val available: Int,
    val extended: Int,
    val expired: Int,
)

fun ApState.balances(today: LocalDate): List<ApBalance> = pools.sortedBy { it.year }.map { pool ->
    val assigned = days.filter { it.poolYear == pool.year }
    val requested = assigned.count { it.status == ApStatus.SOLICITADO }
    val granted = assigned.count { it.status == ApStatus.CONCEDIDO }
    val remaining = pool.quota - requested - granted
    val expired = pool.expiredOn(today)
    ApBalance(pool, requested, granted, assigned.count { !it.status.consumes },
        if (expired) 0 else remaining,
        if (!expired && today.year > pool.year && pool.extensionUntil != null) remaining.coerceAtLeast(0) else 0,
        if (expired) remaining.coerceAtLeast(0) else 0)
}

fun isApEligible(calculator: ShiftCalculator, date: LocalDate): Boolean =
    calculator.shiftFor(date) in setOf(ShiftType.MORNING, ShiftType.AFTERNOON, ShiftType.NIGHT)

fun Incident.blocks(date: LocalDate): Boolean = when (this) {
    is DateIncident -> date in dates
    is VacationIncident -> !date.isBefore(startDate) && !date.isAfter(endDate)
    is ApRequest -> days.any { it.date == date && it.status.consumes }
}

data class ApSelection(val id: String, val date: LocalDate, val poolYear: Int?, val notes: String? = null)
sealed interface ApChange {
    data class Create(val id: String, val selections: List<ApSelection>) : ApChange
    /** Includes all remaining pending dates. Removed dates become CANCELADO, retaining their IDs. */
    data class Edit(val requestId: String, val selections: List<ApSelection>) : ApChange
    data class Resolve(val dayId: String, val status: ApStatus, val notes: String? = null, val reason: String? = null) : ApChange
    data class Reassign(val dayId: String, val poolYear: Int) : ApChange
    data class ConfigurePool(val pool: ApPool) : ApChange
}

class ApValidationException(val problems: List<String>) : IllegalArgumentException(problems.joinToString("\n"))

/** Pure all-or-nothing reducer. No UI, clock, random IDs or persistence dependencies. */
object ApPolicy {
    fun apply(state: ApState, others: List<Incident>, calculator: ShiftCalculator, change: ApChange, at: Instant): ApState {
        val problems = mutableListOf<String>()
        fun fail(message: String): Nothing = throw ApValidationException(listOf(message))
        fun selection(s: ApSelection, old: ApDay? = null): ApDay {
            val year = s.poolYear ?: Int.MIN_VALUE
            return ApDay(s.id, s.date, ApStatus.SOLICITADO, year, notes = s.notes, history = old?.history.orEmpty())
                .recorded(at, if (old == null) "SOLICITUD" else "EDICIÓN")
        }
        fun updateDay(id: String, transform: (ApDay) -> ApDay): ApState {
            if (state.days.none { it.id == id }) fail("AP desconocido")
            return state.copy(requests = state.requests.map { request ->
                request.copy(days = request.days.map { if (it.id == id) transform(if (it.history.isEmpty()) it.recorded(at, "ESTADO PREVIO IMPORTADO") else it) else it })
            })
        }
        val candidate = when (change) {
            is ApChange.Create -> {
                if (change.selections.isEmpty()) fail("Selecciona al menos una fecha")
                if (state.requests.any { it.id == change.id }) fail("Solicitud duplicada")
                state.copy(requests = state.requests + ApRequest(change.id, at, change.selections.map { selection(it) }))
            }
            is ApChange.Edit -> {
                val request = state.requests.find { it.id == change.requestId } ?: fail("Solicitud desconocida")
                val pending = request.days.filter { it.status == ApStatus.SOLICITADO }
                if (pending.isEmpty()) fail("La solicitud no tiene fechas pendientes")
                if (change.selections.any { s -> state.days.any { it.id == s.id } && pending.none { it.id == s.id } })
                    fail("No se pueden editar fechas resueltas ni IDs de otra solicitud")
                val selectedIds = change.selections.map { it.id }.toSet()
                val retained = request.days.filter { it.status != ApStatus.SOLICITADO } + pending
                    .filter { it.id !in selectedIds }.map {
                        it.copy(status = ApStatus.CANCELADO, resolvedAt = at).recorded(at, "RETIRADA EN EDICIÓN")
                    }
                state.copy(requests = state.requests.map {
                    if (it.id != request.id) it else it.copy(days = retained + change.selections.map { s ->
                        selection(s, pending.find { day -> day.id == s.id })
                    })
                })
            }
            is ApChange.Resolve -> updateDay(change.dayId) { old ->
                val allowed = when (old.status) {
                    ApStatus.SOLICITADO -> setOf(ApStatus.CONCEDIDO, ApStatus.DENEGADO, ApStatus.CANCELADO)
                    ApStatus.CONCEDIDO -> setOf(ApStatus.CANCELADO)
                    else -> emptySet()
                }
                if (change.status !in allowed) fail("${old.date}: transición de estado no válida")
                old.copy(status = change.status, resolvedAt = at, notes = change.notes, reason = change.reason)
                    .recorded(at, "RESOLUCIÓN")
            }
            is ApChange.Reassign -> updateDay(change.dayId) { old ->
                if (!old.status.consumes) fail("${old.date}: solo se cambia bolsa en SOLICITADO o CONCEDIDO")
                old.copy(poolYear = change.poolYear).recorded(at, "CAMBIO DE BOLSA")
            }
            is ApChange.ConfigurePool -> state.copy(pools = state.pools.filterNot { it.year == change.pool.year } + change.pool)
        }
        if (candidate.requests.map { it.id }.distinct().size != candidate.requests.size ||
            candidate.days.map { it.id }.distinct().size != candidate.days.size) fail("Identificador AP duplicado")
        // Validate changed consuming dates only: imported historical shifts are never rewritten or reinterpreted.
        val changed = candidate.days.filter { day -> day.status.consumes && state.days.find { it.id == day.id } != day }
        changed.forEach { day ->
            val prefix = "${day.date}: "
            if (!isApEligible(calculator, day.date))
                problems += prefix + if (calculator.shiftFor(day.date) == ShiftType.OFF) "día Libre" else "Saliente"
            if (candidate.days.any { it.id != day.id && it.date == day.date && it.status.consumes })
                problems += prefix + "AP duplicado / solape con AP"
            if (others.any { it.blocks(day.date) }) problems += prefix + "solapamiento con otra incidencia"
            val pool = candidate.pools.find { it.year == day.poolYear }
            if (pool == null) problems += prefix + if (day.poolYear == Int.MIN_VALUE) "elige expresamente una bolsa" else "bolsa no configurada"
            else if (!pool.accepts(day.date)) problems += prefix + "bolsa no válida para esa fecha / prórroga vencida"
        }
        if (change is ApChange.ConfigurePool) candidate.days.filter { it.status.consumes && it.poolYear == change.pool.year }.forEach {
            if (!change.pool.accepts(it.date)) problems += "${it.date}: la nueva prórroga no cubre un AP reservado"
        }
        candidate.pools.forEach { pool ->
            val consumed = candidate.days.count { it.poolYear == pool.year && it.status.consumes }
            val previous = state.days.count { it.poolYear == pool.year && it.status.consumes }
            // Legacy over-quota data is retained; new reservations cannot make the deficit worse.
            if (consumed > pool.quota && (consumed > previous || (change is ApChange.ConfigurePool && change.pool.year == pool.year))) {
                val dates = changed.filter { it.poolYear == pool.year }.joinToString { it.date.toString() }
                problems += "$dates: bolsa ${pool.year} sin saldo"
            }
        }
        if (problems.isNotEmpty()) throw ApValidationException(problems.distinct())
        return candidate
    }
}

enum class Confirmation { CONFIRM, CANCEL, UNKNOWN }
fun parseVoiceConfirmation(texts: List<String>): Confirmation {
    val decisions = texts.map { it.trim().lowercase(java.util.Locale.ROOT) }.map {
        when (it) {
            "confirmar", "confirmo" -> Confirmation.CONFIRM
            "cancelar", "cancelo" -> Confirmation.CANCEL
            else -> Confirmation.UNKNOWN
        }
    }.toSet()
    return if (decisions.size == 1) decisions.first() else Confirmation.UNKNOWN
}
