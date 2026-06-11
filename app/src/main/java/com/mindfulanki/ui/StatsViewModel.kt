package com.mindfulanki.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulanki.data.ReviewRepository
import com.mindfulanki.data.db.DeckSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class StatsUiState(
    val reviewedToday: Int = 0,
    val dueToday: Int = 0,
    val totalCards: Int = 0,
    val deckCount: Int = 0,
)

class StatsViewModel(private val repository: ReviewRepository) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state

    init {
        viewModelScope.launch {
            val decks: List<DeckSummary> = repository.observeDecks().first()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            _state.value = StatsUiState(
                reviewedToday = repository.reviewedToday(startOfDay),
                dueToday = decks.sumOf { it.dueCount },
                totalCards = decks.sumOf { it.totalCount },
                deckCount = decks.size,
            )
        }
    }
}
