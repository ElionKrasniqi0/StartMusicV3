package io.ymusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Central StartMusicV3 visual identity. Palette values feed [DesignPalette] / [DefaultAccent];
 * corner radii are shared by list thumbs, player art, and elevated chrome.
 *
 * Official product name: **StartMusicV3** — via compose-resources `app_name`, not scattered literals.
 * Change brand colors and radii here — do not scatter hex literals across screens.
 */
object Brand {
    /** Official display name resource: [io.ymusic.ui.resources.app_name] → StartMusicV3. */

    val Primary = Color(0xFF7657FF)
    val Secondary = Color(0xFF4936A8)
    /** Soft cyan highlight — secondary accents / progress glow; primary actions use [Primary]. */
    val Highlight = Color(0xFF62D8FF)

    val DarkBackground = Color(0xFF101116)
    val DarkSurface = Color(0xFF1B1921)
    val DarkElevated = Color(0xFF25222D)
    val DarkText = Color(0xFFF7F5FF)
    val DarkTextMuted = Color(0xFFAAA6B7)

    val LightBackground = Color(0xFFF7F5FF)
    val LightSurface = Color(0xFFEFECF8)
    val LightElevated = Color(0xFFE4E0F2)
    val LightText = Color(0xFF1A1625)
    val LightTextMuted = Color(0xFF6B6680)

    val ThumbCorner = 10.dp
    val CardCorner = 14.dp
    val ArtCorner = 12.dp
    val MiniPlayerCorner = 16.dp
}
