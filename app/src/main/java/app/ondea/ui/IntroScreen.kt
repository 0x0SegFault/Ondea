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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.ondea.R
import app.ondea.ui.theme.CycleColors

private const val PAGES = 6

/**
 * A short tour before the setup: what Ondea is, what to record, the temperature chart, how Ondea helps.
 * [replay]: opened again from Settings; the last button closes instead of starting the setup.
 */
@Composable
fun IntroScreen(onDone: () -> Unit, replay: Boolean = false) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val palette = CycleColors.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(stringResource(if (replay) R.string.intro_close else R.string.intro_skip)) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (page) {
                0 -> {
                    Badge(Icons.Outlined.Thermostat, MaterialTheme.colorScheme.primary)
                    Title(R.string.intro_what_title)
                    Body(R.string.intro_what_text)
                }
                1 -> {
                    Badge(Icons.Outlined.Lock, MaterialTheme.colorScheme.primary)
                    Title(R.string.intro_safe_title)
                    Body(R.string.intro_safe_text)
                    Item(Icons.Outlined.WifiOff, palette.fertile, R.string.intro_safe_data)
                    Item(Icons.Outlined.Fingerprint, palette.ovulation, R.string.intro_safe_lock)
                    Item(Icons.Outlined.VisibilityOff, palette.mucus, R.string.intro_safe_hide)
                    Item(Icons.Outlined.Backup, palette.period, R.string.intro_safe_backup)
                    Item(Icons.Outlined.TableChart, palette.fertile, R.string.intro_safe_export)
                    Text(stringResource(R.string.intro_safe_where), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                2 -> {
                    Badge(Icons.Outlined.EditNote, MaterialTheme.colorScheme.primary)
                    Title(R.string.intro_record_title)
                    Item(Icons.Outlined.Thermostat, palette.ovulation, R.string.intro_record_temperature)
                    Item(Icons.Outlined.Opacity, palette.period, R.string.intro_record_period)
                    Item(Icons.Outlined.WaterDrop, palette.mucus, R.string.intro_record_mucus)
                    Item(Icons.Outlined.EditNote, palette.fertile, R.string.intro_record_other)
                }
                3 -> {
                    Badge(Icons.Outlined.Thermostat, palette.ovulation)
                    Title(R.string.intro_measure_title)
                    Body(R.string.intro_measure_text)
                    Item(Icons.Outlined.Straighten, palette.ovulation, R.string.intro_rule_thermometer)
                    Item(Icons.Outlined.Schedule, palette.fertile, R.string.intro_rule_time)
                    Item(Icons.Outlined.Bedtime, palette.mucus, R.string.intro_rule_sleep)
                    Item(Icons.Outlined.WarningAmber, palette.period, R.string.intro_rule_disturbed)
                }
                4 -> {
                    Title(R.string.intro_chart_title)
                    TypicalCurve(showLevels = true)
                    Body(R.string.intro_chart_text)
                    PhaseTexts()
                }
                else -> {
                    Badge(Icons.Outlined.AutoGraph, palette.ovulation)
                    Title(R.string.intro_help_title)
                    Body(R.string.intro_help_text)
                    Text(stringResource(R.string.intro_help_warning), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(PAGES) { i ->
                    Box(Modifier.size(8.dp).clip(CircleShape).background(
                        if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
            if (page > 0) TextButton(onClick = { page-- }) { Text(stringResource(R.string.previous)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (page < PAGES - 1) page++ else onDone() }) {
                Text(stringResource(when {
                    page < PAGES - 1 -> R.string.intro_next
                    replay -> R.string.intro_close
                    else -> R.string.intro_start
                }))
            }
        }
    }
}

/** The tour as a full-screen dialog, opened from Settings. */
@Composable
fun IntroDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { IntroScreen(onDone = onDismiss, replay = true) }
    }
}

@Composable
private fun Badge(icon: ImageVector, tint: Color) {
    Box(Modifier.size(88.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(46.dp), tint = tint)
    }
}

@Composable
private fun Title(id: Int) = Text(stringResource(id), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)

@Composable
private fun Body(id: Int) = Text(stringResource(id), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Item(icon: ImageVector, tint: Color, id: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(id), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(2.dp))
}
