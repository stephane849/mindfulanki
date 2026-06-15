package com.mindfulanki.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulanki.data.apkg.ApkgImportService
import com.mindfulanki.data.settings.AppSettings
import com.mindfulanki.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val importService: ApkgImportService,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings

    fun update(settings: AppSettings) = settingsStore.update(settings)

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
