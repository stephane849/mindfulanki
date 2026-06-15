package com.mindfulanki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * Add or edit a card (front / back). Deck chips appear only in Add mode, per
 * the design. Saving in Add mode keeps the form open with a confirmation so
 * several cards can be added in a row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    cardId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditViewModel = addEditViewModel(cardId),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBar(title = if (state.isEdit) "Edit card" else "Add card", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (!state.isEdit && state.decks.isNotEmpty()) {
                    FieldLabel("DECK")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.decks.forEach { deck ->
                            DeckChip(name = deck.name, selected = deck.id == state.deckId, onClick = { viewModel.setDeck(deck.id) })
                        }
                    }
                }
                LabeledField("FRONT", state.front, viewModel::setFront)
                LabeledField("BACK", state.back, viewModel::setBack)
                state.confirm?.let { TextMMD(text = "✓  $it", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) {
                ButtonMMD(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                    TextMMD(text = if (state.isEdit) "Save changes" else "Save card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    TextMMD(text = text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DeckChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        TextMMD(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
