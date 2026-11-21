package app.ondea.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ondea.LockGate
import app.ondea.R

private enum class Tab(val label: Int, val icon: ImageVector) {
    Today(R.string.tab_today, Icons.Outlined.Thermostat),
    Chart(R.string.tab_chart, Icons.AutoMirrored.Outlined.ShowChart),
    Calendar(R.string.tab_calendar, Icons.Outlined.CalendarMonth),
    Settings(R.string.tab_settings, Icons.Outlined.Settings),
}

@Composable
fun AppRoot(
    vm: MainViewModel,
    unlocked: Boolean,
    onUnlock: () -> Unit,
    canUseLock: () -> Boolean,
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            UiState.Loading -> Centered { CircularProgressIndicator() }
            UiState.Failed -> DataErrorScreen(vm, snackbar)
            is UiState.Ready -> when {
                s.data.settings.appLock && !unlocked -> LockScreen(onUnlock)
                !s.data.settings.onboarded -> OnboardingScreen(onDone = vm::completeOnboarding)
                else -> MainScaffold(vm, s, snackbar, canUseLock)
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel, s: UiState.Ready, snackbar: SnackbarHostState, canUseLock: () -> Boolean) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Today -> TodayScreen(vm, s)
                Tab.Chart -> ChartScreen(s)
                Tab.Calendar -> CalendarScreen(vm, s)
                Tab.Settings -> SettingsScreen(vm, s, canUseLock)
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Centered {
        Icon(Icons.Outlined.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.unlock)) }
    }
}

/** Shown when the encrypted data can't be read (e.g. the keystore key was lost). */
@Composable
private fun DataErrorScreen(vm: MainViewModel, snackbar: SnackbarHostState) {
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmWipe by remember { mutableStateOf(false) }
    val pending by vm.pendingImport.collectAsState()
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.error_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.error_text), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { LockGate.skipNextLock(); open.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.restore_backup))
            }
            OutlinedButton(onClick = { confirmWipe = true }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.erase_start_over))
            }
        }
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
        ConfirmDialog(stringResource(R.string.erase_start_over), stringResource(R.string.erase_confirm),
            stringResource(R.string.erase), onConfirm = vm::wipe, onDismiss = { confirmWipe = false })
    }
}

@Composable
fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
