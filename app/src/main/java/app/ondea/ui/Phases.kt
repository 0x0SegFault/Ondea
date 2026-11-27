package app.ondea.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ondea.R
import app.ondea.ui.theme.CycleColors
import app.ondea.ui.theme.Nunito

/** One phase of a cycle: days [from]..[to], 0-based and inclusive. */
data class Phase(val label: String, val color: Color, val from: Int, val to: Int)

/**
 * Phases of one cycle from its period days and ovulation day. Null [ovulation]: not known yet, the
 * follicular phase is drawn up to [follicularUntil] (today) only.
 */
@Composable
fun cyclePhases(periodDays: Int, ovulation: Int?, days: Int, follicularUntil: Int? = null): List<Phase> {
    val palette = CycleColors.current
    val list = mutableListOf<Phase>()
    if (periodDays > 0) list += Phase(stringResource(R.string.phase_period), palette.period, 0, periodDays - 1)
    if (ovulation == null && follicularUntil != null && follicularUntil >= periodDays) {
        list += Phase(stringResource(R.string.phase_follicular), palette.fertile, periodDays, follicularUntil)
    }
    if (ovulation != null && ovulation > periodDays) {
        list += Phase(stringResource(R.string.phase_follicular), palette.fertile, periodDays, ovulation - 1)
        list += Phase(stringResource(R.string.phase_ovulation), palette.ovulation, ovulation, ovulation)
        if (ovulation + 1 < days) list += Phase(stringResource(R.string.phase_luteal), palette.ovulation.copy(alpha = 0.6f), ovulation + 1, days - 1)
    }
    return list
}

/** Coloured bands with labels where they fit, at the top of a chart. [dayWidth] in px. */
fun DrawScope.drawPhases(phases: List<Phase>, dayWidth: Float, top: Float, height: Float, measurer: TextMeasurer, style: TextStyle, alpha: Float = 1f) {
    phases.forEach { p ->
        val x = p.from * dayWidth + 1f
        val w = (p.to - p.from + 1) * dayWidth - 2f
        drawRoundRect(p.color.copy(alpha = 0.22f * alpha), Offset(x, top), Size(w, height), CornerRadius(height / 2))
        val label = measurer.measure(p.label, style.copy(color = p.color.copy(alpha = alpha)))
        if (label.size.width + 6.dp.toPx() <= w) {
            drawText(label, topLeft = Offset(x + (w - label.size.width) / 2f, top + (height - label.size.height) / 2f))
        }
    }
}

/**
 * The textbook basal temperature curve of a 28-day cycle (illustration, not data): low phase, ovulation on
 * day 14, then a higher level until the next period. Used to explain the chart.
 */
@Composable
fun TypicalCurve(modifier: Modifier = Modifier, showLevels: Boolean = false) {
    val palette = CycleColors.current
    val measurer = rememberTextMeasurer()
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val style = TextStyle(fontFamily = Nunito, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val phases = cyclePhases(periodDays = 5, ovulation = 13, days = 28)
    val description = stringResource(R.string.typical_curve_description)
    // Small fixed wiggles so the curve looks like real readings.
    val wiggle = listOf(0, 3, -2, 1, -1, 2, -3, 1, 0, -2, 2, -1, 1, -4, 4, 6, 8, 7, 9, 8, 6, 9, 7, 8, 6, 7, 5, 3)
    val values = (0 until 28).map { d -> (if (d < 14) 36.40 else 36.40 + 0.40 * minOf(1.0, (d - 13) / 2.5)) + wiggle[d] / 100.0 }
    Canvas(modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
        val dw = size.width / 28f
        val top = 26.dp.toPx()
        val bottom = size.height - 6.dp.toPx()
        fun y(v: Double) = bottom - ((v - 36.25) / 0.75).toFloat() * (bottom - top)
        drawPhases(phases, dw, 0f, 18.dp.toPx(), measurer, style.copy(fontSize = 9.sp))
        val cover = y(36.43)
        drawLine(palette.ovulation.copy(alpha = 0.7f), Offset(0f, cover), Offset(size.width, cover), 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        drawLine(grid, Offset(0f, bottom), Offset(size.width, bottom), 1f)
        drawLine(palette.ovulation, Offset(13.5f * dw, top), Offset(13.5f * dw, bottom), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        val path = Path()
        values.forEachIndexed { d, v -> val p = Offset((d + 0.5f) * dw, y(v)); if (d == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawPath(path, line, style = Stroke(2.dp.toPx()))
        if (showLevels) {
            // Example values, so the user sees how small the change is: a few tenths of a degree.
            listOf(36.40 to "36.40 °C", 36.80 to "36.80 °C").forEach { (v, label) ->
                val t = measurer.measure(label, style.copy(fontSize = 10.sp))
                val x = if (v < 36.6) 13.5f * dw - t.size.width - 6.dp.toPx() else size.width - t.size.width
                drawText(t, topLeft = Offset(x, y(v) + 4.dp.toPx()))
            }
        }
        values.forEachIndexed { d, v -> drawCircle(if (d >= 14) palette.ovulation else line, 2.5.dp.toPx(), Offset((d + 0.5f) * dw, y(v))) }
    }
}

/** The phase explanations under the typical curve. */
@Composable
fun PhaseTexts() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.phase_period_text, R.string.phase_follicular_text, R.string.phase_ovulation_text, R.string.phase_luteal_text)
            .forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
    }
}
