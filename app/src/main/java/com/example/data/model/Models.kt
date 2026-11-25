package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val activeCharacterId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CharacterProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val race: String = "",
    val characterClass: String = "",
    val title: String = "",
    val level: Long = 1L,
    val alignment: String = "Neutral Good",
    val biography: String = "",
    val avatarKey: String = "knight",
    val accentColorHex: String = "#E5B869",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoleplayServer(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val genre: String = "",
    val description: String = "",
    val visibility: String = ServerVisibility.PUBLIC.raw,
    val inviteCode: String = "",
    val bannerKey: String = "tavern",
    val memberCount: Long = 1L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedVisibility: ServerVisibility
        get() = ServerVisibility.fromRaw(visibility)
}

@Keep
@IgnoreExtraProperties
data class ServerMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val characterId: String = "",
    val characterName: String = "",
    val characterRace: String = "",
    val characterClass: String = "",
    val characterTitle: String = "",
    val characterAvatarKey: String = "knight",
    val characterAccentHex: String = "#E5B869",
    val role: String = "member",
    val joinedWithCode: String? = null,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoleplayMessage(
    @DocumentId val id: String = "",
    val serverId: String = "",
    val senderUid: String = "",
    val characterId: String = "",
    val characterName: String = "",
    val characterRace: String = "",
    val characterClass: String = "",
    val characterAvatarKey: String = "knight",
    val characterAccentHex: String = "#E5B869",
    val messageType: String = MessageType.DIALOGUE.raw,
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val parsedType: MessageType
        get() = MessageType.fromRaw(messageType)
}

@Keep
@IgnoreExtraProperties
data class ServerInvite(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val serverId: String = "",
    val serverName: String = "",
    val genre: String = "",
    val visibility: String = ServerVisibility.PUBLIC.raw,
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class ServerVisibility(val raw: String, val label: String) {
    PUBLIC("public", "Public"),
    PRIVATE("private", "Private");

    companion object {
        fun fromRaw(raw: String): ServerVisibility =
            entries.firstOrNull { it.raw == raw } ?: PUBLIC
    }
}

enum class MessageType(val raw: String, val label: String) {
    DIALOGUE("dialogue", "In-Character"),
    ACTION("action", "Action"),
    OOC("ooc", "OOC"),
    ROLL("roll", "Dice Roll");

    companion object {
        fun fromRaw(raw: String): MessageType =
            entries.firstOrNull { it.raw == raw } ?: DIALOGUE
    }
}

object RoleplayPresets {
    val races = listOf(
        "Human", "High Elf", "Tiefling", "Dwarf", "Dragonborn",
        "Half-Orc", "Android", "Starfarer", "Changeling", "Vampire"
    )

    val classes = listOf(
        "Spellblade", "Arcane Mage", "Shadow Rogue", "Paladin", "Bard",
        "Ranger", "Cleric", "Artificer", "Netrunner", "Chronomancer"
    )

    val alignments = listOf(
        "Lawful Good", "Neutral Good", "Chaotic Good",
        "Lawful Neutral", "True Neutral", "Chaotic Neutral",
        "Lawful Evil", "Neutral Evil", "Chaotic Evil"
    )

    val genres = listOf(
        "High Fantasy", "Dark Fantasy", "Sci-Fi & Cyberpunk",
        "Gothic Mystery", "Tavern Slice-of-Life", "Steampunk"
    )

    data class AvatarOption(val key: String, val label: String)

    val avatarOptions = listOf(
        AvatarOption("knight", "Vanguard"),
        AvatarOption("mage", "Arcanist"),
        AvatarOption("crest_ember", "Ember Crest"),
        AvatarOption("crest_moon", "Moon Crest"),
        AvatarOption("crest_forest", "Verdant Crest"),
        AvatarOption("crest_crimson", "Crimson Crest")
    )

    val accentColors = listOf(
        "#E5B869",
        "#6EC6B8",
        "#E07A82",
        "#9E8DF2",
        "#78C272",
        "#F09D63"
    )

    data class BannerOption(val key: String, val label: String)

    val bannerOptions = listOf(
        BannerOption("tavern", "Lantern Guildhall"),
        BannerOption("citadel", "Obsidian Citadel")
    )
}
