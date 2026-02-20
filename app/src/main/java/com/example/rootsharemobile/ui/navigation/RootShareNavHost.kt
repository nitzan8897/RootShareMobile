package com.example.rootsharemobile.ui.navigation

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.auth.GoogleAuthHelper
import com.example.rootsharemobile.ui.components.RootShareBottomNav
import com.example.rootsharemobile.ui.screens.auth.AuthViewModel
import com.example.rootsharemobile.ui.screens.auth.LoginScreen
import com.example.rootsharemobile.ui.screens.auth.RegisterScreen
import com.example.rootsharemobile.ui.screens.chat.ChatViewModel
import com.example.rootsharemobile.ui.screens.home.HomeScreen
import com.example.rootsharemobile.ui.screens.profile.ProfileScreen

/**
 * Main navigation host for the app.
 * Chat tab uses a Fragment NavHostFragment backed by chat_nav_graph.xml + SafeArgs.
 */
@Composable
fun RootShareNavHost(
    googleAuthHelper: GoogleAuthHelper,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val chatViewModel: ChatViewModel = viewModel()

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isInChatRoom by chatViewModel.isInChatRoom.collectAsState()

    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isCheckingAuth = false
    }

    if (isCheckingAuth) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isLoggedIn) NavRoutes.Home.route else NavRoutes.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authRoutes = listOf(NavRoutes.Login.route, NavRoutes.Register.route)
    // Bottom nav hidden on auth screens OR when inside a chat room (Fragment nav)
    val showBottomNav = currentRoute !in authRoutes && isLoggedIn && !isInChatRoom

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
            composable(NavRoutes.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    googleAuthHelper = googleAuthHelper,
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
                    googleAuthHelper = googleAuthHelper,
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

            composable(NavRoutes.Home.route) {
                HomeScreen(getToken = { authViewModel.getAccessToken() })
            }

            composable(NavRoutes.MyGarden.route) {
                PlaceholderScreen(title = "My Garden", subtitle = "Coming soon...")
            }

            // Community tab: self-contained Fragment NavHost using chat_nav_graph.xml + SafeArgs
            composable(NavRoutes.Community.route) {
                ChatNavScreen()
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

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentRoute !in authRoutes) {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

/**
 * Embeds a NavHostFragment driven by chat_nav_graph.xml inside a Compose AndroidView.
 * Navigation between ChatListFragment and ChatRoomFragment is handled by the Fragment
 * NavController with SafeArgs — no Compose navController involvement.
 */
@Composable
private fun ChatNavScreen() {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
    val containerId = remember { View.generateViewId() }
    val tag = "ChatNavHostFragment"

    DisposableEffect(Unit) {
        onDispose {
            activity.supportFragmentManager.findFragmentByTag(tag)?.let { fragment ->
                activity.supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNowAllowingStateLoss()
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Remove any stale fragment from a previous composition
            activity.supportFragmentManager.findFragmentByTag(tag)?.let { stale ->
                activity.supportFragmentManager.beginTransaction()
                    .remove(stale)
                    .commitNowAllowingStateLoss()
            }

            FragmentContainerView(ctx).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { _ ->
            if (activity.supportFragmentManager.findFragmentByTag(tag) == null) {
                val navHostFragment = NavHostFragment.create(R.navigation.chat_nav_graph)
                activity.supportFragmentManager.beginTransaction()
                    .replace(containerId, navHostFragment, tag)
                    .commitNowAllowingStateLoss()
            }
        }
    )
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String = "") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
