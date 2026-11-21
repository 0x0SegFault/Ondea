package app.ondea.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ondea.R

private val LightScheme = lightColorScheme(
    primary = Color(0xFFC2415D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDE3),
    onPrimaryContainer = Color(0xFF4A0418),
    secondary = Color(0xFF8C5A63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7EA),
    onSecondaryContainer = Color(0xFF3A1A20),
    tertiary = Color(0xFF14877A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3F2EC),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF2A1F21),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF2A1F21),
    surfaceVariant = Color(0xFFF5E6E8),
    onSurfaceVariant = Color(0xFF6E5A5E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1F2),
    surfaceContainer = Color(0xFFFCEBEE),
    surfaceContainerHigh = Color(0xFFF8E4E7),
    surfaceContainerHighest = Color(0xFFF2DDE1),
    outline = Color(0xFFB59BA0),
    outlineVariant = Color(0xFFEBD6DA),
    error = Color(0xFFBA1A1A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB1C1),
    onPrimary = Color(0xFF5F1129),
    primaryContainer = Color(0xFF7E2A40),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFE6BDC4),
    onSecondary = Color(0xFF43292E),
    secondaryContainer = Color(0xFF4A3035),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFF7FD8C9),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF0E4A43),
    onTertiaryContainer = Color(0xFFA6F2E4),
    background = Color(0xFF1B1416),
    onBackground = Color(0xFFF0DEE1),
    surface = Color(0xFF1B1416),
    onSurface = Color(0xFFF0DEE1),
    surfaceVariant = Color(0xFF3A2E31),
    onSurfaceVariant = Color(0xFFD7C1C5),
    surfaceContainerLowest = Color(0xFF150F10),
    surfaceContainerLow = Color(0xFF231B1D),
    surfaceContainer = Color(0xFF281F21),
    surfaceContainerHigh = Color(0xFF33292B),
    surfaceContainerHighest = Color(0xFF3E3336),
    outline = Color(0xFF9F8C90),
    outlineVariant = Color(0xFF52434A),
)

/** Colors with a fixed meaning across chart, calendar and ring. */
@Immutable
data class CyclePalette(
    val period: Color,
    val periodSoft: Color,
    val fertile: Color,
    val fertileSoft: Color,
    val peak: Color,
    val ovulation: Color,
    val ovulationSoft: Color,
    val neutral: Color,
    val disturbed: Color,
    val mucus: Color,
)

private val LightCycle = CyclePalette(
    period = Color(0xFFE04A67),
    periodSoft = Color(0xFFFBD3DB),
    fertile = Color(0xFF3FB5A3),
    fertileSoft = Color(0xFFD4F1EB),
    peak = Color(0xFF14877A),
    ovulation = Color(0xFF8A5CD0),
    ovulationSoft = Color(0xFFE6DAF7),
    neutral = Color(0xFFF1E3E6),
    disturbed = Color(0xFFA79A9D),
    mucus = Color(0xFF5B8DEF),
)

private val DarkCycle = CyclePalette(
    period = Color(0xFFFF6F8A),
    periodSoft = Color(0xFF5A2632),
    fertile = Color(0xFF52C7B5),
    fertileSoft = Color(0xFF1A4740),
    peak = Color(0xFF7FE0CF),
    ovulation = Color(0xFFB591F0),
    ovulationSoft = Color(0xFF3E2F5C),
    neutral = Color(0xFF3A2E31),
    disturbed = Color(0xFF8D8083),
    mucus = Color(0xFF8DB2FF),
)

val LocalCycleColors = staticCompositionLocalOf { LightCycle }

object CycleColors {
    val current: CyclePalette
        @Composable get() = LocalCycleColors.current
}

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Nunito = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800))

private val base = Typography()

private fun TextStyle.n(weight: FontWeight? = null) =
    copy(fontFamily = Nunito, fontWeight = weight ?: fontWeight)

private val AppTypography = Typography(
    displayLarge = base.displayLarge.n(FontWeight.ExtraBold),
    displayMedium = base.displayMedium.n(FontWeight.ExtraBold),
    displaySmall = base.displaySmall.n(FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.n(FontWeight.Bold),
    headlineMedium = base.headlineMedium.n(FontWeight.Bold),
    headlineSmall = base.headlineSmall.n(FontWeight.Bold),
    titleLarge = base.titleLarge.n(FontWeight.Bold),
    titleMedium = base.titleMedium.n(FontWeight.Bold),
    titleSmall = base.titleSmall.n(FontWeight.Bold),
    bodyLarge = base.bodyLarge.n(),
    bodyMedium = base.bodyMedium.n(),
    bodySmall = base.bodySmall.n(),
    labelLarge = base.labelLarge.n(FontWeight.Bold),
    labelMedium = base.labelMedium.n(FontWeight.SemiBold),
    labelSmall = base.labelSmall.n(FontWeight.SemiBold).copy(fontSize = 11.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun OndeaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalCycleColors provides if (dark) DarkCycle else LightCycle) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
