package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CharacterProfile
import com.example.data.model.RoleplayServer
import com.example.data.model.ServerInvite
import com.example.data.model.ServerVisibility
import com.example.data.model.UserProfile
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

sealed interface InviteLookupState {
    data object Idle : InviteLookupState
    data object Loading : InviteLookupState
    data class Found(val invite: ServerInvite) : InviteLookupState
    data class Error(val message: String) : InviteLookupState
}

class MainViewModel(
    private val repository: LoreGuildRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialPhotoUrl: String?
) : ViewModel() {

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("Failed to observe user profile")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val charactersState: StateFlow<UiState<List<CharacterProfile>>> =
        repository.observeCharacters(currentUserId)
            .toUiStateFlow("Failed to observe characters")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val publicServersState: StateFlow<UiState<List<RoleplayServer>>> =
        repository.observePublicServers()
            .toUiStateFlow("Failed to load public realms")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myServersState: StateFlow<UiState<List<RoleplayServer>>> =
        repository.observeMyServers(currentUserId)
            .toUiStateFlow("Failed to load joined realms")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _inviteLookupState = MutableStateFlow<InviteLookupState>(InviteLookupState.Idle)
    val inviteLookupState: StateFlow<InviteLookupState> = _inviteLookupState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName, initialPhotoUrl)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun resetInviteLookup() {
        _inviteLookupState.value = InviteLookupState.Idle
    }

    fun saveCharacter(
        characterId: String?,
        name: String,
        race: String,
        characterClass: String,
        title: String,
        level: Long,
        alignment: String,
        biography: String,
        avatarKey: String,
        accentColorHex: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveCharacter(
                userId = currentUserId,
                characterId = characterId,
                name = name,
                race = race,
                characterClass = characterClass,
                title = title,
                level = level,
                alignment = alignment,
                biography = biography,
                avatarKey = avatarKey,
                accentColorHex = accentColorHex
            )
            _isBusy.value = false
            result.onSuccess { savedId ->
                _statusMessage.value = if (characterId.isNullOrBlank()) {
                    "Character forged and saved to your roster."
                } else {
                    "Character dossier updated."
                }
                onSuccess(savedId)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not save character."
            }
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteCharacter(currentUserId, characterId)
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Character removed from roster."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not delete character."
            }
        }
    }

    fun setActiveCharacter(characterId: String) {
        viewModelScope.launch {
            val result = repository.setActiveCharacter(currentUserId, characterId)
            result.onSuccess {
                _statusMessage.value = "Active character updated."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update active character."
            }
        }
    }

    fun createServer(
        playerName: String,
        name: String,
        genre: String,
        description: String,
        visibility: ServerVisibility,
        bannerKey: String,
        character: CharacterProfile,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createServer(
                userId = currentUserId,
                playerName = playerName,
                name = name,
                genre = genre,
                description = description,
                visibility = visibility,
                bannerKey = bannerKey,
                character = character
            )
            _isBusy.value = false
            result.onSuccess { serverId ->
                _statusMessage.value = "Realm created! Invite guildmates to join."
                onCreated(serverId)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to create realm."
            }
        }
    }

    fun lookupInvite(code: String) {
        viewModelScope.launch {
            _inviteLookupState.value = InviteLookupState.Loading
            val result = repository.lookupInvite(code)
            result.onSuccess { invite ->
                _inviteLookupState.value = if (invite != null) {
                    InviteLookupState.Found(invite)
                } else {
                    InviteLookupState.Error("No realm found for code '${code.trim().uppercase()}'.")
                }
            }.onFailure { err ->
                _inviteLookupState.value = InviteLookupState.Error(
                    err.localizedMessage ?: "Unable to verify invite code."
                )
            }
        }
    }

    fun joinServer(
        playerName: String,
        serverId: String,
        inviteCode: String,
        isPublic: Boolean,
        character: CharacterProfile,
        onJoined: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.joinServer(
                userId = currentUserId,
                playerName = playerName,
                serverId = serverId,
                inviteCode = inviteCode,
                isPublic = isPublic,
                character = character
            )
            _isBusy.value = false
            result.onSuccess {
                _inviteLookupState.value = InviteLookupState.Idle
                onJoined(serverId)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to join realm."
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
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
