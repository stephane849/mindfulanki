package com.mindfulanki.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulanki.data.ReviewRepository
import com.mindfulanki.data.apkg.ApkgImportService
import com.mindfulanki.data.db.DeckSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Done(val deckCount: Int, val cardCount: Int, val modern: Boolean) : ImportState
    data class Failed(val message: String) : ImportState
}

class DeckListViewModel(
    private val repository: ReviewRepository,
    private val importService: ApkgImportService,
) : ViewModel() {

    val decks: StateFlow<List<DeckSummary>> = repository.observeDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
