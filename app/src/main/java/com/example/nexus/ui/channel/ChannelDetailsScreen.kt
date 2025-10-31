package com.example.nexus.ui.channel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexus.R
import com.example.nexus.models.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailsScreen(
    viewModel: ChannelDetailsViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit,
    onNavigateToInviteMembers: (String) -> Unit,
    onUp: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val channel = state.channel

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channel?.name ?: "Channel Details") },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (channel != null) {
                        IconButton(onClick = { onNavigateToInviteMembers(channel.id) }) {
                            Icon(Icons.Default.Add, contentDescription = "Invite Members")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (channel != null) {
                Button(
                    onClick = { onNavigateToChat(channel.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Go to Chat")
                }
            }
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
            } else if (channel != null) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = channel.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn {
                    items(state.members) { member ->
                        MemberListItem(member = member)
                    }
                }
            } else {
                Text("Channel not found.")
            }
        }
    }
}

@Composable
fun MemberListItem(member: Contact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_profile_placeholder),
            contentDescription = "Member Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(member.name, style = MaterialTheme.typography.bodyLarge)
    }
}