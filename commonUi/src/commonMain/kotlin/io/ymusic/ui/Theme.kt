package io.ymusic.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The wireframe design language (design/final/…html) as a resolvable palette: the mockups are the
 * light theme (white surfaces, near-black #333 text, gray placeholders, the orange accent); the
 * dark palette mirrors each role on near-black surfaces. Composables read the active palette
 * through [DesignColors] — the call sites look exactly like the old constant object.
 *
 * On-artwork chrome (badge fills/scrims and text over thumbnails or video) is NOT part of the
 * palette split: it sits on imagery, not on the surface, so it keeps fixed colors in both themes
 * ([BadgeInk], [BadgeScrim], and the raw white/dark literals in the player and badge composables).
 */
@Immutable
data class DesignPalette(
    val isDark: Boolean,
    val Accent: Color,
    /** Strongest text — big titles (mockup #1A–#22 blacks). */
    val InkStrong: Color,
    val Ink: Color,
    val InkMuted: Color,
    val InkSoft: Color,
    val InkSecondary: Color,
    val InkFaint: Color,
    val IconDots: Color,
    /** Text/icons on an [InkStrong]-filled control (the play button, selected pills). */
    val OnInk: Color,
    /** Text/icons on an [Accent]-filled control (Sign in, Send, Retry). */
    val OnAccent: Color,
    val Placeholder: Color,
    val PlaceholderDim: Color,
    val PlaceholderIcon: Color,
    val StackNear: Color,
    val StackFar: Color,
    val StackTab: Color,
    val Outline: Color,
    val OutlineSoft: Color,
    val Divider: Color,
    val DividerMini: Color,
    val Surface: Color,
    val SurfaceDim: Color,
    val SurfaceElevated: Color,
    /** Settings section card fill — one visible step off [Surface] in both themes. */
    val SettingsCard: Color,
    /** The search input's gray pill fill. */
    val FieldPill: Color,
    val BrandGreen: Color,
) {
    /** On-artwork badge fill — fixed across themes (sits on imagery, not the surface). */
    val BadgeInk = Color(0xFF333333)
    val BadgeScrim = Color(0xB8000000)
    val WatchedTrack = Color(0x66FFFFFF)
    val WatchedFill = Color(0xFFFF0000)
}

/** The accent when the user has not picked one — [Brand.Primary]. */
val DefaultAccent = Brand.Primary

val LightDesignColors = DesignPalette(
    isDark = false,
    Accent = DefaultAccent,
    InkStrong = Brand.LightText,
    Ink = Color(0xFF2A2540),
    InkMuted = Color(0xFF4A4560),
    InkSoft = Color(0xFF5C5772),
    InkSecondary = Brand.LightTextMuted,
    InkFaint = Color(0xFF8A849C),
    IconDots = Color(0xFF9A94AC),
    OnInk = Color.White,
    OnAccent = Color.White,
    Placeholder = Color(0xFFE2DEEF),
    PlaceholderDim = Color(0xFFDCD7EC),
    PlaceholderIcon = Color(0xFFB0A9C4),
    StackNear = Color(0xFFE8E4F4),
    StackFar = Color(0xFFF0EDF8),
    StackTab = Color(0xFFE6E2F2),
    Outline = Color(0xFFCFC9E0),
    OutlineSoft = Color(0xFFD8D3E8),
    Divider = Color(0xFFE6E2F0),
    DividerMini = Color(0xFFE2DEEC),
    Surface = Brand.LightBackground,
    SurfaceDim = Brand.LightSurface,
    SurfaceElevated = Brand.LightElevated,
    SettingsCard = Color(0xFFEAE6F6),
    FieldPill = Color(0xFFE8E4F4),
    BrandGreen = Color(0xFF1DB954),
)

val DarkDesignColors = DesignPalette(
    isDark = true,
    Accent = DefaultAccent,
    InkStrong = Brand.DarkText,
    Ink = Color(0xFFECEAF6),
    InkMuted = Color(0xFFC8C4D6),
    InkSoft = Color(0xFFB4B0C2),
    InkSecondary = Brand.DarkTextMuted,
    InkFaint = Color(0xFF7E7A8E),
    IconDots = Color(0xFF8E8A9C),
    OnInk = Brand.DarkBackground,
    OnAccent = Color.White,
    Placeholder = Color(0xFF353240),
    PlaceholderDim = Color(0xFF302E3A),
    PlaceholderIcon = Color(0xFF7A7688),
    StackNear = Color(0xFF353240),
    StackFar = Color(0xFF2E2C38),
    StackTab = Color(0xFF323040),
    Outline = Color(0xFF4A4658),
    OutlineSoft = Color(0xFF3E3A4C),
    Divider = Color(0xFF1E1C26),
    DividerMini = Color(0xFF22202A),
    Surface = Brand.DarkBackground,
    SurfaceDim = Brand.DarkSurface,
    SurfaceElevated = Brand.DarkElevated,
    SettingsCard = Color(0xFF2A2734),
    FieldPill = Color(0xFF302D3C),
    BrandGreen = Color(0xFF1DB954),
)

/** Dark with every ground pure black, for OLED screens. */
val AmoledDesignColors = DarkDesignColors.copy(
    Surface = Color.Black,
    SurfaceDim = Color.Black,
    SurfaceElevated = Color.Black,
    SettingsCard = Color.Black,
)

/** [AppSettings.theme] value → whether the UI is dark; "" follows the system. */
fun isDarkTheme(theme: String, systemDark: Boolean): Boolean = when (theme) {
    "light" -> false
    "dark", "amoled" -> true
    else -> systemDark
}

/** [AppSettings.themeAccent] value (`RRGGBB`) → its color; blank or malformed is [DefaultAccent]. */
fun accentColorOf(hex: String): Color {
    val rgb = hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16) ?: return DefaultAccent
    return darkenedAccent(Color(0xFF000000 or rgb))
}

