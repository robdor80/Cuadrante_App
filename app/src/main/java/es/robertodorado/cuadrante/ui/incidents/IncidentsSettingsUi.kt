package es.robertodorado.cuadrante.ui.incidents

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.robertodorado.cuadrante.R
import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.Incident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.ShiftType
import es.robertodorado.cuadrante.model.VacationIncident
import es.robertodorado.cuadrante.ui.abbreviationResource
import es.robertodorado.cuadrante.ui.labelResource
import es.robertodorado.cuadrante.ui.calendar.CalendarDay
import es.robertodorado.cuadrante.ui.calendar.MonthlyCalendarGenerator
import es.robertodorado.cuadrante.ui.theme.visualColors
import es.robertodorado.cuadrante.util.toDatePickerMillis
import es.robertodorado.cuadrante.util.toLocalDateFromDatePicker
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun IncidentsSettingsCard(
    uiState: IncidentsUiState,
    shiftCalculator: ShiftCalculator?,
    onAddDates: (IncidentType, Set<LocalDate>) -> Unit,
    onAddVacation: (LocalDate, LocalDate) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.incidents_settings_title),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = { showAddDialog = true },
                    enabled = shiftCalculator != null && !uiState.isSaving,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.incidents_add))
                }
            }

            when {
                uiState.isLoading -> Text(
                    text = stringResource(R.string.incidents_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                uiState.incidents.isEmpty() -> Text(
                    text = stringResource(R.string.incidents_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> uiState.incidents.forEachIndexed { index, incident ->
                    if (index > 0) HorizontalDivider()
                    IncidentRow(
                        incident = incident,
                        onDelete = { onDelete(incident.id) },
                    )
                }
            }

            if (uiState.hasError) {
                Text(
                    text = stringResource(R.string.incidents_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showAddDialog && shiftCalculator != null) {
        AddIncidentDialog(
            shiftCalculator = shiftCalculator,
            onDismiss = { showAddDialog = false },
            onAddDates = { type, dates ->
                onAddDates(type, dates)
                showAddDialog = false
            },
            onAddVacation = { start, end ->
                onAddVacation(start, end)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun IncidentRow(
    incident: Incident,
    onDelete: () -> Unit,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val typeLabel = incidentTypeLabel(incident.type)
    val dateText = remember(incident, locale) { formatIncidentDates(incident, locale) }
    val deleteDescription = stringResource(R.string.incident_delete, typeLabel)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = typeLabel,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = dateText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = deleteDescription,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncidentDialog(
    shiftCalculator: ShiftCalculator,
    onDismiss: () -> Unit,
    onAddDates: (IncidentType, Set<LocalDate>) -> Unit,
    onAddVacation: (LocalDate, LocalDate) -> Unit,
) {
    var selectedType by remember { mutableStateOf(IncidentType.AP) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDates by remember { mutableStateOf(emptySet<LocalDate>()) }
    var vacationStart by remember { mutableStateOf<LocalDate?>(null) }
    var vacationEnd by remember { mutableStateOf<LocalDate?>(null) }
    var vacationPickerTarget by remember { mutableStateOf<VacationDateTarget?>(null) }
    val canSave = if (selectedType == IncidentType.VACATION) {
        vacationStart?.let { start ->
            vacationEnd?.let { end -> !end.isBefore(start) }
        } == true
    } else {
        selectedDates.isNotEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.incident_add_title),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.incident_select_type),
                    style = MaterialTheme.typography.labelLarge,
                )
                IncidentTypeSelector(
                    selectedType = selectedType,
                    onSelected = { selectedType = it },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (selectedType == IncidentType.VACATION) {
                        VacationRangeSelector(
                            startDate = vacationStart,
                            endDate = vacationEnd,
                            onSelectStart = { vacationPickerTarget = VacationDateTarget.START },
                            onSelectEnd = { vacationPickerTarget = VacationDateTarget.END },
                        )
                    } else {
                        MultiDateCalendar(
                            visibleMonth = visibleMonth,
                            shiftCalculator = shiftCalculator,
                            selectedDates = selectedDates,
                            onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                            onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                            onToggleDate = { date ->
                                selectedDates = if (date in selectedDates) {
                                    selectedDates - date
                                } else {
                                    selectedDates + date
                                }
                            },
        )
    }

    vacationPickerTarget?.let { target ->
        SingleDatePickerDialog(
            initialDate = when (target) {
                VacationDateTarget.START -> vacationStart ?: LocalDate.now()
                VacationDateTarget.END -> vacationEnd ?: vacationStart ?: LocalDate.now()
            },
            onDismiss = { vacationPickerTarget = null },
            onDateSelected = { selectedDate ->
                when (target) {
                    VacationDateTarget.START -> {
                        vacationStart = selectedDate
                        if (vacationEnd?.isBefore(selectedDate) == true) vacationEnd = null
                    }
                    VacationDateTarget.END -> vacationEnd = selectedDate
                }
                vacationPickerTarget = null
            },
        )
    }
}

                if (selectedType != IncidentType.VACATION) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.incident_selected_days,
                            selectedDates.size,
                            selectedDates.size,
                        ),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.incident_cancel))
                    }
                    Button(
                        onClick = {
                            if (selectedType == IncidentType.VACATION) {
                                onAddVacation(requireNotNull(vacationStart), requireNotNull(vacationEnd))
                            } else {
                                onAddDates(selectedType, selectedDates)
                            }
                        },
                        enabled = canSave,
                    ) {
                        Text(stringResource(R.string.incident_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentTypeSelector(
    selectedType: IncidentType,
    onSelected: (IncidentType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IncidentType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onSelected(type) },
                label = {
                    Text(
                        text = incidentTypeLabel(type),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                modifier = Modifier.weight(
                    when (type) {
                        IncidentType.AP -> 0.7f
                        IncidentType.PERMISSION -> 1f
                        IncidentType.VACATION -> 1.3f
                    },
                ),
            )
        }
    }
}

@Composable
private fun VacationRangeSelector(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onSelectStart: () -> Unit,
    onSelectEnd: () -> Unit,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.incident_vacation_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        VacationDateField(
            label = stringResource(R.string.incident_vacation_from),
            value = startDate?.format(formatter),
            onClick = onSelectStart,
        )
        VacationDateField(
            label = stringResource(R.string.incident_vacation_until),
            value = endDate?.format(formatter),
            onClick = onSelectEnd,
        )
    }
}

@Composable
private fun VacationDateField(
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(value ?: stringResource(R.string.incident_select_date))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.toDatePickerMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onDateSelected(it.toLocalDateFromDatePicker()) }
                },
            ) {
                Text(stringResource(R.string.date_picker_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.date_picker_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

private enum class VacationDateTarget {
    START,
    END,
}

@Composable
private fun MultiDateCalendar(
    visibleMonth: YearMonth,
    shiftCalculator: ShiftCalculator,
    selectedDates: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleDate: (LocalDate) -> Unit,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val calendar = remember(visibleMonth, shiftCalculator) {
        MonthlyCalendarGenerator.generate(visibleMonth, shiftCalculator)
    }
    val monthTitle = remember(visibleMonth, locale) {
        visibleMonth.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() } +
            " " + visibleMonth.year
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.incident_work_days_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthButton(
                symbol = stringResource(R.string.calendar_previous_symbol),
                description = stringResource(R.string.incident_previous_month),
                onClick = onPreviousMonth,
            )
            Text(
                text = monthTitle,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            MonthButton(
                symbol = stringResource(R.string.calendar_next_symbol),
                description = stringResource(R.string.incident_next_month),
                onClick = onNextMonth,
            )
        }
        WeekdayHeader()
        calendar.weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(0.86f))
                    } else {
                        SelectableDay(
                            day = day,
                            selected = day.date in selectedDates,
                            locale = locale,
                            onClick = { onToggleDate(day.date) },
                            modifier = Modifier.weight(1f).aspectRatio(0.86f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableDay(
    day: CalendarDay,
    selected: Boolean,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectable = day.shift != ShiftType.OFF
    val colors = day.shift.visualColors()
    val shiftLabel = stringResource(day.shift.labelResource())
    val dateLabel = remember(day.date, locale) {
        day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    }
    val description = stringResource(R.string.incident_date_description, dateLabel, shiftLabel)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .alpha(if (selectable) 1f else 0.42f)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(enabled = selectable, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                color = colors.background,
                contentColor = colors.foreground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(day.shift.abbreviationResource()),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf(
        R.string.weekday_monday_short,
        R.string.weekday_tuesday_short,
        R.string.weekday_wednesday_short,
        R.string.weekday_thursday_short,
        R.string.weekday_friday_short,
        R.string.weekday_saturday_short,
        R.string.weekday_sunday_short,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        weekdays.forEach {
            Text(
                text = stringResource(it),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun MonthButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun incidentTypeLabel(type: IncidentType): String = stringResource(
    when (type) {
        IncidentType.AP -> R.string.incident_type_ap
        IncidentType.PERMISSION -> R.string.incident_type_permission
        IncidentType.VACATION -> R.string.incident_type_vacation
    },
)

private fun formatIncidentDates(incident: Incident, locale: Locale): String {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    return when (incident) {
        is DateIncident -> incident.dates.joinToString(" · ") { it.format(dateFormatter) }
        is VacationIncident -> "${incident.startDate.format(dateFormatter)} – ${incident.endDate.format(dateFormatter)}"
    }
}
