package com.example.rootsharemobile.ui.navigation

/**
 * Navigation routes for the app.
 * Chat navigation (ChatList → ChatRoom) is handled by chat_nav_graph.xml with SafeArgs,
 * not by the Compose NavHost.
 */
sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Register : NavRoutes("register")
    data object Home : NavRoutes("home")
    data object MyGarden : NavRoutes("my_garden")
    data object Community : NavRoutes("community")
    data object Gallery : NavRoutes("gallery")
    data object Profile : NavRoutes("profile")
}
