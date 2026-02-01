package com.example.rootsharemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.rootsharemobile.ui.navigation.RootShareNavHost
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootShareMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootShareNavHost(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
