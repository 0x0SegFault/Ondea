package app.ondea.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ondea.R
import app.ondea.domain.ChartData
import app.ondea.domain.OvulationSource
import app.ondea.domain.Temperature
import app.ondea.ui.theme.CycleColors
import app.ondea.ui.theme.Nunito
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val MIN_DAY_WIDTH = 8.dp
private val MAX_DAY_WIDTH = 32.dp
private val CHART_HEIGHT = 320.dp
private val TOP_PAD = 16.dp
private val BOTTOM_PAD = 64.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartScreen(s: UiState.Ready) {
    val engine = s.engine
    if (engine.cycles.isEmpty()) {
        Centered { Text(stringResource(R.string.no_cycles)) }
        return
    }
    var index by rememberSaveable { mutableIntStateOf(engine.cycles.lastIndex) }
    index = index.coerceIn(0, engine.cycles.lastIndex)
    val chart = engine.chart(index)
    val fahrenheit = s.data.settings.fahrenheit
    val palette = CycleColors.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { index-- }, enabled = index > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.cycle_n, index + 1), style = MaterialTheme.typography.titleLarge)
                Text(
                    chart.cycle.start.dayMonth() + " – " + (chart.cycle.end?.dayMonth() ?: stringResource(R.string.now)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = { index++ }, enabled = index < engine.cycles.lastIndex) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next))
            }
        }

        SectionCard {
            TemperatureChart(chart, fahrenheit)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LegendItem(palette.period, stringResource(R.string.legend_period))
                LegendItem(palette.mucus, stringResource(R.string.legend_mucus))
                LegendItem(palette.fertile, stringResource(R.string.legend_fertile))
                LegendItem(palette.ovulation, stringResource(R.string.legend_ovulation))
                LegendItem(palette.disturbed, stringResource(R.string.legend_disturbed), ring = true)
            }
        }

        val shift = chart.cycle.shift
        val window = chart.window
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                stringResource(R.string.stat_length),
                chart.cycle.naturalLength?.let { stringResource(R.string.n_days, it) } ?: "–",
                palette.period,
            )
            StatTile(
                stringResource(R.string.stat_ovulation),
                window?.let { stringResource(R.string.day_n, it.ovulationIndex + 1) } ?: "–",
                palette.ovulation,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                stringResource(R.string.stat_coverline),
                shift?.let { Temperature.format(it.coverlineC, fahrenheit) + " " + Temperature.unit(fahrenheit) } ?: "–",
                palette.ovulation,
            )
            StatTile(
                stringResource(R.string.stat_luteal),
                chart.cycle.naturalLength.let { length ->
                    if (shift != null && length != null) stringResource(R.string.n_days, length - shift.ovulationIndex - 1) else "–"
                },
                palette.fertile,
            )
        }
        Text(
            stringResource(
                when {
                    window == null -> R.string.summary_no_signs
                    window.source == OvulationSource.TEMPERATURE && window.closed -> R.string.summary_confirmed
                    window.source == OvulationSource.TEMPERATURE -> R.string.summary_waiting_mucus
                    window.source == OvulationSource.LH_TEST -> R.string.summary_lh
                    window.source == OvulationSource.MUCUS -> R.string.summary_mucus_only
                    else -> R.string.summary_predicted
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard(title = stringResource(R.string.how_it_works_title), icon = Icons.Outlined.Lightbulb) {
            Text(stringResource(R.string.how_it_works_temperature), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.how_it_works_lh), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.how_it_works_mucus), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.how_it_works_prediction), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TemperatureChart(chart: ChartData, fahrenheit: Boolean) {
    val measurer = rememberTextMeasurer()
    val palette = CycleColors.current
    val grid = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.colorScheme.primary
    val todayColor = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val labelStyle = TextStyle(fontFamily = Nunito, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val peakStyle = TextStyle(fontFamily = Nunito, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = palette.mucus)

    val display = chart.temps.map { t -> t?.let { Temperature.toDisplay(it, fahrenheit) } }
    val present = display.filterNotNull()
    val step = if (fahrenheit) 0.2 else 0.1
    val defaultMin = if (fahrenheit) 96.8 else 36.0
    val defaultMax = if (fahrenheit) 99.0 else 37.2
    val minY = floor(((present.minOrNull() ?: defaultMin).coerceAtMost(defaultMin + 4 * step) - step) / step) * step
    val maxY = ceil(((present.maxOrNull() ?: defaultMax).coerceAtLeast(minY + 8 * step) + step) / step) * step
    val gridLines = ((maxY - minY) / step).roundToInt()
    val window = chart.window
    val predicted = window?.source == OvulationSource.PREDICTED
    val peakMucus = chart.cycle.mucus.peakIndex

    // Fresh scroll position per cycle: past cycles open at day 1, the current one around today.
    val scroll = key(chart.cycle.start) { rememberScrollState() }
    val density = LocalDensity.current
    LaunchedEffect(chart.cycle.start, chart.todayIndex, scroll.maxValue) {
        val target = chart.todayIndex?.let { with(density) { ((it - 6) * MIN_DAY_WIDTH.toPx()).roundToInt() } } ?: 0
        scroll.scrollTo(target.coerceIn(0, scroll.maxValue))
    }

    fun DrawScope.yOf(v: Double): Float {
        val top = TOP_PAD.toPx()
        val h = size.height - top - BOTTOM_PAD.toPx()
        return top + h * (1f - ((v - minY) / (maxY - minY)).toFloat())
    }

    // Fit the whole cycle in the available width; scroll only below a readable minimum.
    var plotWidthPx by remember { mutableIntStateOf(0) }
    val dayWidth = with(density) { (plotWidthPx.toDp() / chart.days) }.coerceIn(MIN_DAY_WIDTH, MAX_DAY_WIDTH)
    // Label every day when there is room, otherwise day 1 and every 5th day.
    val labelEvery = if (dayWidth >= 20.dp) 1 else 5
    Row(Modifier.fillMaxWidth()) {
        Canvas(Modifier.width(36.dp).height(CHART_HEIGHT)) {
            for (g in 0..gridLines step 2) {
                val v = minY + g * step
                val layout = measurer.measure(String.format(Locale.getDefault(), "%.1f", v), labelStyle)
                drawText(layout, topLeft = Offset(size.width - layout.size.width - 6.dp.toPx(), yOf(v) - layout.size.height / 2f))
            }
        }
        Box(Modifier.weight(1f).onSizeChanged { plotWidthPx = it.width }.horizontalScroll(scroll)) {
            Canvas(Modifier.width(dayWidth * chart.days).height(CHART_HEIGHT)) {
                val dw = dayWidth.toPx()
                val plotTop = TOP_PAD.toPx()
                val plotBottom = size.height - BOTTOM_PAD.toPx()
                val plotH = plotBottom - plotTop
                fun xOf(i: Int) = (i + 0.5f) * dw
                val radius = CornerRadius(6.dp.toPx())

                if (window != null) {
                    val alpha = if (predicted) 0.55f else 1f
                    drawRoundRect(
                        palette.fertileSoft.copy(alpha = alpha),
                        topLeft = Offset(window.fertileStart * dw, plotTop),
                        size = Size((window.fertileEnd - window.fertileStart + 1) * dw, plotH),
                        cornerRadius = radius,
                    )
                    drawRoundRect(
                        palette.fertile.copy(alpha = 0.22f * alpha),
                        topLeft = Offset(window.peakStart * dw, plotTop),
                        size = Size((window.ovulationIndex - window.peakStart + 1) * dw, plotH),
                        cornerRadius = radius,
                    )
                }

                for (g in 0..gridLines) {
                    val y = yOf(minY + g * step)
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = if (g % 2 == 0) 1.2f else 0.5f)
                }

                chart.todayIndex?.let { i ->
                    drawRoundRect(todayColor.copy(alpha = 0.08f), Offset(i * dw + 2f, plotTop), Size(dw - 4f, plotH), radius)
                }

                window?.let { w ->
                    drawLine(
                        palette.ovulation, Offset(xOf(w.ovulationIndex), plotTop), Offset(xOf(w.ovulationIndex), plotBottom),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = if (w.source != OvulationSource.TEMPERATURE) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null,
                    )
                }

                chart.cycle.shift?.let { shift ->
                    val y = yOf(Temperature.toDisplay(shift.coverlineC, fahrenheit))
                    drawLine(
                        palette.ovulation.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    )
                }

                // Line + soft gradient through valid readings; disturbed readings are not connected.
                val points = display.mapIndexedNotNull { i, v ->
                    if (v == null || chart.disturbed[i]) null else Offset(xOf(i), yOf(v))
                }
                if (points.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    val area = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, plotBottom)
                        lineTo(points.first().x, plotBottom)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.22f), line.copy(alpha = 0f)), plotTop, plotBottom))
                    drawPath(path, line, style = Stroke(width = 2.5.dp.toPx()))
                }

                val firstHigh = chart.cycle.shift?.firstHighIndex
                val dot = minOf(4.5.dp.toPx(), dw * 0.36f)
                display.forEachIndexed { i, v ->
                    if (v == null) return@forEachIndexed
                    val c = Offset(xOf(i), yOf(v))
                    if (chart.disturbed[i]) {
                        drawCircle(surface, dot, c)
                        drawCircle(palette.disturbed, dot, c, style = Stroke(1.8.dp.toPx()))
                    } else {
                        val color = if (firstHigh != null && i >= firstHigh) palette.ovulation else line
                        drawCircle(surface, dot * 1.33f, c)
                        drawCircle(color, dot, c)
                    }
                    if (chart.intercourse[i]) {
                        drawCircle(palette.ovulation.copy(alpha = 0.7f), 2.5.dp.toPx(), Offset(xOf(i), plotTop - 7.dp.toPx()))
                    }
                }

                // Rows under the plot: period flow, then mucus quality, then day numbers.
                val rowH = 9.dp.toPx()
                val periodY = plotBottom + 6.dp.toPx()
                val mucusY = periodY + rowH + 5.dp.toPx()
                for (i in 0 until chart.days) {
                    val f = chart.flow[i]
                    if (f > 0) {
                        drawRoundRect(palette.period.copy(alpha = 0.25f + 0.1875f * f), Offset(i * dw + 3f, periodY), Size(dw - 6f, rowH), CornerRadius(rowH / 2))
                    }
                    val m = chart.mucus[i]
                    if (m > 0) {
                        drawRoundRect(palette.mucus.copy(alpha = 0.15f + 0.2125f * m), Offset(i * dw + 3f, mucusY), Size(dw - 6f, rowH), CornerRadius(rowH / 2))
                    }
                    if (i == peakMucus) {
                        val p = measurer.measure("P", peakStyle.copy(color = surface))
                        drawText(p, topLeft = Offset(xOf(i) - p.size.width / 2f, mucusY + (rowH - p.size.height) / 2f))
                    }
                    val isToday = i == chart.todayIndex
                    if (!isToday && i != 0 && (i + 1) % labelEvery != 0) continue
                    val layout = measurer.measure(
                        (i + 1).toString(),
                        if (isToday) labelStyle.copy(color = todayColor, fontWeight = FontWeight.ExtraBold) else labelStyle,
                    )
                    drawText(layout, topLeft = Offset(xOf(i) - layout.size.width / 2f, size.height - layout.size.height - 2.dp.toPx()))
                }
            }
        }
    }
}
