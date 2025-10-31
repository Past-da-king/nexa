package com.example.nexus.ui.channel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexus.models.Channel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicChannelsScreen(
    viewModel: PublicChannelsViewModel = hiltViewModel(),
    onChannelJoined: (String) -> Unit,
    onUp: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Public Channels") },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.error != null) {
                Text("Error: ${state.error}", color = Color.Red)
            } else if (state.publicChannels.isEmpty()) {
                Text("No public channels found.")
            } else {
                LazyColumn {
                    items(state.publicChannels) { channel ->
                        PublicChannelListItem(
                            channel = channel,
                            onJoinClick = { viewModel.joinChannel(channel.id) { onChannelJoined(channel.id) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PublicChannelListItem(
    channel: Channel,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Groups, contentDescription = "Channel Icon", modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(channel.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Button(onClick = onJoinClick) {
                Text("Join")
            }
        }
    }
}