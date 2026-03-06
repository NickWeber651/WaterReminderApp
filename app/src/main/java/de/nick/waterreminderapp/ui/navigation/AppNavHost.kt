package de.nick.waterreminderapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.nick.waterreminderapp.ui.history.HistoryScreen
import de.nick.waterreminderapp.ui.home.HomeScreen
import de.nick.waterreminderapp.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(AppRoutes.SETTINGS) { launchSingleTop = true }
                },
                onNavigateToHistory = {
                    navController.navigate(AppRoutes.HISTORY) { launchSingleTop = true }
                }
            )
        }
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(AppRoutes.HISTORY) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
