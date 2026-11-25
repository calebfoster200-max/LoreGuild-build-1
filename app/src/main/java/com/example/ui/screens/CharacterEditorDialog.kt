package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CharacterProfile
import com.example.data.model.RoleplayPresets
import com.example.ui.components.CharacterAvatar
import com.example.ui.components.parseAccentColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterEditorDialog(
    initialCharacter: CharacterProfile?,
    isBusy: Boolean,
    onSave: (
        characterId: String?,
        name: String,
        race: String,
        characterClass: String,
        title: String,
        level: Long,
        alignment: String,
        biography: String,
        avatarKey: String,
        accentColorHex: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialCharacter?.name ?: "") }
    var title by remember { mutableStateOf(initialCharacter?.title ?: "") }
    var race by remember { mutableStateOf(initialCharacter?.race ?: "Human") }
    var characterClass by remember { mutableStateOf(initialCharacter?.characterClass ?: "Spellblade") }
    var level by remember { mutableLongStateOf(initialCharacter?.level ?: 1L) }
    var alignment by remember { mutableStateOf(initialCharacter?.alignment ?: "Neutral Good") }
    var biography by remember { mutableStateOf(initialCharacter?.biography ?: "") }
    var avatarKey by remember { mutableStateOf(initialCharacter?.avatarKey ?: "knight") }
    var accentHex by remember { mutableStateOf(initialCharacter?.accentColorHex ?: "#E5B869") }
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
                    title = {
                        Text(
                            text = if (initialCharacter == null) "Forge Character" else "Edit Character",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("character_editor_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close editor")
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
                                val trimmedName = name.trim()
                                val trimmedRace = race.trim()
                                val trimmedClass = characterClass.trim()
                                when {
                                    trimmedName.isEmpty() -> validationError = "Please enter a character name."
                                    trimmedRace.isEmpty() -> validationError = "Please choose or enter a race."
                                    trimmedClass.isEmpty() -> validationError = "Please choose or enter a class."
                                    else -> {
                                        validationError = null
                                        onSave(
                                            initialCharacter?.id,
                                            trimmedName,
                                            trimmedRace,
                                            trimmedClass,
                                            title.trim(),
                                            level,
                                            alignment,
                                            biography.trim(),
                                            avatarKey,
                                            accentHex
                                        )
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_character_button")
                        ) {
                            Text(
                                text = if (isBusy) "Saving..." else if (initialCharacter == null) "Save Character to Roster" else "Update Character",
                                style = MaterialTheme.typography.labelLarge
                            )
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
                // Live Portrait Preview Card
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CharacterAvatar(
                            avatarKey = avatarKey,
                            accentColorHex = accentHex,
                            characterName = name.ifBlank { "Hero" },
                            size = 68.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { "Unnamed Wanderer" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (title.isNotBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = parseAccentColor(accentHex, MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                text = "Lv.$level ${race.ifBlank { "Race" }} ${characterClass.ifBlank { "Class" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Portrait & Crest Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Portrait & Crest",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoleplayPresets.avatarOptions.forEach { option ->
                            val selected = avatarKey == option.key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { avatarKey = option.key }
                                    .padding(6.dp)
                                    .testTag("avatar_option_${option.key}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = if (selected) 2.5.dp else 0.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            shape = CircleShape
                                        )
                                        .padding(3.dp)
                                ) {
                                    CharacterAvatar(
                                        avatarKey = option.key,
                                        accentColorHex = accentHex,
                                        characterName = option.label,
                                        size = 52.dp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Accent Color Swatches
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nameplate Aura Color",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoleplayPresets.accentColors.forEach { hex ->
                            val color = parseAccentColor(hex)
                            val selected = accentHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { accentHex = hex }
                                    .testTag("color_swatch_$hex"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected color",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Character Name *") },
                    placeholder = { Text("e.g., Seraphina Blackwood") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_name_input")
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("Title or Epithet (Optional)") },
                    placeholder = { Text("e.g., Keeper of the Ember Tome") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_title_input")
                )

                // Race input + presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = race,
                        onValueChange = { if (it.length <= 40) race = it },
                        label = { Text("Race / Ancestry *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("character_race_input")
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleplayPresets.races.forEach { preset ->
                            FilterChip(
                                selected = race.equals(preset, ignoreCase = true),
                                onClick = { race = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                }

                // Class input + presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = characterClass,
                        onValueChange = { if (it.length <= 40) characterClass = it },
                        label = { Text("Class / Vocation *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("character_class_input")
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleplayPresets.classes.forEach { preset ->
                            FilterChip(
                                selected = characterClass.equals(preset, ignoreCase = true),
                                onClick = { characterClass = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                }

                // Level stepper
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Character Level", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Range 1 – 100",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (level > 1) level-- },
                                modifier = Modifier.testTag("level_decrease_button")
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease level")
                            }
                            Text(
                                text = level.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (level < 100) level++ },
                                modifier = Modifier.testTag("level_increase_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase level")
                            }
                        }
                    }
                }

                // Alignment
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alignment", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleplayPresets.alignments.forEach { option ->
                            FilterChip(
                                selected = alignment == option,
                                onClick = { alignment = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = biography,
                    onValueChange = { if (it.length <= 1000) biography = it },
                    label = { Text("Biography & Backstory") },
                    placeholder = { Text("Describe their origins, mannerisms, flaws, and ambitions...") },
                    minLines = 4,
                    maxLines = 8,
                    supportingText = { Text("${biography.length}/1000") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_biography_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
