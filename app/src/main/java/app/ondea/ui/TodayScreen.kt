package app.ondea.ui

import app.ondea.domain.isAfter
import app.ondea.domain.isBefore
import app.ondea.domain.minusDays
import app.ondea.domain.plusDays
import app.ondea.domain.toEpochDay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.domain.CycleEngine
import app.ondea.domain.FertilityChance
import app.ondea.domain.Forecast
import app.ondea.domain.OvulationSource
import app.ondea.domain.PredictionBasis
import app.ondea.domain.Waiting
import app.ondea.ui.theme.CycleColors

@Composable
fun TodayScreen(vm: MainViewModel, s: UiState.Ready) {
    val engine = s.engine
    val today = engine.today
    val forecast = engine.forecast
    val entry = engine.dayInfo(today).entry
    var pickStart by rememberSaveable { mutableStateOf(false) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    var suggestNew by rememberSaveable { mutableStateOf(false) }
    var explain by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            today.long(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        val ringDescription = forecast?.let {
            stringResource(R.string.cycle_day, it.cycleDay) + ". " +
                stringResource(R.string.of_days, CycleEngine.days(engine.current!!.start, it.nextPeriod))
        } ?: stringResource(R.string.not_tracking_title)
        CycleRing(engine, Modifier.size(250.dp).clearAndSetSemantics { contentDescription = ringDescription }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (forecast != null) {
                    Text(stringResource(R.string.cycle_day_label), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(forecast.cycleDay.toString(), style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.of_days, CycleEngine.days(engine.current!!.start, forecast.nextPeriod)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("–", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (forecast == null) {
            Text(stringResource(R.string.not_tracking_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.not_tracking_text), textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { pickStart = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.new_cycle))
            }
        } else {
            Status(engine, forecast, onExplain = { explain = true })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val palette = CycleColors.current
                StatTile(stringResource(R.string.stat_fertile), forecast.fertileStart.dayMonth() + "–" + forecast.fertileEnd.dayMonth(), palette.fertile, weight = 1.6f)
                StatTile(stringResource(R.string.stat_ovulation), forecast.ovulation.dayMonth(), palette.ovulation)
                StatTile(stringResource(R.string.stat_next_period), forecast.nextPeriod.dayMonth(), palette.period)
            }
            if (forecast.daysLate == 0) FertilityCard(forecast)
        }

        SectionCard(title = stringResource(R.string.morning_temperature), icon = Icons.Outlined.Thermostat) {
            key(today) {
                TemperatureInput(
                    date = today,
                    savedC = entry?.tempC,
                    fahrenheit = s.data.settings.fahrenheit,
                    lastTempC = engine.lastTemperature(),
                    onSave = { c -> vm.editEntry(today, R.string.msg_saved) { it.copy(tempC = c) } },
                )
            }
        }

        SectionCard(title = stringResource(R.string.todays_observations), icon = Icons.Outlined.EditNote) {
            DayDetails(entry) { change ->
                val before = entry?.flow ?: 0
                vm.editEntry(today, change = change)
                // A real period (not spotting) late in the cycle almost always means a new cycle.
                val after = change(entry ?: app.ondea.data.DayEntry(today.toEpochDay())).flow
                if (after >= 2 && before < 2 && (forecast == null || forecast.cycleDay > 10)) suggestNew = true
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.new_cycle))
            }
            if (forecast != null) {
                TextButton(onClick = { confirmStop = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.pause_tracking))
                }
            }
        }

        Text(
            stringResource(R.string.disclaimer_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (pickStart) {
        PastDatePickerDialog(today, today, onPicked = vm::startCycle, onDismiss = { pickStart = false })
    }
    if (explain && forecast != null) {
        OvulationSheet(
            engine = engine,
            onChange = vm::updateSettings,
            onDismiss = { explain = false },
        )
    }
    if (suggestNew) {
        ConfirmDialog(
            stringResource(R.string.new_cycle), stringResource(R.string.new_cycle_suggest),
            stringResource(R.string.start), onConfirm = { vm.startCycle(today) }, onDismiss = { suggestNew = false },
        )
    }
    if (confirmStop) {
        ConfirmDialog(
            stringResource(R.string.pause_tracking), stringResource(R.string.pause_confirm),
            stringResource(R.string.pause), onConfirm = vm::stopTracking, onDismiss = { confirmStop = false },
        )
    }
}

@Composable
private fun Status(engine: CycleEngine, f: Forecast, onExplain: () -> Unit) {
    val palette = CycleColors.current
    val today = engine.today
    val inFertile = !today.isBefore(f.fertileStart) && !today.isAfter(f.fertileEnd)
    val inPeak = !today.isBefore(f.peakStart) && !today.isAfter(f.ovulation)
    val pregnant = f.highDays >= 18

    val accent: Color
    val headline: String
    val detail: String
    when {
        f.daysLate > 0 -> {
            accent = palette.period
            headline = pluralStringResource(R.plurals.period_late, f.daysLate, f.daysLate)
            detail = if (pregnant) stringResource(R.string.maybe_pregnant, f.highDays) else stringResource(R.string.late_hint)
        }
        f.closed && today.isAfter(f.fertileEnd) -> {
            accent = palette.ovulation
            headline = stringResource(R.string.ovulation_confirmed)
            detail = if (pregnant) stringResource(R.string.maybe_pregnant, f.highDays)
            else stringResource(R.string.ovulated_on, f.ovulation.dayMonth(), f.nextPeriod.dayMonth())
        }
        inFertile -> {
            val peakToday = inPeak || f.mucusToday == 4
            accent = if (peakToday) palette.peak else palette.fertile
            headline = stringResource(if (peakToday) R.string.peak_fertility else R.string.fertile_window)
            detail = when {
                f.mucusToday == 4 -> stringResource(R.string.eggwhite_today)
                f.window.waitingFor == Waiting.MUCUS -> stringResource(R.string.waiting_mucus, f.fertileEnd.dayMonth())
                f.ovulationSource == OvulationSource.LH_TEST -> stringResource(R.string.waiting_temperature_after_lh, f.ovulation.dayMonth())
                f.ovulationSource == OvulationSource.MUCUS -> stringResource(R.string.waiting_temperature_after_peak, f.ovulation.dayMonth())
                f.ovulationSource == OvulationSource.TEMPERATURE -> stringResource(R.string.fertile_until_confirmed, f.fertileEnd.dayMonth())
                else -> stringResource(R.string.ovulation_expected, f.ovulation.dayMonth())
            }
        }
        today.isBefore(f.fertileStart) -> {
            accent = MaterialTheme.colorScheme.primary
            val inDays = CycleEngine.days(today, f.fertileStart)
            headline = pluralStringResource(R.plurals.fertile_in, inDays, inDays)
            detail = stringResource(R.string.ovulation_expected, f.ovulation.dayMonth())
        }
        else -> {
            accent = MaterialTheme.colorScheme.primary
            headline = stringResource(R.string.waiting_for_shift)
            detail = stringResource(R.string.waiting_for_shift_text)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(headline, style = MaterialTheme.typography.headlineSmall, color = accent, textAlign = TextAlign.Center)
        Text(detail, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val source = when (f.ovulationSource) {
            OvulationSource.TEMPERATURE -> R.string.source_temperature
            OvulationSource.LH_TEST -> R.string.source_lh
            OvulationSource.MUCUS -> R.string.source_mucus
            OvulationSource.PREDICTED -> when (engine.predictionBasis) {
                PredictionBasis.OWN_DAY -> R.string.source_setting
                PredictionBasis.LEARNED -> R.string.source_prediction
                PredictionBasis.TYPICAL -> R.string.source_typical
            }
        }
        // Tapping the source explains the date and lets the user change it.
        Surface(onClick = onExplain, shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
            Row(
                Modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(source), style = MaterialTheme.typography.labelMedium, color = accent)
                Icon(Icons.Outlined.Tune, stringResource(R.string.why_title), Modifier.size(16.dp), tint = accent)
            }
        }
    }
}

/**
 * Conception chance by day relative to ovulation (Wilcox et al., NEJM 1995), with today highlighted.
 * Bars rather than a single number make the shape of the fertile window easy to grasp.
 */
@Composable
private fun FertilityCard(f: Forecast) {
    val palette = CycleColors.current
    val offset = f.offsetFromOvulation
    val level = if (f.closed && offset > 0) FertilityChance.Level.LOW else FertilityChance.level(offset)
    val (levelText, color) = when (level) {
        FertilityChance.Level.HIGH -> stringResource(R.string.chance_high) to palette.peak
        FertilityChance.Level.MEDIUM -> stringResource(R.string.chance_medium) to palette.fertile
        FertilityChance.Level.LOW -> stringResource(R.string.chance_low) to MaterialTheme.colorScheme.outline
    }
    val pct = (FertilityChance.probability(offset) * 100).roundToInt()
    val description = if (pct > 0) stringResource(R.string.chance_percent, pct) else stringResource(R.string.chance_outside)

    SectionCard(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        title = stringResource(R.string.chance_title),
        icon = Icons.Outlined.Favorite,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(levelText, style = MaterialTheme.typography.headlineSmall, color = color)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.height(72.dp).clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                FertilityChance.BY_DAY.forEachIndexed { i, p ->
                    val isToday = i - 5 == offset
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.width(18.dp).height((p / 0.33 * 52).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(if (isToday) palette.peak else palette.fertile.copy(alpha = 0.3f)),
                        )
                        Text(
                            if (i == 5) stringResource(R.string.ovulation_short) else (i - 5).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) palette.peak else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            stringResource(if (f.ovulationSource == OvulationSource.PREDICTED) R.string.chance_source_predicted else R.string.chance_source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
