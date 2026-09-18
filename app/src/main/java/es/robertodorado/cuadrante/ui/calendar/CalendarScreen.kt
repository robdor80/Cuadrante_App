package es.robertodorado.cuadrante.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.robertodorado.cuadrante.R
import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import es.robertodorado.cuadrante.ui.abbreviationResource
import es.robertodorado.cuadrante.ui.labelResource
import es.robertodorado.cuadrante.ui.theme.TodayIndicator
import es.robertodorado.cuadrante.ui.theme.visualColor
import es.robertodorado.cuadrante.ui.theme.visualColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarMonth: CalendarMonth,
    incidents: List<MonthlyIncidentEntry>,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val monthTitle = remember(calendarMonth.yearMonth, locale) {
        calendarMonth.yearMonth.month
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(locale) else character.toString()
            } + " " + calendarMonth.yearMonth.year
    }
    val incidentTypesByDate = remember(incidents, calendarMonth.yearMonth) {
        incidentTypesByDate(
            incidents = incidents.map(MonthlyIncidentEntry::incident),
            yearMonth = calendarMonth.yearMonth,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.calendar_settings),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonthNavigationButton(
                            symbol = stringResource(R.string.calendar_previous_symbol),
                            description = stringResource(R.string.calendar_previous_month),
                            onClick = onPreviousMonth,
                        )
                        Text(
                            text = monthTitle,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        MonthNavigationButton(
                            symbol = stringResource(R.string.calendar_next_symbol),
                            description = stringResource(R.string.calendar_next_month),
                            onClick = onNextMonth,
                        )
                    }

                    TextButton(
                        onClick = onToday,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.calendar_go_to_today))
                    }

                    WeekdayHeader()

                    calendarMonth.weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            week.forEach { day ->
                                if (day == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(CALENDAR_CELL_ASPECT_RATIO),
                                    )
                                } else {
                                    CalendarDayCell(
                                        day = day,
                                        incidentTypes = incidentTypesByDate[day.date].orEmpty(),
                                        isToday = day.date == today,
                                        locale = locale,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(CALENDAR_CELL_ASPECT_RATIO),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            MonthlyIncidentsCard(
                incidents = incidents,
                locale = locale,
            )
        }
    }
}

@Composable
private fun MonthlyIncidentsCard(
    incidents: List<MonthlyIncidentEntry>,
    locale: Locale,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.calendar_incidents_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (incidents.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_incidents_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                incidents.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    MonthlyIncidentRow(entry = entry, locale = locale)
                }
            }
        }
    }
}

@Composable
private fun MonthlyIncidentRow(
    entry: MonthlyIncidentEntry,
    locale: Locale,
) {
    val typeLabel = stringResource(
        when (entry.incident.type) {
            IncidentType.AP -> R.string.incident_type_ap
            IncidentType.PERMISSION -> R.string.incident_type_permission
            IncidentType.VACATION -> R.string.incident_type_vacation
        },
    )
    val dateText = remember(entry, locale) { formatMonthlyIncident(entry, locale) }

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = typeLabel,
            color = entry.incident.type.visualColor(),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = dateText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatMonthlyIncident(
    entry: MonthlyIncidentEntry,
    locale: Locale,
): String {
    val shortFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    return when (val incident = entry.incident) {
        is DateIncident -> entry.datesInMonth.joinToString(", ") { it.format(shortFormatter) }
        is VacationIncident -> {
            val formatter = if (incident.startDate.year == incident.endDate.year) {
                shortFormatter
            } else {
                DateTimeFormatter.ofPattern("d MMM yyyy", locale)
            }
            "${incident.startDate.format(formatter)} – ${incident.endDate.format(formatter)}"
        }
    }
}

@Composable
private fun MonthNavigationButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineSmall,
        )
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        weekdays.forEach { weekdayResource ->
            Text(
                text = stringResource(weekdayResource),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    incidentTypes: List<IncidentType>,
    isToday: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val shiftColors = day.shift.visualColors()
    val shiftLabel = stringResource(day.shift.labelResource())
    val formattedDate = remember(day.date, locale) {
        day.date.format(
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(locale),
        )
    }
    val dayDescription = stringResource(
        R.string.calendar_day_description,
        formattedDate,
        shiftLabel,
    )
    val shape = MaterialTheme.shapes.small
    val borderColor = if (isToday) TodayIndicator else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isToday) 3.dp else 1.dp

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = shape,
        modifier = modifier
            .border(borderWidth, borderColor, shape)
            .semantics { contentDescription = dayDescription },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (incidentTypes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(INCIDENT_RESERVED_HEIGHT))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SHIFT_BAND_HEIGHT)
                        .background(shiftColors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(day.shift.abbreviationResource()),
                        color = shiftColors.foreground,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (incidentTypes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = SHIFT_BAND_HEIGHT + INCIDENT_BAND_GAP),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    incidentTypes.forEach { incidentType ->
                        Box(
                            modifier = Modifier
                                .size(INCIDENT_DOT_DIAMETER)
                                .background(
                                    color = incidentType.visualColor(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private const val CALENDAR_CELL_ASPECT_RATIO = 0.82f
private val INCIDENT_DOT_DIAMETER = 6.dp
private val INCIDENT_NUMBER_GAP = 2.dp
private val INCIDENT_BAND_GAP = 4.dp
private val INCIDENT_RESERVED_HEIGHT = INCIDENT_NUMBER_GAP + INCIDENT_DOT_DIAMETER + INCIDENT_BAND_GAP
private val SHIFT_BAND_HEIGHT = 22.dp
