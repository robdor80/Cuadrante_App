package es.robertodorado.cuadrante.domain.ap

import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ApPolicyTest {
    private val start = LocalDate.of(2026, 12, 1)
    private val time = Instant.parse("2026-11-05T12:00:00Z")
    private val shifts = ShiftCalculator(start, listOf(ShiftType.MORNING, ShiftType.AFTERNOON, ShiftType.NIGHT, ShiftType.POST_NIGHT, ShiftType.OFF))
    private val working = ShiftCalculator(start, listOf(ShiftType.MORNING))
    private val initial = ApState(listOf(ApPool(2026)))
    private fun create(vararg offsets: Long, pool: Int? = 2026, id: String = "r") = ApChange.Create(id,
        offsets.mapIndexed { index, offset -> ApSelection("$id-$index", start.plusDays(offset), pool) })
    private fun apply(s: ApState = initial, c: ApChange, others: List<Incident> = emptyList(), calc: ShiftCalculator = working) = ApPolicy.apply(s, others, calc, c, time)
    private fun rejected(s: ApState = initial, c: ApChange, others: List<Incident> = emptyList(), calc: ShiftCalculator = working): ApValidationException {
        try { apply(s, c, others, calc); fail("Expected rejection") } catch (e: ApValidationException) { return e }
        error("unreachable")
    }
    @Test fun morningValid() { assertEquals(1, apply(c = create(0), calc = shifts).days.size) }
    @Test fun afternoonValid() { assertEquals(1, apply(c = create(1), calc = shifts).days.size) }
    @Test fun nightValidAndPostNightUnchanged() {
        apply(c = create(2), calc = shifts)
        assertEquals(ShiftType.NIGHT, shifts.shiftFor(start.plusDays(2)))
        assertEquals(ShiftType.POST_NIGHT, shifts.shiftFor(start.plusDays(3)))
    }
    @Test fun offRejected() { assertTrue(rejected(c = create(4), calc = shifts).message!!.contains("Libre")) }
    @Test fun postNightRejected() { assertTrue(rejected(c = create(3), calc = shifts).message!!.contains("Saliente")) }
    @Test fun workedWeekendValid() { assertEquals(DayOfWeek.SATURDAY, start.plusDays(4).dayOfWeek); assertEquals(1, apply(c = create(4)).days.size) }
    @Test fun arbitraryDatesOneRequest() { val s = apply(c = create(0, 9, 25)); assertEquals(1, s.requests.size); assertEquals(3, s.days.size) }
    @Test fun consecutiveDatesValid() { assertEquals(3, apply(c = create(0, 1, 2)).days.size) }
    @Test fun duplicateDatesRejected() { assertTrue(rejected(c = create(0, 0)).message!!.contains(start.toString())) }
    @Test fun overlapApRejected() { rejected(apply(c = create(0)), create(0, id = "new")) }
    @Test fun overlapPermissionRejected() { rejected(c = create(0), others = listOf(DateIncident("p", IncidentType.PERMISSION, listOf(start)))) }
    @Test fun overlapVacationRejected() { rejected(c = create(0), others = listOf(VacationIncident("v", start.minusDays(1), start.plusDays(1)))) }
    @Test fun minimumQuota() { assertEquals(8, ApPool(2026).quota); assertThrows(IllegalArgumentException::class.java) { ApPool(2026, 7) } }
    @Test fun annualQuotasIndependent() {
        val s = ApState(listOf(ApPool(2026, 8), ApPool(2027, 12)))
        assertEquals(listOf(8, 12), s.balances(start).map { it.available })
    }
    @Test fun emptyPoolRejected() { val full = apply(c = create(*(0L..7L).toList().toLongArray())); assertTrue(rejected(full, create(8, id = "new")).message!!.contains("sin saldo")) }
    @Test fun requestedReserves() { val b = apply(c = create(0)).balances(start).single(); assertEquals(1, b.requested); assertEquals(7, b.available) }
    @Test fun grantedMaintainsReservation() { val s = apply(apply(c = create(0)), ApChange.Resolve("r-0", ApStatus.CONCEDIDO)); val b = s.balances(start).single(); assertEquals(0, b.requested); assertEquals(1, b.granted); assertEquals(7, b.available) }
    @Test fun deniedReleasesAndKeepsReason() {
        val s = apply(apply(c = create(0)), ApChange.Resolve("r-0", ApStatus.DENEGADO, reason = "Servicio"))
        assertEquals(8, s.balances(start).single().available); assertEquals("Servicio", s.days.single().reason)
        assertEquals(time, s.days.single().resolvedAt); assertEquals(1, s.balances(start).single().released)
    }
    @Test fun requestedCancellationReleases() { val s = apply(apply(c = create(0)), ApChange.Resolve("r-0", ApStatus.CANCELADO)); assertEquals(8, s.balances(start).single().available) }
    @Test fun grantedCancellationNoMinimumNotice() {
        val s = apply(apply(c = create(0)), ApChange.Resolve("r-0", ApStatus.CONCEDIDO))
        assertEquals(8, apply(s, ApChange.Resolve("r-0", ApStatus.CANCELADO)).balances(start).single().available)
    }
    @Test fun terminalStateCannotResolveAgain() { val s = apply(apply(c = create(0)), ApChange.Resolve("r-0", ApStatus.DENEGADO)); rejected(s, ApChange.Resolve("r-0", ApStatus.CONCEDIDO)) }
    @Test fun extensionExplicitlyConfigured() { assertTrue(ApPool(2026, extensionUntil = LocalDate.of(2027, 3, 1)).accepts(LocalDate.of(2027, 2, 28))) }
    @Test fun withoutExtensionExpiresAtYearEnd() { assertFalse(ApPool(2026).accepts(LocalDate.of(2027, 1, 1))); assertTrue(ApPool(2026).accepts(LocalDate.of(2026, 12, 31))) }
    @Test fun extensionBoundaryExclusive() { val p = ApPool(2026, extensionUntil = LocalDate.of(2027, 3, 1)); assertFalse(p.accepts(LocalDate.of(2027, 3, 1))) }
    @Test fun futurePoolCannotCoverPreviousYear() { assertFalse(ApPool(2027).accepts(start)) }
    @Test fun extensionBalanceAndExpiryNoDoubleCounting() {
        val s = ApState(listOf(ApPool(2026, extensionUntil = LocalDate.of(2027, 3, 1))))
        val active = s.balances(LocalDate.of(2027, 2, 28)).single(); assertEquals(8, active.extended); assertEquals(0, active.expired)
        val expired = s.balances(LocalDate.of(2027, 3, 1)).single(); assertEquals(0, expired.extended); assertEquals(8, expired.expired); assertEquals(0, expired.available)
    }
    @Test fun twoEligiblePoolsRequireExplicitChoice() {
        val s = ApState(listOf(ApPool(2025, extensionUntil = LocalDate.of(2027, 3, 1)), ApPool(2026)))
        rejected(s, create(0, pool = null))
        assertEquals(2025, apply(s, create(0, pool = 2025)).days.single().poolYear)
        assertEquals(2026, apply(s, create(0, pool = 2026)).days.single().poolYear)
    }
    @Test fun reassignReleasesOldAndReservesNew() {
        val s = apply(initial.copy(pools = initial.pools + ApPool(2025, extensionUntil = LocalDate.of(2027, 3, 1))), create(0))
        val updated = apply(s, ApChange.Reassign("r-0", 2025))
        assertEquals(2025, updated.days.single().poolYear); assertEquals(8, updated.balances(start).find { it.pool.year == 2026 }!!.available)
        assertEquals(7, updated.balances(start).find { it.pool.year == 2025 }!!.available); assertEquals(2, updated.days.single().history.size)
    }
    @Test fun grantedCanChangePool() {
        val s = apply(initial.copy(pools = initial.pools + ApPool(2025, extensionUntil = LocalDate.of(2027, 3, 1))), create(0))
        val granted = apply(s, ApChange.Resolve("r-0", ApStatus.CONCEDIDO))
        assertEquals(ApStatus.CONCEDIDO, apply(granted, ApChange.Reassign("r-0", 2025)).days.single().status)
    }
    @Test fun invalidReassignAtomic() { val s = apply(c = create(0)); val original = s.copy(); rejected(s, ApChange.Reassign("r-0", 2027)); assertEquals(original, s) }
    @Test fun insufficientReassignAtomic() {
        val s = apply(initial.copy(pools = initial.pools + ApPool(2025, extensionUntil = LocalDate.of(2027, 3, 1))), create(*(0L..7L).toList().toLongArray(), pool = 2025))
        val withNew = apply(s, create(9, id = "new")); rejected(withNew, ApChange.Reassign("new-0", 2025)); assertEquals(2026, withNew.days.last().poolYear)
    }
    @Test fun editAddsRemovesMovesWithoutChangingRequestId() {
        val s = apply(c = create(0, 1))
        val updated = apply(s, ApChange.Edit("r", listOf(ApSelection("r-0", start.plusDays(5), 2026), ApSelection("added", start.plusDays(8), 2026))))
        assertEquals("r", updated.requests.single().id); assertEquals(3, updated.days.size)
        assertEquals(ApStatus.CANCELADO, updated.days.find { it.id == "r-1" }!!.status)
        assertEquals(6, updated.balances(start).single().available)
        assertEquals(start, updated.days.find { it.id == "r-0" }!!.history.first().date)
    }
    @Test fun partiallyInvalidEditKeepsEverything() {
        val s = apply(c = create(0, 1), calc = shifts); val original = s.copy()
        val e = rejected(s, ApChange.Edit("r", listOf(ApSelection("r-0", start.plusDays(5), 2026), ApSelection("r-1", start.plusDays(4), 2026))), calc = shifts)
        assertTrue(e.message!!.contains(start.plusDays(4).toString())); assertEquals(original, s)
    }
    @Test fun collectsAllFailingDates() { val e = rejected(c = create(3, 4), calc = shifts); assertEquals(2, e.problems.size) }
    @Test fun independentResolutionWithinRequest() {
        val s = apply(c = create(0, 1, 2)); val a = apply(s, ApChange.Resolve("r-0", ApStatus.CONCEDIDO))
        val b = apply(a, ApChange.Resolve("r-1", ApStatus.DENEGADO))
        assertEquals(listOf(ApStatus.CONCEDIDO, ApStatus.DENEGADO, ApStatus.SOLICITADO), b.days.map { it.status }); assertEquals(1, b.requests.size)
        val c = apply(b, ApChange.Edit("r", listOf(ApSelection("r-2", start.plusDays(6), 2026))))
        assertEquals(b.days.take(2), c.days.take(2))
    }
    @Test fun optionalObservationsInAllStates() { for (status in listOf(ApStatus.CONCEDIDO, ApStatus.DENEGADO, ApStatus.CANCELADO)) { val s = apply(apply(c = create(0)), ApChange.Resolve("r-0", status)); assertNull(s.days.single().notes) } }
    @Test fun historyPreservesTransitions() {
        val s = apply(apply(apply(c = create(0)), ApChange.Resolve("r-0", ApStatus.CONCEDIDO, "nota")), ApChange.Resolve("r-0", ApStatus.CANCELADO))
        assertEquals(listOf(ApStatus.SOLICITADO, ApStatus.CONCEDIDO, ApStatus.CANCELADO), s.days.single().history.map { it.status })
        assertEquals("nota", s.days.single().history[1].notes)
    }
    @Test fun poolConfigurationCannotInvalidateReservedExtension() {
        val s = apply(ApState(listOf(ApPool(2025, extensionUntil = LocalDate.of(2027, 3, 1)))), create(0, pool = 2025))
        rejected(s, ApChange.ConfigurePool(ApPool(2025)))
    }
    @Test fun voiceOnlyExactUnambiguousCommands() {
        assertEquals(Confirmation.CONFIRM, parseVoiceConfirmation(listOf(" CONFIRMAR ")))
        assertEquals(Confirmation.CONFIRM, parseVoiceConfirmation(listOf("confirmo")))
        assertEquals(Confirmation.CANCEL, parseVoiceConfirmation(listOf("cancelar", "cancelo")))
        for (phrases in listOf(emptyList(), listOf("no confirmar"), listOf("sí"), listOf("confirmar", "cancelar"), listOf("confirmar", "otra cosa"))) assertEquals(Confirmation.UNKNOWN, parseVoiceConfirmation(phrases))
    }
    @Test fun actualCreationHonorsExclusiveExtensionBoundary() {
        val s = ApState(listOf(ApPool(2026, extensionUntil = LocalDate.of(2027,3,1))))
        val valid = ApChange.Create("r",listOf(ApSelection("d",LocalDate.of(2027,2,28),2026)))
        assertEquals(1,apply(s,valid).days.size)
        val invalid = ApChange.Create("r",listOf(ApSelection("d",LocalDate.of(2027,3,1),2026)))
        assertTrue(rejected(s,invalid).message!!.contains("prórroga vencida"))
    }
    @Test fun oldPoolWithoutExtensionCannotReserveNextYear() {
        rejected(c = ApChange.Create("r",listOf(ApSelection("d",LocalDate.of(2027,1,1),2026))))
    }
    @Test fun missingPoolReportsEverySelectedDate() {
        val e = rejected(c = create(0,1,pool = null))
        assertEquals(2,e.problems.size)
    }
    @Test fun requestMaySpanIndependentAnnualPools() {
        val s = initial.copy(pools = initial.pools + ApPool(2027,10))
        val c = ApChange.Create("r",listOf(ApSelection("a",start,2026),ApSelection("b",LocalDate.of(2027,1,4),2027)))
        val updated = apply(s,c)
        assertEquals(1,updated.requests.size); assertEquals(listOf(7,9),updated.balances(start).map { it.available })
    }
    @Test fun removingAllPendingDatesKeepsRequestInHistory() {
        val s = apply(apply(c = create(0,1)),ApChange.Edit("r",emptyList()))
        assertEquals(1,s.requests.size); assertTrue(s.days.all { it.status == ApStatus.CANCELADO })
        assertEquals(8,s.balances(start).single().available)
    }
    @Test fun legacyDeficitIsNotDiscardedAndCanBeCancelled() {
        val legacy = ApRequest("legacy",null,(0L..8L).map { ApDay("d$it",start.plusDays(it),ApStatus.CONCEDIDO,2026) },true)
        val s = initial.copy(requests = listOf(legacy))
        assertEquals(-1,s.balances(start).single().available)
        rejected(s,create(10))
        assertEquals(0,apply(s,ApChange.Resolve("d0",ApStatus.CANCELADO)).balances(start).single().available)
    }

}
