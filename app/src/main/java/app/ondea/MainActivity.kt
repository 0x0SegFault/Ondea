package app.ondea

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.ondea.ui.AppRoot
import app.ondea.ui.MainViewModel
import app.ondea.ui.UiState
import app.ondea.ui.theme.OndeaTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        unlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED) ?: false
        // Debug builds: `adb shell am start -n <app>/app.ondea.MainActivity --ez demo_data true` shows sample cycles.
        if (intent.getBooleanExtra("demo_data", false)) vm.showDemoData()

        setContent {
            OndeaTheme {
                AppRoot(
                    vm = vm,
                    unlocked = unlocked,
                    onUnlock = ::authenticate,
                    canUseLock = ::deviceCanAuthenticate,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    val settings = (s as? UiState.Ready)?.data?.settings ?: return@collect
                    applySecureScreen(settings.secureScreen)
                    if (settings.appLock && !unlocked) authenticate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDate()
        val settings = (vm.state.value as? UiState.Ready)?.data?.settings
        if (settings?.appLock == true && !unlocked) authenticate()
    }

    override fun onStop() {
        super.onStop()
        // Re-lock whenever the app leaves the screen, except for our own file pickers.
        if (!isChangingConfigurations && !LockGate.consumeSkip()) unlocked = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCKED, unlocked)
    }

    private fun applySecureScreen(secure: Boolean) {
        if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!secure)
    }

    private fun deviceCanAuthenticate(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    private var prompting = false

    private fun authenticate() {
        // A prompt requested while the activity is stopped would be silently dropped; onResume retries.
        if (unlocked || prompting || supportFragmentManager.isStateSaved) return
        if (!deviceCanAuthenticate()) {
            // The phone itself has no screen lock any more, so an app lock would be meaningless.
            unlocked = true
            return
        }
        prompting = true
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    prompting = false
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    prompting = false
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_title))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build(),
        )
    }

    private companion object {
        const val KEY_UNLOCKED = "unlocked"
    }
}

/** Lets the UI open the system file picker without the app re-locking when it comes back. */
object LockGate {
    @Volatile private var skip = false
    fun skipNextLock() {
        skip = true
    }
    fun consumeSkip(): Boolean = skip.also { skip = false }
}
