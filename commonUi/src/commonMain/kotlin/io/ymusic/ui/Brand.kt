package io.ymusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Central StartMusicV3 visual identity. Palette values feed [DesignPalette] / [DefaultAccent];
 * typography feeds [brandTypography]; corner radii are shared by list thumbs, player art, and chrome.
 *
 * Official product name: **StartMusicV3** — via compose-resources `app_name`, not scattered literals.
 * Change brand colors, type, and radii here — do not scatter hex / font literals across screens.
 */
object Brand {
    /** Display name resource: [io.ymusic.ui.resources.app_name] → StartMusicV3. */

    /** Violet accent — primary actions, progress, selected chrome. */
    val Primary = Color(0xFF7B5CFF)
    val Secondary = Color(0xFF4A3A9E)
    /** Soft cyan highlight — secondary accents / progress glow; primary actions use [Primary]. */
    val Highlight = Color(0xFF5EC8F0)

    /** Charcoal grounds — default dark product look. */
    val DarkBackground = Color(0xFF0C0C10)
    val DarkSurface = Color(0xFF16161C)
    val DarkElevated = Color(0xFF22222C)
    val DarkText = Color(0xFFF4F2FA)
    val DarkTextMuted = Color(0xFFA09AAC)

    val LightBackground = Color(0xFFF6F4FB)
    val LightSurface = Color(0xFFEEEAF6)
    val LightElevated = Color(0xFFE4DFF0)
    val LightText = Color(0xFF16141F)
    val LightTextMuted = Color(0xFF6A6478)

    val ThumbCorner = 12.dp
    val CardCorner = 16.dp
    val ArtCorner = 14.dp
    val MiniPlayerCorner = 18.dp
    val SplashCorner = 28.dp

    val TypeFamily = FontFamily.SansSerif

    val Display = TextStyle(
        fontFamily = TypeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    )
    val Title = TextStyle(
        fontFamily = TypeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    )
    val Headline = TextStyle(
        fontFamily = TypeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    )
    val Body = TextStyle(
        fontFamily = TypeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    )
    val Label = TextStyle(
        fontFamily = TypeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    )
    val Caption = TextStyle(
        fontFamily = TypeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp,
    )
}
