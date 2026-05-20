package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.Track
import com.example.network.SoundCloudClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

object PlaybackManager {
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentTrackList = MutableStateFlow<List<Track>>(emptyList())
    val currentTrackList = _currentTrackList.asStateFlow()

    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack = _selectedTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    private val _isRepeatOneEnabled = MutableStateFlow(false)
    val isRepeatOneEnabled = _isRepeatOneEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun selectAndPlayTrack(trackList: List<Track>, track: Track) {
        _currentTrackList.value = trackList
        _selectedTrack.value = track

        scope.launch {
            _isLoading.value = true
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            _durationMs.value = track.duration

            releasePlayer()

            try {
                // Resolve the real streaming URL
                val resolvedDataSource = if (track.isDemo || track.isDownloaded || track.data.startsWith("/")) {
                    track.data
                } else if (track.data.startsWith("http") && !track.data.contains("transcodings")) {
                    track.data
                } else {
                    withContext(Dispatchers.IO) {
                        try {
                            SoundCloudClient.getStreamUrl(track.id, track.data)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // fallback to a demo URL if resolving fails
                            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                        }
                    }
                }

                val currentContext = appContext ?: return@launch
                exoPlayer = ExoPlayer.Builder(currentContext).build().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        true
                    )

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    _isLoading.value = false
                                    _durationMs.value = duration
                                    startProgressTracker()
                                    updateService()
                                }
                                Player.STATE_BUFFERING -> {
                                    _isLoading.value = true
                                }
                                Player.STATE_ENDED -> {
                                    onTrackCompleted()
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                            _isPlaying.value = isPlayingParam
                            if (isPlayingParam) {
                                startProgressTracker()
                            } else {
                                stopProgressTracker()
                            }
                            updateService()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            error.printStackTrace()
                            _isLoading.value = false
                            _isPlaying.value = false
                        }
                    })

                    val mediaItem = MediaItem.fromUri(Uri.parse(resolvedDataSource))
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
                updateService()
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                _isPlaying.value = false
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        try {
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNextTrack() {
        val list = _currentTrackList.value
        if (list.isEmpty()) return

        val current = _selectedTrack.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }

        val nextIndex = when {
            _isShuffleEnabled.value -> {
                if (list.size <= 1) 0 else {
                    var rand = Random.nextInt(list.size)
                    while (rand == currentIndex && list.size > 1) {
                        rand = Random.nextInt(list.size)
                    }
                    rand
                }
            }
            currentIndex == -1 -> 0
            currentIndex == list.size - 1 -> 0
            else -> currentIndex + 1
        }

        val nextTrack = list.getOrNull(nextIndex)
        if (nextTrack != null) {
            selectAndPlayTrack(list, nextTrack)
        }
    }

    fun playPreviousTrack() {
        val list = _currentTrackList.value
        if (list.isEmpty()) return

        if (_currentPositionMs.value > 4000) {
            seekTo(0)
            return
        }

        val current = _selectedTrack.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }

        val prevIndex = when {
            _isShuffleEnabled.value -> {
                if (list.size <= 1) 0 else Random.nextInt(list.size)
            }
            currentIndex <= 0 -> list.size - 1
            else -> currentIndex - 1
        }

        val prevTrack = list.getOrNull(prevIndex)
        if (prevTrack != null) {
            selectAndPlayTrack(list, prevTrack)
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.update { !it }
    }

    fun toggleRepeatOne() {
        _isRepeatOneEnabled.update { !it }
    }

    private fun onTrackCompleted() {
        if (_isRepeatOneEnabled.value) {
            seekTo(0)
            exoPlayer?.play()
        } else {
            playNextTrack()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun releasePlayer() {
        stopProgressTracker()
        exoPlayer?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        exoPlayer = null
    }

    private fun updateService() {
        val context = appContext ?: return
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_UPDATE_NOTIFICATION
        }
        context.startForegroundService(intent)
    }
}