/** The lightest an accent may be: white text on it (Sign in, Send) stays readable. The default sits right at it. */
const val MaxAccentLuminance = 0.3f

/** [color] darkened (same hue and saturation) until its luminance is at most [MaxAccentLuminance]. */
fun darkenedAccent(color: Color): Color {
    if (color.luminance() <= MaxAccentLuminance) return color
    var low = 0f
    var high = 1f
    repeat(16) {
        val mid = (low + high) / 2f
        if (Color(color.red * mid, color.green * mid, color.blue * mid).luminance() <= MaxAccentLuminance) low = mid else high = mid
    }
    return Color(color.red * low, color.green * low, color.blue * low)
}

fun accentHexOf(color: Color): String {
    fun channel(v: Float) = (v * 255f + 0.5f).toInt().coerceIn(0, 255).toString(16).padStart(2, '0')
    return (channel(color.red) + channel(color.green) + channel(color.blue)).uppercase()
}

/** The palette for [AppSettings.theme] and [AppSettings.themeAccent]. */
fun designPaletteFor(theme: String, systemDark: Boolean, accentHex: String = ""): DesignPalette {
    val base = when {
        theme == "amoled" -> AmoledDesignColors
        isDarkTheme(theme, systemDark) -> DarkDesignColors
        else -> LightDesignColors
    }
    val accent = accentColorOf(accentHex)
    return if (accent == base.Accent) base else base.copy(Accent = accent)
}

val LocalDesignColors = staticCompositionLocalOf { LightDesignColors }

/** The active theme's palette — reads like the old `object DesignColors` at every call site. */
val DesignColors: DesignPalette
    @Composable @ReadOnlyComposable
    get() = LocalDesignColors.current

/** Label size of every chip and pill (filters, sort, tags, CTAs, Library actions). */
internal val ChipTextSize = 13.5.sp

/**
 * The spacing scale: every layout spacing (margins, gaps, paddings between elements) is one of
 * these steps — component-internal geometry (pill insets, badge chrome, the 5dp stack tab) is
 * shape, not rhythm, and stays free. Feed layout bands live in `feedLayoutFor`: `<400dp` square
 * list, `320–469` 16:9 list, `≥470` grid — the fewest columns keeping cards ≤324dp wide
 * (`gridColumnsFor`; 3 columns from 690dp).
 */
object DesignSpacing {
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space16 = 16.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
    val Space48 = 48.dp

    /** Window edge margin, compact/medium windows. */
    val MarginCompact = Space16

    /** Window edge margin, expanded (`≥840dp`) windows. */
    val MarginExpanded = Space24

    /** Between grid cards (expanded windows; mobile-class uses [Space8]). */
    val Gutter = Space16

    /** Feed list rows' vertical inset — rows sit 2× this apart. Deliberately off the step scale:
     *  [Space8]'s 16dp rhythm read cramped against 92dp thumbs (review 2026-07-31) and the next
     *  step doubles it, so the row rhythm takes the midpoint. */
    val ListRowVertical = 10.dp

    /** Vertical gap between grid rows, every width class (the compact gutter stays horizontal). */
    val GridRowGap = Space16

    /** Settings row insets inside a section card. Vertical is off the step scale — the rows were tuned
     *  to this rhythm before the scale existed. */
    val SettingsRowHorizontal = Space16
    val SettingsRowVertical = 14.dp

    /** Settings section card: its margin from the screen edge, corner, and the gap above each header. */
    val SettingsCardInset = Space16
    val SettingsCardCorner = Brand.CardCorner
    val SettingsSectionGap = Space24

