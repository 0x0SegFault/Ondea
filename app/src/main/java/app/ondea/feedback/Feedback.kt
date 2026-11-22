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
