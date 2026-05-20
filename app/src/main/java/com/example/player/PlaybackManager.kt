package com.example.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.model.Track
import com.example.network.InvidiousClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

object PlaybackManager {
    private var mediaPlayer: MediaPlayer? = null
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
                // Resolve stream source URL
                val resolvedDataSource = if (track.isDemo || track.isDownloaded || track.data.startsWith("/")) {
                    track.data
                } else {
                    withContext(Dispatchers.IO) {
                        InvidiousClient.getStreamUrl(track.id)
                    }
                }

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    setDataSource(appContext!!, Uri.parse(resolvedDataSource))

                    setOnPreparedListener { mp ->
                        _isLoading.value = false
                        mp.start()
                        _isPlaying.value = true
                        _durationMs.value = mp.duration.toLong()
                        startProgressTracker()
                        updateService()
                    }

                    setOnCompletionListener {
                        onTrackCompleted()
                    }

                    setOnErrorListener { _, _, _ ->
                        _isLoading.value = false
                        _isPlaying.value = false
                        true
                    }

                    prepareAsync()
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
        val player = mediaPlayer ?: return
        if (isPlaying.value) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTracker()
        }
        updateService()
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs.toInt())
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
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        } else {
            playNextTrack()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition.toLong()
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
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
    }

    private fun updateService() {
        val context = appContext ?: return
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_UPDATE_NOTIFICATION
        }
        context.startForegroundService(intent)
    }
}
