package com.mindfulanki.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulanki.AppContainer
import com.mindfulanki.MindfulAnkiApp

@Composable
private fun container(): AppContainer =
    (LocalContext.current.applicationContext as MindfulAnkiApp).container

@Composable
fun deckListViewModel(): DeckListViewModel {
    val c = container()
    return viewModel(factory = viewModelFactory {
        initializer { DeckListViewModel(c.reviewRepository, c.importService) }
    })
}

@Composable
fun reviewViewModel(deckId: Long): ReviewViewModel {
    val c = container()
    return viewModel(key = "review-$deckId", factory = viewModelFactory {
        initializer { ReviewViewModel(deckId, c.reviewRepository) }
    })
}

@Composable
fun statsViewModel(): StatsViewModel {
    val c = container()
    return viewModel(factory = viewModelFactory {
        initializer { StatsViewModel(c.reviewRepository) }
    })
}
