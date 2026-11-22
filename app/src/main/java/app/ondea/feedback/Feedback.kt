package app.ondea.feedback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import app.ondea.LockGate
import java.io.File

/**
 * Bug reports and feature ideas. Ondea has no internet permission: it opens a GitHub form in the
 * user's browser, pre-filled with the app version and phone model. The user sees everything
 * before sending, and nothing is sent without their action.
 */
object Feedback {
    private const val REPO = "https://github.com/0x0SegFault/ondea"
    private const val MAX_REPORT = 3000

    /**
     * Anonymous form (reports/ folder, Scaleway function in Paris). Null until it is deployed:
     * then the app offers only GitHub and "share as text".
     */
    val ANONYMOUS_FORM_URL: String? = null

    enum class Kind(val formValue: String) { BUG("bug"), FEATURE("feature") }

    val anonymousFormAvailable: Boolean get() = ANONYMOUS_FORM_URL != null

    fun anonymousFormUri(context: Context, kind: Kind, what: String? = null): Uri? =
        ANONYMOUS_FORM_URL?.toUri()?.buildUpon()
            ?.appendQueryParameter("kind", kind.formValue)
            ?.appendQueryParameter("version", version(context))
            ?.appendQueryParameter("device", device())
            ?.apply { if (what != null) appendQueryParameter("what", what.take(MAX_REPORT)) }
            ?.build()

    fun githubUri(context: Context, kind: Kind, what: String? = null): Uri =
        if (kind == Kind.BUG) bugReportUri(context, what) else featureRequestUri()

    /** Plain text for "share as text": the user picks the app and sees everything. */
    fun shareText(context: Context, kind: Kind, what: String?): String = buildString {
        append(if (kind == Kind.BUG) "Ondea problem report" else "Ondea feature idea").append("\n\n")
        if (!what.isNullOrBlank()) append(what.take(MAX_REPORT)).append("\n\n")
        append("App version: ").append(version(context)).append('\n')
        append("Device: ").append(device())
    }

    fun share(context: Context, kind: Kind, what: String?, title: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(context, kind, what))
        LockGate.skipNextLock()
        context.startActivity(Intent.createChooser(send, title))
    }

    fun version(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    fun device(): String = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun bugReportUri(context: Context, what: String? = null): Uri =
        "$REPO/issues/new".toUri().buildUpon()
            .appendQueryParameter("template", "bug_report.yml")
            .appendQueryParameter("version", version(context))
            .appendQueryParameter("device", device())
            .apply { if (what != null) appendQueryParameter("what", what.take(MAX_REPORT)) }
            .build()

    fun featureRequestUri(): Uri =
        "$REPO/issues/new".toUri().buildUpon().appendQueryParameter("template", "feature_request.yml").build()

    /** @return false when no browser is installed. */
    fun open(context: Context, uri: Uri): Boolean {
        LockGate.skipNextLock()
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

/**
 * Private crash capture, instead of an online crash reporter. A crash writes the technical error
 * (no health data) to a local file. On the next launch the app offers to report it; the user
 * reads the text in the browser form before sending.
 */
object CrashLog {
    private const val MAX_LINES = 40

    private fun file(context: Context) = File(context.noBackupFilesDir, "last_crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { file(app).writeText(report(app, error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun pending(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun report(context: Context, error: Throwable): String {
        val stack = error.stackTraceToString().lineSequence().take(MAX_LINES).joinToString("\n")
        return "Ondea ${Feedback.version(context)} · ${Feedback.device()}\n\n$stack"
    }
}
