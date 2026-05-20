package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val data: String, // Dynamic storage path or web URL stream
    val isDemo: Boolean = false,
    val isDownloaded: Boolean = false,
    val thumbnailUrl: String? = null,
    val genreTags: String = ""
)

