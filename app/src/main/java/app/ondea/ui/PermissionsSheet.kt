package app.ondea.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ondea.R

/** One Android permission in the release APK, with why Ondea needs it. Keep in sync with PRIVACY.md. */
private class Permission(val icon: ImageVector, @StringRes val name: Int, @StringRes val why: Int, val id: String)

private val PERMISSIONS = listOf(
    Permission(Icons.Outlined.Notifications, R.string.perm_notifications, R.string.perm_notifications_why, "POST_NOTIFICATIONS"),
    Permission(Icons.Outlined.RestartAlt, R.string.perm_boot, R.string.perm_boot_why, "RECEIVE_BOOT_COMPLETED"),
    Permission(Icons.Outlined.Fingerprint, R.string.perm_biometric, R.string.perm_biometric_why, "USE_BIOMETRIC, USE_FINGERPRINT"),
    Permission(Icons.Outlined.PhonelinkLock, R.string.perm_internal, R.string.perm_internal_why, "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.perms_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.perms_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            PERMISSIONS.forEach { p ->
                Item(p.icon, stringResource(p.name), stringResource(p.why), p.id)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Item(Icons.Outlined.Block, stringResource(R.string.perms_not_title), stringResource(R.string.perms_not), null)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(Icons.Outlined.Shield, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.data_title), style = MaterialTheme.typography.bodyLarge)
                    listOf(R.string.data_collected, R.string.data_shared, R.string.data_tracking, R.string.data_stored).forEach {
                        Text("• " + stringResource(it), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Item(icon: ImageVector, title: String, text: String, technical: String?) {
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(icon, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (technical != null) {
                Text(technical, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