    /** Row insets inside the elevated-bar picker dialog ([ElevatedBarDialog]) — the choice rows align
     *  with the title (24) and take a 12 midpoint vertical, matching the content-region picker. */
    val DialogRowHorizontal = Space24
    val DialogRowVertical = 12.dp

    /** Max width for a single-column form (Feedback) — full-bleed text fields on an expanded
     *  window read as ribbons, so the column caps here and centers; matches [ElevatedBarDialog]'s
     *  560 max so forms and dialogs share one content width. */
    val FormMaxWidth = 560.dp
}

/**
 * Shared short motion budgets. Prefer these over ad-hoc durations so screens stay calm —
 * no layout-size animations here (those shake lists); fades and indicator slides only.
 */
object DesignMotion {
    /** Artwork fade-in over placeholder when the decode arrives late (not for cache hits). */
    const val ArtCrossfadeMs = 140

    /** Empty/error surface content opacity settle. */
    const val ContentFadeMs = 160

    /** Tab underline / chrome elevation. */
    const val ChromeMs = 180
}

/**
 * Per-window-class grid metrics. [inset] is each card's hover/ripple breathing room — half the
 * gutter, so cards sit exactly one gutter apart with zero structural row spacing (callers lay rows
 * with `margin - inset` edges and `gutter - 2×inset` gaps).
 */
class GridMetrics(val margin: androidx.compose.ui.unit.Dp, val gutter: androidx.compose.ui.unit.Dp) {
    val inset = gutter / 2
}

fun gridMetricsFor(windowWidth: androidx.compose.ui.unit.Dp): GridMetrics {
    val expanded = windowWidth >= 840.dp
    return GridMetrics(
        margin = if (expanded) DesignSpacing.MarginExpanded else DesignSpacing.MarginCompact,
        gutter = if (expanded) DesignSpacing.Gutter else DesignSpacing.Space8,
    )
}

private fun schemeFor(c: DesignPalette) = with(c) {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    base.copy(
        primary = Accent,
        onPrimary = OnAccent,
        secondary = Ink,
        onSecondary = OnInk,
        tertiary = BrandGreen,
        background = Surface,
        onBackground = Ink,
        surface = Surface,
        onSurface = Ink,
        surfaceVariant = Placeholder,
        onSurfaceVariant = InkSecondary,
        primaryContainer = lerp(Surface, Accent, if (isDark) 0.3f else 0.15f),
        onPrimaryContainer = if (isDark) lerp(Accent, Color.White, 0.35f) else lerp(Accent, Color.Black, 0.4f),
        secondaryContainer = if (isDark) Color(0xFF3F3F3F) else Color(0xFFF0F1F0),
        onSecondaryContainer = Ink,
        surfaceContainer = SurfaceDim,
        surfaceContainerLow = SurfaceDim,
        surfaceContainerHighest = Placeholder,
        outline = Outline,
        outlineVariant = DividerMini,
        scrim = BadgeScrim,
    )
}

/** Material3 type scale driven by [Brand] — keeps Windows and Android chrome consistent. */
fun brandTypography(): Typography = Typography(
    displayLarge = Brand.Display,
    displayMedium = Brand.Display.copy(fontSize = 24.sp, lineHeight = 30.sp),
    displaySmall = Brand.Title,
    headlineLarge = Brand.Title,
    headlineMedium = Brand.Headline.copy(fontSize = 18.sp, lineHeight = 24.sp),
    headlineSmall = Brand.Headline,
    titleLarge = Brand.Headline.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Brand.Headline,
    titleSmall = Brand.Body.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Brand.Body.copy(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = Brand.Body,
    bodySmall = Brand.Caption,
    labelLarge = Brand.Label.copy(fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = Brand.Label,
    labelSmall = Brand.Caption.copy(fontWeight = FontWeight.Medium),
)

@Composable
fun YMusicTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    YMusicTheme(if (dark) DarkDesignColors else LightDesignColors, content)

@Composable
fun YMusicTheme(palette: DesignPalette, content: @Composable () -> Unit) {
    val scheme = remember(palette) { schemeFor(palette) }
    val typography = remember { brandTypography() }
    CompositionLocalProvider(LocalDesignColors provides palette) {
        SyncSystemBars(palette.isDark)
        MaterialTheme(colorScheme = scheme, typography = typography) {
            // No root Surface sets it, so an uncolored Text would fall back to black in dark theme.
            CompositionLocalProvider(LocalContentColor provides palette.Ink) { AccentRipple(palette.Accent, content) }
        }
    }
}

/** Every hover/press background renders in the accent (at the ripple's own alphas). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AccentRipple(accent: Color, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalRippleConfiguration provides
            androidx.compose.material3.RippleConfiguration(color = accent),
        content = content,
    )
}
