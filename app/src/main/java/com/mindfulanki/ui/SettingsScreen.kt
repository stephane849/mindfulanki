package com.mindfulanki.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindfulanki.data.settings.StudyVariant
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD

/** Settings, mirroring the design: Display, Study, Study layout, Decks, About. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = settingsViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importApkg)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Settings", onBack = onBack)

            SectionHeader("DISPLAY")
            MindfulDivider()
            SettingsRow("Inverted colors", "Light text on black, for night reading") {
                SwitchMMD(checked = settings.invert, onCheckedChange = { viewModel.update(settings.copy(invert = it)) })
            }

            SectionHeader("STUDY")
            MindfulDivider()
            SettingsRow("New cards per day", null) {
                Stepper(settings.newPerDay, { viewModel.update(settings.copy(newPerDay = it)) }, min = 0, max = 40, step = 5)
            }
            SettingsRow("Daily goal", "A gentle target — never a streak") {
                Stepper(settings.dailyGoal, { viewModel.update(settings.copy(dailyGoal = it)) }, min = 5, max = 100, step = 5)
            }
            SettingsRow("Show next intervals", "Preview timing on the answer buttons") {
                SwitchMMD(checked = settings.showIntervals, onCheckedChange = { viewModel.update(settings.copy(showIntervals = it)) })
            }

            SectionHeader("STUDY LAYOUT")
            MindfulDivider()
            LayoutOption("Classic", "Centered card, big type", settings.variant == StudyVariant.CLASSIC) {
                viewModel.update(settings.copy(variant = StudyVariant.CLASSIC))
            }
            MindfulDivider()
            LayoutOption("Paper", "Document-style, tap to reveal", settings.variant == StudyVariant.PAPER) {
                viewModel.update(settings.copy(variant = StudyVariant.PAPER))
            }
            MindfulDivider()
            LayoutOption("Focus", "Chrome-free, full screen", settings.variant == StudyVariant.FOCUS) {
                viewModel.update(settings.copy(variant = StudyVariant.FOCUS))
            }

            SectionHeader("DECKS")
            MindfulDivider()
            SettingsRow("Import deck", "Add an Anki .apkg file") {
                LabeledButtonMMD(text = "Import", onClick = { picker.launch(arrayOf("*/*")) })
            }
            ImportNote(importState)

            SectionHeader("ABOUT")
            MindfulDivider()
            TextMMD(
                text = "Mudita Anki follows Mudita Mindful Design: a monochrome, E Ink-first interface — no ripple, no animation, dividers instead of shadows. Spaced repetition without pressure: no streaks, no badges.",
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 22.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    TextMMD(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 7.dp),
    )
}

@Composable
private fun SettingsRow(label: String, desc: String?, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            TextMMD(text = label, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold)
            if (desc != null) {
                TextMMD(text = desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        control()
    }
    MindfulDivider()
}

@Composable
private fun LayoutOption(name: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val markColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(if (selected) Modifier.background(MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                .border(1.5.dp, markColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = CheckIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            TextMMD(text = name, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
            TextMMD(text = desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImportNote(state: ImportState) {
    val text = when (state) {
        ImportState.Idle -> return
        ImportState.Importing -> "Importing…"
        is ImportState.Done -> "Imported ${state.cardCount} cards into ${state.deckCount} deck(s)."
        is ImportState.Failed -> "Import failed: ${state.message}"
    }
    TextMMD(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
    )
}
