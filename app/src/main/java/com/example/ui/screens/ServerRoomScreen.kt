package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageType
import com.example.data.model.RoleplayMessage
import com.example.data.model.ServerMember
import com.example.ui.components.CharacterAvatar
import com.example.ui.components.CharacterPickerSheet
import com.example.ui.components.parseAccentColor
import com.example.ui.state.UiState
import com.example.ui.viewmodel.ServerRoomViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerRoomScreen(
    viewModel: ServerRoomViewModel,
    currentUserId: String,
    playerDisplayName: String,
    userCharacters: List<CharacterProfile>,
    onCreateCharacterClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val eventMessage by viewModel.roomEventMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var draftText by remember { mutableStateOf("") }
    var selectedMessageType by remember { mutableStateOf(MessageType.DIALOGUE) }
    var showRosterSheet by remember { mutableStateOf(false) }
    var showCharacterSwitcher by remember { mutableStateOf(false) }
    var showDiceMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showLeaveOrDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(eventMessage) {
        if (eventMessage != null) {
            snackbarHostState.showSnackbar(eventMessage!!)
            viewModel.clearEventMessage()
        }
    }

    val server = (serverState as? UiState.Success)?.data
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val currentMember = remember(members, currentUserId) {
        members.firstOrNull { it.userId == currentUserId }
    }
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val isOwner = server?.ownerId == currentUserId

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = server?.name ?: "Roleplay Realm",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (server != null) {
                                "${server.genre} • ${members.size} active"
                            } else {
                                "Connecting..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("room_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to realms"
                        )
                    }
                },
                actions = {
                    if (server != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", server.inviteCode))
                                    Toast.makeText(context, "Copied code ${server.inviteCode}", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("room_copy_invite")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = server.inviteCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy invite code",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showRosterSheet = true },
                        modifier = Modifier.testTag("room_roster_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = "Party roster"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("room_menu_button")
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Realm options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Switch Character") },
                                leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showCharacterSwitcher = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isOwner) "Disband Realm" else "Leave Realm",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isOwner) Icons.Rounded.DeleteOutline else Icons.AutoMirrored.Rounded.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showLeaveOrDeleteConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (currentMember != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Speaking as + Mode chips + Dice roller
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    parseAccentColor(currentMember.characterAccentHex, MaterialTheme.colorScheme.primary)
                                ),
                                modifier = Modifier
                                    .clickable { showCharacterSwitcher = true }
                                    .testTag("room_switch_character_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CharacterAvatar(
                                        avatarKey = currentMember.characterAvatarKey,
                                        accentColorHex = currentMember.characterAccentHex,
                                        characterName = currentMember.characterName,
                                        size = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentMember.characterName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = "Switch character",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            listOf(MessageType.DIALOGUE, MessageType.ACTION, MessageType.OOC).forEach { mode ->
                                FilterChip(
                                    selected = selectedMessageType == mode,
                                    onClick = { selectedMessageType = mode },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("mode_chip_${mode.raw}")
                                )
                            }

                            Box {
                                FilterChip(
                                    selected = false,
                                    onClick = { showDiceMenu = true },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Casino,
                                            contentDescription = "Roll dice",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("Roll", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("dice_roll_chip")
                                )
                                DropdownMenu(
                                    expanded = showDiceMenu,
                                    onDismissRequest = { showDiceMenu = false }
                                ) {
                                    listOf(20, 12, 10, 6, 100).forEach { sides ->
                                        DropdownMenuItem(
                                            text = { Text("Roll d$sides") },
                                            onClick = {
                                                showDiceMenu = false
                                                viewModel.rollDice(currentMember, sides)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Text input + send button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draftText,
                                onValueChange = { if (it.length <= 1500) draftText = it },
                                placeholder = {
                                    val hint = when (selectedMessageType) {
                                        MessageType.DIALOGUE -> "Speak as ${currentMember.characterName}..."
                                        MessageType.ACTION -> "Describe ${currentMember.characterName}'s action..."
                                        MessageType.OOC -> "Out-of-character note..."
                                        MessageType.ROLL -> "Message..."
                                    }
                                    Text(hint)
                                },
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("room_message_input")
                            )

                            FilledIconButton(
                                onClick = {
                                    val text = draftText
                                    viewModel.sendMessage(
                                        member = currentMember,
                                        messageType = selectedMessageType,
                                        content = text,
                                        onSent = { draftText = "" }
                                    )
                                },
                                enabled = draftText.isNotBlank() && !isSending,
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("room_send_button")
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send roleplay message"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
            ) {
                when (messagesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (messagesState as UiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is UiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (server != null) {
                                item(key = "realm_opening_header") {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Welcome to ${server.name}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (server.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = server.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Invite code: ${server.inviteCode} • ${members.size} player(s) at the table",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (messages.isEmpty()) {
                                item(key = "empty_scene") {
                                    Text(
                                        text = "The tavern is quiet. Deliver the opening line or roll a d20 to begin the scene.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)
                                    )
                                }
                            } else {
                                items(messages, key = { it.id }) { msg ->
                                    val canDelete = msg.senderUid == currentUserId || isOwner
                                    RoleplayMessageItem(
                                        message = msg,
                                        isOwnMessage = msg.senderUid == currentUserId,
                                        canDelete = canDelete,
                                        onDelete = { viewModel.deleteMessage(msg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRosterSheet) {
        PartyRosterSheet(
            members = members,
            ownerId = server?.ownerId ?: "",
            onDismiss = { showRosterSheet = false }
        )
    }

    if (showCharacterSwitcher) {
        CharacterPickerSheet(
            title = "Switch Active Character",
            subtitle = "Your next messages in this realm will use the selected character.",
            characters = userCharacters,
            selectedCharacterId = currentMember?.characterId,
            onSelectCharacter = { chosen ->
                showCharacterSwitcher = false
                viewModel.switchCharacter(playerDisplayName, chosen)
            },
            onCreateCharacterClicked = {
                showCharacterSwitcher = false
                onCreateCharacterClicked()
            },
            onDismiss = { showCharacterSwitcher = false }
        )
    }

    if (showLeaveOrDeleteConfirm && server != null) {
        AlertDialog(
            onDismissRequest = { showLeaveOrDeleteConfirm = false },
            title = { Text(if (isOwner) "Disband ${server.name}?" else "Leave ${server.name}?") },
            text = {
                Text(
                    if (isOwner) {
                        "This permanently closes the realm and invalidates its invite code."
                    } else {
                        "You can rejoin later with your character if the realm is open or you have the invite code."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveOrDeleteConfirm = false
                        if (isOwner) {
                            viewModel.deleteServer(server.inviteCode, onDeleted = onNavigateBack)
                        } else {
                            viewModel.leaveServer(onLeft = onNavigateBack)
                        }
                    },
                    modifier = Modifier.testTag("confirm_leave_or_delete_realm")
                ) {
                    Text(if (isOwner) "Disband" else "Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveOrDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RoleplayMessageItem(
    message: RoleplayMessage,
    isOwnMessage: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val accent = parseAccentColor(message.characterAccentHex, MaterialTheme.colorScheme.primary)
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeText = message.createdAt?.toDate()?.let { timeFormat.format(it) } ?: "Sending..."

    when (message.parsedType) {
        MessageType.ACTION -> {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_item_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .background(accent, MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "* ${message.characterName} ${message.content}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${message.characterRace} ${message.characterClass} • $timeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        MessageType.ROLL -> {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_item_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Casino,
                        contentDescription = "Dice roll",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${message.characterName}: ${message.content}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        MessageType.OOC -> {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_item_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "(( OOC • ${message.characterName}: ${message.content} ))",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete OOC note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        MessageType.DIALOGUE -> {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isOwnMessage) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    1.dp,
                    if (isOwnMessage) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_item_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CharacterAvatar(
                        avatarKey = message.characterAvatarKey,
                        accentColorHex = message.characterAccentHex,
                        characterName = message.characterName,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.characterName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${message.characterRace} ${message.characterClass} • $timeText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canDelete) {
                                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete message",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartyRosterSheet(
    members: List<ServerMember>,
    ownerId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Party Roster (${members.size})",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Players and their active characters in this realm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(members, key = { it.userId }) { member ->
                    val accent = parseAccentColor(member.characterAccentHex, MaterialTheme.colorScheme.primary)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CharacterAvatar(
                                avatarKey = member.characterAvatarKey,
                                accentColorHex = member.characterAccentHex,
                                characterName = member.characterName,
                                size = 46.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.characterName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (member.userId == ownerId) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "HOST",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${member.characterRace} ${member.characterClass}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent
                                )
                                Text(
                                    text = "Played by ${member.playerName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
