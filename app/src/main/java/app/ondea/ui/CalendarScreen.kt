package app.ondea.ui

import app.ondea.domain.isAfter
import app.ondea.domain.isBefore
import app.ondea.domain.minusDays
import app.ondea.domain.plusDays
import app.ondea.domain.toEpochDay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.domain.CycleEngine
import app.ondea.domain.DayInfo
import app.ondea.ui.theme.CycleColors
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(vm: MainViewModel, s: UiState.Ready) {
    val engine = s.engine
    val today = engine.today
    val palette = CycleColors.current
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today.toJavaLocalDate())) }
    var selected by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous))
            }
            Text(month.title(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            FilledTonalIconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next))
            }
        }

        SectionCard {
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    Text(
                        firstDayOfWeek.plus(d.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .trimEnd('.').replaceFirstChar { it.titlecase() },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val offset = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            val cells = offset + month.lengthOfMonth()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (week in 0 until (cells + 6) / 7) {
                    Row(Modifier.fillMaxWidth()) {
                        for (d in 0 until 7) {
                            val dayNum = week * 7 + d - offset + 1
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (dayNum in 1..month.lengthOfMonth()) {
                                    val date = month.atDay(dayNum).toKotlinLocalDate()
                                    DayCell(date, engine.dayInfo(date), date == today, enabled = !date.isAfter(today)) {
                                        selected = date
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(palette.period, stringResource(R.string.legend_period))
            LegendItem(palette.period.copy(alpha = 0.4f), stringResource(R.string.legend_period_predicted))
            LegendItem(palette.fertile, stringResource(R.string.legend_fertile))
            LegendItem(palette.fertile.copy(alpha = 0.45f), stringResource(R.string.legend_fertile_predicted))
            LegendItem(palette.ovulation, stringResource(R.string.legend_ovulation))
        }
        Text(stringResource(R.string.calendar_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    selected?.let { date ->
        val info = engine.dayInfo(date)
        val cycleDay = engine.cycleIndexOf(date)?.let { CycleEngine.days(engine.cycles[it].start, date) + 1 }
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column {
                    Text(date.long(), style = MaterialTheme.typography.titleLarge)
                    if (cycleDay != null) {
                        Text(stringResource(R.string.cycle_day, cycleDay), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cycle_started_here), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = info.cycleStart, onCheckedChange = {
                        if (it) vm.startCycle(date) else vm.removeCycleStart(date)
                    })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                key(date) {
                    TemperatureInput(
                        date = date,
                        savedC = info.entry?.tempC,
                        fahrenheit = s.data.settings.fahrenheit,
                        lastTempC = engine.lastTemperature(),
                        onSave = { c -> vm.editEntry(date, R.string.msg_saved) { it.copy(tempC = c) } },
                    )
                    DayDetails(info.entry) { change -> vm.editEntry(date, change = change) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, info: DayInfo, isToday: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val palette = CycleColors.current
    val flow = info.entry?.flow ?: 0
    val (bg, fg) = when {
        flow > 0 -> palette.period to Color.White
        info.ovulation -> palette.ovulation to Color.White
        info.peak && info.fertile -> palette.fertile to Color.White
        info.fertile -> palette.fertileSoft to MaterialTheme.colorScheme.onSurface
        info.ovulationPredicted -> palette.ovulationSoft to MaterialTheme.colorScheme.onSurface
        info.periodPredicted -> palette.periodSoft to MaterialTheme.colorScheme.onSurface
        info.fertilePredicted -> palette.fertileSoft.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onSurface
        else -> Color.Transparent to MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bg)
            .then(if (isToday) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = if (enabled) fg else fg.copy(alpha = 0.55f),
            fontWeight = if (isToday || info.cycleStart) FontWeight.ExtraBold else FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val markColor = if (fg == Color.White) Color.White else MaterialTheme.colorScheme.primary
            if (info.entry?.tempC != null) Box(Modifier.size(4.dp).clip(CircleShape).background(markColor))
            if (info.entry?.intercourse == true) {
                Icon(Icons.Filled.Favorite, null, Modifier.size(7.dp), tint = if (fg == Color.White) Color.White else palette.ovulation)
            }
        }
    }
}
