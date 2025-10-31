package com.example.nexus.ui.channel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
fun InviteMembersScreen(
    viewModel: InviteMembersViewModel = hiltViewModel(),
    onInvitesSent: () -> Unit,
    onUp: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Members") },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.sendInvites(onInvitesSent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = state.selectedFriends.isNotEmpty()
            ) {
                Text("Send Invites")
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
            } else if (state.friends.isEmpty()) {
                Text("No friends available to invite.")
            } else {
                LazyColumn {
                    items(state.friends) { friend ->
                        SelectableFriendListItem(
                            friend = friend,
                            isSelected = state.selectedFriends.contains(friend.stableId),
                            onToggleSelection = { viewModel.toggleFriendSelection(friend.stableId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableFriendListItem(
    friend: Contact,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_profile_placeholder),
            contentDescription = "Friend Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(friend.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Selected" else "Unselected",
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}