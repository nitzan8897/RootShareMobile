package com.example.rootsharemobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rootsharemobile.ui.components.RootShareBottomNav
import com.example.rootsharemobile.ui.screens.auth.AuthViewModel
import com.example.rootsharemobile.ui.screens.auth.LoginScreen
import com.example.rootsharemobile.ui.screens.auth.RegisterScreen
import com.example.rootsharemobile.ui.screens.home.HomeScreen
import com.example.rootsharemobile.ui.screens.profile.ProfileScreen

/**
 * Main navigation host for the app.
 * Handles navigation between all screens with authentication flow.
 */
@Composable
fun RootShareNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Track initial auth check
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Wait for auth state to be determined
    LaunchedEffect(Unit) {
        // Give time for DataStore to load
        kotlinx.coroutines.delay(100)
        isCheckingAuth = false
    }

    // Show loading while checking auth state
    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Determine start destination based on auth state
    val startDestination = if (isLoggedIn) NavRoutes.Home.route else NavRoutes.Login.route

    // Get current route to determine if bottom nav should be shown
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Auth routes where bottom nav should be hidden
    val authRoutes = listOf(NavRoutes.Login.route, NavRoutes.Register.route)
    val showBottomNav = currentRoute !in authRoutes && isLoggedIn

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) {
                RootShareBottomNav(
                    selectedRoute = currentRoute ?: NavRoutes.Home.route,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo(NavRoutes.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth screens
            composable(NavRoutes.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = {
                        authViewModel.resetState()
                        navController.navigate(NavRoutes.Register.route)
                    },
                    onLoginSuccess = {
                        authViewModel.resetState()
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = {
                        authViewModel.resetState()
                        navController.popBackStack()
                    },
                    onRegisterSuccess = {
                        authViewModel.resetState()
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Main app screens
            composable(NavRoutes.Home.route) {
                HomeScreen(
                    getToken = { authViewModel.getAccessToken() }
                )
            }

            composable(NavRoutes.MyGarden.route) {
                PlaceholderScreen(title = "My Garden", subtitle = "Coming soon...")
            }

            composable(NavRoutes.Community.route) {
                PlaceholderScreen(title = "Community", subtitle = "Coming soon...")
            }

            composable(NavRoutes.Gallery.route) {
                PlaceholderScreen(title = "Gallery", subtitle = "Coming soon...")
            }

            composable(NavRoutes.Profile.route) {
                ProfileScreen(
                    user = currentUser,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    // Handle auth state changes - navigate to login if logged out
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentRoute !in authRoutes) {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String = "") {
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
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
