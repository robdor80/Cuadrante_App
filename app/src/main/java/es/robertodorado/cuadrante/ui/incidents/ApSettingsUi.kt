package es.robertodorado.cuadrante.ui.incidents

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.data.local.ApProposal
import es.robertodorado.cuadrante.domain.ap.*
import es.robertodorado.cuadrante.model.ApRequest
import es.robertodorado.cuadrante.ui.labelResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Composable
fun ApSettingsCard(viewModel: ApViewModel, calculator: ShiftCalculator?) {
    val state by viewModel.uiState.collectAsState()
    var editor by remember { mutableStateOf<ApRequest?>(null) }
    var creating by remember { mutableStateOf(false) }
    var configuring by remember { mutableStateOf(false) }
    var reassigning by remember { mutableStateOf<ApDay?>(null) }
    var resolving by remember { mutableStateOf<Pair<ApDay, ApStatus>?>(null) }
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Asuntos propios · AP", style = MaterialTheme.typography.titleMedium)
            Text("● AP   ▲ Vacaciones   ■ Permiso\nAzul: solicitado · Verde: concedido · Rojo: denegado", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { creating = true }, enabled = state.loaded && !state.busy && calculator != null) { Text("Solicitar AP") }
            OutlinedButton(onClick = { configuring = true }, enabled = state.loaded && !state.busy) { Text("Configurar bolsa y prórroga") }
            Text("Bolsas anuales", style = MaterialTheme.typography.titleSmall)
            if (state.data.pools.isEmpty()) Text("Configura la cuota del año (mínimo 8 AP).")
            state.data.balances(LocalDate.now()).forEach { b ->
                Text("${b.pool.year}: asignados ${b.pool.quota} · solicitados ${b.requested} · concedidos ${b.granted}\n" +
                    "Disponibles ${b.available} · liberados ${b.released} · prorrogados ${b.extended} · caducados ${b.expired}\n" +
                    (b.pool.extensionUntil?.let { "Prórroga hasta $it (exclusivo)" } ?: "Sin prórroga; caduca al terminar el 31/12/${b.pool.year}"),
                    style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar histórico AP" else "Ver histórico AP (${state.data.days.size})") }
            if (expanded) state.data.requests.sortedByDescending { it.firstDate }.forEach { request ->
                HorizontalDivider()
                Text("Solicitud ${request.id}", style = MaterialTheme.typography.labelLarge)
                Text(request.requestedAt?.toString() ?: "Legado 1.0.0 · fecha de solicitud desconocida", style = MaterialTheme.typography.bodySmall)
                if (request.days.any { it.status == ApStatus.SOLICITADO }) {
                    TextButton(onClick = { editor = request }, enabled = !state.busy && calculator != null) { Text("Editar fechas pendientes") }
                }
                request.days.sortedBy { it.date }.forEach { day ->
                    Text("${day.date} · ${day.status} · bolsa ${day.poolYear}", style = MaterialTheme.typography.bodyMedium)
                    day.resolvedAt?.let { Text("Resolución: $it", style = MaterialTheme.typography.bodySmall) }
                    day.notes?.takeIf { it.isNotBlank() }?.let { Text("Observaciones: $it") }
                    day.reason?.takeIf { it.isNotBlank() }?.let { Text("Motivo: $it") }
                    if (day.status == ApStatus.SOLICITADO) {
                        Row {
                            TextButton(onClick = { resolving = day to ApStatus.CONCEDIDO }, enabled = !state.busy) { Text("Conceder") }
                            TextButton(onClick = { resolving = day to ApStatus.DENEGADO }, enabled = !state.busy) { Text("Denegar") }
                        }
                    }
                    if (day.status.consumes) {
                        Row {
                            TextButton(onClick = { resolving = day to ApStatus.CANCELADO }, enabled = !state.busy) { Text("Cancelar AP") }
                            TextButton(onClick = { reassigning = day }, enabled = !state.busy) { Text("Cambiar bolsa") }
                        }
                    }
                    day.history.forEach { e ->
                        Text("${e.at} · ${e.action}: ${e.date} · ${e.status} · ${e.poolYear}" +
                            (e.notes?.let { " · $it" } ?: "") + (e.reason?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if ((creating || editor != null) && calculator != null) {
        ApEditor(editor, state.data, calculator, onDismiss = { creating = false; editor = null }) { change ->
            viewModel.preview(change)
        }
    }
    // Keep the editor open after invalid proposals so selections and per-date errors can be corrected.
    LaunchedEffect(state.proposal) {
        if (state.proposal != null) { creating = false; editor = null; configuring = false; reassigning = null; resolving = null }
    }
    if (configuring) PoolEditor(state.data, onDismiss = { configuring = false }, onPreview = viewModel::preview)
    reassigning?.let { day ->
        SimpleApDialog("Cambiar bolsa · ${day.date}", { reassigning = null }) {
            Text("La reserva anterior se libera en la misma operación.")
            state.data.pools.filter { it.accepts(day.date) }.sortedBy { it.year }.forEach { pool ->
                OutlinedButton(onClick = { viewModel.preview(ApChange.Reassign(day.id, pool.year)) }) { Text("Revisar bolsa ${pool.year}") }
            }
        }
    }
    resolving?.let { (day, status) ->
        var notes by remember(day.id, status) { mutableStateOf(day.notes.orEmpty()) }
        var reason by remember(day.id, status) { mutableStateOf("") }
        SimpleApDialog("${day.date} → $status", { resolving = null }) {
            OutlinedTextField(notes, { notes = it.take(4000) }, label = { Text("Observaciones (opcionales)") })
            OutlinedTextField(reason, { reason = it.take(4000) }, label = { Text("Motivo (opcional)") })
            Button(onClick = { viewModel.preview(ApChange.Resolve(day.id, status, notes.ifBlank { null }, reason.ifBlank { null })) }) { Text("Revisar cambio") }
        }
    }
    state.error?.let { error ->
        AlertDialog(onDismissRequest = viewModel::clearError, title = { Text("No se ha guardado ningún cambio") },
            text = { Text(error) }, confirmButton = { TextButton(onClick = viewModel::clearError) { Text("Volver a revisar") } })
    }
    state.proposal?.let { proposal -> ApConfirmationDialog(proposal, state.busy, viewModel::confirm, viewModel::cancel) }
}

@Composable
private fun SimpleApDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.fillMaxWidth().heightIn(max = 700.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                content()
                TextButton(onClick = onDismiss) { Text("Volver") }
            }
        }
    }
}

@Composable
private fun PoolEditor(data: ApState, onDismiss: () -> Unit, onPreview: (ApChange) -> Unit) {
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    val existing = data.pools.find { it.year.toString() == year }
    var quota by remember(year) { mutableStateOf(existing?.quota?.toString() ?: "8") }
    var extension by remember(year) { mutableStateOf(existing?.extensionUntil?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    SimpleApDialog("Bolsa anual AP", onDismiss) {
        OutlinedTextField(year, { year = it }, label = { Text("Año de origen") })
        OutlinedTextField(quota, { quota = it }, label = { Text("Cuota (mínimo 8)") })
        OutlinedTextField(extension, { extension = it }, label = { Text("Prórroga exclusiva AAAA-MM-DD (opcional)") })
        Text("Vacío: sin prórroga. Hasta 2027-03-01 permite usar hasta 2027-02-28.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching { ApPool(year.toInt(), quota.toInt(), extension.takeIf { it.isNotBlank() }?.let(LocalDate::parse)) }
                .onSuccess { onPreview(ApChange.ConfigurePool(it)) }
                .onFailure { error = it.message ?: "Configuración no válida" }
        }) { Text("Revisar configuración") }
    }
}

@Composable
private fun ApEditor(request: ApRequest?, data: ApState, calculator: ShiftCalculator, onDismiss: () -> Unit, onPreview: (ApChange) -> Unit) {
    val pending = request?.days?.filter { it.status == ApStatus.SOLICITADO }.orEmpty()
    var selections by remember(request?.id) { mutableStateOf(pending.map { ApSelection(it.id, it.date, it.poolYear, it.notes) }) }
    var invalidDates by remember(request?.id) { mutableStateOf(emptySet<String>()) }
    val requestId = remember(request?.id) { request?.id ?: UUID.randomUUID().toString() }
    var month by remember { mutableStateOf(pending.firstOrNull()?.date?.let(YearMonth::from) ?: YearMonth.now()) }
    SimpleApDialog(if (request == null) "Solicitar AP" else "Editar pendientes", onDismiss) {
        Text("Selecciona fechas en cualquier mes. Cada fecha cubre el turno completo y consume 1 AP. Para mover una fecha, usa su campo Fecha.")
        MultiDateCalendar(month, calculator, selections.map { it.date }.toSet(),
            { month = month.minusMonths(1) }, { month = month.plusMonths(1) }, { date ->
                invalidDates = invalidDates - selections.filter { it.date == date }.map { it.id }.toSet()
                selections = if (selections.any { it.date == date }) selections.filterNot { it.date == date }
                else selections + ApSelection(UUID.randomUUID().toString(), date, null)
            })
        selections.sortedBy { it.date }.forEach { s ->
            key(s.id) {
                var dateText by remember(s.date) { mutableStateOf(s.date.toString()) }
                var dateError by remember { mutableStateOf(false) }
                OutlinedTextField(dateText, { text ->
                    dateText = text
                    val parsed = runCatching { LocalDate.parse(text) }.getOrNull()
                    dateError = parsed == null
                    invalidDates = if (dateError) invalidDates + s.id else invalidDates - s.id
                    if (parsed != null) selections = selections.map { if (it.id == s.id) it.copy(date = parsed) else it }
                }, label = { Text("Fecha AAAA-MM-DD") }, isError = dateError,
                    supportingText = { if (dateError) Text("Introduce una fecha válida antes de continuar") })
                Text("${s.date} · ${stringResource(calculator.shiftFor(s.date).labelResource())}")
                Text("Elige bolsa para esta fecha:")
                val pools = data.pools.filter { it.accepts(s.date) }.sortedBy { it.year }
                if (pools.isEmpty()) Text("No hay bolsa válida. Configúrala antes de solicitar.")
                pools.forEach { pool ->
                    FilterChip(selected = s.poolYear == pool.year, onClick = {
                        selections = selections.map { if (it.id == s.id) it.copy(poolYear = pool.year) else it }
                    }, label = { Text("Bolsa ${pool.year}") })
                }
                OutlinedTextField(s.notes.orEmpty(), { text ->
                    selections = selections.map { if (it.id == s.id) it.copy(notes = text.take(4000).ifBlank { null }) else it }
                }, label = { Text("Observaciones (opcionales)") })
                TextButton(onClick = { selections = selections.filterNot { it.id == s.id }; invalidDates = invalidDates - s.id }) { Text("Quitar fecha") }
            }
        }
        Button(onClick = {
            onPreview(if (request == null) ApChange.Create(requestId, selections) else ApChange.Edit(request.id, selections))
        }, enabled = invalidDates.isEmpty() && (selections.isNotEmpty() || request != null)) { Text("Revisar resumen final") }
    }
}

@Composable
private fun ApConfirmationDialog(proposal: ApProposal, busy: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    var voiceMessage by remember { mutableStateOf<String?>(null) }
    var voiceProposal by remember { mutableStateOf<ApProposal?>(null) }
    val currentProposal by rememberUpdatedState(proposal)
    val currentBusy by rememberUpdatedState(busy)
    val confirm by rememberUpdatedState(onConfirm)
    val cancel by rememberUpdatedState(onCancel)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && voiceProposal == currentProposal && !currentBusy) {
            when (parseVoiceConfirmation(result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty())) {
                Confirmation.CONFIRM -> confirm()
                Confirmation.CANCEL -> cancel()
                Confirmation.UNKNOWN -> voiceMessage = "Orden no inequívoca. Di confirmar o cancelar, o usa los botones."
            }
        }
        voiceProposal = null
    }
    SimpleApDialog("Resumen final · confirmar cambio", { if (!busy) onCancel() }) {
        if (proposal.change is ApChange.ConfigurePool) {
            val pool = proposal.change.pool
            Text("Bolsa ${pool.year} · cuota ${pool.quota} · " + (pool.extensionUntil?.let { "prórroga hasta $it (exclusivo)" } ?: "sin prórroga"))
        }
        val calculator = remember(proposal.settings) { ShiftCalculator(proposal.settings.referenceDate, proposal.settings.shifts) }
        proposal.after.days.filter { it != proposal.before.days.find { old -> old.id == it.id } }.forEach { day ->
            val old = proposal.before.days.find { it.id == day.id }
            Text("${day.date} · ${stringResource(calculator.shiftFor(day.date).labelResource())}\n${day.status} · bolsa ${day.poolYear}" +
                (old?.let { "\nAntes: ${it.date} · ${it.status} · bolsa ${it.poolYear}" } ?: "") +
                (day.notes?.let { "\nObservaciones: $it" } ?: "") + (day.reason?.let { "\nMotivo: $it" } ?: ""))
        }
        proposal.after.balances(LocalDate.now()).forEach { b ->
            Text("Bolsa ${b.pool.year}: quedarán ${b.available} disponibles · ${b.requested} solicitados · ${b.granted} concedidos · ${b.expired} caducados")
        }
        Text("No se guarda ningún cambio hasta confirmar.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onConfirm, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF146B32), contentColor = Color.White),
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = "Confirmar cambio" }) { Text("👍", style = MaterialTheme.typography.headlineMedium) }
            Button(onClick = onCancel, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = "Cancelar cambio" }) { Text("👎", style = MaterialTheme.typography.headlineMedium) }
        }
        OutlinedButton(onClick = {
            try {
                voiceProposal = proposal
                launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Di confirmar o cancelar")
                })
            } catch (_: ActivityNotFoundException) { voiceMessage = "Reconocimiento no disponible. Usa los botones." }
        }, enabled = !busy) { Text("Confirmación por voz") }
        voiceMessage?.let { Text(it) }
    }
}
