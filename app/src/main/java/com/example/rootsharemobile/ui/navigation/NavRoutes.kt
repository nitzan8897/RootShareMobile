package com.example.rootsharemobile.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Register : NavRoutes("register")
    data object Home : NavRoutes("home")
    data object MyGarden : NavRoutes("my_garden")
    data object Community : NavRoutes("community")
    data object Gallery : NavRoutes("gallery")
    data object Profile : NavRoutes("profile")
    data object ChatRoom : NavRoutes("chat_room/{chatId}") {
        fun createRoute(chatId: String) = "chat_room/$chatId"
    }
}
