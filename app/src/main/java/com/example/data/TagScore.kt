package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_scores")
data class TagScore(
    @PrimaryKey val tag: String,
    val score: Int
)
