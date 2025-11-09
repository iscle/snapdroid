package de.badaix.snapcast.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app
 * Using typed routes for type-safe navigation
 */
sealed class SnapcastDestination(val route: String) {
    data object Home : SnapcastDestination("home")
    data object PlayerLogs : SnapcastDestination("player_logs")
}

