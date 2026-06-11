package com.mindfulanki.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindfulanki.data.db.DeckSummary
import com.mudita.mmd.ButtonMMD
import com.mudita.mmd.TextMMD

/**
 * Decks the user has imported, with due/new counts, plus an "Import deck" action
 * that opens the system file picker for an `.apkg`. Static list layout suits the
 * E Ink display (no scroll animation needed for small libraries).
 */
@Composable
fun DeckListScreen(
    onOpenDeck: (Long) -> Unit,
    onOpenStats: () -> Unit,
    viewModel: DeckListViewModel = deckListViewModel(),
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importApkg) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextMMD(text = "MindfulAnki")

            ImportStatusLine(importState, viewModel::acknowledgeImport)

            ButtonMMD(
                text = "Import deck (.apkg)",
                onClick = { picker.launch(arrayOf("*/*")) },
            )
            ButtonMMD(text = "Stats", onClick = onOpenStats)

            HorizontalDivider()

            if (decks.isEmpty()) {
                TextMMD(text = "No decks yet. Import an .apkg file to begin.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(decks, key = { it.id }) { deck ->
                        DeckRow(deck = deck, onClick = { onOpenDeck(deck.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckRow(deck: DeckSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        TextMMD(text = deck.name)
        TextMMD(text = "${deck.dueCount} due · ${deck.newCount} new · ${deck.totalCount} total")
    }
}

@Composable
private fun ImportStatusLine(state: ImportState, onAck: () -> Unit) {
    when (state) {
        ImportState.Idle -> Unit
        ImportState.Importing -> TextMMD(text = "Importing…")
        is ImportState.Done -> {
            val kind = if (state.modern) "modern" else "legacy"
            TextMMD(text = "Imported ${state.cardCount} cards into ${state.deckCount} deck(s) ($kind).")
            ButtonMMD(text = "OK", onClick = onAck)
        }
        is ImportState.Failed -> {
            TextMMD(text = "Import failed: ${state.message}")
            ButtonMMD(text = "Dismiss", onClick = onAck)
        }
    }
}
