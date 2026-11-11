package io.ymusic.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun SuppressPressHaptics(
    active: Boolean,
    interactionSource: InteractionSource,
) {
    if (!active) return
    val view = LocalView.current
    LaunchedEffect(view, interactionSource) {
        var previous: Boolean? = null
        try {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        if (previous == null) previous = view.isHapticFeedbackEnabled
                        view.isHapticFeedbackEnabled = false
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        previous?.let { view.isHapticFeedbackEnabled = it }
                        previous = null
                    }
                }
            }
        } finally {
            // Leave-composition / cancel mid-press must not leave the Activity View muted.
            previous?.let { view.isHapticFeedbackEnabled = it }
        }
    }
}
