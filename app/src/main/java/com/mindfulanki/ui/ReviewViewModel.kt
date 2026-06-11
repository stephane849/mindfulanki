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
    val card: CardEntity? = null,
    val answerShown: Boolean = false,
    val remaining: Int = 0,
    val reviewed: Int = 0,
) {
    val finished: Boolean get() = !loading && card == null
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
            queue.clear()
            queue.addAll(repository.dueQueue(deckId))
            _state.value = ReviewUiState(
                loading = false,
                card = queue.firstOrNull(),
                remaining = queue.size,
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
            )
        }
    }
}
