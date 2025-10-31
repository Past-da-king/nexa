package com.example.nexus.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nexus.R
import com.example.nexus.models.Conversation
import com.example.nexus.ui.theme.NexusTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    conversations: List<Conversation>,
    onConversationClicked: (conversation: Conversation) -> Unit,
    onNewChatClicked: () -> Unit,
    onNewChannelClicked: () -> Unit,
    onPublicChannelsClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nexus") },
                actions = {
                    IconButton(onClick = onPublicChannelsClicked) {
                        Icon(Icons.Default.Groups, contentDescription = "Public Channels")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onNewChatClicked,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_chat))
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = onNewChannelClicked,
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "New Channel")
                }
            }
        }
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_conversations),
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(conversations) { conversation ->
                    ConversationListItem(
                        conversation = conversation,
                        onClick = { onConversationClicked(conversation) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationListItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = stringResource(R.string.profile),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (conversation.isGroup) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Groups, contentDescription = "Group Chat", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conversation.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NexusTheme {
        val dummyConversations = listOf(
            Conversation("1", "Ayanda", "Okay, see you then!", System.currentTimeMillis() - 10000, false),
            Conversation("2", "Falakhe", "Sounds good, thanks for the update!", System.currentTimeMillis() - 60000, false),
            Conversation("3", "UCT Soccer", "Practice is at 5pm", System.currentTimeMillis() - 90000, true)
        )
                    HomeScreen(
                        conversations = dummyConversations,
                        onConversationClicked = {},
                        onNewChatClicked = {},
                        onNewChannelClicked = {},
                        onPublicChannelsClicked = {},
                    )
                }
            }
