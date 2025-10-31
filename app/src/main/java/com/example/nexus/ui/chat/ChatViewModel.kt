package com.example.nexus.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import com.example.nexus.models.Message
import com.example.nexus.models.MessageStatus
import com.example.nexus.models.MessageType
import com.example.nexus.models.RecipientType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// This is our clean UI state holder.
data class ChatScreenState(
    val messages: List<Message> = emptyList(),
    val contactName: String = "Loading...",
    val isConnectionSecure: Boolean = false
)

/**
 * The ViewModel for the ChatScreen.
 *
 * It is initialized with a conversationId from the navigation graph via SavedStateHandle.
 * This ensures it loads the correct chat history and sends messages to the correct recipient.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel // This annotation is needed for Hilt to inject dependencies
class ChatViewModel @Inject constructor(
    private val nexaService: NexaService,
    savedStateHandle: SavedStateHandle // Hilt provides this for us to access nav arguments
) : ViewModel() {

    private val conversationId: StateFlow<String> = savedStateHandle.getStateFlow("conversationId", "")

    init {
        viewModelScope.launch {
            val connectedPeer = nexaService.getConnectedPeer().first()
            val currentConversationId = conversationId.value
            if (connectedPeer?.stableId != currentConversationId) {
                if (currentConversationId.isNotEmpty()){
                    nexaService.requestConnectionTo(currentConversationId)
                }
            }
        }
    }

    private val messagesFlow: Flow<List<Message>> = conversationId.flatMapLatest { id ->
        if (id.isNotEmpty()) {
            nexaService.getMessages(id)
        } else {
            flowOf(emptyList())
        }
    }

    private val contactNameFlow = nexaService.getConnectedPeer().map { it?.name ?: "Disconnected" }

    val state: StateFlow<ChatScreenState> = combine(
        messagesFlow,
        contactNameFlow,
        nexaService.getIsConnectionSecure()
    ) { messages, name, isSecure ->
        ChatScreenState(
            messages = messages,
            contactName = name,
            isConnectionSecure = isSecure
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatScreenState())

    /**
     * Sends a message to the recipient of the current conversation.
     */
    fun sendMessage(text: String, data: String? = null, type: MessageType = MessageType.TEXT) {
        val currentConversationId = conversationId.value
        if (currentConversationId.isNotEmpty()) {
            viewModelScope.launch {
                val message = Message(
                    id = System.currentTimeMillis().toString(),
                    text = text,
                    data = data,
                    isSentByMe = true,
                    messageType = type,
                    status = MessageStatus.SENDING // The service will handle queuing if offline
                )
                nexaService.sendMessage(currentConversationId, message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}