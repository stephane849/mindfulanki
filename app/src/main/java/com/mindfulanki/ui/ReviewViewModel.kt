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
    val reviewed: Int = 0,
    val sessionTotal: Int = 0,
    val newRemaining: Int = 0,
    val dueRemaining: Int = 0,
    val previews: Map<Rating, Long> = emptyMap(),
) {
    val finished: Boolean get() = !loading && card == null

    /** Whether the card on screen has never been reviewed (for the counts highlight). */
    val currentIsNew: Boolean get() = card?.lastReviewEpochMillis == null

    val progress: Float
        get() = if (sessionTotal == 0) 0f else (reviewed.toFloat() / sessionTotal).coerceIn(0f, 1f)

    val position: Int get() = (reviewed + if (card != null) 1 else 0).coerceAtMost(sessionTotal)
}

class ReviewViewModel(
    private val deckId: Long,
    private val repository: ReviewRepository,
    private val newPerDay: Int,
) : ViewModel() {

    private val queue = ArrayDeque<CardEntity>()
    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state

    init {
        loadQueue()
    }

    private fun loadQueue() {
        viewModelScope.launch {
            val cards = repository.studyQueue(deckId, newPerDay)
            queue.clear()
            queue.addAll(cards)
            _state.value = ReviewUiState(
                loading = false,
                deckName = repository.deckName(deckId) ?: "Deck",
                card = queue.firstOrNull(),
                sessionTotal = cards.size,
                previews = queue.firstOrNull()?.let(repository::previewIntervals).orEmpty(),
            ).withCounts()
        }
    }

    fun showAnswer() {
        if (_state.value.card != null) _state.value = _state.value.copy(answerShown = true)
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
                reviewed = _state.value.reviewed + 1,
                previews = next?.let(repository::previewIntervals).orEmpty(),
            ).withCounts()
        }
    }

    /** Recompute remaining new/due tallies from the live queue. */
    private fun ReviewUiState.withCounts(): ReviewUiState {
        val new = queue.count { it.lastReviewEpochMillis == null }
        return copy(newRemaining = new, dueRemaining = queue.size - new)
    }
}
