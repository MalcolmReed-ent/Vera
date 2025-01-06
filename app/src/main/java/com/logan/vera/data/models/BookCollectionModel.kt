package com.logan.vera.data.models

data class BookCollectionModel(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val coverBookId: String?,
    val sortOrder: Int,
    val bookCount: Int = 0
)

data class CollectionWithBooks(
    val collection: BookCollectionModel,
    val books: List<BookModel>
)
