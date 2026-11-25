package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CharacterProfile
import com.example.data.model.RoleplayPresets
import com.example.data.model.ServerInvite
import com.example.data.model.ServerVisibility
import com.example.ui.components.CharacterAvatar
import com.example.ui.viewmodel.InviteLookupState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRealmDialog(
    characters: List<CharacterProfile>,
    initialCharacter: CharacterProfile?,
    isBusy: Boolean,
    onRequestCreateCharacter: () -> Unit,
    onCreateServer: (
        name: String,
        genre: String,
        description: String,
        visibility: ServerVisibility,
        bannerKey: String,
        character: CharacterProfile
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("High Fantasy") }
    var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(ServerVisibility.PUBLIC) }
    var bannerKey by remember { mutableStateOf("tavern") }
    var selectedCharacter by remember(characters, initialCharacter) {
        mutableStateOf(initialCharacter ?: characters.firstOrNull())
    }
    var showCharacterChooser by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Create Roleplay Realm", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("create_realm_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Button(
                            onClick = {
                                val cleanName = name.trim()
                                val cleanGenre = genre.trim()
                                val char = selectedCharacter
                                when {
                                    cleanName.length < 2 -> validationError = "Realm name must be at least 2 characters."
                                    cleanGenre.isEmpty() -> validationError = "Please enter or choose a genre."
                                    char == null -> validationError = "Forge or select a character to host this realm."
                                    else -> {
                                        validationError = null
                                        onCreateServer(
                                            cleanName,
                                            cleanGenre,
                                            description.trim(),
                                            visibility,
                                            bannerKey,
                                            char
                                        )
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_create_realm_button")
                        ) {
                            Text(if (isBusy) "Opening Realm..." else "Open Realm")
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Realm Name *") },
                    placeholder = { Text("e.g., The Sunken Lantern") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("realm_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Visibility", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ServerVisibility.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ServerVisibility.entries.size
                                ),
                                selected = visibility == option,
                                onClick = { visibility = option },
                                icon = {
                                    Icon(
                                        imageVector = if (option == ServerVisibility.PUBLIC) {
                                            Icons.Rounded.Public
                                        } else {
                                            Icons.Rounded.Lock
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("visibility_${option.raw}")
                            ) {
                                Text(if (option == ServerVisibility.PUBLIC) "Public" else "Private (Invite Only)")
                            }
                        }
                    }
                    Text(
                        text = if (visibility == ServerVisibility.PUBLIC) {
                            "Listed in Public Realms for anyone to join, and shareable via invite code."
                        } else {
                            "Hidden from public discovery. Players join exclusively with your invite code."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { if (it.length <= 40) genre = it },
                        label = { Text("Setting / Genre *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("realm_genre_input")
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleplayPresets.genres.forEach { preset ->
                            FilterChip(
                                selected = genre.equals(preset, ignoreCase = true),
                                onClick = { genre = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Realm Banner", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoleplayPresets.bannerOptions.forEach { option ->
                            val isSelected = bannerKey == option.key
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { bannerKey = option.key }
                                    .testTag("banner_${option.key}")
                            ) {
                                Column {
                                    Image(
                                        painter = painterResource(
                                            id = if (option.key == "citadel") {
                                                R.drawable.img_banner_citadel
                                            } else {
                                                R.drawable.img_hero_tavern
                                            }
                                        ),
                                        contentDescription = option.label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(78.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 400) description = it },
                    label = { Text("Lore Hook & Table Rules") },
                    placeholder = { Text("Set the opening scene, tone, and expectations for players...") },
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${description.length}/400") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("realm_description_input")
                )

                // Starting character selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your Character in This Realm", style = MaterialTheme.typography.titleSmall)
                    if (selectedCharacter == null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "You need a character first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onRequestCreateCharacter()
                                    },
                                    modifier = Modifier.testTag("create_realm_forge_character_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Forge")
                                }
                            }
                        }
                    } else {
                        val char = selectedCharacter!!
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCharacterChooser = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CharacterAvatar(
                                    avatarKey = char.avatarKey,
                                    accentColorHex = char.accentColorHex,
                                    characterName = char.name,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(char.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Lv.${char.level} ${char.race} ${char.characterClass}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = "Change character",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCharacterChooser) {
        com.example.ui.components.CharacterPickerSheet(
            title = "Host Realm As",
            subtitle = "Choose which character enters this realm with you.",
            characters = characters,
            selectedCharacterId = selectedCharacter?.id,
            onSelectCharacter = { chosen ->
                selectedCharacter = chosen
                showCharacterChooser = false
            },
            onCreateCharacterClicked = {
                showCharacterChooser = false
                onDismiss()
                onRequestCreateCharacter()
            },
            onDismiss = { showCharacterChooser = false }
        )
    }
}

@Composable
fun JoinByInviteDialog(
    lookupState: InviteLookupState,
    onLookupCode: (String) -> Unit,
    onProceedWithInvite: (ServerInvite) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Key, contentDescription = null) },
        title = { Text("Join Realm by Code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter an invite code from a friend or game master to enter a private or public server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { raw ->
                            val filtered = raw.uppercase().filter { it.isLetterOrDigit() }.take(12)
                            codeInput = filtered
                        },
                        label = { Text("Invite Code") },
                        placeholder = { Text("e.g., TAVERN1") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invite_code_input")
                    )
                    OutlinedButton(
                        onClick = { onLookupCode(codeInput) },
                        enabled = codeInput.length >= 6 && lookupState !is InviteLookupState.Loading,
                        modifier = Modifier.testTag("lookup_invite_button")
                    ) {
                        if (lookupState is InviteLookupState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = "Find realm")
                        }
                    }
                }

                when (lookupState) {
                    is InviteLookupState.Found -> {
                        val invite = lookupState.invite
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = invite.serverName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${invite.genre} • ${if (invite.visibility == "private") "Private" else "Public"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    is InviteLookupState.Error -> {
                        Text(
                            text = lookupState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (lookupState is InviteLookupState.Found) {
                Button(
                    onClick = { onProceedWithInvite(lookupState.invite) },
                    modifier = Modifier.testTag("proceed_invite_join_button")
                ) {
                    Text("Pick Character & Join")
                }
            } else {
                Button(
                    onClick = { onLookupCode(codeInput) },
                    enabled = codeInput.length >= 6 && lookupState !is InviteLookupState.Loading,
                    modifier = Modifier.testTag("find_realm_confirm_button")
                ) {
                    Text("Find Realm")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
