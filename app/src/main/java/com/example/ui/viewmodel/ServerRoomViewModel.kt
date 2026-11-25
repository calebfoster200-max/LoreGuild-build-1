package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageType
import com.example.data.model.RoleplayMessage
import com.example.data.model.RoleplayServer
import com.example.data.model.ServerMember
import com.example.data.repository.LoreGuildRepository
import com.example.ui.state.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class ServerRoomViewModel(
    private val repository: LoreGuildRepository,
    private val serverId: String,
    private val currentUserId: String
) : ViewModel() {

    val serverState: StateFlow<UiState<RoleplayServer?>> =
        repository.observeServer(serverId)
            .toUiStateFlow("Failed to observe realm details")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val membersState: StateFlow<UiState<List<ServerMember>>> =
        repository.observeServerMembers(serverId)
            .toUiStateFlow("Failed to observe party roster")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val messagesState: StateFlow<UiState<List<RoleplayMessage>>> =
        repository.observeServerMessages(serverId)
            .toUiStateFlow("Failed to observe roleplay feed")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _roomEventMessage = MutableStateFlow<String?>(null)
    val roomEventMessage: StateFlow<String?> = _roomEventMessage.asStateFlow()

    fun clearEventMessage() {
        _roomEventMessage.value = null
    }

    fun sendMessage(
        member: ServerMember,
        messageType: MessageType,
        content: String,
        onSent: () -> Unit = {}
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            val result = repository.sendMessage(
                userId = currentUserId,
                serverId = serverId,
                member = member,
                messageType = messageType,
                content = content
            )
            _isSending.value = false
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _roomEventMessage.value = err.localizedMessage ?: "Failed to send message."
            }
        }
    }

    fun rollDice(member: ServerMember, sides: Int) {
        val rolled = Random.nextInt(1, sides + 1)
        val tag = when {
            sides == 20 && rolled == 20 -> " (Critical Success!)"
            sides == 20 && rolled == 1 -> " (Critical Failure!)"
            else -> ""
        }
        sendMessage(
            member = member,
            messageType = MessageType.ROLL,
            content = "Rolled 1d$sides -> $rolled$tag"
        )
    }

    fun switchCharacter(
        playerName: String,
        character: CharacterProfile
    ) {
        viewModelScope.launch {
            val result = repository.switchServerCharacter(
                userId = currentUserId,
                playerName = playerName,
                serverId = serverId,
                character = character
            )
            result.onSuccess {
                _roomEventMessage.value = "Now speaking as ${character.name}."
            }.onFailure { err ->
                _roomEventMessage.value = err.localizedMessage ?: "Unable to switch character."
            }
        }
    }

    fun leaveServer(onLeft: () -> Unit) {
        viewModelScope.launch {
            val result = repository.leaveServer(currentUserId, serverId)
            result.onSuccess {
                onLeft()
            }.onFailure { err ->
                _roomEventMessage.value = err.localizedMessage ?: "Failed to leave realm."
            }
        }
    }

    fun deleteServer(inviteCode: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteServer(currentUserId, serverId, inviteCode)
            result.onSuccess {
                onDeleted()
            }.onFailure { err ->
                _roomEventMessage.value = err.localizedMessage ?: "Failed to disband realm."
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val result = repository.deleteMessage(serverId, messageId)
            result.onFailure { err ->
                _roomEventMessage.value = err.localizedMessage ?: "Unable to delete message."
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(UiState.Error(rootError.message ?: fallbackMessage))
            }

    private companion object {
        const val TAG = "ServerRoomViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
