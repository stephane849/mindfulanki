package com.mindfulanki.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindfulanki.core.fsrs.Rating
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.progress_indicator.LinearProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import kotlin.math.roundToInt

/**
 * Card-centric study flow inspired by the Mudita Anki design: a prominent
 * bordered card holds one prompt at a time, a progress bar tracks the session,
 * and a single action ("Flip card") reveals the answer before grading.
 *
 * Adapted for the Kompakt's E Ink display: grayscale with a bordered (not
 * shadowed) card, strong type hierarchy instead of color, and no flip animation
 * (kind to the slow refresh). Grade buttons show the next interval, like Anki.
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.loading -> CenteredMessage("Loading…")
                state.finished -> SessionComplete(reviewed = state.reviewed, onDone = onDone)
                else -> state.card?.let { card ->
                    Header(deckName = state.deckName, total = state.sessionTotal, onBack = onDone)

                    CardSurface(
                        front = card.front,
                        back = card.back,
                        answerShown = state.answerShown,
                        modifier = Modifier.weight(1f),
                    )

                    SessionProgress(position = state.position, total = state.sessionTotal, fraction = state.progress)

                    if (state.answerShown) {
                        GradeButtons(previews = state.previews, onGrade = viewModel::grade)
                    } else {
                        ButtonMMD(
                            onClick = viewModel::showAnswer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextMMD(text = "Flip card", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(deckName: String, total: Int, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TextMMD(
            text = "←",
            fontSize = 24.sp,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 4.dp),
        )
        TextMMD(text = deckName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        TextMMD(
            text = "$total cards",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardSurface(
    front: String,
    back: String,
    answerShown: Boolean,
    modifier: Modifier = Modifier,
) {
    CardMMD(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (answerShown) {
                    // Keep the prompt visible (muted) above the revealed answer.
                    TextMMD(
                        text = front.ifBlank { "—" },
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDividerMMD(modifier = Modifier.fillMaxWidth())
                    HeroBlock(text = back)
                } else {
                    HeroBlock(text = front)
                }
            }
        }
    }
}

/**
 * Renders text with a tiered hierarchy like the design's kanji/reading/romaji:
 * the first line is the hero (size scales down as it gets longer), and any
 * remaining lines are shown smaller and muted beneath it.
 */
@Composable
private fun HeroBlock(text: String) {
    val trimmed = text.trim().ifBlank { "(blank)" }
    val newline = trimmed.indexOf('\n')
    val head = if (newline >= 0) trimmed.substring(0, newline).trim() else trimmed
    val tail = if (newline >= 0) trimmed.substring(newline + 1).trim() else ""

    val heroSize = when {
        head.length <= 4 -> 56.sp
        head.length <= 14 -> 38.sp
        else -> 28.sp
    }

    TextMMD(
        text = head,
        fontSize = heroSize,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    if (tail.isNotEmpty()) {
        TextMMD(
            text = tail,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionProgress(position: Int, total: Int, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicatorMMD(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
        TextMMD(
            text = "$position / $total",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GradeButtons(previews: Map<Rating, Long>, onGrade: (Rating) -> Unit) {
    // Order mirrors Anki: Again, Hard, Good, Easy.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GradeButton("Again", previews[Rating.AGAIN]) { onGrade(Rating.AGAIN) }
        GradeButton("Hard", previews[Rating.HARD]) { onGrade(Rating.HARD) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GradeButton("Good", previews[Rating.GOOD]) { onGrade(Rating.GOOD) }
        GradeButton("Easy", previews[Rating.EASY]) { onGrade(Rating.EASY) }
    }
}

@Composable
private fun RowScope.GradeButton(
    label: String,
    intervalDays: Long?,
    onClick: () -> Unit,
) {
    ButtonMMD(onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextMMD(text = label, fontWeight = FontWeight.Bold)
            if (intervalDays != null) {
                TextMMD(text = formatInterval(intervalDays), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SessionComplete(reviewed: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(text = "✓", fontSize = 56.sp, fontWeight = FontWeight.Bold)
        TextMMD(text = "Session complete", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        TextMMD(
            text = "Reviewed $reviewed card(s).",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ButtonMMD(onClick = onDone) {
            TextMMD(text = "Back to decks", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TextMMD(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Compact human-readable interval: "3d", "5mo", "2y". */
private fun formatInterval(days: Long): String = when {
    days <= 0 -> "<1d"
    days < 30 -> "${days}d"
    days < 365 -> "${(days / 30.0).roundToInt()}mo"
    else -> "${(days / 365.0).roundToInt()}y"
}
