package app.ondea.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ondea.R
import app.ondea.data.DayEntry
import app.ondea.domain.Temperature
import app.ondea.ui.theme.CycleColors
import app.ondea.ui.theme.Nunito
import java.time.LocalDate

/**
 * Large temperature input with +/- steps. [onSave] receives °C, or null to clear the reading.
 * Keyed on date + stored value by the caller so it resets after saving.
 */
@Composable
fun TemperatureInput(
    date: LocalDate,
    savedC: Double?,
    fahrenheit: Boolean,
    lastTempC: Double?,
    onSave: (Double?) -> Unit,
) {
    var text by rememberSaveable(date, savedC) { mutableStateOf(savedC?.let { Temperature.format(it, fahrenheit) } ?: "") }
    val focus = LocalFocusManager.current
    val parsed = Temperature.parse(text, fahrenheit)
    val invalid = text.isNotBlank() && parsed == null
    val dirty = parsed != savedC && !(text.isBlank() && savedC == null)

    fun nudge(direction: Int) {
        val base = parsed ?: lastTempC ?: 36.5
        val c = Temperature.fromDisplay(Temperature.toDisplay(base, fahrenheit) + direction * Temperature.step(fahrenheit), fahrenheit)
        if (c in Temperature.VALID_RANGE_C) text = Temperature.format(c, fahrenheit)
    }

    fun save() {
        focus.clearFocus()
        if (!invalid) onSave(parsed)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton(Icons.Filled.Remove, stringResource(R.string.decrease)) { nudge(-1) }
            Row(verticalAlignment = Alignment.Bottom) {
                val numberStyle = TextStyle(
                    fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, textAlign = TextAlign.Center,
                    color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                BasicTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                    textStyle = numberStyle,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.width(170.dp),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                lastTempC?.let { Temperature.format(it, fahrenheit) } ?: if (fahrenheit) "97.70" else "36.50",
                                style = numberStyle.copy(color = MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        inner()
                    },
                )
                Text(
                    Temperature.unit(fahrenheit),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            StepButton(Icons.Filled.Add, stringResource(R.string.increase)) { nudge(1) }
        }
        Text(
            if (invalid) stringResource(R.string.temperature_invalid) else stringResource(R.string.temperature_hint),
            style = MaterialTheme.typography.bodySmall,
            color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        if (dirty || savedC == null) {
            Button(onClick = ::save, enabled = !invalid && dirty, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.save_temperature), style = MaterialTheme.typography.titleSmall)
            }
        } else {
            FilledTonalButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.temperature_saved), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) { Icon(icon, description, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
}

/** Period, mucus and other observations of one day. Every change is saved immediately. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayDetails(entry: DayEntry?, onChange: ((DayEntry) -> DayEntry) -> Unit) {
    val palette = CycleColors.current
    val flow = entry?.flow ?: 0
    val mucus = entry?.mucus ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(stringResource(R.string.period))
        ChoiceChips(
            labels = listOf(R.string.flow_none, R.string.flow_spotting, R.string.flow_light, R.string.flow_medium, R.string.flow_heavy)
                .map { stringResource(it) },
            selected = flow,
            accent = palette.period,
            onSelect = { v -> onChange { it.copy(flow = v) } },
            leading = { i ->
                if (i > 0) Icon(Icons.Outlined.WaterDrop, null, Modifier.size((10 + i * 2).dp), tint = palette.period)
            },
        )

        Label(stringResource(R.string.mucus))
        ChoiceChips(
            labels = listOf(R.string.mucus_none, R.string.mucus_dry, R.string.mucus_sticky, R.string.mucus_creamy, R.string.mucus_eggwhite)
                .map { stringResource(it) },
            selected = mucus,
            accent = palette.mucus,
            onSelect = { v -> onChange { it.copy(mucus = v) } },
        )
        Text(
            stringResource(
                when (mucus) {
                    1 -> R.string.mucus_dry_hint
                    2 -> R.string.mucus_sticky_hint
                    3 -> R.string.mucus_creamy_hint
                    4 -> R.string.mucus_eggwhite_hint
                    else -> R.string.mucus_hint
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Label(stringResource(R.string.lh_test))
        ChoiceChips(
            labels = listOf(R.string.lh_none, R.string.lh_negative, R.string.lh_positive).map { stringResource(it) },
            selected = entry?.lh ?: 0,
            accent = palette.ovulation,
            onSelect = { v -> onChange { it.copy(lh = v) } },
        )
        Text(
            stringResource(R.string.lh_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Label(stringResource(R.string.other))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleChip(stringResource(R.string.intercourse), Icons.Outlined.Favorite, entry?.intercourse == true, palette.ovulation) { v ->
                onChange { it.copy(intercourse = v) }
            }
            ToggleChip(stringResource(R.string.disturbed_short), Icons.Outlined.Sick, entry?.disturbed == true, palette.disturbed) { v ->
                onChange { it.copy(disturbed = v) }
            }
        }
        AnimatedVisibility(entry?.disturbed == true) {
            Text(stringResource(R.string.disturbed_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        NoteField(entry?.note ?: "") { note -> onChange { it.copy(note = note) } }
    }
}

@Composable
private fun NoteField(saved: String, onSave: (String) -> Unit) {
    var text by remember(saved) { mutableStateOf(saved) }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(500) },
        label = { Text(stringResource(R.string.note)) },
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused && text.trim() != saved) onSave(text.trim()) },
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        singleLine = true,
    )
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
