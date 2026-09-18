package es.robertodorado.cuadrante.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.robertodorado.cuadrante.R
import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
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
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreen(
        uiState = uiState,
        onPatternSelected = viewModel::selectPattern,
        onShiftAdded = viewModel::addShift,
        onRemoveLast = viewModel::removeLastShift,
        onClear = viewModel::clearCustomShifts,
        onReferenceDateSelected = viewModel::setReferenceDate,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onPatternSelected: (ShiftPatternType) -> Unit,
    onShiftAdded: (ShiftType) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
    onReferenceDateSelected: (LocalDate) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
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
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_intro),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                item {
                    PatternCard(
                        uiState = uiState,
                        onPatternSelected = onPatternSelected,
                        onShiftAdded = onShiftAdded,
                        onRemoveLast = onRemoveLast,
                        onClear = onClear,
                    )
                }
                item {
                    ReferenceDateCard(
                        date = uiState.referenceDate,
                        onChooseDate = { showDatePicker = true },
                    )
                }
                item {
                    Button(
                        onClick = onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
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
                    Spacer(modifier = Modifier.height(20.dp))
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
private fun PatternCard(
    uiState: SettingsUiState,
    onPatternSelected: (ShiftPatternType) -> Unit,
    onShiftAdded: (ShiftType) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShiftType.entries.forEach { shift ->
                        ShiftAddButton(shift = shift, onClick = { onShiftAdded(shift) })
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
        }
    }
}

@Composable
private fun ShiftAddButton(
    shift: ShiftType,
    onClick: () -> Unit,
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
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        modifier = Modifier
            .size(52.dp)
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shifts) { shift ->
                ShiftToken(shift)
            }
        }
    }
}

@Composable
private fun ShiftToken(shift: ShiftType) {
    val colors = shift.visualColors()
    val label = stringResource(shift.labelResource())

    Surface(
        color = colors.background,
        contentColor = colors.foreground,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(44.dp)
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
private fun ReferenceDateCard(
    date: LocalDate,
    onChooseDate: () -> Unit,
) {
    val formattedDate = remember(date) {
        date.format(
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.getDefault()),
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.reference_date_heading),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.reference_date_explanation),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = onChooseDate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(formattedDate)
            }
        }
    }
}

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
            onPatternSelected = {},
            onShiftAdded = {},
            onRemoveLast = {},
            onClear = {},
            onReferenceDateSelected = {},
            onSave = {},
        )
    }
}
