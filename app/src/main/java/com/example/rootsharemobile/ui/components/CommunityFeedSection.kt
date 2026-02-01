package com.example.rootsharemobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.Gray900
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

@Composable
fun CommunityFeedSection(
    posts: List<Post>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onPostClick: ((Post) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Community Feed",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No posts yet. Be the first to share!",
                        fontSize = 16.sp,
                        color = Gray500
                    )
                }
            }
            else -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    posts.forEach { post ->
                        FeedPost(
                            post = post,
                            onClick = { onPostClick?.invoke(post) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommunityFeedSectionPreview() {
    RootShareMobileTheme {
        CommunityFeedSection(posts = sampleFeedPosts)
    }
}

@Preview(showBackground = true)
@Composable
fun CommunityFeedSectionLoadingPreview() {
    RootShareMobileTheme {
        CommunityFeedSection(posts = emptyList(), isLoading = true)
    }
}

@Preview(showBackground = true)
@Composable
fun CommunityFeedSectionEmptyPreview() {
    RootShareMobileTheme {
        CommunityFeedSection(posts = emptyList(), isLoading = false)
    }
}
