package app.ondea.ui

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import app.ondea.data.usesOwnOvulationDay
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
                inUse = f.ovulationSource == OvulationSource.TEMPERATURE,
            )
            SignRow(
                Icons.Outlined.Science, palette.ovulation, stringResource(R.string.sign_lh),
                cycle.lhPositiveIndex?.let { stringResource(R.string.sign_positive_on, dateOf(it)) } ?: notYet,
                inUse = f.ovulationSource == OvulationSource.LH_TEST,
            )
            SignRow(
                Icons.Outlined.WaterDrop, palette.mucus, stringResource(R.string.sign_mucus),
                cycle.mucus.peakIndex?.let { stringResource(R.string.sign_peak_on, dateOf(it)) } ?: notYet,
                inUse = f.ovulationSource == OvulationSource.MUCUS,
            )
            SignRow(
                Icons.Outlined.CalendarMonth, palette.fertile, stringResource(R.string.sign_prediction),
                if (engine.predictionBasis == PredictionBasis.OWN_DAY && engine.data.settings.usesOwnOvulationDay)
                    stringResource(R.string.prediction_setting_detail, engine.predictedOvulationIndex + 1)
                else ondeaEstimateText(engine),
                inUse = f.ovulationSource == OvulationSource.PREDICTED,
            )

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
 * Both ovulation-day values side by side: Ondea's estimate and the user's own day, plus which one
 * predicts. Switching never erases either value.
 */
@Composable
fun OvulationDayControls(engine: CycleEngine, onChange: ((Settings) -> Settings) -> Unit) {
    val settings = engine.data.settings
    val useOwn = settings.usesOwnOvulationDay
    val ownDay = settings.ovulationDay ?: (engine.ondeaOvulationIndex + 1)
    val palette = CycleColors.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.prediction_to_use), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !useOwn,
                onClick = { onChange { it.copy(useOwnOvulationDay = false) } },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.use_ondea)) }
            SegmentedButton(
                selected = useOwn,
                onClick = { onChange { it.copy(ovulationDay = it.ovulationDay ?: ownDay, useOwnOvulationDay = true) } },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.use_mine)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.CalendarMonth, palette.fertile)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ondea_estimate), style = MaterialTheme.typography.bodyLarge)
                Text(ondeaEstimateText(engine), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.my_usual_day), style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.my_usual_day_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            NumberStepper(ownDay, 8..35, stringResource(R.string.day_n, ownDay)) { v ->
                onChange { it.copy(ovulationDay = v, useOwnOvulationDay = it.useOwnOvulationDay ?: false) }
            }
        }
    }
}

/** One line saying what Ondea's own estimate is based on. */
@Composable
fun ondeaEstimateText(engine: CycleEngine): String {
    val day = engine.ondeaOvulationIndex + 1
    return when {
        engine.learnedOvulationIndex != null -> pluralStringResource(
            R.plurals.prediction_learned_detail, engine.pastOvulations.size, day, engine.pastOvulations.size,
        )
        engine.data.settings.ovulationDay != null -> stringResource(R.string.ondea_estimate_own, day)
        else -> stringResource(R.string.prediction_detail, day, engine.avgCycleLength, engine.avgLutealLength)
    }
}
