package io.ymusic.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable

/**
 * While [active], suppress platform haptic feedback for presses on [interactionSource].
 * Used to silence Android View/OEM touch vibration on a single clickable without disabling
 * haptics globally or for other Settings rows.
 */
@Composable
internal expect fun SuppressPressHaptics(
    active: Boolean,
    interactionSource: InteractionSource,
)
