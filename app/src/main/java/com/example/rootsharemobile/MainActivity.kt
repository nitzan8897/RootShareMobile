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
import com.example.rootsharemobile.ui.components.sampleFeaturedPlants
import com.example.rootsharemobile.ui.components.sampleFeedPosts
import com.example.rootsharemobile.ui.screens.home.HomeScreen
import com.example.rootsharemobile.ui.screens.home.HomeScreenPreviewContent
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

class MainActivity : ComponentActivity() {

    // TODO: Replace with actual token from auth system
    // For testing, you can get a token from your backend by logging in
    private val testToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootShareMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        getToken = { testToken },
                        onNavigate = { navItem ->
                            // TODO: Handle navigation to other screens
                        }
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
