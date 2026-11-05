package es.robertodorado.cuadrante.data.local

import es.robertodorado.cuadrante.domain.ap.*
import es.robertodorado.cuadrante.model.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

/** Versioned sidecar. Unknown/corrupt versions fail closed: no fallback that could overwrite data. */
object ApCodec {
    fun encode(state: ApState): String {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(1)
            out.writeInt(state.pools.size)
            state.pools.forEach { p -> out.writeInt(p.year); out.writeInt(p.quota); out.nullable(p.extensionUntil?.toString()) }
            out.writeInt(state.requests.size)
            state.requests.forEach { r ->
                out.text(r.id); out.nullable(r.requestedAt?.toString()); out.writeBoolean(r.legacy)
                out.writeInt(r.days.size)
                r.days.forEach { d ->
                    out.text(d.id); out.text(d.date.toString()); out.text(d.status.name); out.writeInt(d.poolYear)
                    out.nullable(d.resolvedAt?.toString()); out.nullable(d.notes); out.nullable(d.reason)
                    out.writeInt(d.history.size)
                    d.history.forEach { e ->
                        out.text(e.at.toString()); out.text(e.action); out.text(e.date.toString())
                        out.text(e.status.name); out.writeInt(e.poolYear); out.nullable(e.resolvedAt?.toString())
                        out.nullable(e.notes); out.nullable(e.reason)
                    }
                }
            }
        }
        return Base64.getEncoder().encodeToString(buffer.toByteArray())
    }

    fun decode(value: String?): ApState {
        if (value == null) return ApState()
        return DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(value))).use { input ->
            require(input.readInt() == 1) { "Versión AP desconocida; datos conservados" }
            val pools = List(input.count()) { ApPool(input.readInt(), input.readInt(), input.nullable()?.let(LocalDate::parse)) }
            val requests = List(input.count()) {
                val id = input.text(); val requested = input.nullable()?.let(Instant::parse); val legacy = input.readBoolean()
                val days = List(input.count()) {
                    val dayId = input.text(); val date = LocalDate.parse(input.text())
                    val status = ApStatus.valueOf(input.text()); val year = input.readInt()
                    val resolved = input.nullable()?.let(Instant::parse); val notes = input.nullable(); val reason = input.nullable()
                    val history = List(input.count()) {
                        ApEvent(Instant.parse(input.text()), input.text(), LocalDate.parse(input.text()),
                            ApStatus.valueOf(input.text()), input.readInt(), input.nullable()?.let(Instant::parse),
                            input.nullable(), input.nullable())
                    }
                    ApDay(dayId, date, status, year, resolved, notes, reason, history)
                }
                ApRequest(id, requested, days, legacy)
            }
            require(input.available() == 0) { "Ampliación AP desconocida; datos conservados" }
            require(pools.distinctBy { it.year }.size == pools.size)
            require(requests.distinctBy { it.id }.size == requests.size)
            val ids = requests.flatMap { it.days }.map { it.id }
            require(ids.distinct().size == ids.size)
            ApState(pools, requests)
        }
    }
    private fun DataOutputStream.nullable(value: String?) { writeBoolean(value != null); if (value != null) text(value) }
    private fun DataInputStream.nullable(): String? = if (readBoolean()) text() else null
    private fun DataInputStream.count(): Int = readInt().also { require(it >= 0 && it <= available()) }
    private fun DataOutputStream.text(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size); write(bytes)
    }
    private fun DataInputStream.text(): String {
        val bytes = ByteArray(count()); readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    /** Deterministic lazy import. Original incidents_v1 bytes stay intact, including unknown records. */
    fun importLegacy(state: ApState, legacy: List<Incident>): ApState {
        val imported = legacy.filterIsInstance<DateIncident>().filter { it.type == IncidentType.AP }
            .filter { old -> state.requests.none { it.id == old.id } }.groupBy { it.id }.map { (id, records) ->
                ApRequest(id, null, records.flatMap { it.dates }.distinct().sorted().map { date ->
                    ApDay("legacy:$id:$date", date, ApStatus.CONCEDIDO, date.year)
                }, legacy = true)
            }
        val requests = state.requests + imported
        val years = requests.flatMap { it.days }.map { it.poolYear }.distinct()
        return ApState(state.pools + years.filter { y -> state.pools.none { it.year == y } }.map { ApPool(it) }, requests)
    }
}
