package com.example.data

import android.content.Context
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.musicDao()

    // Downloaded Tracks Flow
    val downloadedTracks: Flow<List<Track>> = dao.getDownloadedTracks().map { list ->
        list.map { it.toTrack() }
    }

    suspend fun isTrackDownloaded(trackId: String): Boolean = withContext(Dispatchers.IO) {
        dao.getDownloadedTrackById(trackId) != null
    }

    // Download a track and insert metadata to Room db
    suspend fun downloadTrack(track: Track, streamUrl: String): Unit = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(streamUrl).build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed connection to media server: HTTP code ${response.code}")
        }
        
        val body = response.body ?: throw Exception("Media stream payload empty")
        
        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val localFile = File(downloadsDir, "${track.id}.mp3")
        
        body.byteStream().use { input ->
            localFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // Save to Database
        val downloadedTrack = DownloadedTrack.fromTrack(track, localFile.absolutePath)
        dao.insertDownloadedTrack(downloadedTrack)
    }

    // Delete track from physical disk and database
    suspend fun deleteTrack(trackId: String): Unit = withContext(Dispatchers.IO) {
        val downloadsDir = File(context.filesDir, "downloads")
        val localFile = File(downloadsDir, "$trackId.mp3")
        if (localFile.exists()) {
            localFile.delete()
        }
        dao.deleteDownloadedTrack(trackId)
    }

    // Playlists
    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long): Unit = withContext(Dispatchers.IO) {
        dao.deletePlaylistTracksByPlaylistId(playlistId)
        dao.deletePlaylist(playlistId)
    }

    // Playlist tracks
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return dao.getTracksForPlaylist(playlistId).map { list ->
            list.map { it.toTrack() }
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track): Unit = withContext(Dispatchers.IO) {
        val playlistTrack = PlaylistTrack.fromTrack(playlistId, track)
        dao.insertPlaylistTrack(playlistTrack)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String): Unit = withContext(Dispatchers.IO) {
        dao.deletePlaylistTrack(playlistId, trackId)
    }

    // Tag Scores
    val allTagScores: Flow<List<TagScore>> = dao.getAllTagScoresFlow()

    suspend fun incrementTagScore(tag: String, amount: Int = 1): Unit = withContext(Dispatchers.IO) {
        dao.incrementTagScore(tag, amount)
    }
}
