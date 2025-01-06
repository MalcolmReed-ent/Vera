package com.logan.vera.data.database.dao

import androidx.room.*
import com.logan.vera.data.database.entities.Book
import com.logan.vera.data.database.entities.BookCollection
import com.logan.vera.data.database.entities.BookCollectionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    // Basic book operations
    @Query("SELECT * FROM books ORDER BY lastAccessed DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBook(bookId: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    // Enhanced reading progress tracking
    @Query("""
        UPDATE books SET 
            lastReadChapterIndex = :chapterIndex, 
            lastReadPosition = :position, 
            lastAccessed = :timestamp,
            totalChapters = :totalChapters,
            readProgress = :progress,
            timeSpentReading = timeSpentReading + :timeSpent,
            lastReadDate = :timestamp
        WHERE id = :bookId
    """)
    suspend fun updateReadingProgress(
        bookId: String,
        chapterIndex: Int,
        position: Float,
        totalChapters: Int,
        progress: Float,
        timeSpent: Long = 0,
        timestamp: Long = System.currentTimeMillis()
    )

    // Search functionality
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<Book>>

    // Collection operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: BookCollection)

    @Delete
    suspend fun deleteCollection(collection: BookCollection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionEntry(entry: BookCollectionEntry)

    @Delete
    suspend fun deleteCollectionEntry(entry: BookCollectionEntry)

    @Query("SELECT * FROM book_collections ORDER BY sortOrder ASC")
    fun getAllCollections(): Flow<List<BookCollection>>

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_collection_entries bce ON b.id = bce.bookId
        WHERE bce.collectionId = :collectionId
        ORDER BY bce.sortOrder ASC, b.lastAccessed DESC
    """)
    fun getBooksInCollection(collectionId: String): Flow<List<Book>>

    @Transaction
    @Query("SELECT * FROM book_collections WHERE id = :collectionId")
    suspend fun getCollection(collectionId: String): BookCollection?

    @Query("DELETE FROM book_collection_entries WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBookFromCollection(bookId: String, collectionId: String)

    @Query("UPDATE book_collections SET coverBookId = :bookId WHERE id = :collectionId")
    suspend fun updateCollectionCover(collectionId: String, bookId: String?)
}
