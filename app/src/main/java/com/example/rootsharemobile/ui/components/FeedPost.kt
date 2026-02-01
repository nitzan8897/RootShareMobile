package com.example.rootsharemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.PostType
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray100
import com.example.rootsharemobile.ui.theme.Gray200
import com.example.rootsharemobile.ui.theme.Gray300
import com.example.rootsharemobile.ui.theme.Gray50
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.Gray900
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedPost(
    post: Post,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray50
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = { onClick?.invoke() }
    ) {
        Column {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                if (post.images.isNotEmpty()) {
                    AsyncImage(
                        model = post.images.first(),
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Gray200),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Image",
                            fontSize = 16.sp,
                            color = Gray500
                        )
                    }
                }

                // Post type badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(
                            color = when (post.type) {
                                PostType.UPDATE -> Emerald500
                                PostType.SWAP -> MaterialTheme.colorScheme.tertiary
                                PostType.GIVEAWAY -> MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.typeBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Image indicator dots (if multiple images)
                if (post.images.size > 1) {
                    Row(
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Emerald500)
                        )
                        repeat(minOf(post.images.size - 1, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Gray300)
                            )
                        }
                    }
                }

                // Post content
                Text(
                    text = post.content,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray900,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Tags (extracted from content hashtags)
                if (post.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        post.tags.take(4).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = tag,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Gray900
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Gray100
                                ),
                                border = null,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Engagement stats and user info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Likes and comments
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Likes",
                                modifier = Modifier.size(20.dp),
                                tint = Gray500
                            )
                            Text(
                                text = "${post.likesCount}",
                                fontSize = 14.sp,
                                color = Gray500
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ModeComment,
                                contentDescription = "Comments",
                                modifier = Modifier.size(20.dp),
                                tint = Gray500
                            )
                            Text(
                                text = "${post.commentsCount}",
                                fontSize = 14.sp,
                                color = Gray500
                            )
                        }
                    }

                    // User ID (placeholder - will be replaced with actual user info)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Gray200),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.userId.take(1).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gray500
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sample data for previews
val sampleFeedPosts = listOf(
    Post(
        id = "1",
        userId = "user123",
        content = "Just planted my new flower garden! #Gardening #Flowers",
        type = PostType.UPDATE,
        images = emptyList(),
        likesCount = 24,
        commentsCount = 5
    ),
    Post(
        id = "2",
        userId = "user456",
        content = "Looking to swap my Pothos cuttings! #PlantSwap #Pothos",
        type = PostType.SWAP,
        images = emptyList(),
        likesCount = 12,
        commentsCount = 8
    )
)

@Preview(showBackground = true)
@Composable
fun FeedPostPreview() {
    RootShareMobileTheme {
        FeedPost(post = sampleFeedPosts.first())
    }
}
