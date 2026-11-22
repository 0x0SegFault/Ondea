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

/** How to send a report: anonymous form (when deployed), GitHub, or plain text through any app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(kind: Feedback.Kind, what: String?, onSent: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.report_share_title)
    fun done() { onSent(); onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.report_how_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.feedback_no_health_data), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Feedback.anonymousFormUri(context, kind, what)?.let { uri ->
                Option(Icons.Outlined.VisibilityOff, stringResource(R.string.report_anonymous),
                    stringResource(R.string.report_anonymous_hint)) { Feedback.open(context, uri); done() }
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
