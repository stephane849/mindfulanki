package com.mindfulanki.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD

/**
 * Single place that applies Mudita's Mindful Design theme. [ThemeMMD] supplies
 * the E Ink-optimised color scheme, typography and ripple/animation suppression.
 * Wrapping it here keeps screens free of framework specifics and gives us one
 * spot to adjust if MMD's entry point changes.
 */
@Composable
fun MindfulAnkiTheme(content: @Composable () -> Unit) {
    ThemeMMD {
        content()
    }
}
