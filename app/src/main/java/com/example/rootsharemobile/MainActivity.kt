package com.example.rootsharemobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rootsharemobile.data.auth.GoogleAuthHelper
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.ui.components.sampleFeaturedPlants
import com.example.rootsharemobile.ui.components.sampleFeedPosts
import com.example.rootsharemobile.ui.navigation.RootShareNavHost
import com.example.rootsharemobile.ui.screens.home.HomeScreenPreviewContent
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val googleAuthHelper = GoogleAuthHelper(this)

        // Initialize RetrofitClient with TokenManager for automatic token refresh
        RetrofitClient.init(TokenManager(applicationContext))

        setContent {
            RootShareMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootShareNavHost(googleAuthHelper = googleAuthHelper)
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
