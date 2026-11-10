package com.pantri.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pantri.app.R

/** Colour roles Material doesn't have: soft tints for tiles and pills, and the hero gradients. */
@Immutable
class PantriColors(
    val dark: Boolean,
    val bg: Color,
    val card: Color,
    val ink: Color,
    val sub: Color,
    val muted: Color,
    val line: Color,
    val green: Color,
    val greenLight: Color,
    val greenDeep: Color,
    val yellow: Color,
    val orange: Color,
    val tomato: Color,
    val tints: Map<Tone, Pair<Color, Color>>,
    val shadow: Color,
) {
    val heroGreen: List<Color> get() = listOf(greenLight, greenDeep)
    val heroSunset: List<Color> get() = listOf(orange, tomato)
    fun tint(tone: Tone): Pair<Color, Color> = tints.getValue(tone)
}

enum class Tone { Green, Orange, Red, Yellow }

private val LightColors = PantriColors(
    dark = false, bg = Color(0xFFFFF8EE), card = Color.White, ink = Color(0xFF1F2A24), sub = Color(0xFF5F6B64), muted = Color(0xFF8A948D),
    line = Color(0xFFF1E8D6), green = Color(0xFF2F7A5B), greenLight = Color(0xFF46A478), greenDeep = Color(0xFF1B5440),
    yellow = Color(0xFFF6C62F), orange = Color(0xFFF08A24), tomato = Color(0xFFE8503A),
    tints = mapOf(
        Tone.Green to (Color(0xFFE3F3EA) to Color(0xFF246A4D)),
        Tone.Orange to (Color(0xFFFDE9D2) to Color(0xFFB4560A)),
        Tone.Red to (Color(0xFFFDE7E2) to Color(0xFFB93A26)),
        Tone.Yellow to (Color(0xFFFFF3C9) to Color(0xFF8A6800)),
    ),
    shadow = Color(0xFF3C280A),
)

private val DarkColors = PantriColors(
    dark = true, bg = Color(0xFF111A15), card = Color(0xFF1B2721), ink = Color(0xFFF3EFE4), sub = Color(0xFFB3BEB6), muted = Color(0xFF86928A),
    line = Color(0xFF2A3830), green = Color(0xFF4DB383), greenLight = Color(0xFF3E9A6F), greenDeep = Color(0xFF173F31),
    yellow = Color(0xFFF6C62F), orange = Color(0xFFF2993A), tomato = Color(0xFFE8604A),
    tints = mapOf(
        Tone.Green to (Color(0xFF203B2F) to Color(0xFF8AD9B0)),
        Tone.Orange to (Color(0xFF3D2B18) to Color(0xFFF5B877)),
        Tone.Red to (Color(0xFF3E2421) to Color(0xFFF7A090)),
        Tone.Yellow to (Color(0xFF3A3419) to Color(0xFFF2D96E)),
    ),
    shadow = Color.Black,
)

val LocalPantri = staticCompositionLocalOf { LightColors }

val MaterialTheme.pantri: PantriColors
    @Composable @ReadOnlyComposable get() = LocalPantri.current

private fun variable(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Figtree = FontFamily(variable(R.font.figtree, 400), variable(R.font.figtree, 500), variable(R.font.figtree, 600), variable(R.font.figtree, 700))
val Bricolage = FontFamily(variable(R.font.bricolage_grotesque, 600), variable(R.font.bricolage_grotesque, 700), variable(R.font.bricolage_grotesque, 800))

private fun type(c: Color) = Typography(
    displayLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp, color = c),
    displayMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp, color = c),
    headlineLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = c),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, color = c),
    headlineSmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, color = c),
    titleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = c),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = c),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = c),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = c),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = c),
    bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = c),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = c),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = c),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, color = c),
)

@Composable
fun PantriTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.green, onPrimary = Color(0xFF0B241A), primaryContainer = p.tint(Tone.Green).first, onPrimaryContainer = p.tint(Tone.Green).second,
            secondary = p.orange, onSecondary = Color(0xFF2B1600), tertiary = p.tomato, onTertiary = Color.White,
            background = p.bg, onBackground = p.ink, surface = p.card, onSurface = p.ink, surfaceVariant = p.line, onSurfaceVariant = p.sub,
            outline = p.muted, outlineVariant = p.line, error = Color(0xFFF2766B), surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerHighest = p.line,
        )
    } else {
        lightColorScheme(
            primary = p.green, onPrimary = Color.White, primaryContainer = p.tint(Tone.Green).first, onPrimaryContainer = p.tint(Tone.Green).second,
            secondary = p.orange, onSecondary = Color.White, tertiary = p.tomato, onTertiary = Color.White,
            background = p.bg, onBackground = p.ink, surface = p.card, onSurface = p.ink, surfaceVariant = p.line, onSurfaceVariant = p.sub,
            outline = p.muted, outlineVariant = p.line, error = Color(0xFFC8402B), surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerHighest = p.line,
        )
    }
    CompositionLocalProvider(LocalPantri provides p) {
        MaterialTheme(
            colorScheme = scheme,
            typography = type(p.ink),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
            ),
        ) {
            // Icons without their own tint (back arrows, menus) follow the theme's text colour.
            CompositionLocalProvider(LocalContentColor provides p.ink, content = content)
        }
    }
}
