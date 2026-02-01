package com.example.rootsharemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rootsharemobile.ui.components.sampleFeaturedPlants
import com.example.rootsharemobile.ui.components.sampleFeedPosts
import com.example.rootsharemobile.ui.navigation.RootShareNavHost
import com.example.rootsharemobile.ui.screens.auth.AuthViewModel
import com.example.rootsharemobile.ui.screens.home.HomeScreenPreviewContent
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootShareMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel()
                    RootShareNavHost(
                        authViewModel = authViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainActivityPreview() {
    RootShareMobileTheme {
        HomeScreenPreviewContent(
            featuredPlants = sampleFeaturedPlants,
            feedPosts = sampleFeedPosts
        )
    }
}
