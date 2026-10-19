package app.tessera.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tessera.R

/**
 * A Byzantine apse by lamplight: lapis-dark vaults, gold ground, plaster-white letters, and the smalt
 * itself in every colour of glass. The gold is for what matters; everything else stays in the dark.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val gold: Color,
    val goldSoft: Color,
    val onGold: Color,
    /** The mortar between stones. */
    val grout: Color,
    val danger: Color,
)

val Apse = Palette(
    bg = Color(0xFF0F1220),
    surface = Color(0xFF171B2C),
    surface2 = Color(0xFF20253A),
    line = Color(0xFF2D3350),
    ink = Color(0xFFF3EBDD),
    muted = Color(0xFFB9B2A4),
    faint = Color(0xFF7C7889),
    gold = Color(0xFFD9AA48),
    goldSoft = Color(0xFF3A3122),
    onGold = Color(0xFF1A1408),
    grout = Color(0xFF090B13),
    danger = Color(0xFFD65B44),
)

/** The smalt: twelve glasses a question can be laid in. Indices are stored, so only ever append. */
object Smalt {
    val all = listOf(
        Color(0xFF2F5DA8), // lapis
        Color(0xFF2F9C9A), // turquoise
        Color(0xFF3E8E5A), // malachite
        Color(0xFFD39A3A), // ochre
        Color(0xFFC0603E), // terracotta
        Color(0xFFB9363A), // cinnabar
        Color(0xFFC9708A), // rose
        Color(0xFF7D5BA6), // amethyst
        Color(0xFF8A9A3E), // olive
        Color(0xFF7A3550), // porphyry
        Color(0xFF6FA6D6), // sky
        Color(0xFFD8CDB8), // marble
    )

    val gold = Color(0xFFE2B552)

    fun of(i: Int): Color = all[Math.floorMod(i, all.size)]
}

val LocalPalette = staticCompositionLocalOf { Apse }

/** Cormorant: tall, chiselled capitals, like letters set in a mosaic's border. */
val Serif = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Manrope for everything that has to be read quickly. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

object UType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 40.sp, color = U.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, color = U.c.ink)
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 25.sp, color = U.c.ink)
    /** A question, large, as it is asked. */
    val Ask: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 31.sp, lineHeight = 35.sp, color = U.c.ink)
    val Said: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 21.sp, lineHeight = 27.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.16.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 15.5.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 13.5.sp, lineHeight = 19.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
}

@Composable
fun TesseraTheme(content: @Composable () -> Unit) {
    val p = Apse
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = p.gold,
                onPrimary = p.onGold,
                primaryContainer = p.goldSoft,
                onPrimaryContainer = p.ink,
                secondary = p.gold,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface2,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 15.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
