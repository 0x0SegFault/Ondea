package app.ondea.feedback

import android.content.Context
import androidx.core.content.edit

/**
 * Follow-up codes of the anonymous reports the user opened, kept on this phone only.
 * Holds no report text: only the code, the kind and the date.
 */
class ReportStore(context: Context) {
    data class Sent(val code: String, val kind: Feedback.Kind, val millis: Long)

    private val prefs = context.getSharedPreferences("reports", Context.MODE_PRIVATE)

    fun all(): List<Sent> = prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 3) return@mapNotNull null
        val kind = Feedback.Kind.entries.firstOrNull { it.formValue == parts[1] } ?: return@mapNotNull null
        Sent(parts[0], kind, parts[2].toLongOrNull() ?: return@mapNotNull null)
    }.sortedByDescending { it.millis }

    fun add(code: String, kind: Feedback.Kind, millis: Long = System.currentTimeMillis()) =
        prefs.edit { putStringSet(KEY, all().map(::line).toSet() + line(Sent(code, kind, millis))) }

    fun remove(code: String) = prefs.edit { putStringSet(KEY, all().filter { it.code != code }.map(::line).toSet()) }

    private fun line(s: Sent) = "${s.code}|${s.kind.formValue}|${s.millis}"

    private companion object {
        const val KEY = "sent"
    }
}
