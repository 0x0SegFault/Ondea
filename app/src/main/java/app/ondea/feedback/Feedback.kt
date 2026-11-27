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

    /** Anonymous form (reports/ folder, Scaleway function in Paris). Null: the app offers only GitHub and "share as text". */
    val ANONYMOUS_FORM_URL: String? = "https://nshopefulperlmanczocka0s-fct-interesting-knuth.functions.fnc.fr-par.scw.cloud"

    enum class Kind(val formValue: String) { BUG("bug"), FEATURE("feature") }

    /** Last line of every anonymous issue. Same text as reports/report.js (a test checks it). */
    const val FOOTER = "_Sent anonymously. The sender sees only the status (received, published with its public link, closed), not comments here._"

    /** A random follow-up code (32 hex characters). Only its hash goes in the issue. */
    fun newFollowUpCode(): String {
        val bytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Status page of one anonymous report, opened in the browser (Ondea itself has no internet). */
    fun followUri(code: String): Uri? =
        ANONYMOUS_FORM_URL?.trimEnd('/')?.plus("/follow")?.toUri()?.buildUpon()?.appendQueryParameter("code", code)?.build()

    /** What the issue will contain, as shown in the app before the browser opens. The summary is written in the form. */
    fun preview(context: Context, kind: Kind, what: String?): String = buildString {
        append(if (kind == Kind.BUG) "Bug: …" else "Idea: …").append("\n\n")
        append(if (what.isNullOrBlank()) "…" else what.take(MAX_REPORT)).append("\n\n---\n")
        append("App version: ").append(version(context)).append('\n')
        append("Device: ").append(device()).append("\n\n")
        append(FOOTER)
    }

    val anonymousFormAvailable: Boolean get() = ANONYMOUS_FORM_URL != null

    fun anonymousFormUri(context: Context, kind: Kind, what: String? = null, followUp: String? = null, crash: Boolean = false): Uri? =
        ANONYMOUS_FORM_URL?.toUri()?.buildUpon()
            ?.appendQueryParameter("kind", kind.formValue)
            ?.apply { if (followUp != null) appendQueryParameter("followUp", followUp) }
            ?.apply { if (crash) appendQueryParameter("crash", "1") }
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

    /**
     * Crash text without messages: the first line (app version and phone), error types and code lines.
     * Exception messages can contain user text, so they are removed.
     */
    fun anonymize(trace: String): String = trace.lines().mapIndexedNotNull { i, raw ->
        val line = raw.trimEnd()
        when {
            i == 0 && line.startsWith("Ondea") -> line
            line.trimStart().startsWith("at ") -> "    " + line.trimStart()
            line.trimStart().startsWith("...") -> "    " + line.trimStart()
            line.startsWith("Caused by:") -> "Caused by: " + line.removePrefix("Caused by:").trim().substringBefore(':').trim()
            Regex("^[a-zA-Z_][\\w.$]*(Exception|Error|Throwable)\\b.*").matches(line) -> line.substringBefore(':').trim()
            else -> null
        }
    }.take(MAX_LINES).joinToString("\n")

    private fun report(context: Context, error: Throwable): String {
        val stack = error.stackTraceToString().lineSequence().take(MAX_LINES).joinToString("\n")
        return "Ondea ${Feedback.version(context)} · ${Feedback.device()}\n\n$stack"
    }
}
