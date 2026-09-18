package es.robertodorado.cuadrante.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.robertodorado.cuadrante.R
import es.robertodorado.cuadrante.calculation.ShiftCalculator
import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.ui.calendar.CalendarScreen
import es.robertodorado.cuadrante.ui.calendar.CalendarViewModel
import es.robertodorado.cuadrante.ui.calendar.MonthlyCalendarGenerator
import es.robertodorado.cuadrante.ui.incidents.IncidentsSettingsCard
import es.robertodorado.cuadrante.ui.incidents.IncidentsUiState
import es.robertodorado.cuadrante.ui.incidents.IncidentsViewModel
import es.robertodorado.cuadrante.ui.settings.SaveResult
import es.robertodorado.cuadrante.ui.settings.SettingsUiState
import es.robertodorado.cuadrante.ui.settings.SettingsViewModel
import es.robertodorado.cuadrante.ui.theme.CuadranteTheme
import es.robertodorado.cuadrante.ui.theme.visualColors
import es.robertodorado.cuadrante.util.toDatePickerMillis
import es.robertodorado.cuadrante.util.toLocalDateFromDatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun CuadranteApp(
    settingsViewModel: SettingsViewModel,
    calendarViewModel: CalendarViewModel,
    incidentsViewModel: IncidentsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val incidentsUiState by incidentsViewModel.uiState.collectAsState()
    val visibleMonth by calendarViewModel.visibleMonth.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.saveResult, uiState.savedSettings) {
        if (uiState.saveResult == SaveResult.SUCCESS && uiState.savedSettings != null) {
            showSettings = false
        }
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.savedSettings == null || showSettings -> {
            SettingsScreen(
                uiState = uiState,
                canNavigateBack = uiState.savedSettings != null,
                onBack = { showSettings = false },
                onPatternSelected = settingsViewModel::selectPattern,
                onShiftAdded = settingsViewModel::addShift,
                onRemoveLast = settingsViewModel::removeLastShift,
                onClear = settingsViewModel::clearCustomShifts,
                onReferenceDateSelected = settingsViewModel::setReferenceDate,
                onSave = settingsViewModel::save,
                incidentsUiState = incidentsUiState,
                onAddIncidentDates = incidentsViewModel::addDates,
                onAddVacation = incidentsViewModel::addVacation,
                onDeleteIncident = incidentsViewModel::delete,
                modifier = modifier,
            )
        }
        else -> {
            val settings = requireNotNull(uiState.savedSettings)
            val calendarMonth = remember(settings, visibleMonth) {
                MonthlyCalendarGenerator.generate(
                    yearMonth = visibleMonth,
                    shiftCalculator = ShiftCalculator(
                        referenceDate = settings.referenceDate,
                        shifts = settings.shifts,
                    ),
                )
            }
            CalendarScreen(
                calendarMonth = calendarMonth,
                today = calendarViewModel.today,
                onPreviousMonth = calendarViewModel::showPreviousMonth,
                onNextMonth = calendarViewModel::showNextMonth,
                onToday = calendarViewModel::showToday,
                onOpenSettings = { showSettings = true },
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onPatternSelected: (ShiftPatternType) -> Unit,
    onShiftAdded: (ShiftType) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
    onReferenceDateSelected: (LocalDate) -> Unit,
    onSave: () -> Unit,
    incidentsUiState: IncidentsUiState,
    onAddIncidentDates: (IncidentType, Set<LocalDate>) -> Unit,
    onAddVacation: (LocalDate, LocalDate) -> Unit,
    onDeleteIncident: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val backDescription = stringResource(R.string.settings_back_description)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (canNavigateBack) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.semantics {
                                contentDescription = backDescription
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.settings_back_symbol),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ShiftSettingsCard(
                        uiState = uiState,
                        onPatternSelected = onPatternSelected,
                        onShiftAdded = onShiftAdded,
                        onRemoveLast = onRemoveLast,
                        onClear = onClear,
                        onChooseDate = { showDatePicker = true },
                        onSave = onSave,
                    )
                }
                item {
                    val shiftCalculator = uiState.selectedShifts.takeIf { it.isNotEmpty() }?.let { shifts ->
                        ShiftCalculator(uiState.referenceDate, shifts)
                    }
                    IncidentsSettingsCard(
                        uiState = incidentsUiState,
                        shiftCalculator = shiftCalculator,
                        onAddDates = onAddIncidentDates,
                        onAddVacation = onAddVacation,
                        onDelete = onDeleteIncident,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        ReferenceDatePicker(
            initialDate = uiState.referenceDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                onReferenceDateSelected(date)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun ShiftSettingsCard(
    uiState: SettingsUiState,
    onPatternSelected: (ShiftPatternType) -> Unit,
    onShiftAdded: (ShiftType) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
    onChooseDate: () -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pattern_heading),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = uiState.patternType == ShiftPatternType.SIX_BY_SIX,
                    onClick = { onPatternSelected(ShiftPatternType.SIX_BY_SIX) },
                    label = { Text(stringResource(R.string.pattern_six_by_six)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = uiState.patternType == ShiftPatternType.CUSTOM,
                    onClick = { onPatternSelected(ShiftPatternType.CUSTOM) },
                    label = { Text(stringResource(R.string.pattern_custom)) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.patternType == ShiftPatternType.CUSTOM) {
                Text(
                    text = stringResource(R.string.custom_pattern_instructions),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShiftType.entries.forEach { shift ->
                        ShiftAddButton(
                            shift = shift,
                            onClick = { onShiftAdded(shift) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.pattern_preview),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
            )
            ShiftSequence(
                shifts = uiState.selectedShifts,
                emptyText = stringResource(R.string.custom_pattern_empty),
            )

            if (uiState.patternType == ShiftPatternType.CUSTOM) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onRemoveLast,
                        enabled = uiState.customShifts.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.custom_pattern_remove_last))
                    }
                    TextButton(
                        onClick = onClear,
                        enabled = uiState.customShifts.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.custom_pattern_clear))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            ReferenceDateSection(
                date = uiState.referenceDate,
                onChooseDate = onChooseDate,
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.settings_save))
                }
            }
            when (uiState.saveResult) {
                SaveResult.SUCCESS -> StatusText(
                    text = stringResource(R.string.settings_saved),
                    isError = false,
                )
                SaveResult.ERROR -> StatusText(
                    text = stringResource(R.string.settings_save_error),
                    isError = true,
                )
                null -> Unit
            }
        }
    }
}

