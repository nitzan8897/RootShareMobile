package com.example.rootsharemobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rootsharemobile.ui.screens.auth.AuthViewModel
import com.example.rootsharemobile.ui.screens.auth.LoginScreen
import com.example.rootsharemobile.ui.screens.auth.RegisterScreen
import com.example.rootsharemobile.ui.screens.home.HomeScreen
import com.example.rootsharemobile.ui.theme.Gray500

@Composable
fun RootShareNavHost(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // Determine start destination based on auth state
    val startDestination = if (isLoggedIn) NavRoutes.Home.route else NavRoutes.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login screen
        composable(NavRoutes.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.resetState()
                    navController.navigate(NavRoutes.Register.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register screen
        composable(NavRoutes.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.resetState()
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Home screen
        composable(NavRoutes.Home.route) {
            HomeScreen(
                getToken = { authViewModel.getAccessToken() },
                onNavigate = { navItem ->
                    when (navItem.route) {
                        "home" -> { /* Already on home */ }
                        "my_garden" -> navController.navigate(NavRoutes.MyGarden.route)
                        "community" -> navController.navigate(NavRoutes.Community.route)
                        "gallery" -> navController.navigate(NavRoutes.Gallery.route)
                    }
                }
            )
        }

        // Placeholder screens for other tabs (to be implemented)
        composable(NavRoutes.MyGarden.route) {
            PlaceholderScreen(title = "My Garden")
        }

        composable(NavRoutes.Community.route) {
            PlaceholderScreen(title = "Community")
        }

        composable(NavRoutes.Gallery.route) {
            PlaceholderScreen(title = "Gallery")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Coming soon...",
                color = Gray500
            )
        }
    }
}
