package app.ondea.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.data.Settings
import java.time.LocalDate

@Composable
fun OnboardingScreen(onDone: (Settings, LocalDate) -> Unit) {
    val today = LocalDate.now()
    var lastPeriod by rememberSaveable { mutableStateOf(today) }
    var cycleLength by rememberSaveable { mutableIntStateOf(28) }
    var periodLength by rememberSaveable { mutableIntStateOf(5) }
    var fahrenheit by rememberSaveable { mutableStateOf(false) }
    var knowsOvulation by rememberSaveable { mutableStateOf(false) }
    var ovulationDay by rememberSaveable { mutableIntStateOf(14) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Thermostat, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.welcome_text), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Step(1, stringResource(R.string.last_period_start)) {
            OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(lastPeriod.long())
            }
        }
        Step(2, stringResource(R.string.cycle_length), stringResource(R.string.cycle_length_onboarding_hint)) {
            NumberStepper(cycleLength, 20..45, stringResource(R.string.n_days, cycleLength)) { cycleLength = it }
        }
        Step(3, stringResource(R.string.period_length)) {
            NumberStepper(periodLength, 2..10, stringResource(R.string.n_days, periodLength)) { periodLength = it }
        }
        Step(4, stringResource(R.string.ovulation_day_onboarding), stringResource(R.string.ovulation_day_onboarding_hint)) {
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .toggleable(value = knowsOvulation, role = Role.Switch, onValueChange = {
                        knowsOvulation = it
                        if (it) ovulationDay = (cycleLength - 14).coerceIn(8, 35)
                    })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.know_ovulation_day), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = knowsOvulation, onCheckedChange = null)
            }
            if (knowsOvulation) {
                NumberStepper(ovulationDay, 8..35, stringResource(R.string.day_n, ovulationDay)) { ovulationDay = it }
            }
        }
        Step(5, stringResource(R.string.unit)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = fahrenheit == f,
                        onClick = { fahrenheit = f },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(if (f) "°F" else "°C") }
                }
            }
        }

        SectionCard(title = stringResource(R.string.how_to_measure_title)) {
            Text(stringResource(R.string.how_to_measure), style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .toggleable(value = accepted, role = Role.Checkbox, onValueChange = { accepted = it })
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = accepted, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp))
            Text(stringResource(R.string.disclaimer_accept), style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            enabled = accepted,
            onClick = {
                onDone(
                    Settings(
                        cycleLength = cycleLength,
                        periodLength = periodLength,
                        ovulationDay = if (knowsOvulation) ovulationDay else null,
                        fahrenheit = fahrenheit,
                    ),
                    lastPeriod,
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.start), style = MaterialTheme.typography.titleMedium) }
    }

    if (pickDate) {
        PastDatePickerDialog(lastPeriod, today, onPicked = { lastPeriod = it }, onDismiss = { pickDate = false })
    }
}

@Composable
private fun Step(number: Int, title: String, hint: String? = null, content: @Composable () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (hint != null) {
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
