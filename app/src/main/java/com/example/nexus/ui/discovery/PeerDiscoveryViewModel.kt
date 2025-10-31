package com.example.nexus.ui.discovery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import com.example.nexus.models.Peer
import com.example.nexus.models.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PeerDiscoveryViewModel @Inject constructor(
    private val nexaService: NexaService
) : ViewModel() {

    val state = combine(
        nexaService.getDiscoveredPeers(),
        nexaService.getFriendRequests(),
        nexaService.getConnectionStatus(),
    ) { discoveredPeers, friendRequests, connectionStatus ->
        PeerDiscoveryState(
            discoveredPeers = discoveredPeers,
            friendRequests = friendRequests,
            connectionStatus = connectionStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeerDiscoveryState())

    /**
     * Sends a friend request to a discovered peer.
     */
    fun addFriend(peer: Peer) {
        viewModelScope.launch {
            nexaService.sendFriendRequest(peer)
        }
    }

    /**
     * Accepts a pending friend request.
     */
    fun acceptFriendRequest(contact: Contact) {
        viewModelScope.launch {
            nexaService.acceptFriendRequest(contact)
        }
    }

    /**
     * Rejects a pending friend request.
     */
    fun rejectFriendRequest(stableId: String) {
        viewModelScope.launch {
            nexaService.rejectFriendRequest(stableId)
        }
    }
}