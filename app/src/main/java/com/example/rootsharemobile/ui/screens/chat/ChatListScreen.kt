package com.example.rootsharemobile.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rootsharemobile.data.remote.ConnectionStatus
import com.example.rootsharemobile.ui.components.ChatItemRow
import com.example.rootsharemobile.ui.components.ChatPreview
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray400
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

val sampleChats = listOf(
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

@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onChatClick: (chatId: String) -> Unit = {}
) {
    val chats by chatViewModel.chats.collectAsState()
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredChats by remember(searchQuery, chats) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                chats
            } else {
                chats.filter {
                    it.participantName.contains(searchQuery, ignoreCase = true) ||
                            it.lastMessage.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.connect()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Header with connection status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Community Chat",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            ConnectionIndicator(status = connectionStatus)
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search chats",
                    color = Gray400
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Gray400
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Section header
        Text(
            text = "Recent Chats",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Chat list
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredChats, key = { it.chatId }) { chat ->
                ChatItemRow(
                    chatPreview = chat,
                    onClick = { onChatClick(chat.chatId) }
                )
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(status: ConnectionStatus) {
    val color = when (status) {
        ConnectionStatus.CONNECTED -> Emerald500
        ConnectionStatus.CONNECTING -> Gray400
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
    }
    val label = when (status) {
        ConnectionStatus.CONNECTED -> "Online"
        ConnectionStatus.CONNECTING -> "Connecting..."
        ConnectionStatus.DISCONNECTED -> "Offline"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = Gray500
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatListScreenPreview() {
    // Preview without ViewModel - shows static layout
    RootShareMobileTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Community Chat",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn(modifier = Modifier.padding(top = 80.dp)) {
                items(sampleChats, key = { it.chatId }) { chat ->
                    ChatItemRow(chatPreview = chat)
                }
            }
        }
    }
}
