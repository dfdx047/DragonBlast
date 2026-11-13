package com.dfdx047.dragonblast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dfdx047.dragonblast.R

/**
 * Dragon Blast palette: Raging Blast 2's menus (dark metal, a blue-white electric edge and the ember orange of
 * the logo) toned down. Graphite surfaces with a hint of blue, one warm accent (Ember, a muted amber) used
 * sparingly for the main action and the selected item, and a steel blue for secondary marks. Selections are a
 * thin outline or a slightly lighter surface, never a big saturated block.
 */
object DbColors {
    val Ink = Color(0xFF0B0D11)
    val Graphite = Color(0xFF0E1014)
    val Slate = Color(0xFF161A21)
    val SlateHigh = Color(0xFF1C2129)
    val SlateHighest = Color(0xFF242A34)
    val Line = Color(0xFF2A313C)
    val LineSoft = Color(0xFF1F252E)

    val Ember = Color(0xFFDDA05A)        // main accent: Play, selected tab, slider
    val EmberDeep = Color(0xFFB9783A)
    val EmberGlow = Color(0xFFF2C48A)
    val Steel = Color(0xFF8AA9C7)        // secondary: info marks, links
    val SteelDeep = Color(0xFF4F6B87)
    val Silver = Color(0xFFD5DAE1)
    val Text = Color(0xFFE7E9ED)
    val TextDim = Color(0xFF9AA3AF)
    val TextFaint = Color(0xFF6C7480)

    val Ok = Color(0xFF6FBF8E)
    val Warn = Color(0xFFE0B45C)
    val Error = Color(0xFFE2786B)
}

private val DarkScheme = darkColorScheme(
    primary = DbColors.Ember,
    onPrimary = Color(0xFF2A1A08),
    primaryContainer = Color(0xFF3A2A18),
    onPrimaryContainer = DbColors.EmberGlow,
    secondary = DbColors.Steel,
    onSecondary = Color(0xFF0E1A26),
    secondaryContainer = Color(0xFF1E2833),
    onSecondaryContainer = Color(0xFFD3E2F0),
    tertiary = DbColors.Silver,
    onTertiary = Color(0xFF1A1D22),
    background = DbColors.Graphite,
    onBackground = DbColors.Text,
    surface = DbColors.Graphite,
    onSurface = DbColors.Text,
    surfaceVariant = DbColors.SlateHigh,
    onSurfaceVariant = DbColors.TextDim,
    surfaceContainerLowest = DbColors.Ink,
    surfaceContainerLow = Color(0xFF12151A),
    surfaceContainer = DbColors.Slate,
    surfaceContainerHigh = DbColors.SlateHigh,
    surfaceContainerHighest = DbColors.SlateHighest,
    outline = Color(0xFF4A5260),
    outlineVariant = DbColors.Line,
    error = DbColors.Error,
    onError = Color(0xFF2B0B07),
    inverseSurface = DbColors.Silver,
    inverseOnSurface = DbColors.Graphite,
    scrim = Color(0xCC000000),
)

/** Rajdhani (OFL): squared, technical letters that fit the game's HUD; for titles and the logo only. */
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

private val DbTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 24.sp),
        titleMedium = t.titleMedium.copy(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.3.sp),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    )
}

/** "DRAGON BLAST": wide, heavy and calm. */
val LogoStyle = TextStyle(
    fontFamily = Rajdhani,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    letterSpacing = 4.sp,
)

@Composable
fun DragonBlastTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = DbTypography, content = content)
}
