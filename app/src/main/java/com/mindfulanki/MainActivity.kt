package com.mindfulanki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindfulanki.ui.AddEditScreen
import com.mindfulanki.ui.HomeScreen
import com.mindfulanki.ui.SettingsScreen
import com.mindfulanki.ui.StudyScreen
import com.mindfulanki.ui.appSettings
import com.mindfulanki.ui.theme.MindfulAnkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Invert mode flips the whole palette, so read it at the theme root.
            val settings by appSettings()
            MindfulAnkiTheme(inverted = settings.invert) {
                MindfulAnkiNavHost()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ADD = "add"
    const val STUDY = "study/{deckId}"
    const val EDIT = "edit/{cardId}"
    fun study(deckId: Long) = "study/$deckId"
    fun edit(cardId: Long) = "edit/$cardId"
}

@Composable
private fun MindfulAnkiNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStudy = { deckId -> nav.navigate(Routes.study(deckId)) },
                onAdd = { nav.navigate(Routes.ADD) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.STUDY,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
            StudyScreen(
                deckId = deckId,
                onDone = { nav.popBackStack() },
                onEdit = { cardId -> nav.navigate(Routes.edit(cardId)) },
            )
        }
        composable(Routes.ADD) {
            AddEditScreen(cardId = null, onBack = { nav.popBackStack() })
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            AddEditScreen(cardId = cardId, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
