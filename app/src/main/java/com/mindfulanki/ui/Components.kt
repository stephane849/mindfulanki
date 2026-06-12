package com.mindfulanki.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * Convenience wrapper around MMD's [ButtonMMD], which (like Material's Button)
 * takes a `content` slot rather than a text label. Our screens only ever show a
 * text label, so this keeps the call sites concise.
 */
@Composable
fun LabeledButtonMMD(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonMMD(onClick = onClick, modifier = modifier) {
        TextMMD(text = text)
    }
}
