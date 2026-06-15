package com.mindfulanki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindfulanki.core.fsrs.Rating
import com.mindfulanki.data.settings.StudyVariant
import com.mindfulanki.ui.theme.scriptFamily
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.progress_indicator.LinearProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import kotlin.math.roundToInt

/**
 * The study flow, in whichever of the three design layouts the user has chosen:
 * Classic (centered, big), Paper (document-like, tap to reveal), or Focus
 * (chrome-free, full-bleed). All monochrome and animation-free for E Ink; the
 * answer occupies a reserved region so revealing it doesn't reflow the front.
 */
@Composable
fun StudyScreen(
    deckId: Long,
    onDone: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val settings by appSettings()
    val viewModel = reviewViewModel(deckId, settings.newPerDay)
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading -> Scaffold { p -> CenteredMessage("Loading…", Modifier.padding(p)) }
        state.finished -> SessionDone(reviewed = state.reviewed, onDone = onDone)
        else -> {
            val card = state.card!!
            when (settings.variant) {
                StudyVariant.FOCUS -> FocusLayout(state, settings.showIntervals, viewModel::showAnswer, viewModel::grade, viewModel::undo, onDone)
                StudyVariant.PAPER -> PaperLayout(state, settings.showIntervals, viewModel::showAnswer, viewModel::grade, viewModel::undo, onDone, { onEdit(card.id) })
                StudyVariant.CLASSIC -> ClassicLayout(state, settings.showIntervals, viewModel::showAnswer, viewModel::grade, viewModel::undo, onDone, { onEdit(card.id) })
            }
        }
    }
}