@Composable
private fun ShiftAddButton(
    shift: ShiftType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = shift.visualColors()
    val label = stringResource(shift.labelResource())
    val addDescription = stringResource(R.string.custom_pattern_add_shift, label)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.background,
            contentColor = colors.foreground,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier
            .aspectRatio(1f)
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .semantics { contentDescription = addDescription },
    ) {
        Text(
            text = stringResource(shift.abbreviationResource()),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ShiftSequence(
    shifts: List<ShiftType>,
    emptyText: String,
) {
    if (shifts.isEmpty()) {
        Text(
            text = emptyText,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shifts.chunked(SHIFTS_PER_ROW).forEach { rowShifts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowShifts.forEach { shift ->
                        ShiftToken(
                            shift = shift,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(SHIFTS_PER_ROW - rowShifts.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftToken(
    shift: ShiftType,
    modifier: Modifier = Modifier,
) {
    val colors = shift.visualColors()
    val label = stringResource(shift.labelResource())

    Surface(
        color = colors.background,
        contentColor = colors.foreground,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(shift.abbreviationResource()),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReferenceDateSection(
    date: LocalDate,
    onChooseDate: () -> Unit,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val formattedDate = remember(date, locale) {
        date.format(
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.reference_date_heading),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.reference_date_explanation),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onChooseDate) {
            Text(formattedDate)
        }
    }
}

private const val SHIFTS_PER_ROW = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toDatePickerMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { milliseconds ->
                        onDateSelected(milliseconds.toLocalDateFromDatePicker())
                    }
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
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun StatusText(
    text: String,
    isError: Boolean,
) {
    Text(
        text = text,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    CuadranteTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                isLoading = false,
                patternType = ShiftPatternType.SIX_BY_SIX,
                referenceDate = LocalDate.of(2026, 9, 18),
            ),
            canNavigateBack = true,
            onBack = {},
            onPatternSelected = {},
            onShiftAdded = {},
            onRemoveLast = {},
            onClear = {},
            onReferenceDateSelected = {},
            onSave = {},
            incidentsUiState = IncidentsUiState(isLoading = false),
            onAddIncidentDates = { _, _ -> },
            onAddVacation = { _, _ -> },
            onDeleteIncident = {},
        )
    }
}
