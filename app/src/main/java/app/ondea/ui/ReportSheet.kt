package app.ondea.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.feedback.Feedback
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import app.ondea.feedback.CrashLog
import app.ondea.feedback.ReportStore
import java.text.DateFormat
import java.util.Date

/**
 * How to send a report: anonymous form (when deployed), GitHub, or plain text through any app.
 * A preview shows what the issue will contain. After a crash, [crash] holds the crash text: the user
 * chooses to include it, anonymised (error types and code lines only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(kind: Feedback.Kind, crash: String? = null, onSent: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.report_share_title)
    var includeCrash by rememberSaveable { mutableStateOf(true) }
    val what = crash?.takeIf { includeCrash }?.let { "Crash report:\n```\n${CrashLog.anonymize(it)}\n```" }
    fun done() { onSent(); onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.report_how_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.feedback_no_health_data), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (crash != null) {
                Row(
                    Modifier.fillMaxWidth().toggleable(value = includeCrash, role = Role.Switch, onValueChange = { includeCrash = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.report_include_crash), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.report_include_crash_hint), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = includeCrash, onCheckedChange = null)
                }
            }
            Text(stringResource(R.string.report_preview_title), style = MaterialTheme.typography.titleSmall)
            Text(
                Feedback.preview(context, kind, what),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
            )
            Text(stringResource(R.string.report_preview_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (Feedback.anonymousFormAvailable) {
                Option(Icons.Outlined.VisibilityOff, stringResource(R.string.report_anonymous),
                    stringResource(R.string.report_anonymous_hint)) {
                    val code = Feedback.newFollowUpCode()
                    Feedback.anonymousFormUri(context, kind, what, code, crash = what != null)?.let { uri ->
                        ReportStore(context).add(code, kind)
                        Feedback.open(context, uri)
                    }
                    done()
                }
            }
            Option(Icons.Outlined.Code, stringResource(R.string.report_github), stringResource(R.string.report_github_hint)) {
                Feedback.open(context, Feedback.githubUri(context, kind, what)); done()
            }
            Option(Icons.Outlined.Share, stringResource(R.string.report_share), stringResource(R.string.report_share_hint)) {
                Feedback.share(context, kind, what, shareTitle); done()
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** The anonymous reports opened from this phone. Tap one to see its status in the browser. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ReportStore(context) }
    var sent by remember { mutableStateOf(store.all()) }
    val format = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.my_reports), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (sent.isEmpty()) R.string.my_reports_empty else R.string.my_reports_note),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            sent.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedCard(onClick = { Feedback.followUri(s.code)?.let { Feedback.open(context, it) } }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(if (s.kind == Feedback.Kind.BUG) R.string.feedback_bug else R.string.feedback_feature),
                                style = MaterialTheme.typography.bodyLarge)
                            Text(format.format(Date(s.millis)) + " · " + s.code.take(8), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { store.remove(s.code); sent = store.all() }) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.my_reports_remove))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Option(icon: ImageVector, title: String, hint: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
