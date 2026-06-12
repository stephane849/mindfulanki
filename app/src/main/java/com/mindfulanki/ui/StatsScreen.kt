package com.mindfulanki.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.text.TextMMD

/** A light, glanceable summary — deliberately minimal for a mindful device. */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = statsViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextMMD(text = "Stats")
            HorizontalDivider()
            TextMMD(text = "Reviewed today: ${state.reviewedToday}")
            TextMMD(text = "Due now: ${state.dueToday}")
            TextMMD(text = "Decks: ${state.deckCount}")
            TextMMD(text = "Total cards: ${state.totalCards}")
            HorizontalDivider()
            LabeledButtonMMD(text = "Back", onClick = onBack)
        }
    }
}
