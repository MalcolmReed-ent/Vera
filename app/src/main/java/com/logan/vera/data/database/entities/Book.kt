package com.logan.vera.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String?,
    val description: String?,
    val coverPath: String?,
    val filePath: String,
    val lastReadChapterIndex: Int = 0,
    val lastReadPosition: Float = 0f,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    // New fields for enhanced tracking
    val totalChapters: Int = 0,
    val readProgress: Float = 0f,
    val timeSpentReading: Long = 0,
    val lastReadDate: Long = System.currentTimeMillis()
)
