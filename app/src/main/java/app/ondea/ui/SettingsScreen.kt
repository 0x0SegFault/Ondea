package app.ondea.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.ondea.LockGate
import app.ondea.R
import app.ondea.reminder.Reminder
import app.ondea.ui.theme.CycleColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(vm: MainViewModel, s: UiState.Ready, canUseLock: () -> Boolean) {
    val settings = s.data.settings
    val context = LocalContext.current
    val busy by vm.busy.collectAsState()
    val pending by vm.pendingImport.collectAsState()
    val palette = CycleColors.current

    var reminderOn by remember { mutableStateOf(Reminder.isEnabled(context)) }
    var reminderMinute by remember { mutableIntStateOf(Reminder.minuteOfDay(context)) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var askExportPassword by rememberSaveable { mutableStateOf(false) }
    var chooseBackup by rememberSaveable { mutableStateOf(false) }
    var shareBackup by rememberSaveable { mutableStateOf(false) }
    val shareUri by vm.shareBackupUri.collectAsState()
    val shareTitle = stringResource(R.string.backup_share_title)
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var confirmWipe by rememberSaveable { mutableStateOf(false) }
    var lockUnavailable by rememberSaveable { mutableStateOf(false) }
    var showSources by rememberSaveable { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminderOn = granted
        Reminder.configure(context, granted, reminderMinute)
    }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pw = exportPassword
        exportPassword = null
        if (uri != null && pw != null) vm.exportBackup(uri, pw) else pw?.fill('\u0000')
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(vm::exportCsv)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        SectionCard(title = stringResource(R.string.section_cycle)) {
            SettingBlock(Icons.Outlined.Loop, palette.period, stringResource(R.string.cycle_length),
                stringResource(R.string.cycle_length_hint, s.engine.avgCycleLength)) {
                NumberStepper(settings.cycleLength, 20..45, stringResource(R.string.n_days, settings.cycleLength)) { v ->
                    vm.updateSettings { it.copy(cycleLength = v) }
                }
            }
            Divider()
            SettingBlock(Icons.Outlined.WaterDrop, palette.period, stringResource(R.string.period_length), null) {
                NumberStepper(settings.periodLength, 2..10, stringResource(R.string.n_days, settings.periodLength)) { v ->
                    vm.updateSettings { it.copy(periodLength = v) }
                }
            }
            Divider()
            OvulationDayControls(s.engine, vm::updateSettings)
            Divider()
            SettingBlock(Icons.Outlined.Thermostat, MaterialTheme.colorScheme.primary, stringResource(R.string.unit), null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false, true).forEachIndexed { i, f ->
                        SegmentedButton(
                            selected = settings.fahrenheit == f,
                            onClick = { vm.updateSettings { it.copy(fahrenheit = f) } },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(if (f) "°F" else "°C") }
                    }
                }
            }
        }

        SectionCard(title = stringResource(R.string.section_reminder)) {
            SwitchRow(Icons.Outlined.Notifications, MaterialTheme.colorScheme.primary,
                stringResource(R.string.reminder_daily), stringResource(R.string.reminder_daily_hint), reminderOn) { on ->
                if (on && Build.VERSION.SDK_INT >= 33) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    reminderOn = on
                    Reminder.configure(context, on, reminderMinute)
                }
            }
            if (reminderOn) {
                val time = LocalTime.of(reminderMinute / 60, reminderMinute % 60)
                ClickRow(null, Color.Unspecified, stringResource(R.string.reminder_time),
                    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))) {
                    TimePickerDialog(context, { _, h, m ->
                        reminderMinute = h * 60 + m
                        Reminder.configure(context, true, reminderMinute)
                    }, time.hour, time.minute, DateFormat.is24HourFormat(context)).show()
                }
            }
        }

        SectionCard(title = stringResource(R.string.section_privacy)) {
            SwitchRow(Icons.Outlined.Lock, MaterialTheme.colorScheme.primary, stringResource(R.string.app_lock),
                stringResource(R.string.app_lock_hint), settings.appLock) { on ->
                if (on && !canUseLock()) lockUnavailable = true
                else vm.updateSettings { it.copy(appLock = on) }
            }
            Divider()
            SwitchRow(Icons.Outlined.VisibilityOff, MaterialTheme.colorScheme.primary, stringResource(R.string.secure_screen),
                stringResource(R.string.secure_screen_hint), settings.secureScreen) { on ->
                vm.updateSettings { it.copy(secureScreen = on) }
            }
            Text(stringResource(R.string.privacy_summary), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard(title = stringResource(R.string.section_backup)) {
            Text(stringResource(R.string.backup_explain), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ClickRow(Icons.Outlined.Backup, palette.fertile, stringResource(R.string.backup_create),
                stringResource(R.string.backup_create_hint)) { chooseBackup = true }
            Divider()
            ClickRow(Icons.Outlined.Restore, palette.fertile, stringResource(R.string.restore_backup),
                stringResource(R.string.restore_backup_hint)) {
                LockGate.skipNextLock()
                openBackup.launch(arrayOf("*/*"))
            }
            Divider()
            ClickRow(Icons.Outlined.TableChart, palette.fertile, stringResource(R.string.export_csv),
                stringResource(R.string.export_csv_hint)) {
                LockGate.skipNextLock()
                createCsv.launch("ondea-${LocalDate.now()}.csv")
            }
        }

        SectionCard(title = stringResource(R.string.section_general)) {
            if (Build.VERSION.SDK_INT >= 33) {
                ClickRow(Icons.Outlined.Language, MaterialTheme.colorScheme.primary, stringResource(R.string.language),
                    stringResource(R.string.language_hint)) {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                }
                Divider()
            }
            ClickRow(Icons.Outlined.DeleteForever, MaterialTheme.colorScheme.error, stringResource(R.string.erase_all),
                stringResource(R.string.erase_all_hint), danger = true) { confirmWipe = true }
        }

        SectionCard(title = stringResource(R.string.section_about), icon = Icons.Outlined.Info) {
            val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            Text("${stringResource(R.string.app_name)} $version", style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium)
            ClickRow(Icons.Outlined.MenuBook, palette.ovulation, stringResource(R.string.sources_title),
                stringResource(R.string.sources_hint)) { showSources = true }
            Text(stringResource(R.string.about_science), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.disclaimer_full), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (askExportPassword) {
        PasswordDialog(stringResource(R.string.backup_create), confirmTwice = true, onDone = { pw ->
            if (shareBackup) {
                vm.shareBackup(pw)
            } else {
                exportPassword = pw
                LockGate.skipNextLock()
                createBackup.launch("ondea-backup-${LocalDate.now()}.ondea")
            }
        }, onDismiss = { askExportPassword = false })
    }
    restoreUri?.let { uri ->
        PasswordDialog(stringResource(R.string.restore_backup), confirmTwice = false,
            onDone = { vm.readBackup(uri, it) }, onDismiss = { restoreUri = null })
    }
    pending?.let {
        ConfirmDialog(stringResource(R.string.restore_backup), stringResource(R.string.restore_confirm),
            stringResource(R.string.restore), onConfirm = vm::confirmImport, onDismiss = { vm.pendingImport.value = null })
    }
    if (confirmWipe) {
        ConfirmDialog(stringResource(R.string.erase_all), stringResource(R.string.erase_confirm),
            stringResource(R.string.erase), onConfirm = vm::wipe, onDismiss = { confirmWipe = false })
    }
    if (showSources) SourcesSheet(onDismiss = { showSources = false })
    if (chooseBackup) {
        BackupChoiceSheet(
            onFile = { shareBackup = false; chooseBackup = false; askExportPassword = true },
            onApp = { shareBackup = true; chooseBackup = false; askExportPassword = true },
            onDismiss = { chooseBackup = false },
        )
    }
    // Hand the encrypted file to the app the user picks (Proton Drive, Nextcloud, email…).
    LaunchedEffect(shareUri) {
        val uri = shareUri ?: return@LaunchedEffect
        vm.shareBackupUri.value = null
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/octet-stream")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        LockGate.skipNextLock()
        context.startActivity(Intent.createChooser(send, shareTitle))
    }
    if (lockUnavailable) {
        ConfirmDialog(stringResource(R.string.app_lock), stringResource(R.string.app_lock_unavailable),
            stringResource(R.string.ok), onConfirm = {}, onDismiss = { lockUnavailable = false })
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

@Composable
private fun RowStart(icon: ImageVector?, tint: Color, title: String, subtitle: String?, modifier: Modifier, danger: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingBlock(icon: ImageVector, tint: Color, title: String, subtitle: String?, control: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RowStart(icon, tint, title, subtitle, Modifier)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { control() }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, tint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowStart(icon, tint, title, subtitle, Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ClickRow(icon: ImageVector?, tint: Color, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    RowStart(icon, tint, title, subtitle, Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick), danger)
}
