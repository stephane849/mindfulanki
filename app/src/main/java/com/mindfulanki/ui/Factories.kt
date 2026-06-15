package com.mindfulanki.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulanki.AppContainer
import com.mindfulanki.MindfulAnkiApp
import com.mindfulanki.data.settings.AppSettings

@Composable
private fun container(): AppContainer =
    (LocalContext.current.applicationContext as MindfulAnkiApp).container

/** Current user settings as Compose state (drives the theme and study layout). */
@Composable
fun appSettings(): State<AppSettings> =
    container().settingsStore.settings.collectAsStateWithLifecycle()

@Composable
fun homeViewModel(): HomeViewModel {
    val c = container()
    return viewModel(factory = viewModelFactory {
        initializer { HomeViewModel(c.reviewRepository, c.settingsStore, c.importService) }
    })
}

@Composable
fun reviewViewModel(deckId: Long, newPerDay: Int): ReviewViewModel {
    val c = container()
    return viewModel(key = "study-$deckId", factory = viewModelFactory {
        initializer { ReviewViewModel(deckId, c.reviewRepository, newPerDay) }
    })
}

@Composable
fun settingsViewModel(): SettingsViewModel {
    val c = container()
    return viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(c.settingsStore, c.importService) }
    })
}

@Composable
fun addEditViewModel(cardId: Long?): AddEditViewModel {
    val c = container()
    return viewModel(key = "addedit-${cardId ?: "new"}", factory = viewModelFactory {
        initializer { AddEditViewModel(c.reviewRepository, cardId) }
    })
}
