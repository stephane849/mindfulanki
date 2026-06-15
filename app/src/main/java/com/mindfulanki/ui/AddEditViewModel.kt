package com.mindfulanki.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulanki.data.ReviewRepository
import com.mindfulanki.data.db.DeckEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val decks: List<DeckEntity> = emptyList(),
    val front: String = "",
    val back: String = "",
    val deckId: Long? = null,
    val confirm: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = front.isNotBlank() && back.isNotBlank() && deckId != null
}

/**
 * Backs both Add (cardId == null) and Edit. Generic front/back only — the
 * design's Arabic/transliteration split is collapsed to the two fields our
 * imported cards actually carry.
 */
class AddEditViewModel(
    private val repository: ReviewRepository,
    private val cardId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditUiState(isEdit = cardId != null))
    val state: StateFlow<AddEditUiState> = _state

    init {
        viewModelScope.launch {
            val decks = repository.decks()
            val editing = cardId?.let { repository.card(it) }
            _state.update {
                it.copy(
                    loading = false,
                    decks = decks,
                    front = editing?.front ?: "",
                    back = editing?.back ?: "",
                    deckId = editing?.deckId ?: decks.firstOrNull()?.id,
                )
            }
        }
    }

    fun setFront(v: String) = _state.update { it.copy(front = v, confirm = null) }
    fun setBack(v: String) = _state.update { it.copy(back = v, confirm = null) }
    fun setDeck(id: Long) = _state.update { it.copy(deckId = id, confirm = null) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            if (s.isEdit && cardId != null) {
                repository.updateContent(cardId, s.front.trim(), s.back.trim())
                _state.update { it.copy(saved = true) }
            } else {
                repository.addCard(s.deckId!!, s.front.trim(), s.back.trim())
                val deckName = s.decks.firstOrNull { it.id == s.deckId }?.name.orEmpty()
                _state.update { it.copy(front = "", back = "", confirm = "Card added to $deckName") }
            }
        }
    }
}
