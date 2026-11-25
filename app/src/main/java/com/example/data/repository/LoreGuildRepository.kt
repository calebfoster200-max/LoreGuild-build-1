package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageType
import com.example.data.model.RoleplayMessage
import com.example.data.model.RoleplayServer
import com.example.data.model.ServerInvite
import com.example.data.model.ServerMember
import com.example.data.model.ServerVisibility
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class LoreGuildRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val random = SecureRandom()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(
        displayName: String,
        photoUrl: String?
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "Traveler" }.take(60)
            val cleanPhoto = photoUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "photoUrl" to cleanPhoto,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanPhoto != null) {
                    updatePayload["photoUrl"] = cleanPhoto
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) return@flow
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.GET, docRef.path, userId)
                }
        )
    }

    fun observeCharacters(userId: String): Flow<List<CharacterProfile>> = flow {
        if (userId.isBlank()) return@flow
        val path = "users/$userId/characters"
        emitAll(
            db.collection("users").document(userId).collection("characters")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CharacterProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun saveCharacter(
        userId: String,
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
    ): Result<String> {
        var docPath = "users/$userId/characters"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val cleanName = name.trim().take(50)
            val cleanRace = race.trim().take(40)
            val cleanClass = characterClass.trim().take(40)
            val cleanTitle = title.trim().take(60)
            val cleanLevel = level.coerceIn(1L, 100L)
            val cleanAlignment = alignment.trim().ifBlank { "True Neutral" }.take(30)
            val cleanBio = biography.trim().take(1000)
            val cleanAvatar = avatarKey.trim().ifBlank { "knight" }.take(40)
            val cleanColor = if (accentColorHex.matches(Regex("^#[0-9A-Fa-f]{6,8}$"))) {
                accentColorHex
            } else {
                "#E5B869"
            }

            require(cleanName.isNotEmpty()) { "Character name is required." }
            require(cleanRace.isNotEmpty()) { "Character race is required." }
            require(cleanClass.isNotEmpty()) { "Character class is required." }

            val charactersCol = db.collection("users").document(uid).collection("characters")
            val isNew = characterId.isNullOrBlank()
            val docRef = if (isNew) charactersCol.document() else charactersCol.document(characterId)
            docPath = docRef.path

            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "race" to cleanRace,
                    "characterClass" to cleanClass,
                    "title" to cleanTitle,
                    "level" to cleanLevel,
                    "alignment" to cleanAlignment,
                    "biography" to cleanBio,
                    "avatarKey" to cleanAvatar,
                    "accentColorHex" to cleanColor,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()

                // Auto-select if user has no activeCharacterId yet
                val userDoc = db.collection("users").document(uid).get().await()
                if (userDoc.exists() && userDoc.getString("activeCharacterId").isNullOrBlank()) {
                    db.collection("users").document(uid).update(
                        mapOf(
                            "activeCharacterId" to docRef.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "race" to cleanRace,
                        "characterClass" to cleanClass,
                        "title" to cleanTitle,
                        "level" to cleanLevel,
                        "alignment" to cleanAlignment,
                        "biography" to cleanBio,
                        "avatarKey" to cleanAvatar,
                        "accentColorHex" to cleanColor,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (characterId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(userId: String, characterId: String): Result<Unit> {
        var docPath = "users/$userId/characters/$characterId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(characterId.isNotBlank()) { "Character ID required." }
            val docRef = db.collection("users").document(uid).collection("characters").document(characterId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setActiveCharacter(userId: String, characterId: String): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(characterId.isNotBlank()) { "Character ID required." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "activeCharacterId" to characterId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observePublicServers(): Flow<List<RoleplayServer>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("servers")
                .whereEqualTo("visibility", ServerVisibility.PUBLIC.raw)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoleplayServer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.LIST, "servers", uid)
                }
        )
    }

    fun observeMyServers(userId: String): Flow<List<RoleplayServer>> = flow {
        if (userId.isBlank()) return@flow
        emitAll(
            db.collection("servers")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoleplayServer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.LIST, "servers", userId)
                }
        )
    }

    fun observeServer(serverId: String): Flow<RoleplayServer?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("servers").document(serverId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        RoleplayServer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.GET, docRef.path, uid)
                }
        )
    }

    fun observeServerMembers(serverId: String): Flow<List<ServerMember>> = flow {
        val uid = requireUserId()
        val path = "servers/$serverId/members"
        emitAll(
            db.collection("servers").document(serverId).collection("members")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ServerMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.characterName.lowercase() }
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeServerMessages(serverId: String): Flow<List<RoleplayMessage>> = flow {
        val uid = requireUserId()
        val path = "servers/$serverId/messages"
        emitAll(
            db.collection("servers").document(serverId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(150)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoleplayMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logAndRethrowStreamError(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun createServer(
        userId: String,
        playerName: String,
        name: String,
        genre: String,
        description: String,
        visibility: ServerVisibility,
        bannerKey: String,
        character: CharacterProfile
    ): Result<String> {
        var docPath = "servers"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val cleanName = name.trim().take(60)
            val cleanGenre = genre.trim().ifBlank { "High Fantasy" }.take(40)
            val cleanDesc = description.trim().take(400)
            val cleanOwnerName = playerName.trim().ifBlank { "Host" }.take(60)
            val cleanBanner = bannerKey.trim().ifBlank { "tavern" }.take(40)

            require(cleanName.length >= 2) { "Realm name must be at least 2 characters." }
            require(character.id.isNotBlank()) { "Select a character before creating a realm." }

            val inviteCode = generateInviteCode()
            val serverRef = db.collection("servers").document()
            docPath = serverRef.path
            val memberRef = serverRef.collection("members").document(uid)
            val inviteRef = db.collection("server_invites").document(inviteCode)

            val batch = db.batch()
            batch.set(
                serverRef,
                mapOf(
                    "ownerId" to uid,
                    "ownerName" to cleanOwnerName,
                    "name" to cleanName,
                    "genre" to cleanGenre,
                    "description" to cleanDesc,
                    "visibility" to visibility.raw,
                    "inviteCode" to inviteCode,
                    "bannerKey" to cleanBanner,
                    "memberCount" to 1L,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                memberRef,
                mapOf(
                    "userId" to uid,
                    "playerName" to cleanOwnerName,
                    "characterId" to character.id,
                    "characterName" to character.name.take(50),
                    "characterRace" to character.race.take(40),
                    "characterClass" to character.characterClass.take(40),
                    "characterTitle" to character.title.take(60),
                    "characterAvatarKey" to character.avatarKey.take(40),
                    "characterAccentHex" to character.accentColorHex,
                    "role" to "owner",
                    "createdAt" to null
                ).filterKeys { it != "createdAt" } + mapOf(
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                inviteRef,
                mapOf(
                    "inviteCode" to inviteCode,
                    "serverId" to serverRef.id,
                    "serverName" to cleanName,
                    "genre" to cleanGenre,
                    "visibility" to visibility.raw,
                    "ownerId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(serverRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun lookupInvite(rawCode: String): Result<ServerInvite?> {
        val cleanCode = rawCode.trim().uppercase()
        val docPath = "server_invites/$cleanCode"
        return try {
            requireUserId()
            require(cleanCode.matches(Regex("^[A-Z0-9]{6,12}$"))) {
                "Invite codes are 6-12 letters or numbers."
            }
            val snap = db.collection("server_invites").document(cleanCode).get().await()
            val invite = snap.toObject(
                ServerInvite::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(invite)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinServer(
        userId: String,
        playerName: String,
        serverId: String,
        inviteCode: String,
        isPublic: Boolean,
        character: CharacterProfile
    ): Result<Unit> {
        val serverRef = db.collection("servers").document(serverId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(serverId.isNotBlank()) { "Server ID is required." }
            require(character.id.isNotBlank()) { "Please select a character to enter the realm." }

            val cleanPlayerName = playerName.trim().ifBlank { "Traveler" }.take(60)
            val codeValue = if (isPublic) "PUBLIC" else inviteCode.trim().uppercase()

            val memberRef = serverRef.collection("members").document(uid)
            val batch = db.batch()
            batch.set(
                memberRef,
                mapOf(
                    "userId" to uid,
                    "playerName" to cleanPlayerName,
                    "characterId" to character.id,
                    "characterName" to character.name.take(50),
                    "characterRace" to character.race.take(40),
                    "characterClass" to character.characterClass.take(40),
                    "characterTitle" to character.title.take(60),
                    "characterAvatarKey" to character.avatarKey.take(40),
                    "characterAccentHex" to character.accentColorHex,
                    "role" to "member",
                    "joinedWithCode" to codeValue,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                serverRef,
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, serverRef.path)
            Result.failure(e)
        }
    }

    suspend fun switchServerCharacter(
        userId: String,
        playerName: String,
        serverId: String,
        character: CharacterProfile
    ): Result<Unit> {
        val memberRef = db.collection("servers").document(serverId).collection("members").document(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(character.id.isNotBlank()) { "Character ID required." }

            memberRef.update(
                mapOf(
                    "playerName" to playerName.trim().ifBlank { "Traveler" }.take(60),
                    "characterId" to character.id,
                    "characterName" to character.name.take(50),
                    "characterRace" to character.race.take(40),
                    "characterClass" to character.characterClass.take(40),
                    "characterTitle" to character.title.take(60),
                    "characterAvatarKey" to character.avatarKey.take(40),
                    "characterAccentHex" to character.accentColorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveServer(userId: String, serverId: String): Result<Unit> {
        val serverRef = db.collection("servers").document(serverId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val memberRef = serverRef.collection("members").document(uid)
            val batch = db.batch()
            batch.delete(memberRef)
            batch.update(
                serverRef,
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "memberCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, serverRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteServer(userId: String, serverId: String, inviteCode: String): Result<Unit> {
        val serverRef = db.collection("servers").document(serverId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val batch = db.batch()
            if (inviteCode.isNotBlank()) {
                batch.delete(db.collection("server_invites").document(inviteCode))
            }
            batch.delete(serverRef.collection("members").document(uid))
            batch.delete(serverRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, serverRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        userId: String,
        serverId: String,
        member: ServerMember,
        messageType: MessageType,
        content: String
    ): Result<Unit> {
        val messagesCol = db.collection("servers").document(serverId).collection("messages")
        val docRef = messagesCol.document()
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanContent = content.trim().take(1500)
            require(cleanContent.isNotEmpty()) { "Message cannot be empty." }

            docRef.set(
                mapOf(
                    "serverId" to serverId,
                    "senderUid" to uid,
                    "characterId" to member.characterId,
                    "characterName" to member.characterName,
                    "characterRace" to member.characterRace,
                    "characterClass" to member.characterClass,
                    "characterAvatarKey" to member.characterAvatarKey,
                    "characterAccentHex" to member.characterAccentHex,
                    "messageType" to messageType.raw,
                    "content" to cleanContent,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(serverId: String, messageId: String): Result<Unit> {
        val docRef = db.collection("servers").document(serverId).collection("messages").document(messageId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(length: Int = 7): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(length) {
            repeat(length) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    private fun logAndRethrowStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
