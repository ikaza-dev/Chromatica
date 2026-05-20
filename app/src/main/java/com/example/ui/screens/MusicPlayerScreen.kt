package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.Track
import com.example.player.MusicPlayerViewModel
import com.example.ui.components.DynamicAlbumArt
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.model.TagExtractor
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val searchCategoryTab by viewModel.searchCategoryTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val permissionExplanationRequired by viewModel.permissionExplanationRequired.collectAsState()

    // Playlist states
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val playlistTracks by viewModel.playlistTracks.collectAsState()

    // Download states
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val downloadingTrackIds by viewModel.downloadingTrackIds.collectAsState()

    // Discover state
    val discoverTracks by viewModel.discoverTracks.collectAsState()

    // Screen dynamic listings
    val onlineSearchTracks by viewModel.onlineSearchTracks.collectAsState()
    val filteredLocalTracks by viewModel.filteredLocalTracks.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()

    // Stream playback states
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val isRepeatOneEnabled by viewModel.isRepeatOneEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var playlistToCreateName by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackForPlaylistSelection by remember { mutableStateOf<Track?>(null) }

    // Resolve storage permissions dynamically
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanLocalAudioFiles()
            viewModel.setExplanationFlag(false)
        } else {
            viewModel.setExplanationFlag(true)
        }
    }

    // Auto check/request on launch
    LaunchedEffect(Unit) {
        val permissionState = ContextCompat.checkSelfPermission(context, audioPermission)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            viewModel.scanLocalAudioFiles()
        } else {
            launcher.launch(audioPermission)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Underlying content wrapper with conditional alpha and click disabling when player is expanded
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = if (isPlayerExpanded) 0f else 1f)
                .then(
                    if (isPlayerExpanded) {
                        Modifier.pointerInput(Unit) {}
                    } else {
                        Modifier
                    }
                )
        ) {
            Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Section
                    MusicPlayerHeader()

                    when (activeTab) {
                        0 -> {
                            DiscoverScreen(
                                viewModel = viewModel,
                                discoverTracks = discoverTracks,
                                selectedTrack = selectedTrack,
                                isPlaying = isPlaying,
                                onTrackSelected = { viewModel.selectAndPlayTrack(it) }
                            )
                        }
                        1 -> {
                            // Search Page
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    placeholder = { Text("Search YouTube Music, or Device files...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = CircleShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    singleLine = true
                                )

                                // Category Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = searchCategoryTab == 0,
                                        onClick = { viewModel.setSearchCategoryTab(0) },
                                        label = { Text("Online Lounge") },
                                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    FilterChip(
                                        selected = searchCategoryTab == 1,
                                        onClick = {
                                            viewModel.setSearchCategoryTab(1)
                                            val state = ContextCompat.checkSelfPermission(context, audioPermission)
                                            if (state != PackageManager.PERMISSION_GRANTED) {
                                                launcher.launch(audioPermission)
                                            } else {
                                                viewModel.scanLocalAudioFiles()
                                            }
                                        },
                                        label = { Text("My Device") },
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                if (isSearchingOnline) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchCategoryTab == 1 && permissionExplanationRequired) {
                                        PermissionExplanationCard(
                                            onRequestPermission = { launcher.launch(audioPermission) }
                                        )
                                    } else {
                                        val displayTracks = if (searchCategoryTab == 0) onlineSearchTracks else filteredLocalTracks
                                        if (displayTracks.isEmpty()) {
                                            EmptyStateCard(
                                                isSearching = searchQuery.isNotEmpty(),
                                                isLocal = searchCategoryTab == 1,
                                                onScanFiles = { viewModel.scanLocalAudioFiles() }
                                            )
                                        } else {
                                            TrackLazyList(
                                                tracks = displayTracks,
                                                selectedTrack = selectedTrack,
                                                isPlaying = isPlaying,
                                                downloadedTracks = downloadedTracks,
                                                downloadingTrackIds = downloadingTrackIds,
                                                onTrackSelected = { viewModel.selectAndPlayTrack(it) },
                                                onDownloadClicked = { viewModel.downloadTrack(it) },
                                                onAddToPlaylistClicked = { trackForPlaylistSelection = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Playlists Page
                            if (selectedPlaylist == null) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "My Playlists",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Button(
                                            onClick = { showCreatePlaylistDialog = true },
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "New")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("New")
                                        }
                                    }

                                    if (playlists.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.QueueMusic,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(64.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "No playlists created yet",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Tap 'New' to compile custom lounges",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(playlists) { playlist ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.selectPlaylist(playlist) },
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = playlist.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 16.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Custom Session Playlist",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Playlist Details list
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedPlaylist?.name ?: "",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "${playlistTracks.size} Tracks inside",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (playlistTracks.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(56.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "This custom list is empty",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Search and tap + to compile here.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.weight(1f)) {
                                            TrackLazyList(
                                                tracks = playlistTracks,
                                                selectedTrack = selectedTrack,
                                                isPlaying = isPlaying,
                                                downloadedTracks = downloadedTracks,
                                                downloadingTrackIds = downloadingTrackIds,
                                                onTrackSelected = { viewModel.selectAndPlayTrack(it) },
                                                onDownloadClicked = { viewModel.downloadTrack(it) },
                                                onAddToPlaylistClicked = null, // Already inside
                                                onDeletePlaylistTrackClicked = { trackId ->
                                                    viewModel.removeTrackFromPlaylist(selectedPlaylist!!.id, trackId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }

        // Floating Nav & Player overlay container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Small Bottom Player bar
            AnimatedVisibility(
                visible = selectedTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                selectedTrack?.let { track ->
                    BottomPlayerCard(
                        track = track,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.playNextTrack() },
                        onExpand = { isPlayerExpanded = true }
                    )
                }
            }

            // floating island nav bar
            FloatingIslandNavigationBar(
                activeTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        }
        }

        // Full Screen Player Overlay View
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                selectedTrack?.let { track ->
                    FullScreenPlayerStage(
                        track = track,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        isShuffleEnabled = isShuffleEnabled,
                        isRepeatOneEnabled = isRepeatOneEnabled,
                        onSeek = { viewModel.seekTo(it) },
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.playNextTrack() },
                        onSkipPrevious = { viewModel.playPreviousTrack() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeatOne = { viewModel.toggleRepeatOne() },
                        onMinimize = { isPlayerExpanded = false }
                    )
                }
            }
        }

        // Create Playlist Dialog Option
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("New Session Playlist") },
                text = {
                    OutlinedTextField(
                        value = playlistToCreateName,
                        onValueChange = { playlistToCreateName = it },
                        placeholder = { Text("Chillhop, Lounge, Synthwave...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createPlaylist(playlistToCreateName)
                            playlistToCreateName = ""
                            showCreatePlaylistDialog = false
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Song to Playlist Dialog
        if (trackForPlaylistSelection != null) {
            AlertDialog(
                onDismissRequest = { trackForPlaylistSelection = null },
                title = { Text("Add Track to Playlist") },
                text = {
                    Column {
                        Text(
                            text = "Add \"${trackForPlaylistSelection?.title}\" to compile lounge collections:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (playlists.isEmpty()) {
                            Text(
                                "No playlists created yet",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(playlists) { playlist ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addTrackToPlaylist(playlist.id, trackForPlaylistSelection!!)
                                                trackForPlaylistSelection = null
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = playlist.name,
                                            modifier = Modifier.padding(12.dp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { trackForPlaylistSelection = null }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

@Composable
fun MusicPlayerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Harmony HD",
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Your sleek audiophile lounge",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        }
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun TrackLazyList(
    tracks: List<Track>,
    selectedTrack: Track?,
    isPlaying: Boolean,
    downloadedTracks: List<Track>,
    downloadingTrackIds: Set<String>,
    onTrackSelected: (Track) -> Unit,
    onDownloadClicked: ((Track) -> Unit)?,
    onAddToPlaylistClicked: ((Track) -> Unit)?,
    onDeleteDownloadClicked: ((String) -> Unit)? = null,
    onDeletePlaylistTrackClicked: ((String) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            val isCurrent = track.id == selectedTrack?.id
            val isDownloaded = downloadedTracks.any { it.id == track.id }
            val isDownloading = downloadingTrackIds.contains(track.id)

            val containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTrackSelected(track) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DynamicAlbumArt(
                        title = track.title,
                        artist = track.artist,
                        thumbnailUrl = track.thumbnailUrl,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 11.sp,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCurrent) {
                            AnimatedEqualizerVisualizer(
                                isPlaying = isPlaying,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        // Add to playlist option
                        if (onAddToPlaylistClicked != null) {
                            IconButton(onClick = { onAddToPlaylistClicked(track) }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Track to custom compilation playlist",
                                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Download option
                        if (onDownloadClicked != null) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(
                                    onClick = { onDownloadClicked(track) },
                                    enabled = !isDownloaded
                                ) {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Default.Done else Icons.Default.Download,
                                        contentDescription = if (isDownloaded) "Saved and synced offline" else "Download to storage",
                                        tint = if (isDownloaded) Color(0xFF10B981) else if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Delete downloadedtrack/playlist track
                        if (onDeleteDownloadClicked != null) {
                            IconButton(onClick = { onDeleteDownloadClicked(track.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete from Device",
                                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (onDeletePlaylistTrackClicked != null) {
                            IconButton(onClick = { onDeletePlaylistTrackClicked(track.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Remove From Playlist",
                                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedEqualizerVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(20.dp)
            .height(14.dp)
            .padding(end = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val transition = rememberInfiniteTransition(label = "equalizer")
        
        val bouncingValues = listOf(
            animateEqualizerBar(transition, if (isPlaying) 0.85f else 0.15f, 500),
            animateEqualizerBar(transition, if (isPlaying) 1.0f else 0.1f, 350),
            animateEqualizerBar(transition, if (isPlaying) 0.70f else 0.2f, 650)
        )

        bouncingValues.forEach { heightScale ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightScale)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun animateEqualizerBar(
    transition: InfiniteTransition,
    targetVal: Float,
    durationMs: Int
): Float {
    return if (targetVal <= 0.2f) {
        targetVal
    } else {
        val anim by transition.animateFloat(
            initialValue = 0.15f,
            targetValue = targetVal,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar"
        )
        anim
    }
}

@Composable
fun BottomPlayerCard(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun FullScreenPlayerStage(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffleEnabled: Boolean,
    isRepeatOneEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeatOne: () -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    )

    var dragProgress by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicAlbumArt(
                    title = track.title,
                    artist = track.artist,
                    thumbnailUrl = track.thumbnailUrl,
                    modifier = Modifier
                        .sizeIn(minWidth = 240.dp, minHeight = 240.dp, maxWidth = 300.dp, maxHeight = 300.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                val resolvedProgress = dragProgress ?: if (durationMs > 0) {
                    currentPositionMs.toFloat() / durationMs.toFloat()
                } else {
                    0f
                }

                Slider(
                    value = resolvedProgress.coerceIn(0f, 1f),
                    onValueChange = { dragProgress = it },
                    onValueChangeFinished = {
                        val newPosition = ((dragProgress ?: 0f) * durationMs).toLong()
                        onSeek(newPosition)
                        dragProgress = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentDisplayStr = if (dragProgress != null) {
                        formatDuration(((dragProgress ?: 0f) * durationMs).toLong())
                    } else {
                        formatDuration(currentPositionMs)
                    }

                    Text(
                        text = currentDisplayStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatDuration(durationMs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Toggle Shuffle",
                        modifier = Modifier.size(24.dp),
                        tint = if (isShuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        }
                    )
                }

                IconButton(onClick = onSkipPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = onToggleRepeatOne) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Loop Mode",
                        modifier = Modifier.size(24.dp),
                        tint = if (isRepeatOneEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Showcase your local music collection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We need audio storage read capability to locate and scan your files. Alternatively, enjoy the curated Online Lounge stream anytime!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                shape = CircleShape
            ) {
                Text("Allow Storage Discovery")
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    isSearching: Boolean,
    isLocal: Boolean,
    onScanFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "No matches found" else "Your collection is silent",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSearching) "Try modifying your search query filters." else if (isLocal) "No audio tracks were discovered locally on this device. Plug space card streams above!" else "Curated lounge selections are streaming.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            if (isLocal && !isSearching) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onScanFiles,
                    shape = CircleShape
                ) {
                    Text("Rescan Disk Files")
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000) % 60
    val hours = ms / 3600000

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun DiscoverScreen(
    viewModel: MusicPlayerViewModel,
    discoverTracks: List<Track>,
    selectedTrack: Track?,
    isPlaying: Boolean,
    onTrackSelected: (Track) -> Unit
) {
    val gridState = rememberLazyGridState()

    // Trigger loadMoreDiscoverTracks when scrolling near bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadDiscoverTracks(reset = false)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discover Lounge",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Text badge for smart personalized taste based on listening scoring
            Text(
                text = "TASTE CURATED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = discoverTracks,
                key = { it.id }
            ) { track ->
                DiscoverTrackItem(
                    track = track,
                    isSelected = selectedTrack?.id == track.id || selectedTrack?.id?.startsWith(track.id.substringBefore("_p")) == true,
                    onClick = { onTrackSelected(track) }
                )
            }
        }
    }
}

@Composable
fun DiscoverTrackItem(
    track: Track,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (track.thumbnailUrl != null) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = "Cover Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = track.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            
            Text(
                text = track.artist,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            )

            val tags = TagExtractor.extractTags(track.title, track.artist)
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.take(1).forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingIslandNavigationBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavItem(
                icon = Icons.Default.Explore,
                label = "Discover",
                selected = activeTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            FloatingNavItem(
                icon = Icons.Default.Search,
                label = "Search",
                selected = activeTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
            FloatingNavItem(
                icon = Icons.Default.LibraryMusic,
                label = "Playlists",
                selected = activeTab == 2,
                onClick = { onTabSelected(2) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FloatingNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(250)
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(250)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background active pill
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(alpha = pillAlpha)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
        }
    }
}
