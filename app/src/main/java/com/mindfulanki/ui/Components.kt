package com.mindfulanki.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

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

/**
 * A ripple-free icon button. Uses real monochrome vector icons (not typographic
 * glyphs): Space Grotesk lacks symbols like the gear/check, so glyphs would fall
 * back inconsistently or render as tofu on minimal E Ink ROMs. 48dp tap target.
 */
@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** App bar built on MMD's [TopAppBarMMD] so it inherits the framework's E Ink styling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navigationIcon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBarMMD(
        title = { TextMMD(text = title, fontSize = 21.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    icon = navigationIcon ?: BackIcon,
                    contentDescription = "Back",
                    onClick = onBack,
                )
            }
        },
        actions = actions,
    )
}

/** Thin grey rule, or a strong ink rule, rendered via MMD's [HorizontalDividerMMD]. */
@Composable
fun MindfulDivider(strong: Boolean = false, modifier: Modifier = Modifier) {
    HorizontalDividerMMD(
        modifier = modifier,
        thickness = if (strong) 2.dp else 1.5.dp,
        color = if (strong) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
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
            .size(48.dp)
            .border(1.5.dp, color, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(text = glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
