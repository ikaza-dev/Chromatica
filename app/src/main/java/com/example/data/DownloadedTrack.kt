package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Track

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val localFilePath: String,
    val thumbnailUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        data = localFilePath,
        isDemo = false,
        isDownloaded = true,
        thumbnailUrl = thumbnailUrl
    )

    companion object {
        fun fromTrack(track: Track, localPath: String): DownloadedTrack = DownloadedTrack(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            localFilePath = localPath,
            thumbnailUrl = track.thumbnailUrl
        )
    }
}
