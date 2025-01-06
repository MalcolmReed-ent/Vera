package com.logan.vera.data.models

data class SearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val matches: List<TextMatch>
)

data class TextMatch(
    val text: String,
    val position: Int
)
