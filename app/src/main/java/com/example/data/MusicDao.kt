package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // Downloaded Tracks
    @Query("SELECT * FROM downloaded_tracks ORDER BY timestamp DESC")
    fun getDownloadedTracks(): Flow<List<DownloadedTrack>>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id LIMIT 1")
    suspend fun getDownloadedTrackById(id: String): DownloadedTrack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedTrack(track: DownloadedTrack)

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteDownloadedTrack(id: String)


    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)


    // Playlist Tracks
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY timestamp ASC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<PlaylistTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrack(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracksByPlaylistId(playlistId: Long)

    // Tag Scores
    @Query("SELECT * FROM tag_scores ORDER BY score DESC")
    fun getAllTagScoresFlow(): Flow<List<TagScore>>

    @Query("SELECT * FROM tag_scores WHERE tag = :tag LIMIT 1")
    suspend fun getTagScore(tag: String): TagScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagScore(tagScore: TagScore)

    @Transaction
    suspend fun incrementTagScore(tag: String, amount: Int = 1) {
        val existing = getTagScore(tag)
        val currentScore = existing?.score ?: 0
        insertTagScore(TagScore(tag, currentScore + amount))
    }
}
