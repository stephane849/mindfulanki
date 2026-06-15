package com.mindfulanki.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulanki.data.ReviewRepository
import com.mindfulanki.data.apkg.ApkgImportService
import com.mindfulanki.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Done(val deckCount: Int, val cardCount: Int, val modern: Boolean) : ImportState
    data class Failed(val message: String) : ImportState
}

/** A deck row on Home: name, counts subtitle, badge number, and whether it's cleared for today. */
data class DeckRow(
    val id: Long,
    val name: String,
    val subtitle: String,
    val badge: Int,
    val done: Boolean,
)

data class HomeUiState(
    val loading: Boolean = true,
    val decks: List<DeckRow> = emptyList(),
    val todayCount: Int = 0,
    val goal: Int = 20,
)

class HomeViewModel(
    private val repository: ReviewRepository,
    private val settingsStore: SettingsStore,
    private val importService: ApkgImportService,
) : ViewModel() {

    val ui: StateFlow<HomeUiState> =
        combine(repository.observeDecks(), settingsStore.settings) { summaries, settings ->
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val rows = summaries.map { s ->
                val shownNew = min(s.newCount, settings.newPerDay)
                val total = shownNew + s.dueCount
                DeckRow(
                    id = s.id,
                    name = s.name,
                    subtitle = if (total > 0) "$shownNew new · ${s.dueCount} due" else "Done for today",
                    badge = total,
                    done = total == 0,
                )
            }
            HomeUiState(loading = false, decks = rows, todayCount = repository.reviewedToday(startOfDay), goal = settings.dailyGoal)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    fun importApkg(uri: Uri) {
        _importState.value = ImportState.Importing
        viewModelScope.launch {
            _importState.value = try {
                val summary = importService.import(uri)
                ImportState.Done(summary.deckCount, summary.cardCount, summary.modern)
            } catch (t: Throwable) {
                ImportState.Failed(t.message ?: "Import failed")
            }
        }
    }

    fun acknowledgeImport() {
        _importState.value = ImportState.Idle
    }
}
