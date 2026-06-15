package com.mindfulanki.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Monochrome vector icons used across the app. We use real icons rather than
 * typographic glyphs because the bundled Space Grotesk lacks symbols like the
 * gear and check, which would otherwise fall back inconsistently on E Ink.
 */
internal val BackIcon: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
internal val AddIcon: ImageVector get() = Icons.Filled.Add
internal val SettingsIcon: ImageVector get() = Icons.Filled.Settings
internal val CheckIcon: ImageVector get() = Icons.Filled.Check
internal val UndoIcon: ImageVector get() = Icons.AutoMirrored.Filled.Undo
