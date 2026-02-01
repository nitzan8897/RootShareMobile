package com.example.rootsharemobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rootsharemobile.ui.components.RootShareBottomNav

/**
 * Main navigation host for the app.
 * Handles navigation between all screens.
 */
@Composable
fun RootShareNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.Home.route
) {
    var selectedRoute by remember { mutableStateOf(startDestination) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            RootShareBottomNav(
                selectedRoute = selectedRoute,
                onItemSelected = { item ->
                    selectedRoute = item.route
                    navController.navigate(item.route) {
                        popUpTo(NavRoutes.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Login.route) {
                PlaceholderScreen(title = "Login", subtitle = "Login screen will be added here")
            }

            composable(NavRoutes.Register.route) {
                PlaceholderScreen(title = "Register", subtitle = "Register screen will be added here")
            }

            composable(NavRoutes.Home.route) {
                PlaceholderScreen(title = "Home", subtitle = "Home screen will be added here")
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
                PlaceholderScreen(title = "Profile", subtitle = "Coming soon...")
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
