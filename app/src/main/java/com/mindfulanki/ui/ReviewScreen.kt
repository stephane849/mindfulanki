package com.mindfulanki.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindfulanki.core.fsrs.Rating
import com.mindfulanki.data.db.CardEntity
import com.mudita.mmd.ButtonMMD
import com.mudita.mmd.TextMMD

/**
 * The study flow: show the front, reveal the answer, then grade with the four
 * FSRS ratings. Paged/static layout — one card at a time — which is kind to the
 * E Ink refresh cycle. Grade buttons are full-width for large, reliable taps.
 */
@Composable
fun ReviewScreen(
    deckId: Long,
    onDone: () -> Unit,
    viewModel: ReviewViewModel = reviewViewModel(deckId),
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
            when {
                state.loading -> TextMMD(text = "Loading…")
                state.finished -> SessionComplete(reviewed = state.reviewed, onDone = onDone)
                else -> state.card?.let { card ->
                    TextMMD(text = "${state.remaining} left · ${state.reviewed} done")
                    HorizontalDivider()
                    CardBody(
                        card = card,
                        answerShown = state.answerShown,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalDivider()
                    if (state.answerShown) {
                        GradeButtons(onGrade = viewModel::grade)
                    } else {
                        ButtonMMD(
                            text = "Show answer",
                            onClick = viewModel::showAnswer,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardBody(card: CardEntity, answerShown: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextMMD(text = card.front)
        if (answerShown) {
            HorizontalDivider()
            TextMMD(text = card.back)
        }
    }
}

@Composable
private fun GradeButtons(onGrade: (Rating) -> Unit) {
    // Order mirrors Anki: Again, Hard, Good, Easy.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ButtonMMD(text = "Again", onClick = { onGrade(Rating.AGAIN) }, modifier = Modifier.weight(1f))
        ButtonMMD(text = "Hard", onClick = { onGrade(Rating.HARD) }, modifier = Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ButtonMMD(text = "Good", onClick = { onGrade(Rating.GOOD) }, modifier = Modifier.weight(1f))
        ButtonMMD(text = "Easy", onClick = { onGrade(Rating.EASY) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SessionComplete(reviewed: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(text = "All done for now.")
        TextMMD(text = "Reviewed $reviewed card(s).")
        ButtonMMD(text = "Back to decks", onClick = onDone)
    }
}
