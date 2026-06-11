package com.mindfulanki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindfulanki.ui.DeckListScreen
import com.mindfulanki.ui.ReviewScreen
import com.mindfulanki.ui.StatsScreen
import com.mindfulanki.ui.theme.MindfulAnkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MindfulAnkiTheme {
                MindfulAnkiNavHost()
            }
        }
    }
}

private object Routes {
    const val DECKS = "decks"
    const val STATS = "stats"
    const val REVIEW = "review/{deckId}"
    fun review(deckId: Long) = "review/$deckId"
}

@Composable
private fun MindfulAnkiNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DECKS) {
        composable(Routes.DECKS) {
            DeckListScreen(
                onOpenDeck = { deckId -> nav.navigate(Routes.review(deckId)) },
                onOpenStats = { nav.navigate(Routes.STATS) },
            )
        }
        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            ReviewScreen(deckId = deckId, onDone = { nav.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { nav.popBackStack() })
        }
    }
}
