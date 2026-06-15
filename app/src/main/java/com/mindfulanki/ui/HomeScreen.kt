package com.mindfulanki.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.text.TextMMD
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Home / deck list, per the Mudita Anki design: a gentle "Today" goal, the deck
 * list with new/due counts, and a calm footer. The "+" adds a card; the tune
 * icon opens Settings (where decks are imported). Import is also surfaced here
 * when the library is empty.
 */
@Composable
fun HomeScreen(
    onStudy: (Long) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = homeViewModel(),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importApkg)
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopBar(
                title = "Anki",
                actions = {
                    GlyphIconButton(glyph = "+", onClick = onAdd)
                    GlyphIconButton(glyph = "⚙", onClick = onSettings)
                },
            )

            DailyGoal(todayCount = state.todayCount, goal = state.goal)
            MindfulDivider()

            ImportStatusLine(importState, viewModel::acknowledgeImport)

            if (state.decks.isEmpty() && importState !is ImportState.Importing) {
                EmptyLibrary(onImport = { picker.launch(arrayOf("*/*")) })
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.decks, key = { it.id }) { deck ->
                        DeckRowItem(deck = deck, onClick = { if (!deck.done) onStudy(deck.id) })
                        MindfulDivider()
                    }
                    item {
                        MutedText(
                            "Cards return just before you forget them. Study a little, then put the phone down.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGoal(todayCount: Int, goal: Int) {
    val pct = if (goal <= 0) 0 else min(100, ((todayCount.toFloat() / goal) * 100).roundToInt())
    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            TextMMD(
                text = "TODAY",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextMMD(text = "$todayCount of $goal cards", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct / 100f)
                    .background(MaterialTheme.colorScheme.onSurface),
            )
        }
        if (todayCount >= goal && goal > 0) {
            TextMMD(
                text = "Daily goal reached — nothing more is asked of you.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun DeckRowItem(deck: DeckRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(text = deck.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextMMD(text = deck.subtitle, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (deck.done) {
            TextMMD(text = "✓", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            TextMMD(text = deck.badge.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(text = "No decks yet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        MutedText("Import an Anki .apkg deck to begin.")
        LabeledButtonMMD(text = "Import deck (.apkg)", onClick = onImport)
    }
}

@Composable
private fun ImportStatusLine(state: ImportState, onAck: () -> Unit) {
    when (state) {
        ImportState.Idle -> Unit
        ImportState.Importing -> Box(Modifier.padding(18.dp)) { MutedText("Importing…") }
        is ImportState.Done -> Column(Modifier.padding(18.dp)) {
            val kind = if (state.modern) "modern" else "legacy"
            MutedText("Imported ${state.cardCount} cards into ${state.deckCount} deck(s) ($kind).")
            LabeledButtonMMD(text = "OK", onClick = onAck)
        }
        is ImportState.Failed -> Column(Modifier.padding(18.dp)) {
            MutedText("Import failed: ${state.message}")
            LabeledButtonMMD(text = "Dismiss", onClick = onAck)
        }
    }
}

/** Small muted body text in the footer/empty/status spots. */
@Composable
private fun MutedText(text: String, modifier: Modifier = Modifier) {
    TextMMD(
        text = text,
        fontSize = 13.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
