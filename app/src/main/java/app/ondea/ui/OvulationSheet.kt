package app.ondea.ui

import androidx.compose.material.icons.outlined.AutoGraph

import app.ondea.domain.isAfter
import app.ondea.domain.isBefore
import app.ondea.domain.minusDays
import app.ondea.domain.plusDays
import app.ondea.domain.toEpochDay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.domain.CycleEngine
import app.ondea.data.Settings
import app.ondea.domain.OvulationSource
import app.ondea.domain.PredictionBasis
import app.ondea.ui.theme.CycleColors

/**
 * Explains where the current ovulation date comes from (each sign, which one is in use)
 * and lets the user set their usual ovulation day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvulationSheet(engine: CycleEngine, onChange: ((Settings) -> Settings) -> Unit, onDismiss: () -> Unit) {
    val cycle = engine.current ?: return
    val f = engine.forecast ?: return
    val palette = CycleColors.current
    fun dateOf(index: Int) = cycle.start.plusDays(index.toLong()).dayMonth()
    val notYet = stringResource(R.string.sign_not_yet)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.why_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.why_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.why_timing), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            SignRow(
                Icons.Outlined.Thermostat, palette.ovulation, stringResource(R.string.sign_temperature),
                cycle.shift?.let { stringResource(R.string.sign_ovulation_on, dateOf(it.ovulationIndex)) } ?: notYet,
                inUse = f.ovulationSource == OvulationSource.TEMPERATURE || f.ovulationSource == OvulationSource.TEMP_RISING,
            )
            SignRow(
                Icons.Outlined.Science, palette.ovulation, stringResource(R.string.sign_lh),
                cycle.lhPositiveIndex?.let { stringResource(R.string.sign_positive_on, dateOf(it)) } ?: notYet,
                inUse = f.ovulationSource == OvulationSource.LH_TEST,
            )
            SignRow(
                Icons.Outlined.WaterDrop, palette.mucus, stringResource(R.string.sign_mucus),
                cycle.mucus.peakIndex?.let { stringResource(R.string.sign_peak_on, dateOf(it)) } ?: notYet,
                inUse = f.ovulationSource == OvulationSource.MUCUS || f.ovulationSource == OvulationSource.MUCUS_SIGN,
            )
            SignRow(
                Icons.Outlined.CalendarMonth, palette.fertile, stringResource(R.string.sign_prediction),
                forecastText(engine),
                inUse = f.ovulationSource.calendar,
            )

            if (!f.ovulationSource.firm && f.ovulation != f.plannedOvulation) {
                SignRow(
                    Icons.Outlined.AutoGraph, palette.ovulation, stringResource(R.string.estimate_today),
                    stringResource(R.string.estimate_today_detail, f.ovulation.dayMonth(), f.plannedOvulation.dayMonth()),
                    inUse = true,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OvulationDayControls(engine, onChange)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SignRow(icon: ImageVector, tint: Color, title: String, status: String, inUse: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (inUse) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.15f)) {
                Text(stringResource(R.string.in_use), style = MaterialTheme.typography.labelMedium, color = tint,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }
    }
}

/**
 * The one ovulation day used for the forecast. Only the user changes it. Below it, what the signs of
 * finished cycles show, with a button to use that day when it differs.
 */
@Composable
fun OvulationDayControls(engine: CycleEngine, onChange: ((Settings) -> Settings) -> Unit) {
    val day = engine.predictedOvulationIndex + 1
    val learned = engine.learnedOvulationIndex?.plus(1)
    val palette = CycleColors.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.forecast_day), style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.forecast_day_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            NumberStepper(day, 8..35, stringResource(R.string.day_n, day)) { v ->
                onChange { it.copy(ovulationDay = v) }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.AutoGraph, palette.ovulation)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.signs_show), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (learned == null) stringResource(R.string.signs_show_none)
                    else pluralStringResource(R.plurals.prediction_learned_detail, engine.pastOvulations.size, learned, engine.pastOvulations.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (learned != null && learned != day) {
            OutlinedButton(
                onClick = { onChange { it.copy(ovulationDay = learned, adviceDismissedDay = null) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.advice_use, learned)) }
        }
    }
}

/** One line saying where the forecast day comes from. */
@Composable
fun forecastText(engine: CycleEngine): String {
    val day = engine.predictedOvulationIndex + 1
    return when (engine.predictionBasis) {
        PredictionBasis.LEARNED -> pluralStringResource(
            R.plurals.prediction_learned_detail, engine.pastOvulations.size, day, engine.pastOvulations.size,
        )
        PredictionBasis.OWN_DAY -> stringResource(R.string.prediction_setting_detail, day)
        else -> stringResource(R.string.prediction_detail, day, engine.avgCycleLength, engine.avgLutealLength)
    }
}
