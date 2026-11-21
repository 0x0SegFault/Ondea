package app.ondea.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ondea.R
import app.ondea.OndeaApp
import app.ondea.data.AppData
import app.ondea.data.Cycle
import app.ondea.data.DayEntry
import app.ondea.data.LoadState
import app.ondea.data.NotABackupException
import app.ondea.data.Repository
import app.ondea.data.Settings
import app.ondea.data.WrongPasswordException
import app.ondea.data.withCycleStart
import app.ondea.data.withEntry
import app.ondea.data.withTrackingStopped
import app.ondea.data.withoutCycleStart
import app.ondea.domain.CsvExport
import app.ondea.domain.CycleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface UiState {
    data object Loading : UiState
    data object Failed : UiState
    data class Ready(val data: AppData, val engine: CycleEngine) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as OndeaApp).repository
    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<UiState> = combine(repo.state, today) { s, t ->
        when (s) {
            LoadState.Loading -> UiState.Loading
            is LoadState.Failed -> UiState.Failed
            is LoadState.Ready -> UiState.Ready(s.data, CycleEngine(s.data, t))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages

    /** A decrypted backup waiting for the user to confirm it should replace current data. */
    val pendingImport = MutableStateFlow<AppData?>(null)
    val busy = MutableStateFlow(false)

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    private fun say(@StringRes msg: Int) {
        _messages.tryEmit(msg)
    }

    private fun edit(@StringRes done: Int? = null, transform: (AppData) -> AppData) {
        viewModelScope.launch {
            try {
                repo.update(transform)
                done?.let(::say)
            } catch (_: Exception) {
                say(R.string.msg_save_failed)
            }
        }
    }

    /** Applies [change] to the latest stored entry of [date] (or an empty one). */
    fun editEntry(date: LocalDate, @StringRes done: Int? = null, change: (DayEntry) -> DayEntry) = edit(done) { d ->
        val day = date.toEpochDay()
        d.withEntry(change(d.entries.firstOrNull { it.date == day } ?: DayEntry(day)))
    }

    fun startCycle(date: LocalDate) = edit(R.string.msg_cycle_started) { it.withCycleStart(date.toEpochDay()) }

    fun removeCycleStart(date: LocalDate) = edit { it.withoutCycleStart(date.toEpochDay()) }

    fun stopTracking() = edit(R.string.msg_tracking_paused) { it.withTrackingStopped(LocalDate.now().toEpochDay()) }

    fun updateSettings(transform: (Settings) -> Settings) = edit { it.copy(settings = transform(it.settings)) }

    fun completeOnboarding(settings: Settings, lastPeriodStart: LocalDate) = edit {
        it.copy(
            settings = settings.copy(onboarded = true),
            cycles = it.cycles.ifEmpty { listOf(Cycle(lastPeriodStart.toEpochDay())) },
        )
    }

    fun exportBackup(uri: Uri, password: CharArray) = withBusy {
        try {
            val blob = repo.exportBackup(password)
            write(uri, blob)
            say(R.string.msg_backup_done)
        } catch (_: Exception) {
            say(R.string.msg_backup_failed)
        } finally {
            password.fill('\u0000')
        }
    }

    /** Encrypted backup file ready to hand to another app (Proton Drive, Nextcloud, email…). */
    val shareBackupUri = MutableStateFlow<Uri?>(null)

    fun shareBackup(password: CharArray) = withBusy {
        try {
            val app = getApplication<Application>()
            val blob = repo.exportBackup(password)
            val file = withContext(Dispatchers.IO) {
                val dir = java.io.File(app.cacheDir, "backups").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                java.io.File(dir, "ondea-backup-${LocalDate.now()}.ondea").apply { writeBytes(blob) }
            }
            shareBackupUri.value = androidx.core.content.FileProvider.getUriForFile(app, "${app.packageName}.backups", file)
        } catch (_: Exception) {
            say(R.string.msg_backup_failed)
        } finally {
            password.fill('\u0000')
        }
    }

    fun readBackup(uri: Uri, password: CharArray) = withBusy {
        try {
            val blob = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            }
            pendingImport.value = withContext(Dispatchers.Default) { Repository.decodeBackup(blob, password) }
        } catch (_: WrongPasswordException) {
            say(R.string.msg_wrong_password)
        } catch (_: NotABackupException) {
            say(R.string.msg_not_a_backup)
        } catch (_: Exception) {
            say(R.string.msg_restore_failed)
        } finally {
            password.fill('\u0000')
        }
    }

    fun confirmImport() {
        val data = pendingImport.value ?: return
        pendingImport.value = null
        withBusy {
            try {
                repo.replaceAll(data)
                say(R.string.msg_restore_done)
            } catch (_: Exception) {
                say(R.string.msg_restore_failed)
            }
        }
    }

    fun exportCsv(uri: Uri) = withBusy {
        val data = (state.value as? UiState.Ready)?.data ?: return@withBusy
        try {
            write(uri, CsvExport.build(data).toByteArray(Charsets.UTF_8))
            say(R.string.msg_csv_done)
        } catch (_: Exception) {
            say(R.string.msg_backup_failed)
        }
    }

    fun wipe() = withBusy {
        repo.wipe()
        say(R.string.msg_wiped)
    }

    private suspend fun write(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw java.io.IOException("Cannot open $uri")
    }

    private fun withBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try {
                block()
            } finally {
                busy.value = false
            }
        }
    }
}
