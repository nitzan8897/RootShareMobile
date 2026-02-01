package com.example.rootsharemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rootsharemobile.ui.screens.auth.AuthViewModel
import com.example.rootsharemobile.ui.screens.auth.LoginScreen
import com.example.rootsharemobile.ui.screens.auth.RegisterScreen
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootShareMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthDemo()
                }
            }
        }
    }
}

@Composable
fun AuthDemo() {
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    var showRegister by remember { mutableStateOf(false) }

    when {
        isLoggedIn -> {
            // Logged in - show success message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login Successful!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        showRegister -> {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.resetState()
                    showRegister = false
                },
                onRegisterSuccess = {
                    // Will automatically update isLoggedIn state
                }
            )
        }
        else -> {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.resetState()
                    showRegister = true
                },
                onLoginSuccess = {
                    // Will automatically update isLoggedIn state
                }
            )
        }
    }
}
