package com.logan.vera.data.repository

import android.util.Log
import com.logan.vera.data.database.dao.BookDao
import com.logan.vera.data.database.entities.BookCollection
import com.logan.vera.data.database.entities.BookCollectionEntry
import com.logan.vera.data.models.BookCollectionModel
import com.logan.vera.data.models.BookModel
import com.logan.vera.data.models.CollectionWithBooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BookCollectionRepository"

@Singleton
class BookCollectionRepository @Inject constructor(
    private val bookDao: BookDao
) {
    val allCollections: Flow<List<BookCollectionModel>> = bookDao.getAllCollections()
        .map { collections -> collections.map { it.toModel() } }

    suspend fun createCollection(
        name: String,
        description: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val collection = BookCollection(
                    name = name,
                    description = description
                )
                bookDao.insertCollection(collection)
                Log.d(TAG, "Created collection: ${collection.id}")
                collection.id
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create collection", e)
                throw e
            }
        }
    }

    suspend fun deleteCollection(collectionId: String) {
        withContext(Dispatchers.IO) {
            try {
                bookDao.getCollection(collectionId)?.let { collection ->
                    bookDao.deleteCollection(collection)
                    Log.d(TAG, "Deleted collection: $collectionId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete collection: $collectionId", e)
                throw e
            }
        }
    }

    suspend fun addBookToCollection(bookId: String, collectionId: String) {
        withContext(Dispatchers.IO) {
            try {
                val entry = BookCollectionEntry(
                    bookId = bookId,
                    collectionId = collectionId
                )
                bookDao.insertCollectionEntry(entry)
                Log.d(TAG, "Added book $bookId to collection $collectionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add book to collection", e)
                throw e
            }
        }
    }

    suspend fun removeBookFromCollection(bookId: String, collectionId: String) {
        withContext(Dispatchers.IO) {
            try {
                bookDao.removeBookFromCollection(bookId, collectionId)
                Log.d(TAG, "Removed book $bookId from collection $collectionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove book from collection", e)
                throw e
            }
        }
    }

    fun getCollectionWithBooks(collectionId: String): Flow<CollectionWithBooks?> {
        return combine(
            flow { 
                val collection = bookDao.getCollection(collectionId)
                emit(collection)
            },
            bookDao.getBooksInCollection(collectionId)
        ) { collection, books ->
            collection?.let {
                CollectionWithBooks(
                    collection = it.toModel(books.size),
                    books = books.map { book -> book.toBookModel() }
                )
            }
        }
    }

    suspend fun updateCollectionCover(collectionId: String, bookId: String?) {
        withContext(Dispatchers.IO) {
            try {
                bookDao.updateCollectionCover(collectionId, bookId)
                Log.d(TAG, "Updated collection cover: $collectionId with book $bookId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update collection cover", e)
                throw e
            }
        }
    }

    private fun BookCollection.toModel(bookCount: Int = 0) = BookCollectionModel(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        coverBookId = coverBookId,
        sortOrder = sortOrder,
        bookCount = bookCount
    )

    private fun com.logan.vera.data.database.entities.Book.toBookModel() = BookModel(
        id = id,
        title = title,
        author = author,
        description = description,
        coverPath = coverPath,
        filePath = filePath,
        lastReadChapterIndex = lastReadChapterIndex,
        lastReadPosition = lastReadPosition,
        dateAdded = dateAdded,
        lastAccessed = lastAccessed,
        totalChapters = totalChapters,
        readProgress = readProgress,
        timeSpentReading = timeSpentReading,
        lastReadDate = lastReadDate
    )
}
