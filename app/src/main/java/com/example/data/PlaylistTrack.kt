package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Track

@Entity(tableName = "playlist_tracks")
data class PlaylistTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val data: String,
    val isDemo: Boolean,
    val isDownloaded: Boolean,
    val thumbnailUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        data = data,
        isDemo = isDemo,
        isDownloaded = isDownloaded,
        thumbnailUrl = thumbnailUrl
    )

    companion object {
        fun fromTrack(playlistId: Long, track: Track): PlaylistTrack = PlaylistTrack(
            playlistId = playlistId,
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            data = track.data,
            isDemo = track.isDemo,
            isDownloaded = track.isDownloaded,
            thumbnailUrl = track.thumbnailUrl
        )
    }
}
