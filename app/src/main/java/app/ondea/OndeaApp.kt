package app.ondea

import android.app.Application
import app.ondea.data.EncryptedStore
import app.ondea.feedback.CrashLog
import app.ondea.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OndeaApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        repository = Repository(EncryptedStore(this), appScope)
        repository.load()
    }
}
