package com.example.rootsharemobile.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.ui.components.CommunityFeedSection
import com.example.rootsharemobile.ui.components.FeaturedPlantsSection
import com.example.rootsharemobile.ui.components.RootShareBottomNav
import com.example.rootsharemobile.ui.components.sampleFeaturedPlants
import com.example.rootsharemobile.ui.components.sampleFeedPosts
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    getToken: suspend () -> String?,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe ViewModel state
    val uiState by viewModel.uiState.observeAsState(HomeUiState.Loading)
    val featuredPlants by viewModel.featuredPlants.observeAsState(emptyList())
    val feedPosts by viewModel.feedPosts.observeAsState(emptyList())
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoadingPlants by viewModel.isLoadingPlants.observeAsState(false)
    val isLoadingPosts by viewModel.isLoadingPosts.observeAsState(false)

    // Load data on first composition
    LaunchedEffect(Unit) {
        val token = getToken()
        if (!token.isNullOrEmpty()) {
            viewModel.loadHomeData(token)
        } else {
            // Load sample data for demo when no token is available
            viewModel.loadSampleData()
        }
    }

    // Show error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
        }
    }

    val isRefreshing = isLoadingPlants || isLoadingPosts

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    val token = getToken()
                    if (!token.isNullOrEmpty()) {
                        viewModel.refresh(token)
                    } else {
                        viewModel.loadSampleData()
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load data",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Pull down to retry",
                                fontSize = 14.sp,
                                color = Gray500,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                is HomeUiState.Empty, is HomeUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        FeaturedPlantsSection(
                            plants = featuredPlants,
                            isLoading = isLoadingPlants,
                            onPlantClick = { plant ->
                                // TODO: Navigate to plant detail
                            }
                        )

                        CommunityFeedSection(
                            posts = feedPosts,
                            isLoading = isLoadingPosts,
                            onPostClick = { post ->
                                // TODO: Navigate to post detail
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview-friendly version that doesn't use ViewModel
 */
@Composable
fun HomeScreenPreviewContent(
    featuredPlants: List<Plant>,
    feedPosts: List<Post>,
    modifier: Modifier = Modifier
) {
    var selectedRoute by remember { mutableStateOf("home") }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            RootShareBottomNav(
                selectedRoute = selectedRoute,
                onItemSelected = { item -> selectedRoute = item.route }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            FeaturedPlantsSection(plants = featuredPlants)
            CommunityFeedSection(posts = feedPosts)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    RootShareMobileTheme {
        HomeScreenPreviewContent(
            featuredPlants = sampleFeaturedPlants,
            feedPosts = sampleFeedPosts
        )
    }
}
