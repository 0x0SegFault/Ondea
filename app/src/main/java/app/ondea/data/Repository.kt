package app.ondea.data

import android.content.Context
import androidx.core.util.AtomicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val data: AppData) : LoadState
    data class Failed(val error: Throwable) : LoadState
}

/** The single encrypted data file in app-private storage, written atomically. */
class EncryptedStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "ondea.dat"))
    private val cipher = KeystoreCipher()

    fun read(): ByteArray? {
        if (!file.baseFile.exists()) return null
        return cipher.decrypt(file.readFully())
    }

    fun write(plain: ByteArray) {
        val sealed = cipher.encrypt(plain)
        val out = file.startWrite()
        try {
            out.write(sealed)
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    fun wipe() {
        file.delete()
        cipher.deleteKey()
    }
}

class Repository(private val store: EncryptedStore, private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<LoadState>(LoadState.Loading)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    fun load() {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                _state.value = try {
                    LoadState.Ready(store.read()?.let(::decode) ?: AppData())
                } catch (e: Exception) {
                    LoadState.Failed(e)
                }
            }
        }
    }

    suspend fun update(transform: (AppData) -> AppData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = (_state.value as? LoadState.Ready)?.data ?: return@withLock
            val next = transform(current)
            if (next != current) {
                store.write(encode(next))
                _state.value = LoadState.Ready(next)
            }
        }
    }

    suspend fun replaceAll(data: AppData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.write(encode(data))
            _state.value = LoadState.Ready(data)
        }
    }

    suspend fun wipe() = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.wipe()
            _state.value = LoadState.Ready(AppData())
        }
    }

    suspend fun exportBackup(password: CharArray): ByteArray = withContext(Dispatchers.Default) {
        val data = (_state.value as? LoadState.Ready)?.data ?: AppData()
        BackupCrypto.encrypt(encode(data), password)
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(data: AppData): ByteArray =
            json.encodeToString(AppData.serializer(), data).toByteArray(Charsets.UTF_8)

        fun decode(bytes: ByteArray): AppData =
            json.decodeFromString(AppData.serializer(), bytes.toString(Charsets.UTF_8))

        /** @throws WrongPasswordException, NotABackupException */
        fun decodeBackup(blob: ByteArray, password: CharArray): AppData =
            decode(BackupCrypto.decrypt(blob, password))
    }
}
