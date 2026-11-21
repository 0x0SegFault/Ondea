package app.ondea.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.domain.CycleEngine
import app.ondea.domain.DayInfo
import app.ondea.ui.theme.CycleColors
import app.ondea.ui.theme.CyclePalette
import kotlin.math.cos
import kotlin.math.sin

/** Rounded white card used for every content block. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        IconBadge(icon, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
            }
            content()
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Single-choice row of pill chips. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(
    labels: List<String>,
    selected: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
    leading: (@Composable (Int) -> Unit)? = null,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        labels.forEachIndexed { i, label ->
            FilterChip(
                selected = selected == i,
                onClick = { onSelect(i) },
                label = { Text(label) },
                leadingIcon = leading?.let { { it(i) } },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected == i,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = accent, selectedBorderWidth = 1.5.dp,
                ),
            )
        }
    }
}

/** On/off pill with an icon. */
@Composable
fun ToggleChip(label: String, icon: ImageVector, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = checked,
        onClick = { onChange(!checked) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp), tint = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.18f)),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = checked,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = accent, selectedBorderWidth = 1.5.dp,
        ),
    )
}

/** "- 28 days +" stepper. */
@Composable
fun NumberStepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledTonalIconButton(onClick = { onChange((value - 1).coerceIn(range)) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, stringResource(R.string.decrease))
        }
        Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(110.dp), textAlign = TextAlign.Center)
        FilledTonalIconButton(onClick = { onChange((value + 1).coerceIn(range)) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, stringResource(R.string.increase))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, ring: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).clip(CircleShape)
                .then(if (ring) Modifier.border(2.dp, color, CircleShape) else Modifier.background(color)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Small "label / value" tile used in rows of key facts. */
@Composable
fun RowScope.StatTile(label: String, value: String, accent: Color, weight: Float = 1f) {
    Surface(
        modifier = Modifier.weight(weight),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.10f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

fun CyclePalette.colorFor(info: DayInfo, recordedOrPast: Boolean): Color {
    val flow = info.entry?.flow ?: 0
    return when {
        flow > 0 -> period
        info.ovulation -> ovulation
        info.ovulationPredicted -> ovulation.copy(alpha = 0.55f)
        info.peak && info.fertile -> peak
        info.fertile -> fertile
        info.periodPredicted -> period.copy(alpha = 0.4f)
        info.peak && info.fertilePredicted -> peak.copy(alpha = 0.5f)
        info.fertilePredicted -> fertile.copy(alpha = 0.45f)
        else -> if (recordedOrPast) neutral else neutral.copy(alpha = 0.6f)
    }
}

/**
 * The cycle as a ring of day segments, coloured by phase, with a marker on today.
 * [content] is drawn in the middle.
 */
@Composable
fun CycleRing(engine: CycleEngine, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val palette = CycleColors.current
    val markerBorder = MaterialTheme.colorScheme.primary
    val markerFill = MaterialTheme.colorScheme.surfaceContainerLowest
    val f = engine.forecast
    val cycle = engine.current
    Box(modifier, contentAlignment = Alignment.Center) {
        if (f != null && cycle != null) {
            val total = maxOf(CycleEngine.days(cycle.start, f.nextPeriod), f.cycleDay)
            val colors = (0 until total).map { i ->
                val date = cycle.start.plusDays(i.toLong())
                palette.colorFor(engine.dayInfo(date), !date.isAfter(engine.today))
            }
            Canvas(Modifier.matchParentSize()) {
                val stroke = size.minDimension * 0.075f
                val radius = (size.minDimension - stroke) / 2f - stroke * 0.4f
                val topLeft = Offset(center.x - radius, center.y - radius)
                val sweep = 360f / total
                val gap = if (total > 40) 0.8f else 1.6f
                colors.forEachIndexed { i, c ->
                    drawArc(
                        color = c,
                        startAngle = -90f + i * sweep + gap / 2,
                        sweepAngle = sweep - gap,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                val angle = Math.toRadians((-90f + (f.cycleDay - 0.5f) * sweep).toDouble())
                val pos = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
                drawCircle(markerFill, stroke * 0.85f, pos)
                drawCircle(markerBorder, stroke * 0.85f, pos, style = Stroke(stroke * 0.28f))
            }
        } else {
            Canvas(Modifier.matchParentSize()) {
                val stroke = size.minDimension * 0.075f
                drawCircle(palette.neutral, (size.minDimension - stroke) / 2f - stroke * 0.4f, style = Stroke(stroke))
            }
        }
        content()
    }
}
