package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.signOut
import com.example.data.model.CharacterProfile
import com.example.data.repository.LoreGuildRepository
import com.example.ui.components.CharacterPickerSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharacterEditorDialog
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.CreateRealmDialog
import com.example.ui.screens.JoinByInviteDialog
import com.example.ui.screens.PendingJoinTarget
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RealmsScreen
import com.example.ui.screens.ServerRoomScreen
import com.example.ui.state.UiState
import com.example.ui.theme.LoreGuildTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ServerRoomViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_REALMS = "realms"
const val ROUTE_CHARACTERS = "characters"
const val ROUTE_PROFILE = "profile"
const val ROUTE_SERVER_ROOM = "server_room/{serverId}"

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(
        route = ROUTE_REALMS,
        label = "Realms",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    ),
    TopLevelDestination(
        route = ROUTE_CHARACTERS,
        label = "Characters",
        selectedIcon = Icons.Filled.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories
    ),
    TopLevelDestination(
        route = ROUTE_PROFILE,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoreGuildTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            LoreGuildAuthenticatedApp(
                currentUserId = uid,
                displayName = user.displayName ?: "Traveler",
                email = user.email,
                photoUrl = user.photoUrl?.toString()
            )
        }
    }
}

@Composable
private fun LoreGuildAuthenticatedApp(
    currentUserId: String,
    displayName: String,
    email: String?,
    photoUrl: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val mainViewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MainViewModel(
                    repository = LoreGuildRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialPhotoUrl = photoUrl
                )
            }
        }
    )

    val userProfileState by mainViewModel.userProfileState.collectAsStateWithLifecycle()
    val charactersState by mainViewModel.charactersState.collectAsStateWithLifecycle()
    val publicServersState by mainViewModel.publicServersState.collectAsStateWithLifecycle()
    val myServersState by mainViewModel.myServersState.collectAsStateWithLifecycle()
    val inviteLookupState by mainViewModel.inviteLookupState.collectAsStateWithLifecycle()
    val isBusy by mainViewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by mainViewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            snackbarHostState.showSnackbar(statusMessage!!)
            mainViewModel.clearStatusMessage()
        }
    }

    val characters = (charactersState as? UiState.Success)?.data.orEmpty()
    val userProfile = (userProfileState as? UiState.Success)?.data
    val activeCharacter = remember(characters, userProfile) {
        characters.firstOrNull { it.id == userProfile?.activeCharacterId } ?: characters.firstOrNull()
    }
    val myServers = (myServersState as? UiState.Success)?.data.orEmpty()

    var showCharacterEditor by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<CharacterProfile?>(null) }
    var showCreateRealmDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var pendingJoinTarget by remember { mutableStateOf<PendingJoinTarget?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopLevelNav = currentRoute in TOP_LEVEL_DESTINATIONS.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TOP_LEVEL_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(ROUTE_REALMS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TOP_LEVEL_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(ROUTE_REALMS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_REALMS
                    ) {
                        composable(ROUTE_REALMS) {
                            RealmsScreen(
                                currentUserId = currentUserId,
                                myServersState = myServersState,
                                publicServersState = publicServersState,
                                characters = characters,
                                activeCharacter = activeCharacter,
                                onOpenServerRoom = { serverId ->
                                    navController.navigate("server_room/$serverId")
                                },
                                onRequestCreateServer = {
                                    showCreateRealmDialog = true
                                },
                                onRequestJoinByCode = {
                                    mainViewModel.resetInviteLookup()
                                    showJoinByCodeDialog = true
                                },
                                onRequestJoinServerWithPicker = { target ->
                                    pendingJoinTarget = target
                                },
                                onNavigateToCharacters = {
                                    navController.navigate(ROUTE_CHARACTERS) {
                                        popUpTo(ROUTE_REALMS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_CHARACTERS) {
                            CharactersScreen(
                                charactersState = charactersState,
                                activeCharacterId = activeCharacter?.id,
                                onCreateCharacter = {
                                    editingCharacter = null
                                    showCharacterEditor = true
                                },
                                onEditCharacter = { char ->
                                    editingCharacter = char
                                    showCharacterEditor = true
                                },
                                onDeleteCharacter = { id ->
                                    mainViewModel.deleteCharacter(id)
                                },
                                onSetActiveCharacter = { id ->
                                    mainViewModel.setActiveCharacter(id)
                                }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                displayName = userProfile?.displayName ?: displayName,
                                email = email,
                                photoUrl = photoUrl,
                                characterCount = characters.size,
                                joinedServerCount = myServers.size,
                                activeCharacter = activeCharacter,
                                onManageCharacters = {
                                    navController.navigate(ROUTE_CHARACTERS) {
                                        popUpTo(ROUTE_REALMS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onSignOut = {
                                    signOut(credentialManager, scope)
                                }
                            )
                        }

                        composable(
                            route = ROUTE_SERVER_ROOM,
                            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val serverId = backStackEntry.arguments?.getString("serverId").orEmpty()
                            val roomViewModel: ServerRoomViewModel = viewModel(
                                key = "${currentUserId}_$serverId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        ServerRoomViewModel(
                                            repository = LoreGuildRepository(db),
                                            serverId = serverId,
                                            currentUserId = currentUserId
                                        )
                                    }
                                }
                            )
                            ServerRoomScreen(
                                viewModel = roomViewModel,
                                currentUserId = currentUserId,
                                playerDisplayName = userProfile?.displayName ?: displayName,
                                userCharacters = characters,
                                onCreateCharacterClicked = {
                                    editingCharacter = null
                                    showCharacterEditor = true
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCharacterEditor) {
        CharacterEditorDialog(
            initialCharacter = editingCharacter,
            isBusy = isBusy,
            onSave = { charId, name, race, charClass, title, level, alignment, bio, avatarKey, accentHex ->
                mainViewModel.saveCharacter(
                    characterId = charId,
                    name = name,
                    race = race,
                    characterClass = charClass,
                    title = title,
                    level = level,
                    alignment = alignment,
                    biography = bio,
                    avatarKey = avatarKey,
                    accentColorHex = accentHex,
                    onSuccess = {
                        showCharacterEditor = false
                        editingCharacter = null
                    }
                )
            },
            onDismiss = {
                showCharacterEditor = false
                editingCharacter = null
            }
        )
    }

    if (showCreateRealmDialog) {
        CreateRealmDialog(
            characters = characters,
            initialCharacter = activeCharacter,
            isBusy = isBusy,
            onRequestCreateCharacter = {
                editingCharacter = null
                showCharacterEditor = true
            },
            onCreateServer = { name, genre, desc, visibility, bannerKey, chosenChar ->
                mainViewModel.createServer(
                    playerName = userProfile?.displayName ?: displayName,
                    name = name,
                    genre = genre,
                    description = desc,
                    visibility = visibility,
                    bannerKey = bannerKey,
                    character = chosenChar,
                    onCreated = { serverId ->
                        showCreateRealmDialog = false
                        navController.navigate("server_room/$serverId")
                    }
                )
            },
            onDismiss = { showCreateRealmDialog = false }
        )
    }

    if (showJoinByCodeDialog) {
        JoinByInviteDialog(
            lookupState = inviteLookupState,
            onLookupCode = { code -> mainViewModel.lookupInvite(code) },
            onProceedWithInvite = { invite ->
                showJoinByCodeDialog = false
                val alreadyMember = myServers.any { it.id == invite.serverId }
                if (alreadyMember) {
                    navController.navigate("server_room/${invite.serverId}")
                } else {
                    pendingJoinTarget = PendingJoinTarget(
                        serverId = invite.serverId,
                        serverName = invite.serverName,
                        inviteCode = invite.inviteCode,
                        isPublic = invite.visibility == "public"
                    )
                }
            },
            onDismiss = {
                showJoinByCodeDialog = false
                mainViewModel.resetInviteLookup()
            }
        )
    }

    if (pendingJoinTarget != null) {
        val target = pendingJoinTarget!!
        CharacterPickerSheet(
            title = "Enter ${target.serverName}",
            subtitle = "Choose which character from your profile will represent you in this realm.",
            characters = characters,
            selectedCharacterId = activeCharacter?.id,
            onSelectCharacter = { chosen ->
                pendingJoinTarget = null
                mainViewModel.joinServer(
                    playerName = userProfile?.displayName ?: displayName,
                    serverId = target.serverId,
                    inviteCode = target.inviteCode,
                    isPublic = target.isPublic,
                    character = chosen,
                    onJoined = { joinedId ->
                        navController.navigate("server_room/$joinedId")
                    }
                )
            },
            onCreateCharacterClicked = {
                pendingJoinTarget = null
                editingCharacter = null
                showCharacterEditor = true
            },
            onDismiss = { pendingJoinTarget = null }
        )
    }
}
