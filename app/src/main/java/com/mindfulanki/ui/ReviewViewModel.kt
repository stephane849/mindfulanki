package com.mindfulanki.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulanki.core.fsrs.Rating
import com.mindfulanki.data.ReviewRepository
import com.mindfulanki.data.db.CardEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val deckName: String = "",
    val card: CardEntity? = null,
    val answerShown: Boolean = false,
    val remaining: Int = 0,
    val reviewed: Int = 0,
    val sessionTotal: Int = 0,
    val previews: Map<Rating, Long> = emptyMap(),
) {
    val finished: Boolean get() = !loading && card == null

    /** 1-based index of the card currently on screen, capped at the session size. */
    val position: Int get() = (reviewed + if (card != null) 1 else 0).coerceAtMost(sessionTotal)

    /** Fraction of the session completed, in [0, 1]. */
    val progress: Float
        get() = if (sessionTotal == 0) 0f else (reviewed.toFloat() / sessionTotal).coerceIn(0f, 1f)
}

class ReviewViewModel(
    private val deckId: Long,
    private val repository: ReviewRepository,
) : ViewModel() {

    private val queue = ArrayDeque<CardEntity>()
    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state

    init {
        loadQueue()
    }

    private fun loadQueue() {
        viewModelScope.launch {
            val cards = repository.dueQueue(deckId)
            queue.clear()
            queue.addAll(cards)
            val first = queue.firstOrNull()
            _state.value = ReviewUiState(
                loading = false,
                deckName = repository.deckName(deckId) ?: "Deck",
                card = first,
                remaining = queue.size,
                sessionTotal = cards.size,
                previews = first?.let(repository::previewIntervals).orEmpty(),
            )
        }
    }

    fun showAnswer() {
        if (_state.value.card != null) {
            _state.value = _state.value.copy(answerShown = true)
        }
    }

    fun grade(rating: Rating) {
        val current = _state.value.card ?: return
        viewModelScope.launch {
            repository.grade(current, rating)
            queue.removeFirstOrNull()
            // A lapsed card is re-queued for another pass this session.
            if (rating == Rating.AGAIN) queue.addLast(current)

            val next = queue.firstOrNull()
            _state.value = _state.value.copy(
                card = next,
                answerShown = false,
                remaining = queue.size,
                reviewed = _state.value.reviewed + 1,
                previews = next?.let(repository::previewIntervals).orEmpty(),
            )
        }
    }
}
