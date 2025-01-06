package com.logan.vera.data.models

data class BookModel(
    val id: String,
    val title: String,
    val author: String?,
    val description: String?,
    val coverPath: String?,
    val filePath: String,
    val lastReadChapterIndex: Int,
    val lastReadPosition: Float,
    val dateAdded: Long,
    val lastAccessed: Long,
    val totalChapters: Int,
    val readProgress: Float,
    val timeSpentReading: Long,
    val lastReadDate: Long
)
