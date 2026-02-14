package com.example.rootsharemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray200
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

data class ChatPreview(
    val chatId: String,
    val participantName: String,
    val participantImageUrl: String? = null,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0
)

@Composable
fun ChatItemRow(
    chatPreview: ChatPreview,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (chatPreview.participantImageUrl != null) {
                    AsyncImage(
                        model = chatPreview.participantImageUrl,
                        contentDescription = "${chatPreview.participantName} avatar",
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = chatPreview.participantName.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Name + last message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chatPreview.participantName,
                    fontSize = 16.sp,
                    fontWeight = if (chatPreview.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chatPreview.lastMessage,
                    fontSize = 14.sp,
                    fontWeight = if (chatPreview.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    color = if (chatPreview.unreadCount > 0)
                        MaterialTheme.colorScheme.onSurface
                    else
                        Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Timestamp + unread badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = chatPreview.timestamp,
                    fontSize = 12.sp,
                    color = if (chatPreview.unreadCount > 0)
                        Emerald500
                    else
                        Gray500
                )
                if (chatPreview.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Emerald500),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chatPreview.unreadCount > 99) "99+" else "${chatPreview.unreadCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 76.dp),
            thickness = 0.5.dp,
            color = Gray200
        )
    }
}

// Sample data for previews
val sampleChatPreviews = listOf(
    ChatPreview(
        chatId = "1",
        participantName = "Alice",
        lastMessage = "Hey! Want to swap cuttings this weekend?",
        timestamp = "2m ago",
        unreadCount = 2
    ),
    ChatPreview(
        chatId = "2",
        participantName = "Bob",
        lastMessage = "The basil seeds you gave me finally sprouted!",
        timestamp = "1h ago",
        unreadCount = 0
    ),
    ChatPreview(
        chatId = "3",
        participantName = "Charlie",
        lastMessage = "Is your Monstera still available for trade?",
        timestamp = "Yesterday",
        unreadCount = 1
    )
)

@Preview(showBackground = true)
@Composable
fun ChatItemRowUnreadPreview() {
    RootShareMobileTheme {
        ChatItemRow(chatPreview = sampleChatPreviews[0])
    }
}

@Preview(showBackground = true)
@Composable
fun ChatItemRowReadPreview() {
    RootShareMobileTheme {
        ChatItemRow(chatPreview = sampleChatPreviews[1])
    }
}
