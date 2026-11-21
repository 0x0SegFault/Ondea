package app.ondea.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.ondea.LockGate
import app.ondea.R

/** A study or method Ondea relies on. [doi] is null for sources without a public link. */
private class Source(val citation: String, @StringRes val usedFor: Int, val doi: String?)

private val SOURCES = listOf(
    Source("Wilcox AJ, Weinberg CR, Baird DD. Timing of sexual intercourse in relation to ovulation. N Engl J Med 1995;333:1517–21.",
        R.string.src_wilcox1995, "10.1056/NEJM199512073332301"),
    Source("Wilcox AJ, Dunson D, Baird DD. The timing of the \"fertile window\" in the menstrual cycle. BMJ 2000;321:1259–62.",
        R.string.src_wilcox2000, "10.1136/bmj.321.7271.1259"),
    Source("Ecochard R et al. Chronological aspects of ultrasonic, hormonal, and other indirect indices of ovulation. BJOG 2001;108:822–29.",
        R.string.src_ecochard2001, "10.1111/j.1471-0528.2001.00194.x"),
    Source("Direito A et al. Relationships between the luteinizing hormone surge and other characteristics of the menstrual cycle. Fertil Steril 2013;99:279–85.",
        R.string.src_direito2013, "10.1016/j.fertnstert.2012.08.047"),
    Source("Erden M et al. The LH surge and ovulation re-visited: a systematic review and meta-analysis. Hum Reprod Update 2022;28:717–32.",
        R.string.src_erden2022, "10.1093/humupd/dmac012"),
    Source("McGovern PG et al. Absence of secretory endometrium after false-positive home urine luteinizing hormone testing. Fertil Steril 2004;82:1273–77.",
        R.string.src_mcgovern2004, "10.1016/j.fertnstert.2004.03.070"),
    Source("Frank-Herrmann P et al. The effectiveness of a fertility awareness based method to avoid pregnancy… Hum Reprod 2007;22:1310–19.",
        R.string.src_frank2007, "10.1093/humrep/dem003"),
    Source("Fehring RJ, Schneider M, Raviele K. Variability in the phases of the menstrual cycle. J Obstet Gynecol Neonatal Nurs 2006;35:376–84.",
        R.string.src_fehring2006, "10.1111/j.1552-6909.2006.00051.x"),
    Source("Bull JR et al. Real-world menstrual cycle characteristics of more than 600,000 menstrual cycles. NPJ Digit Med 2019;2:83.",
        R.string.src_bull2019, "10.1038/s41746-019-0152-7"),
    Source("Arbeitsgruppe NFP. Natürlich und sicher (Sensiplan handbook). TRIAS.",
        R.string.src_sensiplan, null),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.sources_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.sources_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SOURCES.forEach { src ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val open = src.doi?.let { doi ->
                    Modifier.clickable(role = Role.Button) {
                        LockGate.skipNextLock()
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://doi.org/$doi".toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // No browser installed: the citation stays readable on screen.
                        }
                    }
                } ?: Modifier
                Column(Modifier.fillMaxWidth().then(open).padding(vertical = 4.dp)) {
                    Text(stringResource(src.usedFor), style = MaterialTheme.typography.bodyLarge)
                    Text(src.citation, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (src.doi != null) {
                        Text("doi.org/${src.doi}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
