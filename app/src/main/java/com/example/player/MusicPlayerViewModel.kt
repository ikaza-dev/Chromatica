package com.example.player

import android.app.Application
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.Playlist
import com.example.model.Track
import com.example.model.TagExtractor
import com.example.data.TagScore
import com.example.network.InvidiousClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = MusicRepository(context)

    // Bridge PlaybackManager states
    val currentTrackList = PlaybackManager.currentTrackList
    val selectedTrack = PlaybackManager.selectedTrack
    val isPlaying = PlaybackManager.isPlaying
    val currentPositionMs = PlaybackManager.currentPositionMs
    val durationMs = PlaybackManager.durationMs
    val isShuffleEnabled = PlaybackManager.isShuffleEnabled
    val isRepeatOneEnabled = PlaybackManager.isRepeatOneEnabled
    val isLoading = PlaybackManager.isLoading

    // Tag Score map state
    val userTagScores: StateFlow<Map<String, Int>> = repository.allTagScores
        .map { list -> list.associate { it.tag to it.score } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    // Discover List visual tracks
    private val _discoverTracks = MutableStateFlow<List<Track>>(emptyList())
    val discoverTracks: StateFlow<List<Track>> = _discoverTracks.asStateFlow()

    private var discoverCurrentPage = 1

    val discoverSeedTracks = listOf(
        Track(
            id = "seed_1",
            title = "Lofi Study Drift",
            artist = "Acoustic Dreams",
            album = "Study Cabin Vol. I",
            duration = 277000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400&auto=format&fit=crop",
            genreTags = "Lo-Fi"
        ),
        Track(
            id = "seed_2",
            title = "Cyber Synth Overdrive",
            artist = "Retro Cruiser",
            album = "Neo Tokyo Drive",
            duration = 373000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1614149162883-504ce4d13909?w=400&auto=format&fit=crop",
            genreTags = "Electronic"
        ),
        Track(
            id = "seed_3",
            title = "Midnight Coffee House",
            artist = "Chillhop Project",
            album = "Late Night Vibe Sesh",
            duration = 544000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400&auto=format&fit=crop",
            genreTags = "Lo-Fi"
        ),
        Track(
            id = "seed_4",
            title = "J-Pop Sakura Bloom",
            artist = "Hanami Circle",
            album = "Tokyo Springtime",
            duration = 345000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=400&auto=format&fit=crop",
            genreTags = "J-Pop"
        ),
        Track(
            id = "seed_5",
            title = "Neon Tokyo Nights",
            artist = "Harajuku Glitch",
            album = "Shibuya Crossing",
            duration = 420000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=400&auto=format&fit=crop",
            genreTags = "J-Pop"
        ),
        Track(
            id = "seed_6",
            title = "Thrash of the Titans",
            artist = "Iron Eclipse",
            album = "Doomsday Device",
            duration = 310000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop",
            genreTags = "Metal"
        ),
        Track(
            id = "seed_7",
            title = "Heavy Metal Thunder",
            artist = "Anarchy Core",
            album = "Steel Requiem",
            duration = 295000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop",
            genreTags = "Metal"
        ),
        Track(
            id = "seed_8",
            title = "Sonata in G Minor",
            artist = "Amadeus Ensemble",
            album = "Grand Symphony",
            duration = 480000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400&auto=format&fit=crop",
            genreTags = "Classical"
        ),
        Track(
            id = "seed_9",
            title = "Midnight Jazz Lounge",
            artist = "Blue Note Trio",
            album = "Smoky Sax Session",
            duration = 385000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=400&auto=format&fit=crop",
            genreTags = "Jazz"
        ),
        Track(
            id = "seed_10",
            title = "Retro Cyber Punk",
            artist = "Vapor Hacker",
            album = "Hack the Matrix",
            duration = 320000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop",
            genreTags = "Synth"
        ),
        Track(
            id = "seed_11",
            title = "Hyper Pop Diamond",
            artist = "Glitter Dream",
            album = "Sugar Crash Party",
            duration = 210000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=400&auto=format&fit=crop",
            genreTags = "Pop"
        ),
        Track(
            id = "seed_12",
            title = "Subway Street Beat",
            artist = "MC Shadow",
            album = "Underground Flow",
            duration = 280000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&auto=format&fit=crop",
            genreTags = "Hip Hop"
        ),
        Track(
            id = "seed_13",
            title = "Sunny Chill Lofi",
            artist = "Melodic Beats",
            album = "Golden Hour Vibes",
            duration = 240000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1494905998402-395d579af36f?w=400&auto=format&fit=crop",
            genreTags = "Lo-Fi"
        ),
        Track(
            id = "seed_14",
            title = "Vaporwave Retro Mall",
            artist = "Glitch Nostalgia",
            album = "Floor 1999",
            duration = 315000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&auto=format&fit=crop",
            genreTags = "Electronic"
        ),
        Track(
            id = "seed_15",
            title = "Harajuku Candy Rush",
            artist = "Kawaii Pop Division",
            album = "Nippon Popstar",
            duration = 290000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=400&auto=format&fit=crop",
            genreTags = "J-Pop"
        ),
        Track(
            id = "seed_16",
            title = "Rebellion of the Riffs",
            artist = "Screaming Phoenix",
            album = "Ashes of Noise",
            duration = 330000,
            data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3",
            isDemo = true,
            thumbnailUrl = "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=400&auto=format&fit=crop",
            genreTags = "Metal"
        )
    )

    fun loadDiscoverTracks(reset: Boolean = false) {
        if (reset) {
            discoverCurrentPage = 1
        }
        val scores = userTagScores.value
        val newTracks = generateTracksForPage(discoverCurrentPage, scores)
        if (reset) {
            _discoverTracks.value = newTracks
        } else {
            _discoverTracks.value = _discoverTracks.value + newTracks
        }
        discoverCurrentPage++
    }

    private fun generateTracksForPage(page: Int, scores: Map<String, Int>): List<Track> {
        val allExpandedTracks = mutableListOf<Track>()
        for (i in 0 until 12) {
            discoverSeedTracks.forEach { seed ->
                val uniqueId = "${seed.id}_p${page}_it${i}"
                allExpandedTracks.add(seed.copy(id = uniqueId))
            }
        }
        
        val sortedList = allExpandedTracks.sortedWith(
            compareByDescending<Track> { track ->
                val trackTags = TagExtractor.extractTags(track.title, track.artist)
                trackTags.sumOf { t -> scores[t] ?: 0 }
            }.thenBy { track ->
                kotlin.math.abs((track.title + track.id).hashCode())
            }
        )
        
        val pageSize = 18
        val startIndex = ((page - 1) * pageSize) % sortedList.size
        val result = mutableListOf<Track>()
        for (idx in 0 until pageSize) {
            val itemIdx = (startIndex + idx) % sortedList.size
            val track = sortedList[itemIdx]
            result.add(track.copy(id = "${track.id}_pg${page}"))
        }
        return result
    }

    // Navigation and Filtering tabs
    private val _activeTab = MutableStateFlow(0) // 0 for Discover, 1 for Search, 2 for Playlists
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _searchCategoryTab = MutableStateFlow(0) // 0 for YouTube, 1 for Device local files
    val searchCategoryTab: StateFlow<Int> = _searchCategoryTab.asStateFlow()

    // Query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Local device tracks scan state
    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _filteredLocalTracks = MutableStateFlow<List<Track>>(emptyList())
    val filteredLocalTracks: StateFlow<List<Track>> = _filteredLocalTracks.asStateFlow()

    // Online search tracks state
    private val _onlineSearchTracks = MutableStateFlow<List<Track>>(emptyList())
    val onlineSearchTracks: StateFlow<List<Track>> = _onlineSearchTracks.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    // Database states
    val downloadedTracks: StateFlow<List<Track>> = repository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _playlistTracks = MutableStateFlow<List<Track>>(emptyList())
    val playlistTracks: StateFlow<List<Track>> = _playlistTracks.asStateFlow()

    // Downloading states
    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _permissionExplanationRequired = MutableStateFlow(false)
    val permissionExplanationRequired: StateFlow<Boolean> = _permissionExplanationRequired.asStateFlow()

    init {
        PlaybackManager.init(context)
        loadDemoLoungeTracks()
        
        // Load initial Discover tracks
        loadDiscoverTracks(reset = true)
        
        // Listen to tag score changes and update discover tracks automatically
        viewModelScope.launch {
            userTagScores.collect {
                loadDiscoverTracks(reset = true)
            }
        }
        
        // React to search queries
        viewModelScope.launch {
            _searchQuery.collect { query ->
                filterLocalTracks(query)
            }
        }
    }

    private fun loadDemoLoungeTracks() {
        val list = listOf(
            Track(
                id = "demo_1",
                title = "Lofi Study Drift",
                artist = "Acoustic Dreams",
                album = "Study Cabin Vol. I",
                duration = 277000,
                data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                isDemo = true
            ),
            Track(
                id = "demo_2",
                title = "Cyber Synth Overdrive",
                artist = "Retro Cruiser",
                album = "Neo Tokyo Drive",
                duration = 373000,
                data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                isDemo = true
            ),
            Track(
                id = "demo_3",
                title = "Midnight Coffee House",
                artist = "Chillhop Project",
                album = "Late Night Vibe Sesh",
                duration = 544000,
                data = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                isDemo = true
            )
        )
        _onlineSearchTracks.value = list
    }

    // Set Active UI Nav Tabs
    fun setActiveTab(index: Int) {
        _activeTab.value = index
    }

    fun setSearchCategoryTab(index: Int) {
        _searchCategoryTab.value = index
        _searchQuery.value = ""
        if (index == 0 && _onlineSearchTracks.value.firstOrNull()?.isDemo == false && _onlineSearchTracks.value.isEmpty()) {
            loadDemoLoungeTracks()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            if (_searchCategoryTab.value == 0) {
                loadDemoLoungeTracks()
            }
        } else {
            if (_searchCategoryTab.value == 0) {
                performOnlineYouTubeSearch(query.trim())
            }
        }
    }

    private fun filterLocalTracks(query: String) {
        val baseList = _localTracks.value
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            _filteredLocalTracks.value = baseList
        } else {
            _filteredLocalTracks.value = baseList.filter {
                it.title.lowercase().contains(cleanQuery) || it.artist.lowercase().contains(cleanQuery)
            }
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    private fun performOnlineYouTubeSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Delay to buffer typed letters
            kotlinx.coroutines.delay(600)
            _isSearchingOnline.value = true
            try {
                val results = withContext(Dispatchers.IO) {
                    InvidiousClient.searchTracks(query)
                }
                val tracks = results.filter { it.videoId != null && it.title != null }.map { res ->
                    val titleText = res.title!!
                    val artistText = res.author ?: "Unknown Artist"
                    val extractedTagsList = TagExtractor.extractTags(titleText, artistText)
                    val tagsString = extractedTagsList.joinToString(",")
                    Track(
                        id = res.videoId!!,
                        title = titleText,
                        artist = artistText,
                        album = "YouTube Single",
                        duration = (res.lengthSeconds ?: 0L) * 1000,
                        data = res.videoId!!, // Treat videoId as stream key resolver later
                        isDemo = false,
                        thumbnailUrl = "https://img.youtube.com/vi/${res.videoId}/hqdefault.jpg",
                        genreTags = tagsString
                    )
                }
                _onlineSearchTracks.value = tracks
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingOnline.value = false
            }
        }
    }

    // Scan the device for external music tracks
    fun scanLocalAudioFiles() {
        val tracks = mutableListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol).toString()
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol) ?: ""

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = album,
                            duration = duration,
                            data = dataPath,
                            isDemo = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _localTracks.value = tracks
        filterLocalTracks(_searchQuery.value)
    }

    fun setExplanationFlag(required: Boolean) {
        _permissionExplanationRequired.value = required
    }

    // Playback integration
    fun selectAndPlayTrack(track: Track) {
        viewModelScope.launch {
            // Process and record genre tags to Room DB to score
            val tags = TagExtractor.extractTags(track.title, track.artist)
            tags.forEach { tag ->
                repository.incrementTagScore(tag, 1)
            }

            val isDownloaded = repository.isTrackDownloaded(track.id)
            val playbackTrack = if (isDownloaded) {
                // Fetch the locally downloaded track details (with its offloaded disk absolutePath)
                downloadedTracks.value.firstOrNull { it.id == track.id } ?: track
            } else {
                track
            }

            // Figure out the current active stream queue list so skips flow naturally
            val queue = when (_activeTab.value) {
                0 -> _discoverTracks.value // Discover queue!
                1 -> {
                    // Search queue
                    if (_searchCategoryTab.value == 0) _onlineSearchTracks.value else _localTracks.value
                }
                2 -> _playlistTracks.value // Playlist queue
                else -> downloadedTracks.value
            }

            // Play track via central controller
            PlaybackManager.selectAndPlayTrack(queue, playbackTrack)
        }
    }

    fun togglePlayPause() {
        PlaybackManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        PlaybackManager.seekTo(positionMs)
    }

    fun playNextTrack() {
        PlaybackManager.playNextTrack()
    }

    fun playPreviousTrack() {
        PlaybackManager.playPreviousTrack()
    }

    fun toggleShuffle() {
        PlaybackManager.toggleShuffle()
    }

    fun toggleRepeatOne() {
        PlaybackManager.toggleRepeatOne()
    }

    // Download tracks offline saved to Room DB
    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            if (_downloadingTrackIds.value.contains(track.id)) return@launch
            _downloadingTrackIds.update { it + track.id }
            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    InvidiousClient.getStreamUrl(track.id)
                }
                repository.downloadTrack(track, streamUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _downloadingTrackIds.update { it - track.id }
            }
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            repository.deleteTrack(trackId)
        }
    }

    // Playlists Management
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.trim().isNotEmpty()) {
                repository.createPlaylist(name.trim())
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(playlistId)
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getTracksForPlaylist(playlist.id).collect { tracks ->
                    _playlistTracks.value = tracks
                }
            }
        } else {
            _playlistTracks.value = emptyList()
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do not release player so background services can persist indefinitely!
    }
}
