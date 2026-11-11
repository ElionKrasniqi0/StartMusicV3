package io.ymusic.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable

@Composable
internal actual fun SuppressPressHaptics(
    active: Boolean,
    interactionSource: InteractionSource,
) = Unit