/* ---------- Variant A — Classic ---------- */
@Composable
private fun ClassicLayout(
    state: ReviewUiState,
    showIntervals: Boolean,
    onReveal: () -> Unit,
    onGrade: (Rating) -> Unit,
    onUndo: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val card = state.card!!
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBar(title = state.deckName, onBack = onBack, actions = { UndoButton(state.canUndo, onUndo); EditButton(onEdit) })
            StudyCounts(state)
            // Front sits in the top half; the answer in a reserved lower half, so
            // the question never moves when the answer appears.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextMMD(text = card.front, fontSize = 44.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, fontFamily = scriptFamily(card.front))
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (state.answerShown) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MindfulDivider(strong = true, modifier = Modifier.width(120.dp))
                        TextMMD(text = card.back, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = scriptFamily(card.back))
                    }
                }
            }
            StudyBottom {
                if (state.answerShown) {
                    GradeRow(state.previews, showIntervals, onGrade)
                } else {
                    ButtonMMD(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                        TextMMD(text = "Show answer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/* ---------- Variant B — Paper ---------- */
@Composable
private fun PaperLayout(
    state: ReviewUiState,
    showIntervals: Boolean,
    onReveal: () -> Unit,
    onGrade: (Rating) -> Unit,
    onUndo: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val card = state.card!!
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBar(title = state.deckName, onBack = onBack, actions = { UndoButton(state.canUndo, onUndo); EditButton(onEdit) })
            StudyCounts(state)
            // Document flow: front stays anchored at the top; the answer reveals
            // beneath it without moving the question.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .clickable(enabled = !state.answerShown, onClick = onReveal)
                    .padding(horizontal = 26.dp, vertical = 16.dp),
            ) {
                TextMMD(text = card.front, fontSize = 30.sp, fontWeight = FontWeight.Medium, fontFamily = scriptFamily(card.front))
                if (state.answerShown) {
                    MindfulDivider(strong = true, modifier = Modifier.padding(vertical = 20.dp))
                    TextMMD(text = card.back, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = scriptFamily(card.back))
                } else {
                    TextMMD(
                        text = "Tap the card to show the answer",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                }
            }
            StudyBottom {
                if (state.answerShown) {
                    GradeGrid(state.previews, showIntervals, onGrade)
                } else {
                    ButtonMMD(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                        TextMMD(text = "Show answer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/* ---------- Variant C — Focus ---------- */
@Composable
private fun FocusLayout(
    state: ReviewUiState,
    showIntervals: Boolean,
    onReveal: () -> Unit,
    onGrade: (Rating) -> Unit,
    onUndo: () -> Unit,
    onBack: () -> Unit,
) {
    val card = state.card!!
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Progress hairline (MMD), plus back + undo affordances.
            LinearProgressIndicatorMMD(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(icon = BackIcon, contentDescription = "Back", onClick = onBack)
                Box(Modifier.weight(1f))
                if (state.canUndo) IconButton(icon = UndoIcon, contentDescription = "Undo last grade", onClick = onUndo)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(enabled = !state.answerShown, onClick = onReveal)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextMMD(text = card.front, fontSize = 48.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, fontFamily = scriptFamily(card.front))
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (state.answerShown) {
                    TextMMD(text = card.back, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = scriptFamily(card.back))
                } else {
                    TextMMD(
                        text = "TAP TO REVEAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.answerShown) GradeStrip(state.previews, showIntervals, onGrade)
        }
    }
}

/* ---------- shared study pieces ---------- */

@Composable
private fun StudyCounts(state: ReviewUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountItem("${state.newRemaining} new", active = state.currentIsNew)
        CountItem("${state.dueRemaining} due", active = !state.currentIsNew)
    }
}

@Composable
private fun CountItem(text: String, active: Boolean) {
    TextMMD(
        text = text,
        fontSize = 13.5.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        textDecoration = if (active) TextDecoration.Underline else null,
        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StudyBottom(content: @Composable () -> Unit) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) { content() }
}

@Composable
private fun EditButton(onEdit: () -> Unit) {
    TextMMD(
        text = "Edit",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(onClick = onEdit).padding(10.dp),
    )
}

/** Undo affordance shown in the study chrome once a grade has been applied. */
@Composable
private fun UndoButton(canUndo: Boolean, onUndo: () -> Unit) {
    if (canUndo) IconButton(icon = UndoIcon, contentDescription = "Undo last grade", onClick = onUndo)
}

private val GRADES = listOf(
    Rating.AGAIN to "Again",
    Rating.HARD to "Hard",
    Rating.GOOD to "Good",
    Rating.EASY to "Easy",
)

@Composable
private fun GradeRow(previews: Map<Rating, Long>, showIntervals: Boolean, onGrade: (Rating) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GRADES.forEach { (rating, label) ->
            GradeButton(label, previews[rating], showIntervals, rating == Rating.GOOD, stacked = true, Modifier.weight(1f).height(58.dp)) { onGrade(rating) }
        }
    }
}

@Composable
private fun GradeGrid(previews: Map<Rating, Long>, showIntervals: Boolean, onGrade: (Rating) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(GRADES.subList(0, 2), GRADES.subList(2, 4)).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (rating, label) ->
                    GradeButton(label, previews[rating], showIntervals, rating == Rating.GOOD, stacked = false, Modifier.weight(1f).height(58.dp)) { onGrade(rating) }
                }
            }
        }
    }
}

@Composable
private fun GradeStrip(previews: Map<Rating, Long>, showIntervals: Boolean, onGrade: (Rating) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .background(MaterialTheme.colorScheme.outline),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
        GRADES.forEach { (rating, label) ->
            GradeButton(label, previews[rating], showIntervals, rating == Rating.GOOD, stacked = true, Modifier.weight(1f).fillMaxHeight(), bordered = false, shape = null) { onGrade(rating) }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    intervalDays: Long?,
    showIntervals: Boolean,
    primary: Boolean,
    stacked: Boolean,
    modifier: Modifier,
    bordered: Boolean = true,
    shape: Shape? = RoundedCornerShape(14.dp),
    onClick: () -> Unit,
) {
    val bg = if (primary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val fg = if (primary) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    var box = if (shape != null) modifier.background(bg, shape) else modifier.background(bg)
    if (bordered && shape != null) box = box.border(1.5.dp, MaterialTheme.colorScheme.onSurface, shape)
    box = box.clickable(onClick = onClick)

    Box(box, contentAlignment = Alignment.Center) {
        // Solid ink/paper for the interval rather than alpha-muted grey, which
        // dithers poorly on E Ink.
        val interval = if (showIntervals && intervalDays != null) formatInterval(intervalDays) else null
        if (stacked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (interval != null) TextMMD(text = interval, fontSize = 13.sp, color = fg)
                TextMMD(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextMMD(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg)
                if (interval != null) TextMMD(text = interval, fontSize = 13.sp, color = fg)
            }
        }
    }
}

@Composable
private fun SessionDone(reviewed: Int, onDone: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .height(84.dp)
                    .width(84.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(42.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CheckIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(40.dp).height(40.dp),
                )
            }
            TextMMD(text = "Session complete", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextMMD(
                text = "$reviewed ${if (reviewed == 1) "card" else "cards"} reviewed.\nThat is enough for now.",
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ButtonMMD(onClick = onDone) { TextMMD(text = "Back to decks", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
