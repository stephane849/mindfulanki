package com.mindfulanki.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mudita.mmd.ThemeMMD

/**
 * Mudita Mindful Design palette, ported 1:1 from the design handoff's mmd.css
 * tokens. Monochrome E Ink: a single ink + paper plus a ramp of greys.
 */
object MindfulColors {
    val Ink = Color(0xFF0A0A0A)
    val Paper = Color(0xFFFFFFFF)
    val G1 = Color(0xFFF4F4F2)
    val G2 = Color(0xFFE2E2DF)
    val G3 = Color(0xFFBCBCB8)
    val G4 = Color(0xFF6F6F6C)
    val G5 = Color(0xFF3A3A38)
}

private val LightScheme: ColorScheme = lightColorScheme(
    primary = MindfulColors.Ink,
    onPrimary = MindfulColors.Paper,
    secondaryContainer = MindfulColors.G1,
    onSecondaryContainer = MindfulColors.Ink,
    background = MindfulColors.Paper,
    onBackground = MindfulColors.Ink,
    surface = MindfulColors.Paper,
    onSurface = MindfulColors.Ink,
    surfaceVariant = MindfulColors.G1,
    onSurfaceVariant = MindfulColors.G4,
    outline = MindfulColors.Ink,
    outlineVariant = MindfulColors.G2,
)

/** "Inverted colors" night mode from the design: light text on black. */
private val InvertedScheme: ColorScheme = darkColorScheme(
    primary = MindfulColors.Paper,
    onPrimary = MindfulColors.Ink,
    secondaryContainer = MindfulColors.G5,
    onSecondaryContainer = MindfulColors.Paper,
    background = MindfulColors.Ink,
    onBackground = MindfulColors.Paper,
    surface = MindfulColors.Ink,
    onSurface = MindfulColors.Paper,
    surfaceVariant = MindfulColors.G5,
    onSurfaceVariant = MindfulColors.G3,
    outline = MindfulColors.Paper,
    outlineVariant = MindfulColors.G5,
)

/**
 * Applies the MMD theme. [ThemeMMD] supplies E Ink typography and ripple/
 * animation suppression; we hand it our palette so "invert" night mode swaps
 * ink and paper across the whole tree.
 */
@Composable
fun MindfulAnkiTheme(inverted: Boolean = false, content: @Composable () -> Unit) {
    ThemeMMD(
        colorScheme = if (inverted) InvertedScheme else LightScheme,
        typography = AppTypography,
    ) {
        content()
    }
}
