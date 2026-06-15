package com.mindfulanki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * Convenience wrapper around MMD's [ButtonMMD], which (like Material's Button)
 * takes a `content` slot rather than a text label.
 */
@Composable
fun LabeledButtonMMD(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ButtonMMD(onClick = onClick, modifier = modifier) {
        TextMMD(text = text, fontWeight = FontWeight.Bold)
    }
}

/** A round, ripple-free icon button rendered from a glyph (E Ink friendly, no icon deps). */
@Composable
fun GlyphIconButton(glyph: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(text = glyph, fontSize = 22.sp)
    }
}

/** App bar: 56dp tall, optional back glyph, bold title, trailing slot. */
@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) GlyphIconButton(glyph = "←", onClick = onBack)
        TextMMD(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        actions()
    }
}

/** Thin divider (1.5dp grey) or a strong 2dp ink rule, per the design tokens. */
@Composable
fun MindfulDivider(strong: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (strong) 2.dp else 1.5.dp)
            .background(if (strong) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
    )
}

/** Circular −/value/+ stepper from the design's Settings screen. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int, max: Int, step: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepperButton(glyph = "−", enabled = value > min) { onChange((value - step).coerceAtLeast(min)) }
        TextMMD(text = value.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        StepperButton(glyph = "+", enabled = value < max) { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepperButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(38.dp)
            .border(1.5.dp, color, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(text = glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
