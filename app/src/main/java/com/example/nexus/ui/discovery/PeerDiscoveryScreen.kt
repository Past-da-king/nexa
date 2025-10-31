package com.example.nexus.ui.discovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.nexus.R
import com.example.nexus.models.Contact
import com.example.nexus.models.Peer
import kotlinx.coroutines.delay

// The State Holder remains the same
data class PeerDiscoveryState(
    val discoveredPeers: List<Peer> = emptyList(),
    val friendRequests: List<Contact> = emptyList(),
    val connectionStatus: String = "Idle"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerDiscoveryScreen(
    navController: NavController,
    viewModel: PeerDiscoveryViewModel = hiltViewModel(),
    onAcceptRequest: (contact: Contact) -> Unit,
    onRejectRequest: (stableId: String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(state.connectionStatus) {
        if (state.connectionStatus != "Idle") {
            statusMessage = state.connectionStatus
            delay(3000)
            statusMessage = ""
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Nearby", "Requests")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Discover") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            if (index == 1 && state.friendRequests.isNotEmpty()) {
                                Badge { Text(state.friendRequests.size.toString()) }
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> NearbyPeersList(
                    peers = state.discoveredPeers,
                    onAddFriendClicked = { peer -> viewModel.addFriend(peer) }
                )
                1 -> FriendRequestsList(
                    requests = state.friendRequests,
                    onAccept = { contact -> viewModel.acceptFriendRequest(contact) },
                    onReject = { stableId -> viewModel.rejectFriendRequest(stableId) }
                )
            }
        }
    }
}


@Composable
private fun NearbyPeersList(
    peers: List<Peer>,
    onAddFriendClicked: (Peer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (peers.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Text("Searching for nearby peers...", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(peers) { peer ->
                    PeerListItem(peer = peer, onAddFriendClicked = { onAddFriendClicked(peer) })
                }
            }
        }
    }
}


@Composable
private fun FriendRequestsList(
    requests: List<Contact>,
    onAccept: (Contact) -> Unit,
    onReject: (String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending friend requests.", textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("These people want to connect with you:", style = MaterialTheme.typography.titleMedium) }
            items(requests) { request ->
                FriendRequestListItem(
                    request = request,
                    onAccept = { onAccept(request) },
                    onReject = { onReject(request.stableId) }
                )
            }
        }
    }
}

@Composable
private fun PeerListItem(peer: Peer, onAddFriendClicked: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Profile",
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(peer.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = onAddFriendClicked, enabled = peer.publicKeyString != null) {
                Text("Add Friend")
            }
        }
    }
}

@Composable
private fun FriendRequestListItem(
    request: Contact,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Profile",
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(request.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onReject) {
                    Text("Reject")
                }
            }
        }
    }
}